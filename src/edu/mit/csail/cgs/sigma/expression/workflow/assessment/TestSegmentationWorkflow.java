package edu.mit.csail.cgs.sigma.expression.workflow.assessment;

import java.io.File;
import java.io.IOException;
import java.util.*;

import edu.mit.csail.cgs.sigma.IteratorCacher;
import edu.mit.csail.cgs.sigma.JSONOutputIterator;
import edu.mit.csail.cgs.sigma.expression.segmentation.Segment;
import edu.mit.csail.cgs.sigma.expression.segmentation.SegmentationParameters;
import edu.mit.csail.cgs.sigma.expression.simulation.SimParameters;
import edu.mit.csail.cgs.sigma.expression.simulation.SimpleSimulator;
import edu.mit.csail.cgs.sigma.expression.simulation.SimulatorGenerator;
import edu.mit.csail.cgs.sigma.expression.transcription.TranscriptionParameters;
import edu.mit.csail.cgs.sigma.expression.workflow.Workflow;
import edu.mit.csail.cgs.sigma.expression.workflow.WorkflowProperties;
import edu.mit.csail.cgs.sigma.expression.workflow.models.DataSegment;
import edu.mit.csail.cgs.sigma.expression.workflow.models.ProbeLine;
import edu.mit.csail.cgs.sigma.expression.workflow.models.RegionKey;
import edu.mit.csail.cgs.sigma.expression.workflow.models.TranscriptCall;
import edu.mit.csail.cgs.utils.models.Model;
import edu.mit.csail.cgs.viz.eye.ModelHistogram;
import edu.mit.csail.cgs.viz.paintable.PaintableFrame;

public class TestSegmentationWorkflow {
	
	public static void main(String[] args) { 
		String simName = "sim1";
		TestSegmentationWorkflow tsw = new TestSegmentationWorkflow(new WorkflowProperties(), new SimParameters(simName));
		tsw.process(50);
		tsw.displayResults();
	}

	public WorkflowProperties props;
	public SimParameters simParams;
	public SegmentationParameters segParams;
	public SimulatorGenerator simgen;
	public boolean strand;
	public String key, expt, strain;
	public Integer[] channels;
	public ModelHistogram intensityHist, breakHist; 
	
	private LinkedList<EvalModel> results;
	
	public TestSegmentationWorkflow(WorkflowProperties ps) { 
		this(ps, new SimParameters("sim1"));
	}
	
	public TestSegmentationWorkflow(WorkflowProperties ps, SimParameters sim) { 
		props = ps;
		key = "test";
		simParams = sim;
		strand = true;
		//transParams = props.getDefaultTranscriptionParameters();
		segParams = props.getDefaultSegmentationParameters();
		simgen = new SimulatorGenerator(sim, strand);
		expt = "matalpha";
		strain = props.parseStrainFromKey(key);
		channels = props.getIndexing(key).findChannels(strain, expt);
		
		segParams.doHRMA = false;
		
		results = new LinkedList<EvalModel>();
		breakHist = new ModelHistogram("breakpointError");
		intensityHist = new ModelHistogram("intensityError");		
		
		PaintableFrame pf1 = new PaintableFrame("Breakpoint Error", breakHist);
		PaintableFrame pf2 = new PaintableFrame("Intensity Error", intensityHist);
	}
	
	public void displayResults() { 
	}
	
	public void process(int n) { 
		for(int i = 0; i < n; i++) { 
			process();
		}
	}
	
	public void process() {
		Workflow worker = new Workflow(props);

		Iterator<ProbeLine> probes = simgen.execute();

		System.out.println("segParams.doHRMA: " + segParams.doHRMA);
		IteratorCacher<DataSegment> segs = 
			new IteratorCacher<DataSegment>(
					worker.completeSegmentation(
							probes, key, expt, segParams));

		EvalModel res = processResults(segs.iterator());
		results.add(res);
		
		breakHist.addModel(res); breakHist.rebin();
		intensityHist.addModel(res); intensityHist.rebin();
	}
	
