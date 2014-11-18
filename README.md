
===========
refine_viaf
===========

refine_viaf implements an OpenRefine reconciliation service that
queries the Virtual International Authority File (VIAF) public
API.

The purpose of this module is to enable anyone to host their service.

Features
--------

* Support for the types of names provided by VIAF: Corporate Names,
  Geographic Names, Personal Names, Works, Expressions

* Support for making requests to the VIAF API using a thread pool for
  faster performance.

* Works with Django out of the box, but can be used with any web
  framework with a bit of glue code.

  
Installation
------------

Installation via pip is coming soon. In the meantime:

1. Clone this repository

    git clone TODO

2. Activate the virtualenv you're using, if applicable.

2. Run this command

    python setup.py install

Django Setup
------------

This package comes with a ready-to-use Django view that you can use in
your new or existing project.

1. Add this app to your Django project's settings.py file, and add
   some configuration options:

```
    INSTALLED_APPS = (
        ...
        'refine_viaf',
    )

    # service name to display in OpenRefine
    REFINE_VIAF_SERVICE_NAME = "VIAF (via myserver)"
    # list of preferred sources to use for displaying a name
    REFINE_VIAF_PREFERRED_SOURCES = ["LC"]
    # enable threading
    REFINE_VIAF_THREADING = True
    # number of threads to use: keep in mind VIAF enforces a limit of
    # 6 simultaneous requests
    REFINE_VIAF_THREADPOOL_SIZE = 3
```
    
2. Add an entry to your urls.py file:

    ```
    url(r'^reconcile/viaf', 'refine_viaf.views.reconcile'),
    ```

3. Your reconciliation service is now ready to use.

Configuring OpenRefine
----------------------

1. In OpenRefine, select "Reconcile" and "Start Reconciling..." in the
   pull-down menu beside a column whose values you want to reconcile.

2. Click "Add Standard Service..."

3. Type in http://localhost:8000/reconcile/viaf

Other Web Frameworks
--------------------

The reconcile_viaf module is web agnostic: with a bit of glue code, it
can be used with any web framework. See the reconcile_viaf.views
module for how it works with Django, and copy and adapt the code to
your purposes.

There is a lot of debugging output you can direct to a file or stdout
by editing your logging confirguation settings.py file:

```
    LOGGING = {
        'version': 1,
        'disable_existing_loggers': True,
        'formatters': {
            'verbose': {
                'format': '%(levelname)s %(asctime)s %(module)s %(process)d %(thread)d %(message)s'
            },
            'simple': {
                'format': '%(levelname)s %(message)s'
            },
        },
        'handlers': {
            'console': {
                'level': 'DEBUG',
                'class': 'logging.StreamHandler',
                'formatter': 'verbose'
            },
        },
        'loggers': {
            'refine_viaf': {
                'handlers': ['console'],
                'level': 'DEBUG',
            }
        }
    }
```

Resources
---------

Specification for the Reconciliation Service API:

https://github.com/OpenRefine/OpenRefine/wiki/Reconciliation-Service-Api

This code drew inspiration from these other projects:

https://github.com/rdmpage/phyloinformatics
https://github.com/mikejs/reconcile-demo/

License
-------

This code is distributed under a BSD license. See the file LICENSE for
details.
