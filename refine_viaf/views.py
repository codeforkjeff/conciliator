
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
def reconcile(request):
    """ Django view """

    r = refine_viaf.Reconcile(request.REQUEST, config)
    json_data = r.reconcile()
    return HttpResponse(json_data, content_type="application/json")
