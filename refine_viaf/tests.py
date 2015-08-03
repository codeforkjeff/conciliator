# coding=utf-8

import json

from django.test.client import RequestFactory
import json
import requests
from unittest import TestCase

import refine_viaf
import refine_viaf.views

import json

class ReconcileTestCase(TestCase):

    def test_basic(self):
        config = refine_viaf.create_config()

        request = {
            'queries': '{"q0":{"query":"Nate Silver","limit":3},"q1":{"query":"艾, 未未","limit":3},"q2":{"query":"George Soros","limit":3}}' }
        r = refine_viaf.Reconcile(request, config)
        result = json.loads(r.reconcile())

        self.assertEqual(len(result.keys()), 3, "expected 3 search results")
        self.assertEqual(len(result['q0']['result']), 3, "expected 3 matches for Nate Silver")
        self.assertEqual(len(result['q1']['result']), 1, "expected one match for Ai Wei Wei")
        self.assertFalse(result['q1']['result'][0]['match'], "expected non-exact match for Ai Wei Wei")
        self.assertEqual(len(result['q2']['result']), 3, "expected 3 matches for George Soros")

    def test_preferred_source_ndl(self):
        config = refine_viaf.create_config()
        config['REFINE_VIAF_PREFERRED_SOURCES'] = ["NDL"]

        request = {
            'queries': '{"q0":{"query":"Nate Silver","limit":3},"q1":{"query":"艾, 未未","limit":3},"q2":{"query":"George Soros","limit":3}}' }
        r = refine_viaf.Reconcile(request, config)
        result = json.loads(r.reconcile())

        self.assertEqual(len(result.keys()), 3, "expected 3 search results")
        self.assertEqual(len(result['q0']['result']), 3, "expected 3 matches for Nate Silver")
        self.assertEqual(len(result['q1']['result']), 1, "expected one match for Ai Wei Wei")
        self.assertTrue(result['q1']['result'][0]['match'], "expected exact match for Ai Wei Wei")
        self.assertEqual(len(result['q2']['result']), 3, "expected 3 matches for George Soros")


class FunctionsTest(TestCase):
    """
    Unit tests for functions
    """

    def test_search_for_guessing_column_type(self):
        # this is the type of query OpenRefine makes to guess types of names

        query = {"query":"Robert Oppenheimer", "limit":3}

        matches = refine_viaf.search(query, ["LC"])

        self.assertTrue(len(matches) > 0)


    def test_search(self):

        query = {"query":"George Washington","type":"/people/person","type_strict":"should"}

        matches = refine_viaf.search(query, ["LC"])

        self.assertTrue(len(matches) > 0)


    def test_search_unicode(self):

        # pass in actual unicode char here
        query = {"query": u"Dadisho\u2019 Qatraya","type":"/people/person","type_strict":"should"}

        matches = refine_viaf.search(query, ["LC"])

        self.assertTrue(len(matches) > 0)


class ViewTest(TestCase):
    """
    Unit tests for view
    """

    def setUp(self):
        self.factory = RequestFactory()


    def test_search(self):

        postdata = {"queries":"""{"q0":{"query":"Spain","type":"/location/location","type_strict":"should"},"q1":{"query":"Scotland, Edinburgh?","type":"/location/location","type_strict":"should"},"q2":{"query":"Italy, northeastern? Ferrara?","type":"/location/location","type_strict":"should"},"q3":{"query":"Germany, Freiburg","type":"/location/location","type_strict":"should"},"q4":{"query":"Netherlands, Utrecht","type":"/location/location","type_strict":"should"},"q5":{"query":"Netherlands, northern","type":"/location/location","type_strict":"should"},"q6":{"query":"Italy, Sicily?","type":"/location/location","type_strict":"should"}}"""}

        request = self.factory.post('/reconcile/viaf', postdata)

        response = refine_viaf.views.reconcile(request)

        self.assertEqual(response.status_code, 200)

        # make sure json response is valid
        json.loads(response.content)


    def test_search_unicode(self):

        # pass in str literal '\u2019' as part of post data
        postdata = {"queries":"""{"q0":{"query":"Dadisho\u2019 Qatraya","type":"/people/person","type_strict":"should"}}"""}

        request = self.factory.post('/reconcile/viaf', postdata)

        response = refine_viaf.views.reconcile(request)

        self.assertEqual(response.status_code, 200)

        # make sure json response is valid
        json.loads(response.content)
