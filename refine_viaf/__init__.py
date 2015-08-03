
"""
An OpenRefine reconciliation service for VIAF.

This module is web framework agnostic, so this code needs to be hooked
up to something that can handle the request/response transport, so it
can operate as an actual web service.

"""

import collections
import json
import logging
import time
import traceback
import urllib

from bs4 import BeautifulSoup
import requests


logger = logging.getLogger(__name__)

NameType = collections.namedtuple('NameType', ['id', 'display_name', 'viaf_type_code', 'cql_string' ])

# ids are from freebase identifier ns
NAME_TYPES = [
    NameType("/people/person", "Person", "Personal", u"local.personalNames all \"%s\""),
    NameType("/organization/organization", "Corporate Name", "Corporate", u"local.corporateNames all \"%s\""),
    NameType("/location/location", "Geographic Name", "Geographic", u"local.geographicNames all \"%s\""),
    # can't find better freebase ids for these two
    NameType("/book/book", "Work", "UniformTitleWork", u"local.uniformTitleWorks all \"%s\""),
    NameType("/book/book edition", "Expression", "UniformTitleExpression", u"uniformTitleExpressions all \"%s\""),
]

METADATA = {
    # 'name' is added via config parameter
    "identifierSpace": "http://rdf.freebase.com/ns/user/hangy/viaf",
    "schemaSpace": "http://rdf.freebase.com/ns/type.object.id",
    "view": {
        "url": "http://viaf.org/viaf/{{id}}"
    },
    "defaultTypes": [{"id": nametype.id, "name": nametype.display_name} for nametype in NAME_TYPES],
}


def get_name_type(fieldname, value):
    """ finds NameType record by value in fieldname """
    return [nt for nt in NAME_TYPES if getattr(nt, fieldname) == value][0]


def create_config():
    """ returns a new config dict with default setting values """
    return {
        "REFINE_VIAF_SERVICE_NAME": "VIAF Reconciliation Service",
        "REFINE_VIAF_PREFERRED_SOURCES": ["LC"],
        "REFINE_VIAF_THREADING": False,
        # NOTE: VIAF seems to have a limit of 6 simultaneous
        # requests. To be conservative, we default to 3.
        "REFINE_VIAF_THREADPOOL_SIZE": 3,
    }


def get_name(record, ns, preferred_sources):
    """
    returns name from one of the preferred sources
    """
    name = ""

    sources_to_names = {}

    # each record under 'mainheadings' contains a name and the
    # multiple sources that use that name
    main_headings = record.find(ns + "mainheadings")
    for heading_record in main_headings.find_all(ns + "data"):
        name = heading_record.find(ns + "text").contents[0]
        for source in heading_record.find(ns + "sources").find_all(ns + "s"):
            source = source.contents[0]
            sources_to_names[source] = name

    for preferred_source in preferred_sources:
        if sources_to_names.get(preferred_source):
            name = sources_to_names[preferred_source]
            break

    if not name and len(sources_to_names) > 0:
        # grab one arbitrarily, I guess
        name = sources_to_names[sources_to_names.keys()[0]]

    return name


def parse_response_xml(xmlstr, query_type, search_term, preferred_sources):
    """
    parses the XML response from VIAF, and returns a list of match
    dictionaries, to be serialized to JSON
    """

    matches = []

    soup = BeautifulSoup(xmlstr)

    # numbered ns prefix
    ns_num = 2

    for record in soup.find_all("record"):

        ns = "ns" + str(ns_num) + ":"

        viaf_id = record.find(ns + "viafid").contents[0]

        name = get_name(record, ns, preferred_sources)

        # if no query type passed into this fn, get it from the record
        if query_type:
            nametype = get_name_type("id", query_type)
        else:
            nametype = get_name_type("viaf_type_code", record.find(ns + "nametype").contents[0])

        result = {
            'id' : viaf_id,
            'name': name,
            'type': [
                {
                    "id": nametype.id,
                    "name": nametype.display_name,
                },
            ],
            'score': 1,
            'match': search_term == name,
        }

        logger.debug(u"parsed match=%r" % (result,))

        matches.append(result)

        ns_num += 1

    # helps reclaim memory
    soup.decompose()

    return matches


