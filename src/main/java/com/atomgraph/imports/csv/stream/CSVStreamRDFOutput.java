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
package com.atomgraph.imports.csv.stream;

import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import java.io.InputStreamReader;
import java.io.OutputStream;
import org.apache.jena.query.Query;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFLib;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Martynas Jusevičius <martynas@atomgraph.com>
 */
public class CSVStreamRDFOutput //implements StreamingOutput
{

    private static final Logger log = LoggerFactory.getLogger(CSVStreamRDFOutput.class);

    private final String base;
    private final InputStreamReader isr;
    private final Query query;
    private final char delimiter;
    private CSVStreamRDFProcessor processor;
    
    public CSVStreamRDFOutput(InputStreamReader csv, String base, Query query, char delimiter)
    {
        this.base = base;
        this.isr = csv;
        this.query = query;
        this.delimiter = delimiter;
    }
    
    //@Override
    public void write(OutputStream os)
    {
        StreamRDF stream = StreamRDFLib.writer(os);
        processor = new CSVStreamRDFProcessor(stream, getBase(), getQuery());
        
        CsvParserSettings parserSettings = new CsvParserSettings();
        parserSettings.setLineSeparatorDetectionEnabled(true);
        parserSettings.setProcessor(processor);
        parserSettings.setHeaderExtractionEnabled(true);
        parserSettings.getFormat().setDelimiter(getDelimiter());

        CsvParser parser = new CsvParser(parserSettings);
        parser.parse(getInputStreamReader());
        stream.finish(); // write the statements into the stream
    }

    public InputStreamReader getInputStreamReader()
    {
        return isr;
    }
    
    public String getBase()
    {
        return base;
    }
       
    public Query getQuery()
    {
        return query;
    }
    
    public char getDelimiter()
    {
        return delimiter;
    }
    
    public CSVStreamRDFProcessor getCSVStreamRDFProcessor()
    {
        return processor;
    }
    
}
