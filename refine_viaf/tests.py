# coding=utf-8

import json

from django.test import TestCase

import refine_viaf

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
