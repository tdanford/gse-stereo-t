/*
 * Author: tdanford
 * Date: Jun 3, 2009
 */
package edu.mit.csail.cgs.sigma.expression.segmentation.fitters;

import edu.mit.csail.cgs.sigma.expression.segmentation.InputData;
import edu.mit.csail.cgs.sigma.expression.segmentation.Segment;

public class SteinmetzFitter implements SegmentFitter {
	
	private Integer[] defaultChannels = new Integer[] { 0 };
	
	public SteinmetzFitter() {
	}

	public Double[] fit(int j1, int j2, InputData data, Integer[] channels) {
		if(channels == null) { channels = defaultChannels; }  
		double sum = 0.0;
		int c = 0;
		
		Double[][] values = data.values();
		for(int k = 0; k < channels.length; k++) { 
			for(int j = j1; j < j2; j++) { 
				Double value = values[k][j];
				if(value != null) { 
					sum += value;
					c += 1;
				}
			}
		}
		
		double mean = sum / (double)Math.max(1, c);
		
		sum = 0.0;
		c = 0;
		
		for(int k = 0; k < channels.length; k++) { 
			for(int j = j1; j < j2; j++) { 
				Double value = values[k][j];
				if(value != null) {
					double diff = value - mean;
					sum += (diff*diff);
					c += 1;
				}
			}
		}
		
		double var = mean / (double)Math.max(c, 1);
		
		return new Double[] { mean, var };
	}

	public int numParams() {
		return 2;
	}

	public Double score(int j1, int j2, Double[] params, InputData data, Integer[] channels) {
		if(channels == null) { channels = defaultChannels; }  
		Double mean = params[0], var = params[1];
		return Math.log(var);
	}

	public int type() {
		return Segment.LINE;
	} 
}


