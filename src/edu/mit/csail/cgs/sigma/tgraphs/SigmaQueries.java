/*
 * Author: tdanford
 * Date: Apr 18, 2009
 */
package edu.mit.csail.cgs.sigma.tgraphs;

import java.util.*;
import java.awt.Color;
import java.io.IOException;

import edu.mit.csail.cgs.cgstools.tgraphs.GraphContext;
import edu.mit.csail.cgs.cgstools.tgraphs.GraphQuery;
import edu.mit.csail.cgs.cgstools.tgraphs.QueryEvaluator;
import edu.mit.csail.cgs.cgstools.tgraphs.SerialQueryEvaluator;
import edu.mit.csail.cgs.cgstools.tgraphs.TaggedGraph;
import edu.mit.csail.cgs.cgstools.tgraphs.patterns.ContextFilterPattern;
import edu.mit.csail.cgs.cgstools.tgraphs.patterns.ContextPredicatePattern;
import edu.mit.csail.cgs.cgstools.tgraphs.patterns.GraphPattern;
import edu.mit.csail.cgs.cgstools.tgraphs.patterns.Node;
import edu.mit.csail.cgs.cgstools.tgraphs.patterns.Unique;
import edu.mit.csail.cgs.ewok.verbs.Filter;
import edu.mit.csail.cgs.sigma.expression.workflow.WholeGenome;
import edu.mit.csail.cgs.sigma.expression.workflow.WorkflowProperties;
import edu.mit.csail.cgs.sigma.expression.workflow.models.DataSegment;
import edu.mit.csail.cgs.utils.models.Model;
import edu.mit.csail.cgs.utils.models.data.DataFrame;
import edu.mit.csail.cgs.utils.models.data.DataRegression;
import edu.mit.csail.cgs.viz.colors.Coloring;
import edu.mit.csail.cgs.viz.eye.ModelScatter;

public class SigmaQueries {
	
	public static void main(String[] args) { 
		GraphQuery query = new OverlappingQuery();

		WorkflowProperties props = new WorkflowProperties();
		String strain = "txns288c";
		String expt = "matalpha";

		NewSigmaGraph graph = NewSigmaGraph.loadGraph(strain, expt);

		QueryEvaluator eval = new SerialQueryEvaluator(graph);

		SerialQueryEvaluator.printQueryPath(query.queryPath(), System.out);
		Iterator<GraphContext> contexts = eval.eval(query);
		
		ModelScatter scatter = new ModelScatter();
		scatter.synchronizeProperty(ModelScatter.xScaleKey, scatter, ModelScatter.yScaleKey);
		
		DataFrame frame = new DataFrame(ModelScatter.ScatterPoint.class);

		System.out.println("\nResults:");
		Color c = Coloring.clearer(Coloring.clearer(Color.red));
		
		while(contexts.hasNext()) { 
			GraphContext context = contexts.next();
			DataSegment s1 = graph.findSegment(context.lookup("s1"));
			DataSegment s2 = graph.findSegment(context.lookup("s2"));
			
			if(s1.start < s2.start) {  // for uniqueness sake.
				Double d1 = s1.getExpectedDifferential(expt);
				Double d2 = s2.getExpectedDifferential(expt);
				
				ModelScatter.ScatterPoint p = 
					new ModelScatter.ScatterPoint(d1, d2, c, s1.toString());
				
				scatter.addModel(p);
				frame.addObject(p);
			}
		}
		
		DataRegression reg = new DataRegression(frame, "y ~ x - 1");
		reg.calculate();
		
		new ModelScatter.InteractiveFrame(scatter, expt);
		
		Map<String,Double> coefs = reg.collectCoefficients();
		for(String cname : coefs.keySet()) { 
			System.out.println(String.format("%s : %f", cname, coefs.get(cname)));
		}
	}
	
	public static class TestQuery extends GraphQuery { 
		public Gene g1 = new Gene("g1");
		public Segment s1 = new Segment("s1");
		public Sense sense = new Sense(g1, s1);
	}
	
	public static class OverlappingQuery extends GraphQuery { 
		public Segment s1 = new Segment("s1");
		public Segment s2 = new Segment("s2");
		public Overlapping over = new Overlapping(s1, s2);
	}

	public static class TandemQuery extends GraphQuery { 
		public Segment s1 = new Segment("s1");
		public Segment s2 = new Segment("s2");
		public Tandem edge = new Tandem(s1, s2);
	}
}
