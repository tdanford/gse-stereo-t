/*
 * Author: tdanford
 * Date: June 22, 2009
 */
package edu.mit.csail.cgs.sigma.expression.workflow.assessment;

import java.util.*;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.io.*;

import edu.mit.csail.cgs.sigma.IteratorCacher;
import edu.mit.csail.cgs.sigma.expression.segmentation.Segment;
import edu.mit.csail.cgs.sigma.expression.segmentation.SegmentationParameters;
import edu.mit.csail.cgs.sigma.expression.simulation.LogAdditiveSimulator;
import edu.mit.csail.cgs.sigma.expression.simulation.ProbeSimulator;
import edu.mit.csail.cgs.sigma.expression.simulation.SimParameters;
import edu.mit.csail.cgs.sigma.expression.transcription.TranscriptionParameters;
import edu.mit.csail.cgs.sigma.expression.workflow.Workflow;
import edu.mit.csail.cgs.sigma.expression.workflow.WorkflowProperties;
import edu.mit.csail.cgs.sigma.expression.workflow.models.DataSegment;
import edu.mit.csail.cgs.sigma.expression.workflow.models.TranscriptCall;
import edu.mit.csail.cgs.utils.models.Model;
import edu.mit.csail.cgs.utils.models.ModelInput;
import edu.mit.csail.cgs.viz.eye.OverlayModelPaintable;
import edu.mit.csail.cgs.viz.paintable.AbstractPaintable;
import edu.mit.csail.cgs.viz.paintable.PaintableFrame;
import edu.mit.csail.cgs.viz.paintable.layout.LayeredPaintable;
import edu.mit.csail.cgs.viz.paintable.layout.StackedPaintable;

/**
 * Uses a ProbeSimulator to regenerate artificial probes, runs the segmentation,
 * and appends the results to a file from which visualizations  and analyses
 * can be created.
 * 
 * @author tdanford
 */
public class SyntheticSegmentationTest {
	
