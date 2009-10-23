/*
 * Author: tdanford
 * Date: May 11, 2009
 */
package edu.mit.csail.cgs.sigma.expression.transcription.viz;

import java.io.*;

import edu.mit.csail.cgs.datasets.general.Region;
import edu.mit.csail.cgs.datasets.species.Genome;
import edu.mit.csail.cgs.sigma.expression.workflow.WorkflowIndexing;
import edu.mit.csail.cgs.sigma.expression.workflow.WorkflowProperties;

public class TestMultiViz {

	public static void main(String[] args) { 
		test_viz(args);
	}
	
	public static void test_viz(String[] args) { 
		try {
			WorkflowProperties props = new WorkflowProperties();
			
			String key = "test";
			String strain = props.parseStrainFromKey(key);

			WorkflowIndexing indexing = props.getIndexing(key);
			
			File testw = new File(props.getDirectory(), "test_plus.datasegs");
			File testtransw = new File(props.getDirectory(), "test_plus_matalpha_0.transcripts");
			File testc = new File(props.getDirectory(), "test_negative.datasegs");
			File testtransc = new File(props.getDirectory(), "test_negative_matalpha_0.transcripts");
			
			MultiViz viz = new MultiViz(props, key, testw, testc);

			Genome genome = props.getSigmaProperties().getGenome(strain);

			Region region = new Region(genome, "11", 0, 10000);
			viz.setRegion(region);

			int idx = -1;

			GenericRegionFrame frame = new GenericRegionFrame(new StackedRegionPaintable(region, viz));

			idx = viz.addDataSegmentChannel("matalpha");
			viz.getDataChannel(idx).loadTranscripts(testtransw);
			viz.getDataChannel(idx+1).loadTranscripts(testtransc);
			
			System.out.println("Starting frame...");

			frame.addActions(viz.collectCallTranscriptsActions());
			frame.showMe();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	

}
