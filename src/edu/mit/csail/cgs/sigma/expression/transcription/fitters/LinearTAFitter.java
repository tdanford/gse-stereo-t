/*
 * Author: tdanford
 * Date: Dec 16, 2008
 */
package edu.mit.csail.cgs.sigma.expression.transcription.fitters;

import java.util.*;

import edu.mit.csail.cgs.sigma.expression.transcription.TranscriptArrangement;
import edu.mit.csail.cgs.sigma.expression.transcription.priors.TAPrior;
import edu.mit.csail.cgs.utils.numeric.JamaUtils;
import edu.mit.csail.cgs.utils.probability.NormalDistribution;

import Jama.Matrix;

public class LinearTAFitter implements TAFitter {
	
	public TAPrior prior;
	
	public LinearTAFitter(TAPrior p) { 
		prior = p;
	}

	public TAFit fitTranscripts(TranscriptArrangement arr) {
		Matrix M = arr.createDesignMatrix();
		Matrix obs = arr.createObservationMatrix();
		
		//JamaUtils.printMatrix(M, System.out);
		//System.out.println();

		Matrix transcripts = null;
		try { 
			transcripts = M.solve(obs);
		} catch(RuntimeException ex) {
			/*
			System.out.flush();
			System.err.flush();

			System.err.println(ex.getMessage());
			System.err.println(String.format("M: %dx%d", M.getRowDimension(), M.getColumnDimension()));
			System.err.println(String.format("obs: %dx%d", obs.getRowDimension(), obs.getColumnDimension()));
			
			System.err.println("M Matrix: "); JamaUtils.printMatrix(M);
			System.err.println("obs Matrix: "); JamaUtils.printMatrix(obs);
			System.err.flush();
			*/

			return null;
		} 

		Matrix predicted = M.times(transcripts);

		/*
		System.out.flush();
		System.err.println(arr.toString());
		System.err.println("\tM: " + M.getRowDimension() + "x" + M.getColumnDimension());
		System.err.println("\tobs: " + obs.getRowDimension() + "x" + obs.getColumnDimension());
		System.err.print("\tTranscript Predictions: (" + transcripts.getRowDimension() + "x" + transcripts.getColumnDimension() + ") "); 
		System.err.println();
		JamaUtils.printMatrix(transcripts.transpose(), System.err);
		System.err.flush();
		*/ 
		
		double var = 0.0;
		double err = 0.0;
		double ll = 0.0;
		
		//Double[] levels = new Double[arr.getNumCalls()*2];
		Double[] levels = new Double[arr.calls.length+1];
		
		double eps = 0.001;
		
		for(int i = 0; i < levels.length; i++) { 
			levels[i] = transcripts.get(i, 0);		
			
			if(i < arr.calls.length) {
				// An intercept term. 
				if(levels[i] < -eps) { 
					//System.err.println(String.format("\t%d: negative-intercept", i)); System.err.flush();
					return null;
				}
			} else { 
				// A slope term.
				if(arr.cluster.strand().equals("+")) { 
					if(levels[i] > eps) { 
						//System.err.println(String.format("\t%d: negative-slope", i)); System.err.flush();
						return null;
					}
				} else { 
					if(levels[i] < -eps) { 
						//System.err.println(String.format("\t%d: negative-slope", i)); System.err.flush();
						return null;
					}
				}
			}
		}
		
		int length = predicted.getRowDimension();
		Double[] predArray = new Double[length];
		
		for(int i = 0; i < predicted.getRowDimension(); i++) { 
			double diff = predicted.get(i, 0) - obs.get(i, 0);
			predArray[i] = predicted.get(i, 0);
			err += Math.abs(diff);
			var += diff*diff;
		}
		
		err /= (double)length;
		var /= (double)length;
		
		var = Math.sqrt(var);
		
		for(int i = 0; i < arr.calls.length; i++) { 
			NormalDistribution ndist = new NormalDistribution(predicted.get(i, 0), var);
			ll += ndist.calcLogProbability(obs.get(i, 0));
		}
	
		TAFit fit = new TAFit(arr, prior, levels, err, ll);
		fit.predicted = predArray;
		return fit;
	}

}
