/*
 * Author: tdanford
 * Date: Apr 27, 2009
 */
package edu.mit.csail.cgs.sigma.expression.simulation;

import java.util.*;
import java.io.*;

import edu.mit.csail.cgs.sigma.expression.workflow.WorkflowProperties;
import edu.mit.csail.cgs.sigma.expression.workflow.models.ProbeLine;

public class SimpleSimulator implements ProbeSimulator {
	
	public static void main(String[] args) {
		
		String name = args.length > 0 ? args[0] : "sim1";
		
		SimParameters params = new SimParameters(name);
		
		SimpleSimulator wsim = new SimpleSimulator(params, true);
		SimpleSimulator csim = new SimpleSimulator(params, false);
		
		WorkflowProperties props = new WorkflowProperties();
		File woutput = new File(props.getDirectory(), 
				String.format("test_plus.data"));
		File coutput = new File(props.getDirectory(), 
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

	private double falloff;
	
	private Random rand;
	private String chrom, strand;
	
	private ArrayList<SimProbe> probes;
	private ArrayList<SimTranscript> transcripts;
	private ArrayList<SimExpt> expts;

	public SimpleSimulator(SimParameters params, boolean str) {
		chrom = params.chrom;
		strand = str ? "+" : "-";
		falloff = 0.001;
		rand = new Random();
		probes = new ArrayList<SimProbe>();
		transcripts = new ArrayList<SimTranscript>();
		expts = new ArrayList<SimExpt>();

		for(SimParameters.SimExpt expt : params.expts) {
			addExpt(expt.additive, expt.scaling);
		}
		
		int pwidth = (int)Math.round((double)(params.end - params.start) / (double)(params.probes+1));
		
		for(int i = 0; i < params.probes; i++) { 
			addProbe(params.start + (i+1) * pwidth);
		}
		
		for(SimParameters.SimTranscript st : (str ? params.plus : params.minus)) { 
			addTranscript(st.start, st.end, st.intensity);
		}
	}
	
	public void save(File f) throws IOException { 
		PrintStream ps = new PrintStream(new FileOutputStream(f));
		save(ps);
		ps.close();
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.csail.cgs.sigma.expression.simulation.ProbeSimulator#probes()
	 */
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
	
	public void addTranscript(int s, int e, double i, Integer... ps) { 
		transcripts.add(new SimTranscript(s, e, i, ps));
	}
	
	public void addTranscript(int s, int e, double i) { 
		transcripts.add(new SimTranscript(s, e, i));
	}
	
	public void addExpt(double add, double scal) { 
		expts.add(new SimExpt(add, scal));
	}
	
	private class SimExpt { 
		
		public Double additive;
		public Double scale;
		public Double noise;
		
		public SimExpt() { 
			this(0.0, 1.0);
		}
		
		public SimExpt(double a, double s) { 
			additive = a;
			scale = s;
			noise = 0.5;
		}
		
		public double background() { 
			return Math.abs(rand.nextGaussian() * 0.5) + 1.0;
		}
		
		public double signal(double input) { 
			return additive + (input * scale);
		}
		
		public double noise(double input) { 
			return Math.abs(input + rand.nextGaussian() * noise);
		}
	}
	
	private class SimTranscript { 

		public Double intensity;
		public Integer start, end;
		public Set<Integer> present; // expts
		
		public SimTranscript(int s, int e, double i) { 
			start = s; 
			end = e; 
			intensity = i;
			present = new TreeSet<Integer>();
			for(int k = 0; k < expts.size(); k++) { present.add(k); }
		}
		
		public SimTranscript(int s, int e, double i, Integer... ps) {  
			start = s; 
			end = e; 
			intensity = i;
			present = new TreeSet<Integer>();
			for(int k = 0; k < ps.length; k++) { 
				present.add(ps[k]);
			}
		}
		
		public int distance(SimProbe p) {
			return end - p.location;
		}
		
		public double intensity(int distance) { 
			return intensity - falloff * distance;
		}
	}

	private class SimProbe { 
		
		public Integer location;
		
		public SimProbe(int l) { 
			location = l;
		}
		
		public double signal(int e) { 
			SimExpt expt = expts.get(e);
			double baseline = expt.background();
			for(SimTranscript trans : overlappingTranscripts(e)) { 
				baseline += trans.intensity(trans.distance(this));
			}
			return expt.noise(expt.signal(baseline));
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

