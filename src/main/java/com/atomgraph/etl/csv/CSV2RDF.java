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
package com.atomgraph.etl.csv;

import com.atomgraph.etl.csv.stream.CSVStreamRDFOutput;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;

/**
 * Main entry point to the transformation.
 * 
 * @author Martynas Jusevičius <martynas@atomgraph.com>
 */
public class CSV2RDF
{
    private static final char DEFAULT_DELIMITER = ",".charAt(0);
    
    public static void main(String[] args) throws IOException, URISyntaxException
    {
        InputStream csv = System.in;
        
        if (csv.available() == 0 || args.length < 2 || args.length > 4)
        {
            System.out.println("CSV input: stdin");
            System.out.println("Parameters: <baseURI> <queryFile> [<delimiter> <maxCharsPerColumn>]");
            System.out.println("Example: cat sample.csv | java -jar csv2rdf-1.0.0-SNAPSHOT-jar-with-dependencies.jar https://localhost/ mapping.rq > sample.ttl");
            System.exit(1);
        }

        URI baseURI = new URI(args[0]);
        Path queryPath = Paths.get(args[1]);
        
        char delimiter = DEFAULT_DELIMITER;
        if (args.length > 2)
        {
            String delimiterStr = args[2];
            if (delimiterStr.length() > 1)
            {
                System.out.println("Delimiter must be a single character");
                System.exit(1);
            }
            delimiter = delimiterStr.charAt(0);
        }
        
        Integer maxCharsPerColumn = null;
        if (args.length > 3)
        {
            String maxCharsPerColumnStr = args[3];
            
            try
            {
                maxCharsPerColumn = Integer.valueOf(maxCharsPerColumnStr);
            }
            catch (NumberFormatException ex)
            {
                System.out.println("Cannot parse '" + maxCharsPerColumnStr + "' as an integer");
                System.exit(1);
            }
        }
        
        byte[] encoded = Files.readAllBytes(queryPath);
        String queryString = new String(encoded, StandardCharsets.UTF_8);
        Query query = QueryFactory.create(queryString, baseURI.toString());
        if (!(query.isConstructType() || query.isDescribeType()))
        {
            System.out.println("Only CONSTRUCT or DESCRIBE queries are supported");
            System.exit(1);
        }

        try (InputStreamReader reader =  new InputStreamReader(csv, StandardCharsets.UTF_8))
        {
            CSVStreamRDFOutput rdfOutput = new CSVStreamRDFOutput(reader, baseURI.toString(), query, delimiter, maxCharsPerColumn);
            Writer out = new BufferedWriter(new OutputStreamWriter(System.out)); // needed to write UTF-8 characters
            rdfOutput.write(out);
        }
    }
    
}