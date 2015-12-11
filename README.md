
refine_viaf
===========

refine_viaf implements a [VIAF](http://viaf.org) (Virtual
International Authority File) Reconciliation Service for
[OpenRefine](http://openrefine.org). This is used for resolving many
types of names (ie. persons, organizations, geographic regions) to
standard IDs representing those entities.

This project was rewritten in Java in Dec 2015. The old version
written in Python is no longer being maintained, but can be found in
the [python-deprecated branch](https://github.com/codeforkjeff/refine_viaf/tree/python-deprecated).

Features
--------

* Support for the types of names provided by VIAF: Corporate Names,
  Geographic Names, Personal Names, Works, Expressions

* Good performance (uses threads; stable memory usage)

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

Download the latest release and unzip it. (TODO)

Run this command:

```
java -jar refine_viaf.jar
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

You can change the service name that appears in OpenRefine by editing
the `refine_viaf.properties` file.

Resources
---------

Specification for the Reconciliation Service API:

https://github.com/OpenRefine/OpenRefine/wiki/Reconciliation-Service-Api

This code drew inspiration from these other projects:

* https://github.com/rdmpage/phyloinformatics
* https://github.com/mikejs/reconcile-demo/

License
-------

This code is distributed under a GNU General Public License. See the
file LICENSE for details.
