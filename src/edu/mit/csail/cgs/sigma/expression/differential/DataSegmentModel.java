/*
 * Author: tdanford
 * Date: Mar 17, 2009
 */
package edu.mit.csail.cgs.sigma.expression.differential;

import java.util.*;
import java.lang.reflect.*;

import edu.mit.csail.cgs.cgstools.singlevarcalculus.Value;
import edu.mit.csail.cgs.cgstools.singlevarcalculus.FunctionModel;
import edu.mit.csail.cgs.cgstools.singlevarcalculus.VariableFunction;
import edu.mit.csail.cgs.cgstools.singlevarcalculus.binary.SumFunction;
import edu.mit.csail.cgs.cgstools.slicer.*;
import edu.mit.csail.cgs.sigma.expression.segmentation.Segment;
import edu.mit.csail.cgs.sigma.expression.workflow.models.DataSegment;
import edu.mit.csail.cgs.utils.models.Model;
import edu.mit.csail.cgs.utils.models.ModelFieldAnalysis;

/*
 * y_ijk = e_ij + l_ij * m_j * x_jk + yeps_ijk  [ y_ijk ~ N( ... , ysigma_ij ) ]
 * e_ij = n_i + eeps_ij  [ e_ij ~ N(n_i, esigma) ]
 * m_j = s + meps_j  [ m_j ~ N(s, msigma) ]
 * n_i = g + neps_i  [ n_i ~ N(g, nsigma) ] 
 * 
 * yeps_ijk ~ N(0, ysigma_ij) 
 * eeps_ij ~ N(0, esigma)
 * meps_j ~ N(0, msigma)
 * neps_i ~ N(0, nsigma)
 * 
 * -- Is the fact that we're estimating mu's here going to 
 * -- lead to identifiability problems later? 
 * 
 * g ~ N(mu_g, sigma_g) 
 * s ~ N(mu_s, sigma_s) 
 */

/**
 * Corresponds to IJ in our plate diagram
 */
public class DataSegmentModel extends Model {
	
	public DataSegment data;

	public ChannelModel channel;
	public SliceModel slice;
	
	public Double e, ysigma;

	public DataSegmentModel(DataSegment s, ChannelModel c, SliceModel sl) {
		data = s;
		channel = c;
		slice = sl;
		
		e = 1.0;
		
		channel.segments.add(this);
		slice.segments.add(this);
	}
	
	/**
	 * resampling function for the e_ij parameters. 
	 */
	public FunctionModel logCondE() { 
		return new SumFunction(new LogProbExpr(), new LogLikeExpr());
	}
	
	public class LogProbExpr extends UnnormalizedGaussian {
		
		public LogProbExpr() {  
			super(channel.n, channel.total.esigma);
		}
	}

	public class LogLikeExpr extends FunctionModel {  
		
		public LogLikeExpr() { 
		}

		public Double eval(Double expr) {
			double sum = 0.0;
			double var = ysigma;
			int base = data.strand.equals("+") ? data.end : data.start;
			
			for(int i = 0; i < data.dataLocations.length; i++) { 
				int delta = Math.abs(data.dataLocations[i] - base);
				double mean = expr + slice.m * (double)delta;
				
				double value = data.dataValues[channel.index][i];
				sum += logNormal(value, mean, var);
			}
			
			return sum;
		}
	}
	
	public static double logNormal(double value, double mean, double var) { 
		double diff = (value-mean);
		diff *= diff;
		return -diff / (2.0 * var);
	}

}


