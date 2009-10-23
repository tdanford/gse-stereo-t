/*
 * Author: tdanford
 * Date: Jun 15, 2009
 */
package edu.mit.csail.cgs.sigma.expression.transcription.recursive;

import java.util.*;
import java.awt.Color;
import java.io.*;

import edu.mit.csail.cgs.cgstools.singlevarcalculus.*;
import edu.mit.csail.cgs.cgstools.singlevarcalculus.binary.DifferenceFunction;
import edu.mit.csail.cgs.sigma.expression.transcription.TranscriptArrangement;
import edu.mit.csail.cgs.viz.paintable.Paintable;
import edu.mit.csail.cgs.viz.paintable.PaintableFrame;
import edu.mit.csail.cgs.viz.paintable.PaintableScale;
import edu.mit.csail.cgs.viz.paintable.layout.StackedPaintable;

public class ClusterLikelihoods {

	private ClusterData data;
	private Integer[] channels;
	
	private double[][] gammas;
	private int[] fiveprime, threeprime;
	private double variance, lambda;
	private Map<Integer,Set<Integer>> transcripts;
	
	public ClusterLikelihoods(ClusterData d, Integer[] chs, int[] five, int[] three, double v, double l) {
		data = d;
		channels = chs.clone();
		
		gammas = new double[five.length][channels.length];
		for(int t = 0; t < gammas.length; t++) { 
			for(int i = 0; i < gammas[t].length; i++) { 
				gammas[t][i] = 1.0;
			}
		}
		
		transcripts = new TreeMap<Integer,Set<Integer>>();
		fiveprime = five;
		threeprime = three;
		
		if(fiveprime.length != threeprime.length) {
			String msg = String.format("%d != %d", fiveprime.length, threeprime.length);
			throw new IllegalArgumentException(msg);
		}
		
		variance = v;
		lambda = l;
		
		for(int t = 0; t < fiveprime.length; t++) { 
			transcripts.put(t, new TreeSet<Integer>());
		}
		
		for(int i =0; i < data.size(); i++) {
			int loc = data.location(i);
			for(int t = 0; t < fiveprime.length; t++) {
				if(fiveprime[t] <= loc && threeprime[t] >= loc) { 
					transcripts.get(t).add(i);
				}
			}
		}
	}
	
	public FunctionModel getLGammaFunction(int t, int k) { 
		return new LGamma(t, k);
	}
	
	public FunctionModel getLLambdaFunction() { 
		return new LLambda();
	}
	
	public FunctionModel getDLGammaFunction(int t, int k) { 
		return new DLGamma(t, k);
	}
	
	public FunctionModel getDLLambdaFunction() { 
		return new DLLambda();
	}
	
	public double delta(int i, int t) {
		int diff = threeprime[t] - data.location(i);
		return diff >= 0 ? (double)diff : 0.0;
	}
	
	public void setTranscriptBounds(int t, int i1, int i2) { 
		for(int i = i1; i < i2; i++) { 
			transcripts.get(i).add(t);
		}
	}
	
	public double[][] gammas() { return gammas; } 

	public void optimize() {
		boolean iterating = true;
		double eps = 0.01;
		int iters = 0;
		
		FunctionModel dllambda = new DLLambda();
		
		while(iterating) {
			
			iters += 1;
			//System.out.print(String.format("%d:", iters));
			iterating = false;

			for(int t = 0; t < gammas.length; t++) {
				for(int k = 0; k < gammas[t].length; k++) {
					FunctionModel m = new DLGamma(t, k);
					double z = findZero(m, 1.0, eps);
					z = Math.max(z, 0.001);
					double d = z - gammas[t][k];
					iterating = iterating || Math.abs(d) >= eps;
					gammas[t][k] = z;
					//System.out.print(String.format(" g%d=%.5f (%.5f)", t, gammas[t], Math.log(gammas[t])));
				}
			}
			
			double lz = findZero(dllambda, 1.0e-5, 1.0e-6);
			double dz = lz - lambda;
			lambda = lz;
			//System.out.println(String.format(", lmbda=%f", lambda));
		}
	}

	public static double findZero(FunctionModel f, double lowerBound) {
		return findZero(f, lowerBound, 1.0e-6);
	}
	
