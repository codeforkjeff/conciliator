"""
Django view for reconciliation service.
"""

import django.conf
from django.http import HttpResponse
from django.views.decorators.csrf import csrf_exempt

import refine_viaf

config = refine_viaf.create_config()

# override using django config
for setting in config.keys():
    if hasattr(django.conf.settings, setting):
        config[setting] = getattr(django.conf.settings, setting)


@csrf_exempt
def reconcile(request, preferred_sources=None):
    """ Django view """

    local_config = config
    if preferred_sources:
        local_config = dict(local_config)
        local_config['REFINE_VIAF_PREFERRED_SOURCES'] = map(lambda item: item.upper(), preferred_sources.split(","))

    r = refine_viaf.Reconcile(request.REQUEST, local_config)
    json_data = r.reconcile()
    return HttpResponse(json_data, content_type="application/json")
