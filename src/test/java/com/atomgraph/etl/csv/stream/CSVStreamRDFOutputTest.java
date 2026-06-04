/**
 *  Copyright 2026 Martynas Jusevičius <martynas@atomgraph.com>
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
package com.atomgraph.etl.csv.stream;

import com.univocity.parsers.common.TextParsingException;
import java.io.StringReader;
import java.io.StringWriter;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CSVStreamRDFOutputTest
{

    private static final String BASE = "http://example.com/";
    private static final Query IDENTITY_QUERY = QueryFactory.create("CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o }");

    @Test
    void writeProducesNonEmptyRDFOutput()
    {
        String csv = "name,age\nAlice,30\nBob,25\n";
        CSVStreamRDFOutput output = new CSVStreamRDFOutput(new StringReader(csv), BASE, IDENTITY_QUERY, ',', null);

        StringWriter writer = new StringWriter();
        output.write(writer);

        assertFalse(writer.toString().isBlank());
    }

    @Test
    void writeWithCustomDelimiterParsesCorrectly()
    {
        String csv = "name;age\nAlice;30\n";
        CSVStreamRDFOutput output = new CSVStreamRDFOutput(new StringReader(csv), BASE, IDENTITY_QUERY, ';', null);

        output.write(new StringWriter());

        assertEquals(1, output.getCSVStreamRDFProcessor().getSubjectCount());
        assertEquals(2, output.getCSVStreamRDFProcessor().getTripleCount());
    }

    @Test
    void writeTwoRowsCountsSubjectsCorrectly()
    {
        String csv = "name,age\nAlice,30\nBob,25\n";
        CSVStreamRDFOutput output = new CSVStreamRDFOutput(new StringReader(csv), BASE, IDENTITY_QUERY, ',', null);

        output.write(new StringWriter());

        assertEquals(2, output.getCSVStreamRDFProcessor().getSubjectCount());
        assertEquals(4, output.getCSVStreamRDFProcessor().getTripleCount());
    }

    @Test
    void headerOnlyProducesEmptyOutput()
    {
        String csv = "name,age\n";
        CSVStreamRDFOutput output = new CSVStreamRDFOutput(new StringReader(csv), BASE, IDENTITY_QUERY, ',', null);

        output.write(new StringWriter());

        assertEquals(0, output.getCSVStreamRDFProcessor().getSubjectCount());
        assertEquals(0, output.getCSVStreamRDFProcessor().getTripleCount());
    }

    @Test
    void maxCharsPerColumnThrowsOnLongValue()
    {
        // "name" header (4 chars) fits within maxCharsPerColumn=5;
        // the data value "Hello World" (11 chars) exceeds it and must cause a TextParsingException
        String csv = "name\nHello World\n";
        CSVStreamRDFOutput output = new CSVStreamRDFOutput(new StringReader(csv), BASE, IDENTITY_QUERY, ',', 5);

        assertThrows(TextParsingException.class, () -> output.write(new StringWriter()));
    }

}