def search(query, preferred_sources):
    """
    Does a search, using parameters specified in passed-in 'query'
    dict, which looks like one of the following:

    1)
    {
        "query":"Robert Oppenheimer",
        "limit":3
    }

    This type of query happens when OpenRefine does an initial "loose"
    query over some values to determine what type(s) are applicable to
    the column, and when the user chooses "Reconcile against no
    particular type".

    2)
    {
        "query": "Geoffrey Chaucer",
        "type": "/people/person",
        "type_strict": "should"
    }

    This is the typical case for reconciliing a name in a particular
    row.
    """
    logger.debug(u"doing search on query=%r" % (query,))

    query_text = query['query']
    query_type = query.get('type')

    if query_type is None:
        query_param = u"local.mainHeadingEl all \"%s\"" % (query_text,)
    else:
        query_param = get_name_type("id", query_type).cql_string % (query_text,)

    # call to encode() is crucial, or quote_plus will barf
    query_param_quoted = urllib.quote_plus(query_param.encode('utf-8'))

    # note that both XML and JSON data from VIAF often don't validate
    # because of unescaped chars, esp ampersands in XML and quotation
    # marks in strings in JSON. though XML is more heavyweight to
    # parse, BeautifulSoup can handle invalid XML, and I'm not sure
    # how to handle invalid JSON

    # setting sortKeys=holdingscount gives more reasonable results;
    # default maximumRecords to 3, since that's the max that
    # openrefine will show
    url = u"http://www.viaf.org/viaf/search?query=%s&sortKeys=holdingscount&maximumRecords=%s&httpAccept=application/xml" % \
          (query_param_quoted, query.get('limit', 3))

    logger.debug(u"making request for %r" % (url,))
    response = requests.get(url)
    if response.status_code == 200:
        return parse_response_xml(response.text, query_type, query_text, preferred_sources)
    else:
        logger.error(u"VIAF API Error: query=%r, HTTP code=%r, response=%r" % (query, response.status_code, response.text))

    return []


def search_worker(task):
    """
    wrapper fn for search(), to be used with map
    """
    key, query, preferred_sources = task

    result = {}
    try:
        result = search(query, preferred_sources)
    except Exception, e:
        tb = traceback.format_exc()
        logger.error(u"Error occurred in search_worker: %r" % (tb,))

    return { key: {"result": result }}



class Reconcile:
    """
    Framework-agnostic reconciliation code.
    """

    def __init__(self, request_params, config):
        """
        request_params is a dict-like object containing URL parameters.
        config is a dict-like object providing certain keys
        """
        self.request_params = request_params
        self.config = config


    def reconcile(self):
        """
        Performs the reconciliation; returns a JSON string response.
        """

        logger.debug("config=%r" % (self.config,))

        start_time = time.time()
        num_queries = 0

        json_response = None

        preferred_sources = self.config["REFINE_VIAF_PREFERRED_SOURCES"]

        query = self.request_params.get('query')
        queries = self.request_params.get('queries')

        if query:

            logger.debug(u"query=%r" % (query,))

            num_queries = 1

            if query.startswith("{"):
                query = json.loads(query)
            results = search(query, preferred_sources)
            json_response = self.jsonpify(self.request_params, {"result": results})

        elif queries:

            logger.debug(u"queries=%r" % (queries,))
            queries = json.loads(queries)
            results = {}

            threaded = self.config['REFINE_VIAF_THREADING']

            map_fn = map
            if threaded:
                from multiprocessing.dummy import Pool
                pool = Pool(self.config['REFINE_VIAF_THREADPOOL_SIZE'])
                map_fn = pool.map

            worker_results = map_fn(search_worker, [(k,v, preferred_sources) for k,v in queries.items()])

            if threaded:
                pool.close()
                pool.join()

            for worker_result in worker_results:
                num_queries += 1
                results.update(worker_result)

            json_response = self.jsonpify(self.request_params, results)

        else:
            metadata = { 'name' : self.config['REFINE_VIAF_SERVICE_NAME']}
            metadata.update(METADATA)
            json_response = self.jsonpify(self.request_params, metadata)

        if num_queries > 0:
            logger.info("%d queries took %fs" % (num_queries, time.time() - start_time))

        return json_response


    def jsonpify(self, request_params, obj):
        """
        Like jsonify but wraps result in a JSONP callback if a 'callback'
        query param is supplied.
        """
        json_data = None
        callback = request_params.get('callback')
        if callback:
            json_data = u"%s(%s)" % (callback, json.dumps(obj))
        else:
            json_data = json.dumps(obj)

        return json_data
