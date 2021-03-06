/*
 * Author: tdanford
 * Date: May 3, 2009
 */
package edu.mit.csail.cgs.sigma.expression.rma;

import java.io.PrintStream;
import java.util.*;

import edu.mit.csail.cgs.cgstools.singlevarcalculus.FunctionModel;
import edu.mit.csail.cgs.cgstools.slicer.SliceSampler;
import edu.mit.csail.cgs.sigma.expression.transcription.TranscriptArrangement;
import edu.mit.csail.cgs.sigma.expression.transcription.fitters.TAFit;
import edu.mit.csail.cgs.sigma.expression.transcription.priors.TAPrior;
import edu.mit.csail.cgs.utils.models.*;

public class SingleExptHDot extends Model {
	
	// Probe leve values.
	public Double[] y;

	// Transcript level values and parameters
	public Double[] gammas;      // transcript intensities
	public Double lambda;        // falloff
	public Integer[][] deltas;   // transcript,probe offsets
	
	// Hierarchical parameters
	public Double sigma_y;       // distribution of y's residuals.
	public Double zeta_lambda;	 // exponential parameter for lambda 
	public Double zeta_gamma;    // exponential parameter for gammas

	private TranscriptArrangement arrangement;
	
	private FunctionModel[] logp_gammas;
	private FunctionModel logp_lambda;
	
	private SliceSampler[] slice_gammas;
	private SliceSampler slice_lambda;
	
	public SingleExptHDot() {}
	
	public SingleExptHDot(TranscriptArrangement arr, int ch) { 
		int M = arr.calls.length;
		arrangement = arr;
		y = arr.cluster.values[ch].clone();
	
		Integer[] px = arr.cluster.locations;
		
		deltas = new Integer[M][y.length];
		gammas = new Double[M];
		
		boolean orient = arr.cluster.strand().equals("+");
		
		for(int i = 0; i < M; i++) {
			int start = arr.callStartpoint(i);
			int end = arr.callEndpoint(i);
			gammas[i] = 2.0;
		
			for(int j = 0; j < y.length; j++) {
				// the deltas need to be *positive*, since the prediction 
				// method (below) subtracts lambda*deltas[i][j] from the 
				// 3' end prediction. 
				deltas[i][j] = orient ? end - px[j] : px[j] - start;
			}
		}

		lambda = 0.001;
		sigma_y = 0.01;
		zeta_lambda = 1.0 / lambda;
		zeta_gamma = 2.0;
		
		update();
	}
	
	public SingleExptHDot(Double[] py, Integer[][] offs) { 
		int M = offs.length;
		arrangement = null;
		y = py.clone();
		deltas = offs.clone();
		gammas = new Double[M];
		
		for(int i = 0; i < M; i++) {
			gammas[i] = 2.0;
			if(deltas[i].length != py.length) { 
				throw new IllegalArgumentException();
			}
		}

		lambda = 0.001;
		sigma_y = 1.0;
		zeta_lambda = 1.0 / lambda;
		zeta_gamma = 2.0;
		
		update();		
	}

	public void update() { 
		double window = 0.5;
		
		logp_gammas = new FunctionModel[gammas.length];
		slice_gammas = new SliceSampler[gammas.length];
		for(int i = 0; i < gammas.length; i++) { 
			logp_gammas[i] = new LogProbGamma(i);
			slice_gammas[i] = new SliceSampler(logp_gammas[i], window, gammas[i]);
		}
		
		logp_lambda = new LogProbLambda();
		double lambda_window = 0.001;
		slice_lambda = new SliceSampler(logp_lambda, lambda_window, lambda);
	}
	
	public void sampleGammas() { 
		for(int i = 0; i < gammas.length; i++) { 
			gammas[i] = slice_gammas[i].nextLogX();
		}
	}
	
	public void sampleLambda() { 
		lambda = slice_lambda.nextLogX();
	}
	
	public void sample() { 
		sampleGammas();
		sampleLambda();
		
		//estimateSigmaY();
		//estimateZetaGamma();
		//estimateZetaLambda();
	}
	
	public void estimateSigmaY() {
		double sum = 0.0;
		for(int i = 0; i < y.length; i++) { 
			double r = residual(i);
			sum += (r * r);
		}
		
		sum /= (double)y.length;
		
		sigma_y = sum;
	}
	
	public void estimateZetaLambda() { 
		zeta_lambda = 1.0 / lambda;   
	}
	
	public void estimateZetaGamma() { 
		double sum = 0.0;
		for(int j = 0; j < gammas.length; j++) { 
			sum += gammas[j];
		}
		sum /= (double)gammas.length;
		zeta_gamma = sum / (double)2.0;
	}
	
	public double residual(int i) { 
		return y[i] - prediction(i);
	}
	
	public double prediction(int i) { 
		double prediction = 0.0;
		for(int j = 0; j < gammas.length; j++) { 
			if(deltas[j][i] != 0) { 
				prediction += 
					Math.exp(gammas[j] - deltas[j][i] * lambda);
			}
		}
		prediction = Math.log(prediction > 0.0 ? prediction : 1.0);
		return prediction;
	}
	
	public Double[] predictions() {
		Double[] p = new Double[y.length];
		for(int i = 0; i < p.length; i++) { p[i] = prediction(i); }
		return p;
	}
	
	public class LogProbLambda extends FunctionModel {
		
		public LogProbLambda() {}

