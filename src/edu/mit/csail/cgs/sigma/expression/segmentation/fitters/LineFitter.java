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

public class LineFitter implements SegmentFitter {
	
	public static FunctionModel zero = new ConstantFunction(0.0);
	
	private int errorCount;
	private Set<String> errorMessages;
	private boolean accelerate;
	
	private Double logProbLine;
	private FunctionModel slopePrior, interceptPrior, variancePrior;
	
	public LineFitter() { 
		this(0.0, zero, zero, zero);
	}
	
	public LineFitter(double logLineProb, double varPenalty) { 
		this(logLineProb, zero, zero, Priors.expPrior(varPenalty));
	}
	
	public LineFitter(double logLineProb, double meanInt, double meanSlope, double meanVar) { 
		this(logLineProb, Priors.expPrior(meanInt), Priors.expPrior(meanSlope), Priors.expPrior(meanVar));
	}
	
	public LineFitter(double logLineP, FunctionModel intPrior, FunctionModel slpPrior, FunctionModel varPrior) {
		slopePrior = slpPrior;
		interceptPrior = intPrior;
		variancePrior = varPrior;
		logProbLine = logLineP;
		
		errorCount = 0;
		errorMessages = new TreeSet<String>();
		accelerate = true;
		
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
	
	private boolean checkMatrices(String name, Matrix m1, Matrix m2) { 
		int m1r = m1.getRowDimension(), m1c = m1.getColumnDimension();
		int m2r = m2.getRowDimension(), m2c = m2.getColumnDimension();
		
		double eps = 0.001;
		
		boolean error = false;
		
		if(m1r != m2r || m1c != m2c) {
			System.err.println(String.format("M1 (%d,%d) doesn't match M2 (%d, %d)",
					m1r, m1c, m2r, m2c));
			error = true;
		} else { 
			for(int i = 0; i < m1r; i++) { 
				for(int j = 0; j < m1c; j++) { 
					double v1 = m1.get(i, j), v2 = m2.get(i, j);
					if(Math.abs(v1-v2) >= eps) { 
						System.err.println(String.format("M1 (%.3f) != M2 (%.3f) at %d,%d", 
								v1, v2, i, j));
						error = true;
					}
				}
			}
		}
		
		if(error) { 
			System.err.println("M1:");
			JamaUtils.printMatrix(m1, System.err, 3);
			System.err.println("M2:");
			JamaUtils.printMatrix(m2, System.err, 3);
		}
		
		return !error;
	}
	
	public Double[] fit(int j1, int j2, InputData data, Integer[] channels) {
		Integer[] locations = data.locations();
		Double[][] values = data.values();
		int offsetStart = locations[j1];

		DataRegression<RPModel> regression = null;
		DataFrame<RPModel> frame = null;
		Matrix X = null, y = null;

		if(accelerate && data instanceof RegressionInputData) {
			RegressionInputData rid = (RegressionInputData)data;
			regression = rid.regression;
			frame = regression.getFrame();

			if(j2-j1 < 2) { 
				return new Double[] { -1.0, -1.0, -1.0 };
			}
			
			PackedBitVector v = rid.createRegionSelector(j1, j2-1);
			PackedBitVector channelBits = rid.channelBits.get(channels[0]);
			for(int i = 1; i < channels.length; i++) { 
				channelBits.or(rid.channelBits.get(channels[i]));
			}
			v.and(channelBits);

			int[] rows = v.asIndices(true);
			int c1 = 0, c2 = rid.X.getColumnDimension()-1;
			
			X = rid.X.getMatrix(rows, c1, c2);
			y = rid.y.getMatrix(rows, 0, 0);

			double shift = (double)offsetStart;
			for(int i = 0; i < X.getRowDimension(); i++) {
				X.set(i, 1, X.get(i, 1)-shift);
			}

		} else {
			ArrayList<RPModel> models = new ArrayList<RPModel>();
			
			for(int i = j1; i < j2; i++) { 
				for(int k = 0; k < channels.length; k++) { 
					int ch = channels[k];
					if(values[ch][i] != null) { 
						models.add(new RPModel(i, locations[i]-offsetStart, ch, values[ch][i]));
					}
				}
			}
			
			frame = new DataFrame<RPModel>(RPModel.class, models);
			regression = new DataRegression<RPModel>(frame, "value ~ offset");
			
			X = regression.getPredictorMatrix();
			y = regression.getPredictedVector();
		}

		try { 
			regression.calculate(X, y);
			Map<String,Double> coeffs = regression.collectCoefficients();

			Double intercept = coeffs.get("(Intercept)");
			Double slope = coeffs.get("offset");
			Double s2 = regression.getS2();

			return new Double[] { intercept, slope, s2 };

		} catch(RuntimeException e) {
			
			for(int i = 0; i < frame.size(); i++) { 
				RPModel m = frame.object(i);
			}
			
			errorCount += 1;
			
			if(e.getMessage() != null) { 
				errorMessages.add(e.getMessage());
			}
			
			return new Double[] { -1.0, -1.0, -1.0 };
		}
	}
	
	public Double score(int j1, int j2, Double[] params, InputData data, Integer[] channels) {
		
		// 0: intercept
		// 1: slope
		// 2: variance
		
		Integer[] locations = data.locations();
		Double[][] values = data.values();
		
		double intercept = params[0], slope = params[1], variance = params[2];
		double uniformSlope = data.strand().equals("+") ? slope : -slope;

		if(variance < -0.5) { 
			return -Double.MAX_VALUE;
		}

		int offsetStart = locations[j1];
		NormalDistribution ndist = new NormalDistribution(0.0, variance);

		//Double sum = offset;
		Double sum = logProbLine +  
			interceptPrior.eval(intercept) + 
			slopePrior.eval(uniformSlope) + 
			variancePrior.eval(variance);
		
		for(int i = j1; i < j2; i++) { 
			int offset = locations[i]-offsetStart;
			double pred = intercept + offset*slope;

			for(int k = 0; k < channels.length; k++) { 
				int channel = channels[k];

				Double value = values[channel][i];
				if(value != null) {
					double diff = value - pred;
					double logp = ndist.calcLogProbability(diff);
					sum += logp;
				}
			}
		}
		
		//sum -= varPenalty*params[2];
		
		return sum;
	}
}

