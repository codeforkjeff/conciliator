
conciliator
===========

conciliator is a growing collection of
[OpenRefine](http://openrefine.org) reconciliation services, as well
as a Java framework for creating them. A reconciliation service tries
to match variant text (usually names of things) to standard IDs for
the entity represented by that text.

This project supercedes [refine_viaf](https://github.com/codeforkjeff/refine_viaf).

Public Server
-------------

If your needs are low and you can't or don't want to run this software
yourself, you can use the public server at
<http://refine.codefork.com/>. Visit that address for more
instructions.

General Features
----------------

* Out of the box support for the following data sources:

  - [VIAF](http://viaf.org) - Virtual International Authority File
  - [ORCID](http://orcid.org) - digital identifiers for researchers
  - more to come (if you can contribute, please submit pull requests!)

* Good performance (uses threads; stable memory usage; caches results)

* Super easy to run (works on Linux, Mac, Windows)

VIAF Data Source Features
-------------------------

* Support for the following types of names provided by VIAF: Corporate
  Names, Geographic Names, Personal Names, Works, Expressions

* "Proxy mode" to retrieve IDs used by source institutions, instead of
  VIAF IDs. (NOTE: hyperlinks to source record pages in OpenRefine are
  supported for BNE, DNB, ICCU, JPG, LC, NDL, SELIBR, SUDOC, and
  WKP. Links are BROKEN for BNC, BNF, DBC, and NUKAT. For all other
  sources, the links will take you to the VIAF page.)

ORCID Data Source Features
--------------------------

* It works!

Running Conciliator on Your Own Computer
----------------------------------------

Install Java 1.7 or greater if you don't already have it.

Download the .jar file for the
[latest release](https://github.com/codeforkjeff/conciliator/releases). Alternatively,
you can download the source code tarball or clone this repository, and
build the .jar file using maven.

Run this command:

```
java -jar conciliator-1.0.0.jar
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

4. Follow the instructions on the dialog box to start reconciling
   names.

Creating Your Own Data Source
-----------------------------

1. Clone this repository to get the source code.

2. Create a class for your data source that extends
   `com.codefork.refine.datasource.WebServiceDataSource`. Implement
   the abstract methods as required. Write a test or two if you like.

3. Create a class for the service metadata response, extending
   `com.codefork.refine.resources.ServiceMetaDataResponse`

4. Register your new data source class in the `conciliator.properties`
   file by adding the following lines:

   ```
   datasource.new_source=com.company.MyDataSource
   datasource.new_source.name=My DataSource
   ```

5. Build a new .jar by running `mvn package`. Run the .jar file as in
   the instructions above, and you should be able to access the service
   for your new data source at:

   ```
   http://localhost:8080/reconcile/new_source
   ```

Advanced Usage
--------------

To build from the source code, install maven and type:

```
mvn package
```

If you want to host this software on a server for long-term usage or
if you want to enable logging for debugging purposes, take a look at
`run.sh` for some helpful options.

You can change run-time options by editing the
`conciliator.properties` file.

TODO
----

A few aspects of the Reconciliation Service API aren't implemented by
this framework yet.

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
[this page](http://codefork.com/blog/index.php/2016/10/24/announcing-conciliator/). Hearing
from users really motivates me to continue improving this project.

License
-------

This code is distributed under a GNU General Public License. See the
file LICENSE for details.
