/*
 * Author: tdanford
 * Date: Oct 29, 2008
 */
package edu.mit.csail.cgs.sigma.expression.segmentation.fitters;

import edu.mit.csail.cgs.sigma.expression.segmentation.InputData;
import edu.mit.csail.cgs.sigma.expression.segmentation.Segment;
import edu.mit.csail.cgs.utils.probability.NormalDistribution;


/**
 * @see edu.mit.csail.cgs.sigma.expression.segmentation.fitters.SegmentFitter
 * @author tdanford
 *
 */
public class ExponentialFitter implements SegmentFitter {
	
	public ExponentialFitter() { 
	}
	
	/**
	 * 
	 * @return the number of parameters. Here is 2: mean and variance.
	 */
	public int numParams() { return 1; }

	/**
	 *  
	 * @param j1 index of the probe representing the start of the region of interest (not its genomic coordinate)
	 * @param j2 index of the probe representing the end of the region of interest (not its genomic coordinate)
	 * @param locations locations of the probes
	 * @param values values of the probes. Rows represent different channels.
	 * @return the mean and variance of the region of interest.
	 */
	public Double[] fit(int j1, int j2, InputData data, Integer[] channels){
		double sum = 0.0;
		int count = 0;
		Double[][] values = data.values();
		
		for(int k = 0; k < channels.length; k++) { 
			int ch = channels[k];
			if(ch < 0 || ch >= values.length) { 
				for(int k1 = 0; k1 < channels.length; k1++) { 
					System.err.print(channels[k1] + " ");
				}
				System.err.println();
				throw new IllegalArgumentException(String.format("%d not in [0, %d)", 
						ch, values.length));
			}
		}
		
		for(int i = j1; i < j2; i++) { 
			for(int k = 0; k < channels.length; k++) { 
				int channel = channels[k];
				/*
				if(values[channel][i] == null) { 
					throw new IllegalArgumentException(String.format("%d,%d is null", 
							channel, i));
				}
				 */
				if(values[channel][i] != null) { 
					sum += values[channel][i];
					count += 1;
				}
			}
		}

		double mean = sum / (double)Math.max(count, 1);
		sum = 0.0;
		for(int i = j1; i < j2; i++) {
			for(int k = 0; k < channels.length; k++) { 
				int ch = channels[k];
				if(values[ch][i] != null) { 
					double diff = values[ch][i] - mean;
					sum += (diff*diff);
				}
			}
		}
		
		double var = sum / (double)Math.max(count, 1);
		return new Double[] { mean, var };
	}
    

	/**
	 * 
	 * @param j1 index of the probe representing the start of the region of interest (not its genomic coordinate)
	 * @param j2 index of the probe representing the end of the region of interest (not its genomic coordinate)
	 * @param params the parameters of this region: <tt>params[0]=mean</tt>, <tt>params[1]=variance</tt> 
	 * @param locations locations of the probes
	 * @param values values of the probes. Rows represent different channels.
	 * @return The probability score of the region of interest under the parameters of the used model
	 */
	public Double score(int j1, int j2, Double[] params, InputData data, Integer[] channels) {
		Double[][] values = data.values();
		double lambda = params[0];
		double loglambda = Math.log(lambda);
		double sum = 0.0;
		
		for(int j = j1; j < j2; j++) {
			for(int k = 0; k < channels.length; k++) { 
				int ch = channels[k];
				Double value = values[ch][j];
				if(value != null) { 
					double logp = loglambda - lambda * value;
					sum += logp;
				}
			}
		}
		
		return sum;
	}

	public int type() {
		return Segment.FLAT;
	} 
}
