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

public class UniformBackgroundEstimation implements BackgroundEstimation<UniformModel> {
	
	private Double lambda;
	
	public UniformBackgroundEstimation() { 
		this(1.0);
	}

	public UniformBackgroundEstimation(double l) {
		lambda = l;
	}

	public UniformModel estimateModel(Iterator<ProbeLine> ps, int channel) {
		Double[] values = extractValues(ps, channel);
		Double mean = mean(values);
		double[] b = bounds(values);
		System.out.println(String.format("\t-> Mean Value: %.5f", mean));		
		System.out.println(String.format("\t-> Min/Max Value: %.5f / %.5f", b[0], b[1]));		
		UniformParams bps = estimate(values);
		System.out.println("\t-> Estimated: " + bps.toString());
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

	private UniformParams estimate(Double[] s) { 
		UniformParams bp = new UniformParams(s);
		return bp;
	}
	
	public static double mean(Double[] s) { 
		return subtractedMean(s, 0.0);
	}
	
	public static double subtractedMean(Double[] s, Double baseline) { 
		double sum = 0.0;
		for(int i = 0; i < s.length; i++) { 
			sum += (s[i] - baseline);
		}
		return sum / (double)s.length;
	}
	
	public static double[] bounds(Double[] s) { 
		Double min = null, max = null;
		for(int i = 0; i < s.length; i++) { 
			if(min == null) { 
				min = max = s[i];
			} else { 
				min = Math.min(min, s[i]);
				max = Math.max(max, s[i]);
			}
		}
		return new double[] { min, max };
	}
	
	private class UniformParams extends Model { 
		
		public Double alpha, x;
		
		public UniformParams(Double[] s) {
			Double sbar = mean(s);
			alpha = lambda / (double)s.length;
			x = sbar - 1.0 / alpha;
		}
		
		public Double[] params() { return new Double[] { alpha, x }; }
		
		public UniformModel model() { 
			return new UniformModel(x);
		}
		
		public String toString() { 
			return String.format("UniformParams: [alpha=%f, X=%f]",
					alpha, x);
		}
	}
}
