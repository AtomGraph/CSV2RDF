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
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * Main entry point to the transformation.
 * 
 * @author Martynas Jusevičius <martynas@atomgraph.com>
 */
@Command(name = "csv2rdf")
public class CSV2RDF
{
    private static final char DEFAULT_DELIMITER = ",".charAt(0);

    private final InputStream csvIn;
    private final OutputStream rdfOut;

    @Parameters(paramLabel = "query-file", index = "0", description = "File with SPARQL CONSTRUCT/DESCRIBE query used for the RDF transformation\nExample: mapping.rq")
    private Path queryFile;

    @Parameters(paramLabel = "base", index = "1", description = "Base URI of the RDF output data\nExample: https://localhost/")
    private URI baseURI;

    @Option(names = { "-d", "--delimiter" }, description = "Field delimiter character used in the input data (default: ${DEFAULT-VALUE})")
    private char delimiter = DEFAULT_DELIMITER;

    @Option(names = { "--input-charset" }, description = "Input charset (default: ${DEFAULT-VALUE})")
    private Charset inputCharset = StandardCharsets.UTF_8;

    @Option(names = { "--output-charset" }, description = "Output charset (default: ${DEFAULT-VALUE})")
    private Charset outputCharset = StandardCharsets.UTF_8;

    @Option(names = { "--max-chars-per-column" }, description = "Maximum number of characters allowed for any given value being written/read. Used to avoid OutOfMemoryErrors (default: 4096)")
    private Integer maxCharsPerColumn;

    public static void main(String[] args) throws IOException, URISyntaxException
    {
        CSV2RDF csv2rdf = new CSV2RDF(System.in, System.out);

        try
        {
            CommandLine.ParseResult parseResult = new CommandLine(csv2rdf).parseArgs(args);
            if (!CommandLine.printHelpIfRequested(parseResult)) csv2rdf.convert();
        }
        catch (CommandLine.ParameterException ex)
        { // command line arguments could not be parsed
            System.err.println(ex.getMessage());
            ex.getCommandLine().usage(System.err);
        }
    }
    
    public CSV2RDF(InputStream csvIn, OutputStream rdfOut)
    {
        this.csvIn = csvIn;
        this.rdfOut = rdfOut;
    }
    
    public void convert() throws IOException
    {
        if (csvIn.available() == 0) throw new IllegalStateException("CSV input not provided");
        
        byte[] encoded = Files.readAllBytes(queryFile);
        String queryString = new String(encoded, StandardCharsets.UTF_8);
        Query query = QueryFactory.create(queryString, baseURI.toString());
        if (!(query.isConstructType() || query.isDescribeType())) throw new IllegalStateException("Only CONSTRUCT or DESCRIBE queries are supported");

        try (Reader reader =  new BufferedReader(new InputStreamReader(csvIn, inputCharset)))
        {
            CSVStreamRDFOutput rdfOutput = new CSVStreamRDFOutput(reader, baseURI.toString(), query, delimiter, maxCharsPerColumn);
            Writer out = new BufferedWriter(new OutputStreamWriter(rdfOut, outputCharset)); // needed to write UTF-8 characters
            rdfOutput.write(out);
        }
    }
    
}