/*
 * Author: tdanford
 * Date: May 20, 2009
 */
package edu.mit.csail.cgs.sigma.expression.workflow.assessment;

import java.awt.Color;
import java.util.Iterator;
import java.util.Map;

import edu.mit.csail.cgs.cgstools.tgraphs.GraphContext;
import edu.mit.csail.cgs.cgstools.tgraphs.GraphQuery;
import edu.mit.csail.cgs.cgstools.tgraphs.QueryEvaluator;
import edu.mit.csail.cgs.cgstools.tgraphs.SerialQueryEvaluator;
import edu.mit.csail.cgs.sigma.expression.workflow.WorkflowProperties;
import edu.mit.csail.cgs.sigma.expression.workflow.models.DataSegment;
import edu.mit.csail.cgs.sigma.tgraphs.NewSigmaGraph;
import edu.mit.csail.cgs.sigma.tgraphs.SigmaQueries;
import edu.mit.csail.cgs.sigma.tgraphs.SigmaQueries.OverlappingQuery;
import edu.mit.csail.cgs.utils.models.data.DataFrame;
import edu.mit.csail.cgs.utils.models.data.DataRegression;
import edu.mit.csail.cgs.viz.colors.Coloring;
import edu.mit.csail.cgs.viz.eye.ModelScatter;
import edu.mit.csail.cgs.viz.paintable.PaintableFrame;

public class SpatialSegmentExpression {

	public static void main(String[] args) { 
		GraphQuery query = new SigmaQueries.OverlappingQuery();
		//GraphQuery query = new SigmaQueries.TandemQuery();

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
		Color c = Coloring.clearer(Coloring.clearer(Color.blue));
		
		while(contexts.hasNext()) { 
			GraphContext context = contexts.next();
			DataSegment s1 = graph.findSegment(context.lookup("s1"));
			DataSegment s2 = graph.findSegment(context.lookup("s2"));
			
			if(s1.start < s2.start) {  // for uniqueness sake.
				Double d1 = s1.getExpectedDifferential(expt);
				Double d2 = s2.getExpectedDifferential(expt);
				
				boolean addPoint = false;

				ModelScatter.ScatterPoint p = 
					new ModelScatter.ScatterPoint(d1, d2, c, s1.toString());
				
				if(s1.isDifferential(expt) || s2.isDifferential(expt)) { 
					if(s1.isDifferential(expt) && s2.isDifferential(expt)) { 
						p.color = Coloring.clearer(Color.red);
						addPoint = true;
					} else { 
						p.color = Coloring.clearer(Color.orange);						
					}
				}
				
				if(addPoint) { 
					scatter.addModel(p);
					frame.addObject(p);
				}
			}
		}
		
		DataRegression reg = new DataRegression(frame, "y ~ x - 1");
		reg.calculate();
		
		//new ModelScatter.InteractiveFrame(scatter, expt);
		PaintableFrame pf = new PaintableFrame(expt, scatter);
		
		Map<String,Double> coefs = reg.collectCoefficients();
		for(String cname : coefs.keySet()) { 
			System.out.println(String.format("%s : %f", cname, coefs.get(cname)));
		}
	}
}
