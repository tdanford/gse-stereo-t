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

/**
 * Estimates just a single value (the difference in means), and returns a model 
 * which shifts all values down to that level.
 * 
 * @author tdanford
 *
 */
public class ShiftedBackgroundEstimation implements BackgroundEstimation<UniformModel> {
	
	private double baseline;
	
	public ShiftedBackgroundEstimation(double base) {
		baseline = base;
	}

	public UniformModel estimateModel(Iterator<ProbeLine> ps, int channel) {
		Double[] values = extractValues(ps, channel);
		Double mean = mean(values);
		Double diff = mean - baseline;
		return new UniformModel(-diff);
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
}
