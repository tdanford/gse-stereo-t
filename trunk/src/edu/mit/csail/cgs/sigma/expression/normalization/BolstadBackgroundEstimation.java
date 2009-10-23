/*
 * Author: tdanford
 * Date: Apr 15, 2009
 */
package edu.mit.csail.cgs.sigma.expression.normalization;

import java.util.*;

import edu.mit.csail.cgs.sigma.IteratorCacher;
import edu.mit.csail.cgs.sigma.expression.workflow.models.ProbeLine;
import edu.mit.csail.cgs.utils.Closeable;

public class BolstadBackgroundEstimation implements BackgroundEstimation<BolstadModel> {

	public BolstadBackgroundEstimation() { 
	}

	public BolstadModel estimateModel(Iterator<ProbeLine> ps, int channel) {
		Double[] values = extractValues(ps, channel);
		double mu = estimateMean(values);
		double alpha = estimateUpperTailExponential(values, mu);
		double sigma = estimateLowerTailStdev(values, mu);
		return new BolstadModel(alpha, mu, sigma);
	}
	
	private Double[] extractValues(Iterator<ProbeLine> ps, int channel) {
        ArrayList<Double> valuelist = new ArrayList<Double>();
        while(ps.hasNext()) { 
            ProbeLine p = ps.next();
            Double val = p.values[channel];
            if(val != null) { 
                valuelist.add(val);
            }
        }
        return valuelist.toArray(new Double[0]);
	}

	private double estimateMean(Double[] values) { 
		double sum = 0.0;
		int c = 0;
		for(int i = 0; i < values.length; i++) { 
			if(values[i] != null) { 
				c += 1;
				sum += values[i];
			}
		}
		return sum / (double)c;
	}
	
	private double estimateLowerTailStdev(Double[] values, Double mean) { 
		double sum = 0.0;
		int c = 0;
		
		for(int i = 0; i < values.length; i++) { 
			if(values[i] != null && values[i] < mean) { 
				c += 1;
				double diff = (values[i] - mean);
				sum += (diff*diff);
			}
		}

		double var = sum / (double)c;
		double stdev = Math.sqrt(var);
		return stdev;
	}
	
	private double estimateUpperTailExponential(Double[] values, Double mean) {
		double sum = 0.0;
		int c = 0;
		
		for(int i = 0; i < values.length; i++) { 
			if(values[i] != null && values[i] > mean) { 
				c += 1;
				sum += values[i];
			}
		}
		
		double m = sum / (double)c;
		return 1.0 / m;
	}
}
