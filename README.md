
refine_viaf
===========

refine_viaf implements a [VIAF](http://viaf.org) (Virtual
International Authority File) Reconciliation Service for
[OpenRefine](http://openrefine.org). This is used for resolving many
types of names (ie. persons, organizations, geographic regions) to
standard IDs representing those entities.

This project was rewritten in Java in Dec 2015. For more info, see
[this page](http://codefork.com/blog/index.php/2015/12/10/a-major-update-to-refine-viaf/). The
old version written in Python is no longer being maintained, but can
be found in the
[python-deprecated branch](https://github.com/codeforkjeff/refine_viaf/tree/python-deprecated).

Features
--------

* Support for the following types of names provided by VIAF: Corporate
  Names, Geographic Names, Personal Names, Works, Expressions

* "Proxy mode" to reconcile names to IDs used by source
  institutions, instead of VIAF IDs. (Hyperlinks to source record
  pages in OpenRefine are supported for BNE, BNF, DNB, JPG, LC, NDL,
  SELIBR, SUDOC, and WKP; for other sources, the links will take you
  to the VIAF page.)

* Good performance (uses threads; stable memory usage; caches results)

* Super easy to run

Public Server
-------------

If your needs are low and you can't or don't want to run this software
yourself, you can use the public server at
<http://refine.codefork.com/>. Visit that address for more
instructions.

Running the Service on Your Computer
------------------------------------

Install Java 1.7 or greater if you don't already have it.

Download the .jar file for the
[latest release](https://github.com/codeforkjeff/refine_viaf/releases). Alternatively,
you can download the source code tarball or clone this repository, and
build the .jar file using maven.

Run this command:

```
java -jar refine_viaf-1.1.jar
```

That's it! You should see some messages as the application starts
up. Now you're ready to configure OpenRefine to use the service. When
you're done with it, hit Ctrl-C to quit the application.

Configuring OpenRefine
----------------------

1. In OpenRefine, chose a column of names you want to reconcile, and
   select "Reconcile" and "Start Reconciling..." in the column
   pull-down menu.

2. Click "Add Standard Service..."

3. To reconcile against names from any VIAF source, type in:

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
    IDs, use "proxy mode." For example, to get Library of Congress
    IDs for names, type in (note that, in this mode, the source ID at
    the end is required):
    
    ```
    http://localhost:8080/reconcile/viafproxy/LC
    ```

4. Follow the instructions on the dialog box to start reconciling
   names.

Advanced Usage
--------------

To build from the source code, install maven and type:

```
mvn package
```

If you want to host this software on a server for long-term usage or
if you want to enable logging for debugging purposes, take a look at
`run.sh` for some helpful options.

You can change several run-time options (including the service name
that appears in OpenRefine and turning caching on/off) by editing the
`refine_viaf.properties` file.

Resources
---------

Specification for the Reconciliation Service API:

https://github.com/OpenRefine/OpenRefine/wiki/Reconciliation-Service-Api

This code drew inspiration from these other projects:

* https://github.com/rdmpage/phyloinformatics
* https://github.com/mikejs/reconcile-demo/

Do you use this thing??
-----------------------

If so, please take a few seconds to leave a comment on
[this page](http://codefork.com/blog/index.php/2015/12/10/a-major-update-to-refine-viaf/). Hearing
from users really motivates me to continue improving this project.

License
-------

This code is distributed under a GNU General Public License. See the
file LICENSE for details.
