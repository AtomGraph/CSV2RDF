# CSV2RDF
Streaming, transforming CSV to RDF converter

Reads CSV/TSV data as generic CSV/RDF, transforms each row using SPARQL `CONSTRUCT` or `DESCRIBE`, and streams the output triples.
The generic CSV/RDF format is based on the minimal mode of [Generating RDF from Tabular Data on the Web](https://www.w3.org/TR/2015/REC-csv2rdf-20151217/#dfn-minimal-mode).

Such transformation-based approach enables:
* building resource URIs on the fly
* fixing/remapping datatypes
* mapping different groups of values to different RDF structures

CSV2RDF differs from [tarql](https://tarql.github.io) in the way how mapping queries use graph patterns in the `WHERE` clause. tarql queries operate on a table of bindings
(provided as an implicit `VALUES` block) in which CSV column names become variable names. CSV2RDF generates an intermediary RDF graph for each CSV row (using column names as relative-URI properties)
that the `WHERE` patterns explicitly match against.

Build
-----

    mvn clean install

That should produce an executable JAR file `target/csv2rdf-1.0.0-SNAPSHOT-jar-with-dependencies.jar` in which dependency libraries will be included.

Usage
-----

The CSV data is read from `stdin`, UTF-8 encoding is expected. The resulting RDF data is written to `stdout`.

Parameters:
1. `baseURI` - the base URI for the data (also becomes the `BASE` URI of the SPARQL query)
2. `queryFile` - a text file with SPARQL 1.1 [`CONSTRUCT`](https://www.w3.org/TR/sparql11-query/#construct) or [`DESCRIBE`](https://www.w3.org/TR/sparql11-query/#describe) query string
3. `delimiter` - optional value delimiter character, by default `,`.
4. `maxCharsPerColumn` - optional max characters per column value, by default 4096

_Note that delimiters might have a [special meaning](https://www.tldp.org/LDP/abs/html/special-chars.html) in shell._ Therefore, always enclose them in single quotes, e.g. `';'` when executing CSV2RDF from shell.

If you want to retrieve the raw CSV/RDF output, use the [identity transform](https://en.wikipedia.org/wiki/Identity_transform) query `CONSTRUCT WHERE { ?s ?p ?o }`.

Example
-------

CSV data in `parking-facilities.csv`:
    
    postDistrict,roadCode,houseNumber,name,FID,long,lat,address,postcode,parkingSpace,owner,parkingType,information
    1304 København K,24,5,Adelgade 5 p_hus.0,p_hus.0,12.58228733,55.68268042,Adelgade 5,1304,92,Privat,P-Kælder,"Adelgade 5-7, Q-park."

`CONSTRUCT` query in `parking-facilities.rq`:

    PREFIX schema:     <https://schema.org/> 
    PREFIX geo:        <http://www.w3.org/2003/01/geo/wgs84_pos#> 
    PREFIX xsd:        <http://www.w3.org/2001/XMLSchema#> 
    PREFIX rdf:        <http://www.w3.org/1999/02/22-rdf-syntax-ns#>

    CONSTRUCT
    {
        ?parking a schema:ParkingFacility ;
            geo:lat ?lat ;
            geo:long ?long ;
            schema:name ?name ;
            schema:streetAddress ?address ;
            schema:postalCode ?postcode ;
            schema:maximumAttendeeCapacity ?spaces ;
            schema:additionalProperty ?parkingType ;
            schema:comment ?information ;
            schema:identifier ?id .
    }
    WHERE
    {
        ?parkingRow <#FID> ?id ;
            <#name> ?name ;
            <#address> ?address ;
            <#lat> ?lat_string ;
            <#postcode> ?postcode ;
            <#parkingSpace> ?spaces_string ;
            <#parkingType> ?parkingType ;
            <#information> ?information ;
            <#long> ?long_string . 

        BIND(URI(CONCAT(STR(<>), ?id)) AS ?parking) # building URI from base URI and ID
        BIND(xsd:integer(?spaces_string) AS ?spaces)
        BIND(xsd:float(?lat_string) AS ?lat)
        BIND(xsd:float(?long_string) AS ?long)
    }

Execution from shell:

    cat parking-facilities.csv | java -jar csv2rdf-1.0.0-SNAPSHOT-jar-with-dependencies.jar https://localhost/ parking-facilities.rq > parking-facilities.ttl

Output in `parking-facilities.ttl`:

    <https://localhost/p_hus.0> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://schema.org/ParkingFacility> .
    <https://localhost/p_hus.0> <http://www.w3.org/2003/01/geo/wgs84_pos#long> "12.58228733"^^<http://www.w3.org/2001/XMLSchema#float> .
    <https://localhost/p_hus.0> <https://schema.org/identifier> "p_hus.0" .
    <https://localhost/p_hus.0> <https://schema.org/additionalProperty> "P-Kælder" .
    <https://localhost/p_hus.0> <https://schema.org/comment> "Adelgade 5-7, Q-park." .
    <https://localhost/p_hus.0> <https://schema.org/postalCode> "1304" .
    <https://localhost/p_hus.0> <http://www.w3.org/2003/01/geo/wgs84_pos#lat> "55.68268042"^^<http://www.w3.org/2001/XMLSchema#float> .
    <https://localhost/p_hus.0> <https://schema.org/streetAddress> "Adelgade 5" .
    <https://localhost/p_hus.0> <https://schema.org/name> "Adelgade 5 p_hus.0" .
    <https://localhost/p_hus.0> <https://schema.org/maximumAttendeeCapacity> "92"^^<http://www.w3.org/2001/XMLSchema#integer> .

Query examples
--------------

More mapping query examples can be found under [LinkedDataHub](../../../LinkedDataHub) [`city-graph`](../../../LinkedDataHub/tree/master/apps/demo/city-graph/queries) demo app.

Dependencies
------------

* [Apache Jena](https://jena.apache.org/)
* [uniVocity-parsers](https://www.univocity.com/pages/univocity_parsers_tutorial)