	public static double[] findZeroBrackets(FunctionModel f, double start) { 
		double fstart = f.eval(start);
		
		if(Double.isInfinite(fstart) || Double.isNaN(fstart)) { 
			throw new IllegalArgumentException(String.format(
					"Illegal start: %f (%f)", start, f.eval(start)));
		}
		
		boolean sign = fstart >= 0.0;
		double lowerSpace = 1.0, upperSpace = 1.0;
		double upper = start+upperSpace, lower = start-lowerSpace;
		double pupper = start, plower = start;
		
		double fupper = f.eval(upper), flower = f.eval(lower);
		boolean usign = fupper >= 0.0, lsign = flower >= 0.0;
		int iters = 0;

		while((isBad(fupper) || usign==sign) && (isBad(flower) || lsign == sign)) { 
			iters += 1;
			
			if(isBad(fupper)) {
				upperSpace /= 2.0;
				upper -= upperSpace;
			} else { 
				upperSpace *= 2.0;
				upper += upperSpace;
			}

			fupper = f.eval(upper);
			usign = fupper >= 0.0;
			
			if(isBad(flower)) { 
				lowerSpace /= 2.0;
				lower += lowerSpace;
			} else { 
				lowerSpace *= 2.0;
				lower -= lowerSpace;
			}
			
			flower = f.eval(lower);
			lsign = flower >= 0.0;
		}
		
		if(usign != sign) {
			//System.out.println(String.format("\n%f -> (%f, %f]", start, start, upper));
			return new double[] { start, upper }; 
		}
		
		if(lsign != sign) { 
			//System.out.println(String.format("\n%f -> [%f, %f)", start, lower, start));
			return new double[] { lower, start }; 
		}
		
		throw new IllegalArgumentException("Couldn't find zero brackets!");
	}
	
	public static boolean isBad(double d) { return Double.isNaN(d) || Double.isInfinite(d); }

	public static double findZero(FunctionModel f, double start, double eps) {
		
		double[] bounds = findZeroBrackets(f, start);
		double lower = bounds[0], upper = bounds[1];
		double ly = f.eval(lower), uy = f.eval(upper);
		
		//System.out.println(String.format("Zero: [%f, %f] (%f, %f)", lower, upper, ly, uy));
		
		boolean lsign = ly < 0.0;
		boolean usign = uy <= 0.0;
		
		while(Math.abs(upper-lower) > eps) { 
			double middle = (upper+lower)/2.0;
			double my = f.eval(middle);
			boolean msign = my < 0.0;

			if(my == 0.0) { 
				return middle; 
			} else if (msign == lsign) {
				lower = middle;
				ly = my;
				lsign = msign;
				//System.out.println(String.format("\tU: [%f, %f] (%f, %f)", lower, upper, ly, uy));				
			} else { 
				upper = middle;
				uy = my;
				usign = msign;
				//System.out.println(String.format("\tL: [%f, %f] (%f, %f)", lower, upper, ly, uy));				
			}
		}
		
		double v = (lower+upper)/2.0;
		//System.out.println(String.format("\t-> %f", v));
		return v; 
	}
	
	public double error() { 
		double sum = 0.0;

		for(int i = 0; i < data.size(); i++) {
			for(int k = 0; k < channels.length; k++) { 
				double pi = 0.0;

				for(Integer t : transcripts.keySet()) { 
					if(transcripts.get(t).contains(i)) { 
						double gammai = gammas[t][k];
						double dit = delta(i, t);

						double pit = gammai * Math.exp(-lambda * dit);
						pi += pit;					
					}
				}

				double zi = Math.log(pi);
				double err = Math.abs(data.values(i)[channels[k]] - zi);

				sum += err;
			}
		}

		return sum;				
	}

	public double likelihood() { 
		double c1 = 0.0;
		double c3 = -1.0 / (double)Math.sqrt(2.0 * variance);
		double sum = 0.0;

		double cpart = -0.5 * Math.log(Math.sqrt(2.0 * Math.PI * variance));

		for(int i = 0; i < data.size(); i++) {
			for(int k = 0; k < channels.length; k++) {
				c1 += cpart;
				double pi = 0.0;

				for(Integer t : transcripts.keySet()) { 
					if(transcripts.get(t).contains(i)) { 
						double gammai = gammas[t][k];
						double dit = delta(i, t);

						double pit = gammai * Math.exp(-lambda * dit);
						pi += pit;					
					}
				}

				double zi = Math.log(pi);
				double err = data.values(i)[channels[k]] - zi;

				sum += (err*err);
			}
		}

		return c1 + c3 * sum;		
	}

