/*
 * Author: tdanford
 * Date: Jun 16, 2009
 */
package edu.mit.csail.cgs.sigma.expression.transcription.recursive;

import java.util.*;

public class TestData {
	
	private Random rand;

	private int channels;
	private double lambda; 
	private Map<Integer,ArrayList<Double>> gammas;
	private ArrayList<int[]> transcripts;
	
	private int[] x;
	private double[][] y;
	
	private double noise;
	
	public TestData(int[] x, double n) {
		this(x, n, 1);
	}
	
	public TestData(int[] x, double n, int ch) { 
		this.x = x.clone();
		Arrays.sort(this.x);
		channels = ch;
		y = new double[channels][x.length];
		if(x.length != y[0].length) { throw new IllegalArgumentException(); }
		noise = n;
		rand = new Random();
		lambda = 0.01;
		gammas = new TreeMap<Integer,ArrayList<Double>>();
		transcripts = new ArrayList<int[]>();
	}
	
	public void addTranscript(double g, int t1, int t2) {
		int t = transcripts.size();
		gammas.put(t, new ArrayList<Double>());
		transcripts.add(new int[] { t1, t2 });
		for(int j = 0; j < channels; j++) { 
			gammas.get(t).add(g);
		}
		if(t1 > t2) { throw new IllegalArgumentException(); }
	}
	
	public void setTranscriptLevel(int t, int j, double g) { 
		gammas.get(t).set(j, g);
	}
	
	public void addTranscript(double[] gs, int t1, int t2) {
		if(gs.length != channels) { 
			throw new IllegalArgumentException();
		}
		int t = transcripts.size();
		gammas.put(t, new ArrayList<Double>());
		transcripts.add(new int[] { t1, t2 });
		for(int j = 0; j < channels; j++) { 
			gammas.get(t).add(gs[j]);
		}
		if(t1 > t2) { throw new IllegalArgumentException(); }
	}
	
	public void generate() {
		for(int j = 0; j < channels; j++) { 
			for(int i = 0; i < y[j].length; i++) {
				//double sum = Math.abs(rand.nextGaussian());
				double sum = 1.0;

				//System.out.print(String.format("p%d", i));
				for(int t = 0; t < gammas.size(); t++) { 
					int[] bounds = transcripts.get(t);
					if(bounds[0] <= x[i] && bounds[1] >= x[i]) { 
						double delta = (double)(bounds[1] - x[i]);
						double gamma = gammas.get(t).get(j);
						double value = gamma * Math.exp(-lambda * delta);
						sum += value;
						//System.out.print(String.format(" %d:%.2f", t, value));
					}
				}
				//System.out.println();
				y[j][i] = Math.log(sum) + rand.nextGaussian()*noise;
			}
		}
	}
	
	public int[] x() { return x; }
	public double[][] y() { return y; }
	public double lambda() { return lambda; }
	
	public double[] gammas(int j) { 
		double[] g = new double[gammas.size()];
		for(int t = 0; t < transcripts.size(); t++) { 
			g[t] = gammas.get(t).get(j);
		}
		return g;
	}
	
	public int[] threePrime() { 
		int[] th = new int[transcripts.size()];
		for(int t = 0; t < th.length; t++) { 
			th[t] = transcripts.get(t)[1];
		}
		return th;
	}
	
	public int getNumChannels() { return channels; }
	public int getNumTranscripts() { return transcripts.size(); }
	public double gamma(int t, int j) { return gammas.get(t).get(j); }
	public int[] bounds(int t) { return transcripts.get(t); }

	public double noise() {
		return noise;
	}

	public int[] indexBounds(int t) {
		int[] b = new int[] { x.length+1, -1 };
		int[] bounds = transcripts.get(t);
		for(int i = 0; i < x.length; i++) {
			if(bounds[0] <= x[i] && bounds[1] >= x[i]) { 
				b[0] = Math.min(i, b[0]);
				b[1] = Math.max(i, b[1]);
			}
		}
		return b;
	}
}
