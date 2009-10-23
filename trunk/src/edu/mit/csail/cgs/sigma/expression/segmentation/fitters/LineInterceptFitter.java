/*
 * Author: tdanford
 * Date: Oct 29, 2008
 */
package edu.mit.csail.cgs.sigma.expression.segmentation.fitters;

import java.util.*;

import Jama.Matrix;
import edu.mit.csail.cgs.cgstools.singlevarcalculus.ConstantFunction;
import edu.mit.csail.cgs.cgstools.singlevarcalculus.FunctionModel;
import edu.mit.csail.cgs.sigma.expression.segmentation.InputData;
import edu.mit.csail.cgs.sigma.expression.segmentation.RPModel;
import edu.mit.csail.cgs.sigma.expression.segmentation.RegressionInputData;
import edu.mit.csail.cgs.sigma.expression.segmentation.Segment;
import edu.mit.csail.cgs.sigma.expression.segmentation.SegmentationParameters;
import edu.mit.csail.cgs.sigma.expression.segmentation.SegmentationProperties;
import edu.mit.csail.cgs.utils.PackedBitVector;
import edu.mit.csail.cgs.utils.models.*;
import edu.mit.csail.cgs.utils.models.data.DataFrame;
import edu.mit.csail.cgs.utils.models.data.DataRegression;
import edu.mit.csail.cgs.utils.numeric.JamaUtils;
import edu.mit.csail.cgs.utils.probability.NormalDistribution;

public class LineInterceptFitter implements SegmentFitter {
	
	public static FunctionModel zero = new ConstantFunction(0.0);
	
	private int errorCount;
	private Set<String> errorMessages;
	
	private Double logProbLine;
	private FunctionModel slopePrior, interceptPrior, variancePrior;
	
	public LineInterceptFitter() { 
		this(0.0, zero, zero, zero);
	}
	
	public LineInterceptFitter(double logLineProb, double varPenalty) { 
		this(logLineProb, zero, zero, Priors.expPrior(varPenalty));
	}
	
	public LineInterceptFitter(double logLineProb, double meanInt, double meanSlope, double meanVar) { 
		this(logLineProb, Priors.expPrior(meanInt), Priors.expPrior(meanSlope), Priors.expPrior(meanVar));
	}
	
	public LineInterceptFitter(double logLineP, FunctionModel intPrior, FunctionModel slpPrior, FunctionModel varPrior) {
		slopePrior = slpPrior;
		interceptPrior = intPrior;
		variancePrior = varPrior;
		logProbLine = logLineP;
		
		System.out.println(String.format("LineInterceptFitter(" +
				"logProbeLine=%f)", logProbLine));
		
		errorCount = 0;
		errorMessages = new TreeSet<String>();
		
		shiftSlopePrior(0.001);
	}
	
	public void shiftSlopePrior(double amount) { 
		slopePrior = new Priors.ShiftedPrior(amount, slopePrior);
	}
	
	public int getNumErrors() { return errorCount; }
	public Set<String> getErrors() { return errorMessages; }

	/**
	 * 
	 * @return the number of parameters. Here is 3: intercept, slope and s2.
	 */
	public int numParams() {
		return 3;
	}

	public int type() {
		return Segment.LINE;
	} 
	
	public Double[] fit(int j1, int j2, InputData data, Integer[] channels) {
		Integer[] locations = data.locations();
		Double[][] values = data.values();
		int offsetStart = locations[j1];

		Matrix X = null, y = null;

		int N = 0;
		for(int i = j1; i < j2; i++) { 
			for(Integer ch : channels) { 
				if(values[ch][i] != null) { 
					N += 1;
				}
			}
		}
		
		X = new Matrix(N, channels.length+1, 0.0);
		y = new Matrix(N, 1, 0.0);
		
		for(int i = j1, h = 0; i < j2; i++) {
			int offset = locations[i] - offsetStart;
			
			for(int k = 0; k < channels.length; k++) { 
				int ch = channels[k];
				Double value = values[ch][i];
				if(value != null) { 
					X.set(h, k, 1.0);
					X.set(h, channels.length, (double)offset);
					y.set(h, 0, value);
					h++;
				}
			}
		}
		
		try { 
			Matrix beta = DataRegression.leastSquares(X, y);
			double s2 = DataRegression.s2(X, y, beta);
			
			Double slope = beta.get(channels.length, 0);
			
			Double[] params = new Double[channels.length+2];
			for(int i = 0; i < channels.length; i++) { 
				params[i] = beta.get(i, 0);
			}
			
			params[channels.length] = slope;
			params[channels.length+1] = s2;

			return params;

		} catch(RuntimeException e) {
			
			errorCount += 1;
			
			if(e.getMessage() != null) { 
				errorMessages.add(e.getMessage());
			}

			Double[] p = new Double[channels.length+2];
			for(int i = 0; i < p.length; i++) { p[i] = -1.0; }
			return p;
		}
	}
	
	public Double score(int j1, int j2, Double[] params, InputData data, Integer[] channels) {
		
		// 0: intercept
		// 1: slope
		// 2: variance
		
		Integer[] locations = data.locations();
		Double[][] values = data.values();

		double variance = params[params.length-1];
		double slope = params[params.length-2];
		
		double uniformSlope = data.strand().equals("+") ? slope : -slope;

		if(variance < -0.5) { 
			return -Double.MAX_VALUE;
		}

		int offsetStart = locations[j1];
		NormalDistribution ndist = new NormalDistribution(0.0, variance);

		Double sum = logProbLine +  
			slopePrior.eval(uniformSlope) + 
			variancePrior.eval(variance);
		
		for(int i = 0; i < params.length-2; i++) { 
			sum += interceptPrior.eval(params[i]);
		}
		
		for(int i = j1; i < j2; i++) { 
			int offset = locations[i]-offsetStart;
			double base = offset*slope;

			for(int k = 0; k < channels.length; k++) { 
				int channel = channels[k];
				
				double pred = base + params[k];  // params[k] is the k'th intercept
				Double value = values[channel][i];
				
				if(value != null) {
					double diff = value - pred;
					double logp = ndist.calcLogProbability(diff);
					sum += logp;
				}
			}
		}
		
		return sum;
	}
}

