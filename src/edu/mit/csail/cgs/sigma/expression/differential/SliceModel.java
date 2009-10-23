/*
 * Author: tdanford
 * Date: Mar 19, 2009
 */
package edu.mit.csail.cgs.sigma.expression.differential;

import java.util.ArrayList;

import edu.mit.csail.cgs.cgstools.singlevarcalculus.FunctionModel;
import edu.mit.csail.cgs.cgstools.singlevarcalculus.binary.SumFunction;
import edu.mit.csail.cgs.cgstools.slicer.UnnormalizedGaussian;
import edu.mit.csail.cgs.sigma.expression.segmentation.Segment;
import edu.mit.csail.cgs.sigma.expression.workflow.models.DataSegment;
import edu.mit.csail.cgs.utils.models.Model;

/**
 * Corresponds to J in the plate diagram.
 */
public class SliceModel extends Model { 

	public TotalModel total;
	public ArrayList<DataSegmentModel> segments;
	
	public Double m;
	
	public SliceModel(TotalModel t) { 
		total = t;
		total.slices.add(this);
		segments = new ArrayList<DataSegmentModel>();
		m = 1.0;
	}
	
	/**
	 * resampling function for m_j
	 */
	public FunctionModel logCondM() { 
		return new SumFunction(new LogProbM(), new LogLikeM());
	}
	
	public class LogProbM extends UnnormalizedGaussian { 
		public LogProbM() { 
			super(total.s, total.msigma);
		}
	}
	
	public class LogLikeM extends FunctionModel { 
		public LogLikeM() {
		}

		public Double eval(Double m_value) { 
			double sum = 0.0;

			for(int i = 0; i < segments.size(); i++) {
				DataSegment seg = segments.get(i).data;
				int channel = segments.get(i).channel.index;

				if(seg.segmentTypes[channel].equals(Segment.LINE)) {
					int base = seg.strand.equals("+") ? seg.end : seg.start;
					double e = segments.get(i).e;
					double var = segments.get(i).ysigma;
					
					for(int j = 0; j < seg.dataLocations.length; j++) { 
						int delta = Math.abs(seg.dataLocations[j] - base);
						double value = seg.dataValues[channel][j];

						double mean = e + m_value * (double)delta;
						double diff = value-mean;
						diff *= diff;
						diff /= (2.0 * var);
						
						sum += diff;
					}
				}
			}
			
			return sum; 
		}
	}
}