	public static void main(String[] args) {
		String name = args.length > 0 ? args[0] : "noise=2.0";
		int iters = args.length > 1 ? Integer.parseInt(args[1]) : 0;
		double noise = args.length > 2 ? Double.parseDouble(args[2]) : 1.0;
		
		SyntheticSegmentationTest test = new SyntheticSegmentationTest(name, new String[] {});

		try {

			for(int i = 0; i < iters; i++) {
				System.out.println(String.format(
						"*************** Test Iteration %d ****************", i));
				test.test(noise);
				test.save();
			}
			
			Color[] colors = new Color[] { Color.red, Color.blue, Color.orange, Color.gray };
			Set<String> names = test.loadNames();
			
			String[] nameArray = names.toArray(new String[0]);
			CoveragePaintable[] ptbArray = new CoveragePaintable[nameArray.length];
			
			for(int i =0; i < nameArray.length; i++) { 
				int[] coverage = test.loadCoverage(nameArray[i], 10000);
				Color color = colors[i % colors.length];
				CoveragePaintable pt = new CoveragePaintable(coverage, color);
				ptbArray[i] = pt;
				System.out.println(String.format("%d: %s", i, nameArray[i]));
			}
			
			//StackedPaintable over = new StackedPaintable(ptbArray);
			LayeredPaintable over = new LayeredPaintable(ptbArray);
			over.setBgColor(Color.white);

			Collection<RunResults> results = test.loadResults(name);
			Map<Integer,Collection<RunResults>> bySize = splitByNumCalls(results);
			RegionResults t1 = new RegionResults(2000, 7000, 10.0);
			RegionResults t2 = new RegionResults(3500, 4500, 12.0);
			
			Integer[] bins = new Integer[] { 
					-8000, -4000, -1000, -500, 0, 
					500, 1000, 4000, 8000
			};

			for(Integer calls : bySize.keySet()) {
				Collection<RegionResults> t1Hits = 
					closeCalls(bySize.get(calls), t1, 150);
				Collection<RegionResults> t2Hits = 
					closeCalls(bySize.get(calls), t2, 150);
				
				Collection<Integer> t1Dists = callDistances(bySize.get(calls), t1); 
				Collection<Integer> t2Dists = callDistances(bySize.get(calls), t2);
				
				Integer[] t1Bins = binResults(t1Dists, bins);
				Integer[] t2Bins = binResults(t2Dists, bins);
					
				Double t1Mean = meanIntensity(t1Hits), t2Mean = meanIntensity(t2Hits);	
				System.out.println(String.format("%d=%d (%.2f, %.2f)", calls, bySize.get(calls).size(), t1Mean, t2Mean));
				System.out.println(String.format("\tt1: %s", binString(t1Bins)));
				System.out.println(String.format("\tt2: %s", binString(t2Bins)));
				System.out.println();
			}
			System.out.println();

			PaintableFrame pf = new PaintableFrame("Coverage", over);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private String key, expt;
	private String[] args;
	private WorkflowProperties props;
	private SegmentationParameters params;
	private Workflow worker;

	private SimParameters simParams;
	
	private File outputFile;
	
	private String name;
	private ArrayList<RunResults> results;

	public SyntheticSegmentationTest(String n, String[] arglist) { 
		args = arglist.clone();
		name = n;
		
		key = "test";
		expt = "matalpha";
		props = new WorkflowProperties();
		params = props.getDefaultSegmentationParameters();
		worker = new Workflow(props, args);
		
		simParams = new SimParameters("sim2");

		outputFile = new File(props.getDirectory(), "segmentation-test.txt");
		results = new ArrayList<RunResults>();
	}
	
	public Set<String> loadNames() throws IOException { 

		Set<String> names = new TreeSet<String>();
		
		BufferedReader br = new BufferedReader(new FileReader(outputFile));
		ModelInput.LineReader<RunResults> resultReader = 
			new ModelInput.LineReader<RunResults>(RunResults.class, br);
		
		RunResults res = null;
		while((res = resultReader.readModel()) != null) {
			names.add(res.name);
		}
		br.close();

		return names;
	}
	
	public static String binString(Integer[] bins) { 
		StringBuilder sb = new StringBuilder();
		int total = 0;
		for(Integer b : bins) { 
			sb.append(b + " ");
			total += b;
		}
		sb.append(String.format("=%d", total));
		return sb.toString();
	}
	
	public static Integer[] binResults(Collection<Integer> values, Integer[] bounds) { 
		Integer[] bins = new Integer[bounds.length+1];
		for(int i = 0; i < bins.length; i++) { bins[i] = 0; }
		for(Integer v : values) { 
			if(v < bounds[0]) { 
				bins[0] += 1;
			} else if (v >= bounds[bounds.length-1]) { 
				bins[bins.length-1] += 1;
			} else { 
				for(int i = 0; i < bounds.length-1; i++) { 
					if(v >= bounds[i] && v < bounds[i+1]) { 
						bins[i+1] += 1;
						break;
					}
				}
			}
		}
		return bins;
	}
	
	public static Double meanIntensity(Collection<RegionResults> rrs) { 
		double sum = 0.0;
		for(RegionResults rr : rrs) { 
			sum += rr.intensity;
		}
		return sum / (double)Math.max(1, rrs.size());
	}
	
	public static Collection<RegionResults> closeCalls(Collection<RunResults> rrs, RegionResults target, int threshold) { 
		ArrayList<RegionResults> res = new ArrayList<RegionResults>();
		for(RunResults rr : rrs) { 
			for(RegionResults reg : rr.calls) { 
				if(reg.overlaps(target)) {  
					Integer[] mm = reg.distances(target, null);
					if(Math.abs(mm[0]) <= threshold && Math.abs(mm[1]) <= threshold) { 
						res.add(reg);
					}
				}
			}
		}
		return res;
	}
	
	public static Collection<Integer> callDistances(Collection<RunResults> rrs, RegionResults target) { 
		ArrayList<Integer> values = new ArrayList<Integer>();
		for(RunResults rr : rrs) { 
			Integer[] mm = null;
			for(RegionResults reg : rr.calls) { 
				mm = reg.distances(target, mm);
			}
			if(mm != null) { 
				values.add(mm[0]);
				values.add(mm[1]);
			}
		}
		return values;
	}
	
	public static Map<Integer,Collection<RunResults>> splitByNumCalls(Collection<RunResults> rrs) { 
		Map<Integer,Collection<RunResults>> map = 
			new TreeMap<Integer,Collection<RunResults>>();
		int max = 0;
		
		for(RunResults rr : rrs) { 
			max = Math.max(max, rr.calls.length);
			if(!map.containsKey(rr.calls.length)) { 
				map.put(rr.calls.length, new LinkedList<RunResults>());
			}
			map.get(rr.calls.length).add(rr);
		}
		
		for(int i = 0; i < max; i++) { 
			if(!map.containsKey(i)) { 
				map.put(i, new LinkedList<RunResults>());
			}
		}
		
		return map;
	}
	
	public Collection<RunResults> loadResults(String n) throws IOException { 
		LinkedList<RunResults> results = new LinkedList<RunResults>();
		
		BufferedReader br = new BufferedReader(new FileReader(outputFile));
		ModelInput.LineReader<RunResults> resultReader = 
			new ModelInput.LineReader<RunResults>(RunResults.class, br);
		
		RunResults res = null;
		while((res = resultReader.readModel()) != null) {
			if(res.name.equals(n)) { 
				results.add(res);
			}
		}
		br.close();

		return results;
	}
	
	public int[] loadCoverage(String n, int size) throws IOException {
		int[] coverage = new int[size];
		for(int i = 0; i < size; i++) { coverage[i] = 0; }
		
		BufferedReader br = new BufferedReader(new FileReader(outputFile));
		ModelInput.LineReader<RunResults> resultReader = 
			new ModelInput.LineReader<RunResults>(RunResults.class, br);
		
		RunResults res = null;
		while((res = resultReader.readModel()) != null) {
			if(res.name.equals(n)) { 
				res.annotateCoverage(coverage);
			}
		}
		br.close();

		return coverage;
	}

	public void test(double noise) {
		simParams.noise = noise;
		LogAdditiveSimulator sim = new LogAdditiveSimulator(simParams, true);
		
		Iterator<DataSegment> segs = 
			worker.completeSegmentation(sim.probes(), key, expt, params);
		IteratorCacher<DataSegment> seger = new IteratorCacher<DataSegment>(segs);
		
		TranscriptionParameters tparams = new TranscriptionParameters();
		Iterator<TranscriptCall> calls = 
			worker.completeCalling(seger.iterator(), key, tparams, expt);

		recordRun(seger.iterator(), calls);
	}
	
	private void recordRun(Iterator<DataSegment> segs, Iterator<TranscriptCall> calls) { 
		RunResults rr = new RunResults(name, segs, calls);
		results.add(rr);
	}
	
	private void save() throws IOException { 
		FileOutputStream fos = new FileOutputStream(outputFile, true);
		PrintStream ps = new PrintStream(fos);
		
		for(RunResults res : results) {
			ps.println(res.asJSON().toString());
		}
		ps.close();
		results.clear();
	}
	
	public static class RegionResults extends Model {
		
		public Integer start, end; 
		public Double intensity;
		
		public RegionResults() {}
		
		public RegionResults(DataSegment seg) { 
			start = seg.start;
			end = seg.end;
			intensity = seg.predicted(0, seg.end);
		}

		public RegionResults(TranscriptCall call) { 
			start = call.start;
			end = call.end;
			intensity = call.intensities[0];
		}
		
		public RegionResults(Integer s, Integer e, Double i) { 
			start = s; 
			end = e;
			intensity = i;
		}
		
		public boolean contains(Integer pt) { 
			return start <= pt && end >= pt;
		}
		
		public boolean overlaps(RegionResults rr) { 
			return contains(rr.start) || rr.contains(start);
		}
		
		public int overlapSize(RegionResults rr) { 
			if(!overlaps(rr)) { return 0; }
			return Math.min(end, rr.end) - Math.max(start, rr.start) + 1;
		}
		
		public Integer[] distances(RegionResults rr, Integer[] prev) { 
			if(!overlaps(rr)) { return prev; }
			Integer startdiff = rr.start-start, enddiff = rr.end-end;
			Integer minstart = startdiff, minend = enddiff;
			if(prev != null) { 
				if(Math.abs(minstart) < Math.abs(prev[0])) {
					prev[0] = minstart;
				}
				if(Math.abs(minend) < Math.abs(prev[1])) {
					prev[1] = minend;
				}
			}
			return new Integer[] { minstart, minend };
		}
	}
	
	public static class RunResults extends Model {
		
		public String name;
		public RegionResults[] segments;
		public RegionResults[] calls;
		
		public RunResults() {}
		
		public RunResults(String n, Iterator<DataSegment> segs, Iterator<TranscriptCall> cs) {
			name = n;
			ArrayList<RegionResults> res = new ArrayList<RegionResults>();
			Integer[] channels = new Integer[] { 0 } ;
			
			while(segs.hasNext()) { 
				DataSegment seg = segs.next();
				if(seg.hasConsistentType(Segment.LINE, channels)) { 
					res.add(new RegionResults(seg));
				}
			}
			
			segments = res.toArray(new RegionResults[0]);
			
			res.clear();

			while(cs.hasNext()) { 
				TranscriptCall call = cs.next();
				res.add(new RegionResults(call));
			}
			
			calls = res.toArray(new RegionResults[0]);
		}
		
		public void annotateCoverage(int[] cover) { 
			for(RegionResults intv : segments) { 
				for(int i = intv.start; i <= intv.end && i < cover.length; i++) { 
					cover[i] += 1;
				}
			}
		}
	}
	
	public static class CoveragePaintable extends AbstractPaintable {
		
		private int[] coverage;
		private Color color;
		
		public CoveragePaintable(int[] c, Color cc) { 
			coverage = c;
			color = cc;
		}

		public void paintItem(Graphics g, int x1, int y1, int x2, int y2) {
			int w = x2-x1, h = y2-y1-10;
			int coverBinSize = Math.max(1, (int)Math.floor((double)coverage.length / (double)w));
			
			int[] c = new int[w];
			int cmax = 1;
			
			for(int i = 0; i < w; i++) {
				int b1 = coverBinSize * i;
				int b2 = b1 + coverBinSize;
				c[i] = 0;
				for(int j = b1, s = 0; j < b2; j++) { 
					c[i] += coverage[j];
				}
				cmax = Math.max(cmax, c[i]);
			}
			
			int py = -1;
			g.setColor(color);
			
			Graphics2D g2 = (Graphics2D)g;
			Stroke oldStroke = g2.getStroke();
			g2.setStroke(new BasicStroke(3.0f));
			
			for(int x = x1, i = 0; x < x2; x++, i++) {
				double f = (double)c[i] / (double)cmax;
				int y = y2 - (int)Math.round(f * (double)h);
				if(x > x1) { 
					g.drawLine(x-1, py, x, y);
				}
				py = y;
			}
			
			g2.setStroke(oldStroke);
		}
	}
}