	public static class EvalModel extends Model { 
		public Double breakpointError;
		public Double intensityError;
		
		public EvalModel() {}
		
		public EvalModel(double bk, double ie) { 
			breakpointError = bk;
			intensityError = ie;
		}
	}
	
	public EvalModel processResults(Iterator<DataSegment> segs) { 

		Map<Integer,Integer> breakDists = new TreeMap<Integer,Integer>();
		Map<SimulatorGenerator.TrueSegment,Double> segValues = 
			new TreeMap<SimulatorGenerator.TrueSegment,Double>();
		
		System.out.println("+ breakpoints : " + simgen.trueBreakpoints(true));
		System.out.println("- breakpoints : " + simgen.trueBreakpoints(false));
		
		
		for(Integer breakpt : simgen.trueBreakpoints()) { 
			breakDists.put(breakpt, null);
		}
		
		for(SimulatorGenerator.TrueSegment seg : simgen.trueSegments()) { 
			segValues.put(seg, null);
		}
		
		while(segs.hasNext()) { 
			DataSegment seg = segs.next();
			
			if(!seg.hasConsistentType(Segment.LINE, channels)) { 
				continue;
			}
			
			int b1 = seg.start, b2 = seg.end;
			
			for(Integer bkpt : breakDists.keySet()) { 
				Integer value = breakDists.get(bkpt);
				Integer d = Math.min(Math.abs(b1 - bkpt), Math.abs(b2 - bkpt));
				if(value == null || d < value) { 
					breakDists.put(bkpt, d);
					System.out.println(String.format("Breakpoint: %d matched by %d-%d (%d)", bkpt, b1, b2, d));
				}
			}

			RegionKey segKey = new RegionKey(seg.chrom, seg.start, seg.end, seg.strand);
			for(SimulatorGenerator.TrueSegment tseg : segValues.keySet()) {
				Integer threept = tseg.end;
				if(segKey.start <= threept && segKey.end >= threept) {
					double sum = 0.0;
					double mean = 0.0;
					int c = 0;
					for(int i = 0; i < seg.dataValues.length; i++) { 
						double pred = seg.predicted(i, threept);
						double error = Math.abs(pred - tseg.intensity);
						mean += pred;
						sum += error;
						c += 1;
					}
					
					sum /= (double)Math.max(1, c);
					mean /= (double)Math.max(1, c);
					
					Double value = segValues.get(tseg);
					
					if(value == null || sum < value) {
						System.out.println(String.format("Segment (%s:%d-%d:%s, %.2f) matched by (%s:%d-%d:%s, %.2f) : %.3f", 
								tseg.chrom, tseg.start, tseg.end, tseg.strand, tseg.intensity, 
								segKey.chrom, segKey.start, segKey.end, segKey.strand, mean, sum));
						segValues.put(tseg, sum);
					}
				}
			}
		}
		
		Double meanBkError = averageIntegers(breakDists);
		Double meanSegLevelError = averageDoubles(segValues);
		
		System.out.println("\n");
		System.out.println(String.format("Average Breakpoint Error: %.3f", meanBkError));
		System.out.println(String.format("Average Seg Level Error: %.3f", meanSegLevelError));
		System.out.println("\n");
		
		return new EvalModel(meanBkError, meanSegLevelError);
	}
	
	public <X> Double averageIntegers(Map<X,Integer> map) { 
		double sum = 0.0;
		int c = 0;
		for(X value : map.keySet()) {
			Integer v = map.get(value);
			if(v != null) { 
				sum += v;
				c += 1;
			}
		}
		return sum / (double)Math.max(1, c);
	}
	
	public <X> Double averageDoubles(Map<X,Double> map) { 
		double sum = 0.0;
		int c = 0;
		for(X value : map.keySet()) { 
			Double v = map.get(value);
			if(v != null) {
				sum += v;
				c += 1;
			}
		}
		return sum / (double)Math.max(1, c);
	}
}
