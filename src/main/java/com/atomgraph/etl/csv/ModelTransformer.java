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

import java.util.function.BiFunction;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdf.model.Model;

/**
 * Transforms an RDF graph using a SPARQL query.
 * 
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 */
public class ModelTransformer implements BiFunction<Query, Model, Model>
{
    
    @Override
    public Model apply(Query construct, Model model)
    {
        // execute CONSTRUCT to map from CSV/RDF to domain RDF vocab
        try (QueryExecution qex = QueryExecutionFactory.create(construct, model))
        {
            return qex.execConstruct();
        }
    }
        
}