		public Double eval(Double lmbda) {
			Double gammaLike = logp_exponential(lmbda, zeta_lambda);
			for(int i = 0; i < y.length; i++) {
				double pred = 0.0;
				
				for(int j = 0; j < gammas.length; j++) {
					
					if(deltas[j][i] != 0) {
						double transcriptPrediction = 
							Math.exp(
								gammas[j] - (double)deltas[j][i] * lmbda);
						pred += transcriptPrediction;
					}
				}
				
				pred = Math.log(pred);
				
				double residual = y[i] - pred;
				gammaLike += logp_normal(residual, 0.0, sigma_y); 
			}
			
			return gammaLike;
		} 
	}
	
	public class LogProbGamma extends FunctionModel {
		
		private int idx;
		
		public LogProbGamma(int i) { 
			idx = i;
		}

		public Double eval(Double g) {
			Double gammaLike = logp_erlang2(g, zeta_gamma);
			
			for(int i = 0; i < y.length; i++) {
				double prediction = 0.0;
				for(int j = 0; j < gammas.length; j++) {
					double gj = j == idx ? g : gammas[j];
					if(deltas[j][i] != 0) {
						double pred = 
							Math.exp(
									gj - (double) deltas[j][i] * lambda);
						prediction += pred;
					}
				}
				prediction = Math.log(prediction > 0.0 ? prediction : 1.0);
				double residual = y[i] - prediction;
				gammaLike += logp_normal(residual, 0.0, sigma_y);
			}
			
			return gammaLike;
		} 
	}

	public void display() { display(System.out); }
	
	public void display(PrintStream ps) { 
		for(int i = 0; i < y.length; i++) { 
			ps.println(String.format("y[%d]: %.2f (%.2f)", i, y[i], residual(i)));
		}
		displayParameters(ps);
	}
	
	public void displayParameters(PrintStream ps) { 
		for(int j = 0; j < gammas.length; j++) { 
			ps.println(String.format("gammas[%d]:  %.2f", j, gammas[j]));
		}
		ps.println(String.format("lambda: %.5f", lambda));
		
		ps.println(String.format("sigma_eps: %.3f", sigma_y));
		ps.println(String.format("a_lambda: %.3f", zeta_lambda));
		ps.println(String.format("theta_gamma: %.3f", zeta_gamma));		
	}
	
	public static Double logp_exponential(double x, double a) {
		if(x < 0.0) { return -Double.MAX_VALUE; }
		return Math.log(a) - (a * x);
	}
	
	public static Double logp_normal(double x, double mean, double var) { 
		double diff = x - mean;
		diff *= diff;
		double expt = -diff / (2.0 * var);
		double logc = -Math.log(Math.sqrt(2.0 * Math.PI * var));
		return logc + expt;
	}
	
	public static Double logp_erlang2(double x, double theta) {
		if(x < 0.0) { return -Double.MAX_VALUE; }
		double numer = -x / theta;
		double denom = 2.0 * theta;
		return Math.log(x) + numer - denom;
	}

	public double logLikelihood() { 
		double ll = 0.0;
		
		for(int i = 0; i < y.length; i++) { 
			ll += logp_normal(residual(i), 0.0, sigma_y);
		}
		for(int j = 0; j < gammas.length; j++) { 
			ll += logp_erlang2(gammas[j], zeta_gamma);
		}
		ll += logp_exponential(lambda, zeta_lambda);
		
		return ll;
	}
	
	public double error() { 
		double err = 0.0;
		for(int i = 0; i < y.length; i++) { 
			double r = residual(i);
			err += (r*r);
		}
		return err;
	}
	
	public TAFit createFit(TAPrior prior) {
		if(arrangement == null) { throw new IllegalArgumentException(); }
		Double[] params = new Double[gammas.length+1];
		for(int i = 0; i < gammas.length; i++) { params[i] = gammas[i]; }
		params[gammas.length] = lambda;
		return new TAFit(arrangement, prior, params, error(), logLikelihood()); 
	}

	public static void main(String[] args) { 
		test();
	}
	
	public static void test() {
		Random rand = new Random();
		
		double var = 0.2;
		double y1 = Math.abs(1.0 + rand.nextGaussian() * var); 
		double y2 = Math.abs(1.0 + rand.nextGaussian() * var); 
		double y3 = Math.abs(1.0 + rand.nextGaussian() * var); 
		double y4 = Math.abs(1.0 + rand.nextGaussian() * var); 
		double y5 = Math.abs(1.0 + rand.nextGaussian() * var); 
		double y6 = Math.abs(1.0 + rand.nextGaussian() * var);
		
		double[] g = new double[] { 5.0, 10.0 };
		double lambda = 0.01;
		
		Double[] y = new Double[] { y1, y2, y3, y4, y5, y6 };
		Integer[][] offs = new Integer[][] { 
				{ 250, 200, 100, 10, 0, 0 },
				{ 0, 0, 250, 200, 100, 10 },
		};
		
		for(int j =0; j < g.length; j++) { 
			for(int i = 0; i < y.length; i++) {
				if(offs[j][i] > 0) { 
					double addend = 
						Math.max(0.0, g[j] - (double)offs[j][i] * lambda);
					y[i] += addend;
					System.out.println(String.format("g[%d]: y[%d] += %.3f", j, i, addend));
				}
			}
		}
		
		SingleExptHDot trans = new SingleExptHDot(y, offs);
		int N = 1000;
		
		for(int i = 0; i < N; i++) {
			System.out.println(String.format("Sample: %d", i));
			trans.sample();
			trans.displayParameters(System.out);
			System.out.println();
		}
		
		for(int i = 0; i < g.length; i++) { 
			System.out.println(String.format("g[%d] = %.3f", i, g[i]));
		}
		System.out.println(String.format("lambda=%.4f", lambda));
		System.out.println();
		
		trans.display(System.out);
	}
}
