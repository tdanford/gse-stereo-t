/*
 * Author: tdanford
 * Date: Apr 15, 2009
 */
package edu.mit.csail.cgs.sigma.expression.normalization;

import java.util.*;

import edu.mit.csail.cgs.sigma.IteratorCacher;
import edu.mit.csail.cgs.sigma.expression.workflow.models.ProbeLine;
import edu.mit.csail.cgs.utils.Closeable;
import edu.mit.csail.cgs.utils.models.Model;

public class BolstadEMBackgroundEstimation implements BackgroundEstimation<BolstadModel> {

	public BolstadEMBackgroundEstimation() { 
	}

	public BolstadModel estimateModel(Iterator<ProbeLine> ps, int channel) {
		Double[] values = extractValues(ps, channel);
		BolstadParams bps = estimate(values);
		System.out.println("Final: " + bps.toString());
		return bps.model();
	}
	
	private Double[] extractValues(Iterator<ProbeLine> ps, int channel) {
        ArrayList<Double> valuelist = new ArrayList<Double>();
        while(ps.hasNext()) { 
            ProbeLine p = ps.next();
            if(p.isValidValue(channel) && p.values[channel] > 0.0) { 
                valuelist.add(p.values[channel]);
            }
        }
        System.out.println(String.format("Extracted %d values.", valuelist.size()));
        return valuelist.toArray(new Double[0]);
	}

	private BolstadParams estimate(Double[] s) { 
		BolstadParams bp = new BolstadParams(s);
		
		Double[] x = new Double[s.length];
		for(int i = 0; i < x.length; i++) { 
			x[i] = 0.0;
		}
		
		double llDiff = 1.0;
		int maxIter = 1000;
		double diffToler = 1.0e-6;
		double lastLL =	bp.expectation(s, x);
		
		for(int i = 0; i < maxIter /*&& llDiff < diffToler*/; i++) {
			double ll = bp.expectation(s, x);
			bp.maximize(s, x);

			llDiff = ll - lastLL;
			lastLL = ll;
			
			System.out.println(String.format("[%d]: + %f", i, llDiff));
			System.out.println(String.format("\tLL: %f", ll));
			System.out.println(String.format("\t%s", bp.toString()));
		}

		return bp;
	}
	
	public static class BolstadParams extends Model { 
		
		public Double alpha, mean, var;
		
		public BolstadParams() { 
			alpha = 1.0;
			mean = 1.0;
			var = 1.0;
		}
		
		public BolstadParams(Double[] s) { 
			alpha = 0.0;
			mean = 0.0;
			var = 0.0;
			
			for(int i = 0; i < s.length; i++) {
				mean += s[i];
			}
			mean /= (double)s.length;
			
			for(int i = 0; i < s.length; i++) {
				double diff = s[i] - mean;
				if(s[i] > mean) { 
					alpha += diff;
				} else { 
					var += (diff*diff);
				}
			}
			
			alpha = 1.0 / (alpha / (double)s.length);
			var /= (double)s.length;
		}
		
		public Double[] params() { return new Double[] { alpha, mean, Math.sqrt(var) }; }
		
		public BolstadModel model() { 
			return new BolstadModel(alpha, mean, Math.sqrt(var)); 
		}
		
		public void maximize(Double[] s, Double[] x) {
			
			double meanSum = 0.0;
			double alphaSum = 0.0;
			
			for(int i = 0; i < x.length; i++) { 
				double y = s[i] - x[i];
				alphaSum += y;
				meanSum += x[i];
			}
			
			alphaSum /= (double)x.length;
			alpha = 1.0 / alphaSum;

			meanSum /= (double)x.length;
			mean = meanSum;
			
			double varSum = 0.0;
			for(int i = 0; i < x.length; i++) { 
				double d = x[i] - mean;
				d *= d;
				varSum += d;
			}
			varSum /= (double)(x.length - 1);
			var = varSum;
		}
		
		public String toString() { 
			return String.format("BolstadParams: [alpha=%f, mean=%f, std=%f]",
					alpha, mean, Math.sqrt(var));
		}
		
		public double expectation(Double[] s, Double[] x) {
			BolstadModel model = model();
			double ll = 0.0;
			
			for(int i = 0; i < s.length; i++) { 
				double expx = model.expectedX(s[i]);
				//System.out.println(String.format("%f (%f) -> %f", s[i], x[i], expx));
				x[i] = Math.max(expx, 0.0);
				double y = s[i] - x[i];
				
				if(x[i] < 0.0 || y < 0.0) { 
					System.err.println(String.format("%d: s=%f, x=%f, y=%f", i, s[i], x[i], y));
				}
				
				ll += logNormalLikelihood(x[i], mean, var);
				ll += logExponentialLikelihood(y, alpha);
			}
			
			return ll;
		}
	}
	
	public static double log2 = Math.log(2.0);
	public static double logPi = Math.log(Math.PI);
	
	public static double logNormalLikelihood(double x, double mean, double var) { 
		double d = (x - mean); 
		double expt = -(d * d) / (2.0 * var);
		//double logc = -0.5 * (log2 + logPi + Math.log(var));
		double logc = -0.5 * Math.log(var);
		return expt + logc;
	}
	
	public static double logExponentialLikelihood(double x, double alpha) { 
		return Math.log(alpha) - (alpha * x);
	}
}
