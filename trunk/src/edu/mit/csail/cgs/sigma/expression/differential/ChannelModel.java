/*
 * Author: tdanford
 * Date: Mar 19, 2009
 */
package edu.mit.csail.cgs.sigma.expression.differential;

import java.util.ArrayList;

import edu.mit.csail.cgs.cgstools.singlevarcalculus.FunctionModel;
import edu.mit.csail.cgs.cgstools.singlevarcalculus.binary.SumFunction;
import edu.mit.csail.cgs.cgstools.slicer.UnnormalizedGaussian;
import edu.mit.csail.cgs.utils.models.Model;

/**
 * Corresponds to I in the plate diagram.
 */
public class ChannelModel extends Model { 

	public Integer index;  // the index of the channel itself (i.e., dataValues[idx])
	public TotalModel total;
	
	public ArrayList<DataSegmentModel> segments;
	
	public Double n;
	
	public ChannelModel(TotalModel m, int idx) {
		index = idx;
		total = m;
		total.channels.add(this);
		segments = new ArrayList<DataSegmentModel>();
		n = 1.0;
	}
	
	/**
	 * resampling function for the n_i parameter.
	 */
	public FunctionModel logCondN() { 
		return new SumFunction(new LogProbN(), 
				SumFunction.add(buildLogEProbs()));
	}
	
	public class LogProbN extends UnnormalizedGaussian { 
		public LogProbN() { 
			super(total.g, total.nsigma);
		}
	}
	
	public class LogLikelihoodN extends FunctionModel {
		private int idx;

		public LogLikelihoodN(int i) {
			idx = i;
		}
		
		public Double eval(Double n_value) { 
			double diff = segments.get(idx).e - n_value;
			diff *= diff;
			return -diff / (2.0 * total.esigma);
		}
	}
	
	public FunctionModel[] buildLogEProbs() { 
		FunctionModel[] f = new FunctionModel[segments.size()];
		for(int i = 0; i < f.length; i++) { 
			f[i] = new LogLikelihoodN(i);
		}
		return f;
	}
}
