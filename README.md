
# conciliator

conciliator is a growing collection of
[OpenRefine](http://openrefine.org) reconciliation services, as well
as a Java framework for creating them. A reconciliation service tries
to match variant text (usually names of things) to standard IDs for
the entity represented by that text.

This project supercedes [refine_viaf](https://github.com/codeforkjeff/refine_viaf).

<!-- markdown-toc start - Don't edit this section. Run M-x markdown-toc-refresh-toc -->

**Table of Contents**

- [Public Server](#public-server)
- [General Features](#general-features)
- [Data Source Features](#data-source-features)
    - [VIAF](#viaf)
    - [ORCID](#orcid)
    - [Open Library](#open-library)
    - [Solr](#solr)
- [Running Conciliator on Your Own Computer](#running-conciliator-on-your-own-computer)
- [Configuring OpenRefine](#configuring-openrefine)
- [Creating Your Own Data Source](#creating-your-own-data-source)
- [Advanced Usage](#advanced-usage)
- [TODO](#todo)
- [Resources](#resources)
- [Do you use this thing??](#do-you-use-this-thing)
- [License](#license)

<!-- markdown-toc end -->

## Public Server

If your needs are low and you can't or don't want to run this software
yourself, you can use the public server at
<http://refine.codefork.com/>. Visit that address for more
instructions.

## General Features

* Out of the box support for the following data sources:

  - [VIAF](http://viaf.org) - Virtual International Authority File
  - [ORCID](http://orcid.org) - digital identifiers for researchers
  - [Open Library](http://openlibrary.org) - an open, editable library catalog
  - Any [Apache Solr](https://lucene.apache.org/solr/) collection
  - more to come (if you can contribute, please submit pull requests!)

* Good performance (uses threads; stable memory usage; caches results)

* Super easy to run (works on Linux, Mac, Windows)

## Data Source Features

### VIAF

* Support for the following types of names provided by VIAF: Corporate
  Names, Geographic Names, Personal Names, Works, Expressions

* "Proxy mode" to retrieve IDs used by source institutions, instead of
  VIAF IDs. (NOTE: hyperlinks to source record pages in OpenRefine are
  supported for BNE, DNB, ICCU, JPG, LC, NDL, SELIBR, SUDOC, and
  WKP. Links are BROKEN for BNC, BNF, DBC, and NUKAT. For all other
  sources, the links will take you to the VIAF page.)

### ORCID

* Uses the ORCID v2.1 API. The detailed search results of the v1.2 API
  are no longer supported, so n+1 requests are made to fetch name
  details, which is gross, but it's the best we can do. Heavy use may
  cause the ORCID API to start returning rate-limiting errors.

* Properties are supported as a way to do fielded searches using Solr syntax.
  For lists of valid field names to use in the "As Property" box, see the section titled
  "Search for specific elements by field" on [this page](https://members.orcid.org/api/tutorial/search-orcid-registry),
  and the list of identifier fields on the
  [Supported Work Identifiers](https://members.orcid.org/api/resources/supported-work-identifiers) page.

  For example, if you have a column containing Scopus EIDs, you can select the "Include?" checkbox
  for it and enter "eid" in the "As Property" box on the reconciliation screen.

* By default, queries are keyword searches on the entire ORCID bios,
  which can return odd results sometimes. The "smartnames" mode (see
  the instructions below) splits up names and searches on the
  given-names and family-name fields specifically; if there are no
  results, it falls back to a keyword search.

### Open Library

* Open Library has rate limits on its API, so requests are not run in a
  threadpool. Expect it to be slow.

* Support for including additional columns (useful for specifying
  author(s), for example, to help narrow down searches for common book
  titles). If no results are found, the code tries again with only the
  original column.

### Solr

* Any Apache Solr collection can be used as a data source. See the
  sample commented-out lines in the `conciliator.properties` file for
  more details.

## Running Conciliator on Your Own Computer

Using Docker is the easiest and preferred way to build and run the application:

```
docker build -t conciliator .
./run_docker.sh
```

An alternative way to run conciliator using docker is available
[here](https://hub.docker.com/r/tobinski/docker-codefork-conciliator/).

If you don't have Docker, you can run the application as follows:

Install Java 11 if you don't already have it.

Download the .jar file for the
[latest release](https://github.com/codeforkjeff/conciliator/releases). Alternatively,
you can download the source code tarball or clone this repository, and
build the .jar file using maven.

Run this command:

```
# replace VERSION with the release you downloaded
java -jar conciliator-VERSION.jar
```

That's it! You should see some messages as the application starts
up. Now you're ready to configure OpenRefine to use the service. When
you're done with it, hit Ctrl-C to quit the application.

If a file named `conciliator.properties` exists in the current
directory, conciliator will use the options found in it. See the
sample file in this repository.

By default, conciliator will run on port 8080, which is used in the
example URLs below. To use a different port, set the `server.port`
property as follows when running the program:

```
java -Dserver.port=7000 -jar conciliator-VERSION.jar
```

## Configuring OpenRefine

1. In OpenRefine, chose a column of names you want to reconcile, and
   select "Reconcile" and "Start Reconciling..." in the column
   pull-down menu.

2. Click "Add Standard Service..."

3. Enter a URL based on the data source you wish to use. 

    To reconcile against names from any VIAF source, type in:

    ```
    http://localhost:8080/reconcile/viaf
    ```

    To reconcile against a specific VIAF source, append its code to
    the end of the path. For example, to search only names from the
    Bibliothèque nationale de France, type in:
    
    ```
    http://localhost:8080/reconcile/viaf/BNF
    ```

    To retrieve the IDs used by source institutions, rather than VIAF
    IDs, use "proxy mode." For example, to search only names from the
    Library of Congress and retrieve their IDs, type in:
    
    ```
    http://localhost:8080/reconcile/viafproxy/LC
    ```

    To use ORCID:

    ```
    http://localhost:8080/reconcile/orcid
    ```

    To use ORCID with "smartnames" mode when reconciliing names:

    ```
    http://localhost:8080/reconcile/orcid/smartnames
    ```

    To use Open Library: (On the reconciliation screen, under the
    "Also use relevant details from other columns" panel, you can
    check the "Include?" box for columns to include in the query. Give
    them any name in the "As Property" box. If no results are found
    with these column values added to the query, the service will try
    again with only the original selected column.)

    ```
    http://localhost:8080/reconcile/openlibrary
    ```

4. Follow the instructions on the dialog box to start reconciling
   names.

## Creating Your Own Data Source

1. Clone this repository to get the source code. The code you create
   in the next steps should live under a new
   `com.codefork.refine.NEW_SOURCE` package so that Spring's
   auto-scanning picks it up.

2. Create a class for your data source that extends `DataSource` for
   very bare-bones functionality, or `WebServiceDataSource` if you are
   making requests to another web service. See the other data sources
   for some template code. Implement the abstract methods as required.
   
3. Create a controller that autowires your new DataSource and hooks up
   a unique path, e.g. `/reconcile/new_source`. See VIAFController for
   an example.

4. Write a test or two if you like.

5. Set some default properties in `Config` if your data source has any
   settings you want to be configurable.

6. Build a new .jar by running `mvn clean package`. Run the .jar file
   as in the instructions above, and you should be able to access the
   service for your new data source at:

   ```
   http://localhost:8080/reconcile/new_source
   ```

## Advanced Usage

To build from the source code, install maven and type:

```
mvn package
```

If you want to host this software on a server for long-term usage or
if you want to enable logging for debugging purposes, take a look at
`run.sh` for some helpful options.

You can change run-time options by editing the
`conciliator.properties` file.

To see usage/error statistics for the service, go to `http://localhost:8080/stats`  

## TODO

- A few aspects of the Reconciliation Service API aren't implemented by
  this framework yet.

## Resources

Specification for the Reconciliation Service API:

https://reconciliation-api.github.io/specs/latest/

This code drew inspiration from these other projects:

* https://github.com/rdmpage/phyloinformatics
* https://github.com/mikejs/reconcile-demo/

## Do you use this thing??

Apparently, you do. Here's a bibliography of things that reference conciliator:

https://github.com/codeforkjeff/conciliator/wiki

If you use conciliator, please take a few seconds to leave a comment
on
[this page](http://codefork.com/blog/index.php/2016/10/24/announcing-conciliator/). Hearing
from users really motivates me to continue improving this project.

## License

This code is distributed under a GNU General Public License. See the
file LICENSE for details.
