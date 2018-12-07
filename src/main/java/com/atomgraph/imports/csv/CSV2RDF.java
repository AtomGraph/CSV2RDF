/**
 *  Copyright 2018 Martynas Jusevičius <martynas@atomgraph.com>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.atomgraph.imports.csv;

import com.atomgraph.imports.csv.stream.CSVStreamRDFOutput;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;

/**
 *
 * @author Martynas Jusevičius <martynas@atomgraph.com>
 */
public class CSV2RDF
{

    
    public static void main(String[] args)
    {
        String csvFilename = "C:\\Users\\pumba\\WebRoot\\AtomGraph\\CSV2RDF\\src\\main\\resources\\parking-facilities.csv"; // args[0];
        String queryFilename = "C:\\Users\\pumba\\WebRoot\\AtomGraph\\CSV2RDF\\src\\main\\resources\\parking-facilities.rq"; // args[1];
        String baseURI = "http://localhost/";
        char delimiter = ",".charAt(0);
        
        Path queryPath = Paths.get(queryFilename);

        try
        {
            byte[] encoded = Files.readAllBytes(queryPath);
            String queryString = new String(encoded, StandardCharsets.UTF_8);
            Query query = QueryFactory.create(queryString, baseURI);
            
            try (InputStreamReader reader =  new InputStreamReader(new FileInputStream(csvFilename)))
            {
                CSVStreamRDFOutput rdfOutput = new CSVStreamRDFOutput(reader, baseURI, query, delimiter);
                rdfOutput.write(System.out);
            }
        }
        catch (IOException ex)
        {
            Logger.getLogger(CSV2RDF.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}