	public class LGamma extends FunctionModel {
		
		private int gammat, gammak;
		
		public LGamma(int gt, int gk) { 
			gammat = gt;
			gammak = gk;
		}

		public Double eval(Double gamma) {
			double c1 = 0.0;
			double c3 = -1.0 / (double)Math.sqrt(2.0 * variance);
			double sum = 0.0;
			
			double cpart = -0.5 * Math.log(Math.sqrt(2.0 * Math.PI * variance));
		
			for(Integer i : transcripts.get(gammat)) { 
				c1 += cpart;
				double pi = 0.0;

				for(int t = 0; t < fiveprime.length; t++) { 
					if(transcripts.get(t).contains(i)) { 
						double gammai = gammat == t ? gamma : gammas[t][gammak];
						double dit = delta(i, t);

						double pit = gammai * Math.exp(-lambda * dit);
						pi += pit;
					}
				}

				double zi = Math.log(pi);
				double err = data.values(i)[channels[gammak]] - zi;

				sum += (err*err);
			}
			
			return c1 + c3 * sum;
		} 
	}

	public class DLGamma extends FunctionModel {
		
		private int gammat, gammak;
		
		public DLGamma(int gt, int gk) { 
			gammat = gt;
			gammak = gk;
		}

		public Double eval(Double gamma) {
			double c = 1.0 / (double)Math.sqrt(variance);
			double sum = 0.0;
			for(Integer i : transcripts.get(gammat)) { 
				double pi = 0.0;

				for(int t = 0; t < fiveprime.length; t++) { 
					if(transcripts.get(t).contains(i)) { 
						double gammai = gammat == t ? gamma : gammas[t][gammak];
						double dit = delta(i, t);

						double pit = gammai * Math.exp(-lambda * dit);
						pi += pit;						
					}
				}

				double zi = Math.log(pi);
				double err = data.values(i)[channels[gammak]] - zi;
				double ratio = (Math.exp(-lambda * delta(i, gammat))) / pi;
				double termi = (err * ratio);

				sum += termi;
			}

			return c * sum;
		} 
	}

	public class LLambda extends FunctionModel {
		
		public LLambda() { 
		}

		public Double eval(Double lmbda) {
			double c1 = 0.0;
			double c3 = -1.0 / (double)Math.sqrt(2.0 * variance);
			double sum = 0.0;

			double cpart = -0.5 * Math.log(Math.sqrt(2.0 * Math.PI * variance));

			for(int i = 0; i < data.size(); i++) { 
				for(int k = 0; k < channels.length; k++) { 
					c1 += cpart;
					double pi = 0.0;

					for(Integer t : transcripts.keySet()) { 
						if(transcripts.get(t).contains(i)) { 
							double dit = delta(i, t);
							double gammai = gammas[t][k];
							double pit = gammai * Math.exp(-lmbda * dit);
							pi += pit;								
						}
					}

					double zi = Math.log(pi);
					double err = data.values(i)[channels[k]] - zi;

					sum += (err*err);
				}
			}

			return c1 + c3 * sum;
		} 
	}

	public class DLLambda extends FunctionModel {
		
		public DLLambda() {}
		
		public Double eval(Double lmbda) {
			double c = 1.0 / (double)Math.sqrt(variance);
			double sum = 0.0;

			for(int i = 0; i < data.size(); i++) {
				for(int k = 0; k < channels.length; k++) { 
					double pi = 0.0;
					double pdi = 0.0;

					for(Integer t : transcripts.keySet()) { 
						if(transcripts.get(t).contains(i)) { 
							double gammai = gammas[t][k];
							double dit = delta(i, t);

							double falloff = Math.exp(-lmbda * dit);
							double pit = gammai * falloff;
							double pdit = pit * dit;

							pi += pit;
							pdi += pdit;						
						}
					}

					double zi = Math.log(pi);
					double err = data.values(i)[channels[k]] - zi;
					double ratio = pdi / pi;
					double termi = (err * ratio);

					sum += termi;
				}
			}
			
			return c * sum;
		} 
	}
}
