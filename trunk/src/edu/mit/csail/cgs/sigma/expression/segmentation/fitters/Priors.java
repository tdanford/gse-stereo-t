/*
 * Author: tdanford
 * Date: Jun 8, 2009
 */
package edu.mit.csail.cgs.sigma.expression.segmentation.fitters;

import java.util.*;

import cern.jet.random.Gamma;
import cern.jet.random.engine.DRand;

import edu.mit.csail.cgs.cgstools.singlevarcalculus.ConstantFunction;
import edu.mit.csail.cgs.cgstools.singlevarcalculus.FunctionModel;
import edu.mit.csail.cgs.sigma.expression.segmentation.*;

public class Priors {
	
	public static Double logistic(Double x) { 
		return 1.0 / (1.0 + Math.exp(-x));
	}
	
	public static Double logLogistic(Double x) { 
		return - Math.log(1.0 + Math.exp(-x));
	}
	
	public static FunctionModel zero = new ConstantFunction(0.0);
	
	public static FunctionModel multiplePrior(double m) {
		return new MultiplePrior(m);
	}

	public static FunctionModel expPrior(double mean) { 
		double lmbda = 1.0 / mean;
		return new LogExponentialPrior(lmbda); 
	}
	
	public static FunctionModel gammaPrior(double a, double b) { 
		return new LogGammaPrior(a, b);
	}
	
	public static FunctionModel positive(FunctionModel m) { 
		return new TruncatedPrior(0.0, m);
	}
	
	public static Double logPoisson(int k, double lmbda) { 
		double sum = -lmbda + (double)k * Math.log(lmbda);
		for(int i = 2; i <= k; i++) { 
			sum -= Math.log((double)i);
		}
		return sum;
	}
	
	public static class LogisticSignalPrior extends FunctionModel { 
		private double softness, cutpoint;
		
		public LogisticSignalPrior(double soft, double cut) { 
			softness = soft; 
			cutpoint = cut;
		}

		public Double eval(Double input) {
			double x = (input - cutpoint) / softness;
			return logLogistic(x);
		}
	}
	
	public static class LogisticNoisePrior extends FunctionModel {
		private double softness, cutpoint;
		
		public LogisticNoisePrior(double soft, double cut) { 
			softness = soft; 
			cutpoint = cut;
		}

		public Double eval(Double input) {
			double x = (input - cutpoint) / softness;
			return Math.log(1.0 - logistic(x));
		}
	}
	
	public static class TruncatedPrior extends FunctionModel {
		
		private double truncationPoint;
		private FunctionModel prior;
		
		public TruncatedPrior(double t, FunctionModel f) { 
			truncationPoint = t;
			prior = f;
		}
		
		public Double eval(Double input) { 
			if(input >= truncationPoint) { 
				return prior.eval(input);
			} else { 
				return -Double.MAX_VALUE;
			}
		}
	}
	
	public static class ShiftedPrior extends FunctionModel {
		
		private double shift; 
		private FunctionModel prior;
		
		public ShiftedPrior(double s, FunctionModel f) { 
			shift = s;
			prior = f;
		}

		public Double eval(Double input) {
			return prior.eval(input + shift);
		} 
	}
	
	public static class LogExponentialPrior extends FunctionModel {
		
		private double lambda, logLambda;
		
		public LogExponentialPrior(double l) { 
			lambda = l;
			logLambda = Math.log(lambda);
		}

		public Double eval(Double input) {
			if(input < 0.0) { return -Double.MAX_VALUE; }
			return logLambda - (lambda * input); 
		} 
	}
	
	public static class MultiplePrior extends FunctionModel { 
		private double coeff; 
		public MultiplePrior(double m) { coeff = m; }
		public Double eval(Double input) { return coeff * input; }
	}
	
	public static class LogGammaPrior extends FunctionModel {
		
		private double k, theta;
		private Gamma gamma;
		
		public LogGammaPrior(double k, double theta) {
			this.k = k;
			this.theta = theta;
			gamma = new Gamma(k, theta, new DRand());
		}

		public Double eval(Double input) {
			if(input < 0.0) { return -Double.MAX_VALUE; }
			return Math.log(gamma.pdf(input));
		} 
	}
	
	private SegmentationParameters params;

	public Priors(SegmentationParameters pms) { 
		params = pms;
	}

	public static FunctionModel linearPrior(Double coeff) {
		return new LinearPrior(coeff, 0.0);
	}
	
	public static class LinearPrior extends FunctionModel { 
		private double slope, intercept;
		public LinearPrior(double s, double i) { 
			slope = s; intercept = i;
		}
		public Double eval(Double input) { 
			return (input * slope) + intercept;
		}
	}
}
