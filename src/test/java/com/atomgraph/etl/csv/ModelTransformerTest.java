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
package com.atomgraph.etl.csv;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ModelTransformerTest
{

    private final ModelTransformer transformer = new ModelTransformer();

    @Test
    void applyIdentityConstructReturnsIsomorphicModel()
    {
        Model input = ModelFactory.createDefaultModel();
        input.add(
            input.createResource("http://example.com/s"),
            RDF.type,
            input.createResource("http://example.com/Type")
        );

        Query query = QueryFactory.create("CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o }");
        Model result = transformer.apply(query, input);

        assertTrue(result.isIsomorphicWith(input));
    }

    @Test
    void applyEmptyInputReturnsEmptyModel()
    {
        Model input = ModelFactory.createDefaultModel();
        Query query = QueryFactory.create("CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o }");

        Model result = transformer.apply(query, input);

        assertTrue(result.isEmpty());
    }

    @Test
    void applyFilteringConstructReturnsSubset()
    {
        Model input = ModelFactory.createDefaultModel();
        input.add(
            input.createResource("http://example.com/s"),
            input.createProperty("http://example.com/#name"),
            "Alice"
        );
        input.add(
            input.createResource("http://example.com/s"),
            input.createProperty("http://example.com/#age"),
            "30"
        );

        // Only map name, not age
        Query query = QueryFactory.create(
            "PREFIX ex: <http://example.com/#> " +
            "CONSTRUCT { ?s ex:name ?name } WHERE { ?s ex:name ?name }"
        );
        Model result = transformer.apply(query, input);

        assertEquals(1, result.size());
    }

}
