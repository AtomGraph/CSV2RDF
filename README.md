# CSV2RDF
Streaming, transforming CSV to RDF converter

Reads CSV/TSV data as generic CSV/RDF, transforms each row using SPARQL `CONSTRUCT` or `DESCRIBE`, and streams the output triples.
The generic CSV/RDF format is based on minimal mode of [Generating RDF from Tabular Data on the Web](https://www.w3.org/TR/2015/REC-csv2rdf-20151217/#dfn-minimal-mode)

Such transformation-based approach enables:
* building resource URIs on the fly
* fixing/remapping datatypes
* mapping different groups of values to different RDF structures

Build
-----

    mvn clean install

That should produce an executable JAR file `target/csv2rdf-1.0.0-SNAPSHOT-jar-with-dependencies.jar` in which dependency libraries will be included.

Usage
-----

The CSV data is read from `stdin`. The resulting RDF data is written to `stdout`.

Parameters:
1. `baseURI` - the base URI for the data (also set on the query)
2. `queryFile` - a text file with SPARQL 1.1 [`CONSTRUCT`](https://www.w3.org/TR/sparql11-query/#construct) or [`DESCRIBE`](https://www.w3.org/TR/sparql11-query/#describe) query string
3. `delimiter` - optional value delimiter character, by default `,`

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

    cat parking-facilities.csv | java -jar csv2rdf-1.0.0-SNAPSHOT-jar-with-dependencies.jar http://localhost/ parking-facilities.rq > parking-facilities.ttl

Output in `parking-facilities.ttl`:

    <http://localhost/p_hus.0> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://schema.org/ParkingFacility> .
    <http://localhost/p_hus.0> <http://www.w3.org/2003/01/geo/wgs84_pos#long> "12.58228733"^^<http://www.w3.org/2001/XMLSchema#float> .
    <http://localhost/p_hus.0> <https://schema.org/identifier> "p_hus.0" .
    <http://localhost/p_hus.0> <https://schema.org/additionalProperty> "P-Kælder" .
    <http://localhost/p_hus.0> <https://schema.org/comment> "Adelgade 5-7, Q-park." .
    <http://localhost/p_hus.0> <https://schema.org/postalCode> "1304" .
    <http://localhost/p_hus.0> <http://www.w3.org/2003/01/geo/wgs84_pos#lat> "55.68268042"^^<http://www.w3.org/2001/XMLSchema#float> .
    <http://localhost/p_hus.0> <https://schema.org/streetAddress> "Adelgade 5" .
    <http://localhost/p_hus.0> <https://schema.org/name> "Adelgade 5 p_hus.0" .
    <http://localhost/p_hus.0> <https://schema.org/maximumAttendeeCapacity> "92"^^<http://www.w3.org/2001/XMLSchema#integer> .

Dependencies
------------

* [Apache Jena](https://jena.apache.org/)
* [uniVocity-parsers](https://www.univocity.com/pages/univocity_parsers_tutorial)