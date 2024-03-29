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
package com.atomgraph.etl.csv.stream;

import com.atomgraph.etl.csv.ModelTransformer;
import com.univocity.parsers.common.ParsingContext;
import com.univocity.parsers.common.processor.RowProcessor;
import java.util.function.BiFunction;
import org.apache.jena.atlas.lib.IRILib;
import org.apache.jena.query.Query;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFOps;

/**
 * Converts an array of values to generic RDF, transforms it with a query and sends the result triples to stream.
 * 
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 */
public class CSVStreamRDFProcessor implements RowProcessor
{

    private final StreamRDF stream;
    private final String base;
    private final BiFunction<Query, Model, Model> function = new ModelTransformer();
    private final Query query;
    private int subjectCount, tripleCount;
    
    public CSVStreamRDFProcessor(StreamRDF stream, String base, Query query)
    {
        this.stream = stream;
        this.base = base;
        if (!(query.isConstructType() || query.isDescribeType())) throw new IllegalArgumentException("Only CONSTRUCT and DESCRIBE queries can be used for transformation");
        this.query = query;
    }
    
    @Override
    public void processStarted(ParsingContext context)
    {
        subjectCount = tripleCount = 0;
        if (getBase() != null) getStreamRDF().base(getBase());
    }

    @Override
    public void rowProcessed(String[] row, ParsingContext context)
    {
        Model rowModel = transformRow(row, context);
        StreamRDFOps.sendTriplesToStream(rowModel.getGraph(), getStreamRDF()); // send the transformed RDF to the stream
    }

    public Model transformRow(String[] row, ParsingContext context)
    {
        Model rowModel = ModelFactory.createDefaultModel();
        Resource subject = rowModel.createResource();
        subjectCount++;
        
        int cellNo = 0;
        for (String cell : row)
        {
            if (cell != null && context.headers()[cellNo] != null)
            {
                String fragmentId = IRILib.encodeUriComponent(context.headers()[cellNo]);
                Property property = rowModel.createProperty(getBase(), "#" + fragmentId);
                subject.addProperty(property, cell);
                tripleCount++;
            }
            cellNo++;
        }

        return getFunction().apply(getQuery(), rowModel); // transform row
    }
    
    @Override
    public void processEnded(ParsingContext context)
    {
    }
    
    public StreamRDF getStreamRDF()
    {
        return stream;
    }
    
    public String getBase()
    {
        return base;
    }
    
    public BiFunction<Query, Model, Model> getFunction()
    {
        return function;
    }
    
    public Query getQuery()
    {
        return query;
    }
    
    public int getSubjectCount()
    {
        return subjectCount;
    }
    
    public int getTripleCount()
    {
        return tripleCount;
    }
    
}