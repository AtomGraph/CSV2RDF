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

import com.univocity.parsers.common.ParsingContext;
import java.io.StringWriter;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFLib;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CSVStreamRDFProcessorTest
{

    private static final String BASE = "http://example.com/";
    private static final Query IDENTITY_QUERY = QueryFactory.create("CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o }");

    @Mock
    private ParsingContext context;

    private final StreamRDF stream = StreamRDFLib.writer(new StringWriter());

    @Test
    void constructorRejectsSelectQuery()
    {
        Query selectQuery = QueryFactory.create("SELECT * WHERE { ?s ?p ?o }");
        assertThrows(IllegalArgumentException.class, () -> new CSVStreamRDFProcessor(stream, BASE, selectQuery));
    }

    @Test
    void transformRowCreatesTriples()
    {
        when(context.headers()).thenReturn(new String[]{"name", "age"});

        CSVStreamRDFProcessor processor = new CSVStreamRDFProcessor(stream, BASE, IDENTITY_QUERY);
        processor.processStarted(context);

        Model result = processor.transformRow(new String[]{"Alice", "30"}, context);

        assertEquals(2, result.size());
        assertEquals(1, processor.getSubjectCount());
        assertEquals(2, processor.getTripleCount());
    }

    @Test
    void transformRowSkipsNullCells()
    {
        when(context.headers()).thenReturn(new String[]{"name", "age"});

        CSVStreamRDFProcessor processor = new CSVStreamRDFProcessor(stream, BASE, IDENTITY_QUERY);
        processor.processStarted(context);

        Model result = processor.transformRow(new String[]{null, "30"}, context);

        assertEquals(1, result.size());
        assertEquals(1, processor.getTripleCount());
    }

    @Test
    void transformRowSkipsNullHeader()
    {
        when(context.headers()).thenReturn(new String[]{"name", null});

        CSVStreamRDFProcessor processor = new CSVStreamRDFProcessor(stream, BASE, IDENTITY_QUERY);
        processor.processStarted(context);

        Model result = processor.transformRow(new String[]{"Alice", "30"}, context);

        assertEquals(1, result.size());
        assertEquals(1, processor.getTripleCount());
    }

    @Test
    void processStartedResetsCounters()
    {
        when(context.headers()).thenReturn(new String[]{"name"});

        CSVStreamRDFProcessor processor = new CSVStreamRDFProcessor(stream, BASE, IDENTITY_QUERY);
        processor.processStarted(context);
        processor.transformRow(new String[]{"Alice"}, context);
        assertEquals(1, processor.getSubjectCount());

        processor.processStarted(context);
        assertEquals(0, processor.getSubjectCount());
        assertEquals(0, processor.getTripleCount());
    }

    @Test
    void propertyIriUsesBaseAndEncodedHeader()
    {
        when(context.headers()).thenReturn(new String[]{"name"});

        CSVStreamRDFProcessor processor = new CSVStreamRDFProcessor(stream, BASE, IDENTITY_QUERY);
        processor.processStarted(context);

        Model result = processor.transformRow(new String[]{"Alice"}, context);

        Statement stmt = result.listStatements().nextStatement();
        assertEquals("http://example.com/#name", stmt.getPredicate().getURI());
    }

    @Test
    void headerWithSpacesIsPercentEncoded()
    {
        when(context.headers()).thenReturn(new String[]{"first name"});

        CSVStreamRDFProcessor processor = new CSVStreamRDFProcessor(stream, BASE, IDENTITY_QUERY);
        processor.processStarted(context);

        Model result = processor.transformRow(new String[]{"Alice"}, context);

        Statement stmt = result.listStatements().nextStatement();
        assertTrue(stmt.getPredicate().getURI().contains("first%20name"));
    }

    @Test
    void headerWithHashIsPercentEncoded()
    {
        when(context.headers()).thenReturn(new String[]{"#col"});

        CSVStreamRDFProcessor processor = new CSVStreamRDFProcessor(stream, BASE, IDENTITY_QUERY);
        processor.processStarted(context);

        Model result = processor.transformRow(new String[]{"val"}, context);

        Statement stmt = result.listStatements().nextStatement();
        assertTrue(stmt.getPredicate().getURI().contains("%23col"));
    }

    @Test
    void headerWithSlashIsPercentEncoded()
    {
        when(context.headers()).thenReturn(new String[]{"a/b"});

        CSVStreamRDFProcessor processor = new CSVStreamRDFProcessor(stream, BASE, IDENTITY_QUERY);
        processor.processStarted(context);

        Model result = processor.transformRow(new String[]{"val"}, context);

        Statement stmt = result.listStatements().nextStatement();
        assertTrue(stmt.getPredicate().getURI().contains("a%2Fb"));
    }

    @Test
    void cellValueIsPlainStringLiteral()
    {
        when(context.headers()).thenReturn(new String[]{"name"});

        CSVStreamRDFProcessor processor = new CSVStreamRDFProcessor(stream, BASE, IDENTITY_QUERY);
        processor.processStarted(context);

        Model result = processor.transformRow(new String[]{"Alice"}, context);

        Literal lit = result.listStatements().nextStatement().getLiteral();
        assertEquals("Alice", lit.getString());
        assertTrue(lit.getLanguage().isEmpty());
        assertTrue(lit.getDatatypeURI() == null
            || lit.getDatatypeURI().equals(XSDDatatype.XSDstring.getURI()));
    }

    @Test
    void numericCellIsStillStringLiteral()
    {
        when(context.headers()).thenReturn(new String[]{"age"});

        CSVStreamRDFProcessor processor = new CSVStreamRDFProcessor(stream, BASE, IDENTITY_QUERY);
        processor.processStarted(context);

        Model result = processor.transformRow(new String[]{"42"}, context);

        Literal lit = result.listStatements().nextStatement().getLiteral();
        assertEquals("42", lit.getString());
        assertTrue(lit.getLanguage().isEmpty());
        assertTrue(lit.getDatatypeURI() == null
            || lit.getDatatypeURI().equals(XSDDatatype.XSDstring.getURI()));
    }

}
