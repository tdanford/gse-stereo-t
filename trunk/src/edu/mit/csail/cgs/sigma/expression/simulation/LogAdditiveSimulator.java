/*
 * Author: tdanford
 * Date: Apr 27, 2009
 */
package edu.mit.csail.cgs.sigma.expression.simulation;

import java.util.*;
import java.io.*;

import edu.mit.csail.cgs.sigma.expression.workflow.WorkflowProperties;
import edu.mit.csail.cgs.sigma.expression.workflow.models.ProbeLine;

public class LogAdditiveSimulator implements ProbeSimulator {
	
	public static void main(String[] args) {
		
		String name = args.length > 0 ? args[0] : "sim2";
		System.out.println(String.format("Simulating: %s", name));
		
		SimParameters params = new SimParameters(name);
		
		LogAdditiveSimulator wsim = new LogAdditiveSimulator(params, true);
		LogAdditiveSimulator csim = new LogAdditiveSimulator(params, false);
		
		WorkflowProperties props = new WorkflowProperties();
		
		File dir = props.getDirectory();
		
		deleteFiles(dir, "test_");
		deleteFiles(dir, "test-");
		
		File woutput = new File(dir, 
				String.format("test_plus.data"));
		File coutput = new File(dir,
				String.format("test_negative.data"));
		
		try {
			wsim.save(woutput);
			System.out.println(String.format("Saved: %s", 
					woutput.getAbsolutePath()));
			
			csim.save(coutput);
			System.out.println(String.format("Saved: %s", 
					coutput.getAbsolutePath()));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void deleteFiles(File dir, String prefix) { 
		File[] lst = dir.listFiles(new DelFilenameFilter(prefix));
		for(int i = 0; i < lst.length; i++) { 
			lst[i].delete();
		}
	}
	
	public static class DelFilenameFilter implements FilenameFilter {
		
		public String prefix;
		
		public DelFilenameFilter(String p) { 
			prefix = p;
		}

		public boolean accept(File dir, String name) {
			return name.startsWith(prefix);
		} 
		
	}

	private Random rand;
	private String chrom, strand;
	private double noise;
	
	private ArrayList<SimProbe> probes;
	private ArrayList<SimTranscript> transcripts;
	private ArrayList<SimExpt> expts;

	public LogAdditiveSimulator(SimParameters params, boolean str) {
		chrom = params.chrom;
		strand = str ? "+" : "-";
		rand = new Random();
		probes = new ArrayList<SimProbe>();
		transcripts = new ArrayList<SimTranscript>();
		expts = new ArrayList<SimExpt>();
		noise = params.noise;

		for(SimParameters.SimExpt expt : params.expts) {
			addExpt(expt.additive, params.noise);
		}
		
		int pwidth = (int)Math.round((double)(params.end - params.start) / (double)(params.probes+1));
		
		for(int i = 0; i < params.probes; i++) { 
			addProbe(params.start + (i+1) * pwidth);
		}
		
		for(SimParameters.SimTranscript st : (str ? params.plus : params.minus)) { 
			addTranscript(st.start, st.end, st.intensity, st.slope);
		}
	}
	
	public void save(File f) throws IOException { 
		PrintStream ps = new PrintStream(new FileOutputStream(f));
		save(ps);
		ps.close();
	}
	
	public Iterator<ProbeLine> probes() {
		ArrayList<ProbeLine> lines = new ArrayList<ProbeLine>();
		for(SimProbe p : probes) {
			Double[] values = new Double[expts.size()];
			for(int i = 0; i < values.length; i++) { 
				values[i] = p.signal(i);
			}
			lines.add(new ProbeLine(chrom, strand, p.location, values));
		}
		return lines.iterator();
	}
	
	public void save(PrintStream ps) {
		Iterator<ProbeLine> prbs = probes();
		while(prbs.hasNext()) { 
			ProbeLine pb = prbs.next();
			ps.println(pb.toString());
		}
	}
	
	public void addProbe(int loc) { 
		probes.add(new SimProbe(loc));
	}
	
	public void addTranscript(int s, int e, double i, double l, Integer... ps) { 
		transcripts.add(new SimTranscript(s, e, i, l, ps));
	}
	
	public void addTranscript(int s, int e, double i, double l) { 
		transcripts.add(new SimTranscript(s, e, i, l));
	}
	
	public void addExpt(double b, double n) { 
		expts.add(new SimExpt(b, n));
	}
	
	private class SimExpt { 
		
		public Double baseline;
		public Double multiplicativeNoise;
		
		public SimExpt() { 
			this(1.0, noise);
		}
		
		public SimExpt(double base, double noise) {
			baseline = base;
			multiplicativeNoise = noise;
		}
		
		public double background() { 
			return Math.exp(Math.abs(baseline * rand.nextGaussian()));
		}
		
		public double noise(double input) { 
			return Math.abs(input + rand.nextGaussian() * multiplicativeNoise);
		}
	}
	
	private class SimTranscript { 

		public Double intensity, lambda;
		public Integer start, end;
		public Set<Integer> present; // expts
		
		public SimTranscript(int s, int e, double i, double l) { 
			start = s; 
			end = e; 
			intensity = Math.exp(i);
			lambda = l;
			present = new TreeSet<Integer>();
			for(int k = 0; k < expts.size(); k++) { present.add(k); }
		}
		
		public SimTranscript(int s, int e, double i, double l, Integer... ps) {  
			start = s; 
			end = e; 
			intensity = Math.exp(i);
			lambda = l;
			present = new TreeSet<Integer>();
			for(int k = 0; k < ps.length; k++) { 
				present.add(ps[k]);
			}
		}
		
		public int distance(SimProbe p) {
			return strand.equals("+") ? end - p.location : 
				p.location - start;
		}
		
		public double intensity(int distance) { 
			return intensity * Math.exp(-lambda * distance);
		}
	}

	private class SimProbe { 
		
		public Double probeNoise; 
		public Integer location;
		
		public SimProbe(int l) { 
			probeNoise = 1.0;
			location = l;
		}
		
		public double signal(int e) { 
			SimExpt expt = expts.get(e);
			double baseline = expt.background();
			for(SimTranscript trans : overlappingTranscripts(e)) { 
				baseline += trans.intensity(trans.distance(this));
			}
			return expt.noise(Math.log(probeNoise * baseline));
		}
	
		public Collection<SimTranscript> overlappingTranscripts(int e) { 
			ArrayList<SimTranscript> over = new ArrayList<SimTranscript>();
			for(SimTranscript t : transcripts) { 
				if(t.start <= location && t.end >= location && t.present.contains(e)) { 
					over.add(t);
				}
			}
			return over;
		}
	}

}

