/*
 * Author: tdanford
 * Date: Jul 2, 2009
 */
package edu.mit.csail.cgs.sigma.expression.transcription.fitters;

import edu.mit.csail.cgs.cgstools.singlevarcalculus.FunctionModel;
import edu.mit.csail.cgs.sigma.expression.segmentation.fitters.Priors;
import edu.mit.csail.cgs.sigma.expression.transcription.Cluster;
import edu.mit.csail.cgs.sigma.expression.transcription.TranscriptionParameters;
import edu.mit.csail.cgs.sigma.expression.transcription.recursive.Endpts;
import edu.mit.csail.cgs.sigma.expression.transcription.recursive.Likelihoods;
import edu.mit.csail.cgs.sigma.expression.transcription.recursive.MultiLikelihoods;

public class MaxLikeEndptsFitter implements EndptsFitter {
	
	private TranscriptionParameters params;
	private Cluster cluster;  // the input cluster.
	private Integer[][] gammas;
	private Integer[] channels;
	
	public MaxLikeEndptsFitter(TranscriptionParameters p, Cluster c, Integer[] chs, Integer[][] gs) {
		params = p;
		cluster = c;
		channels = chs != null ? chs : cluster.channels;
		gammas = gs;
	}

	public EndptsFit fitTranscripts(Endpts[] epts, Double bestScore) {
		
		/*
		 * An arrangement has a prior, \pi(A), which has three logical components:
		 * 
		 * (1) a function that penalizes the number of transcripts in A (the 'size')
		 * 
		 * (2) a function that penalizes the length of transcripts in A 
		 *     (that is, we prefer longer transcripts to shorter ones)
		 *     
		 * (3) a function that penalizes the number (and possibly size) of the 
		 *     overlap regions implied by A.  (This penalty needs to be tuned
		 *     to provide a counterweight to function component #2, because 
		 *     longer transcripts are more likely to cause overlaps with other 
		 *     transcripts).
		 *     
		 * All three of these components need to be assessed in order to provide 
		 * the complexity (nee "prior") penalty on any given arrangement.  
		 * 
		 * Furthermore! Since the Endpts[] array (i.e., the arrangement) has already
		 * been specified by the time we reach this function, all three components
		 * of the complexity penalty can be immediately calculated.  If those
		 * values exceed the already computed "best score," even before fitting, 
		 * then we can simply return without fitting.  
		 * 
		 * This speeds things up noticeably.  
		 */
		
		double baseScore = 0.0;
		double meanTranscripts = params.extraTranscripts * (double)cluster.segments.length;
		double overlapPenalty = params.overlapPenalty;
		
		int trans = epts.length-1;
		baseScore += Priors.logPoisson(trans, meanTranscripts);

		for(int i = 0; i < epts.length; i++) {
			Endpts e1 = epts[i];

			for(int j = i + 1; j < epts.length; j++) {
				Endpts e2 = epts[j];
				if(e1.isOverlapping(e2)) {
					int overlap = Math.min(e1.end, e2.end) - Math.max(e1.start, e2.start);
					
					for(int k = Math.max(e1.start, e2.start); k < Math.min(e1.end, e2.end); k++) {
						int len = cluster.segments[k].dataLocations.length; 
						baseScore += (double)len * overlapPenalty;						
					}
				}
			}
		}

		// This is the early cutoff clause -- if our base complexity penalty 
		// is already worse than the best score, then there's no need to fit 
		// any parameters at all.  
		if(bestScore != null && bestScore > baseScore) { 
			return null;
		}
		
		//Likelihoods like = new Likelihoods(cluster, epts, channel);
		MultiLikelihoods like = new MultiLikelihoods(cluster, epts, channels, gammas);

		//like.setDebugPrint(true);
		//like.debugPrintZeroFinding=true;
		
		if(like.coordinateOptimize()) { 
			double meanGamma = params.meanIntensity;
			double gammaA = Math.floor(meanGamma);
			double gammaB = meanGamma / gammaA;
			FunctionModel gammaPrior = Priors.gammaPrior(gammaA, gammaB);
			
			double[][] gammas = like.gammas();
			
			for(int i = 0; i < gammas.length; i++) {
				for(int j = 0; j < gammas[i].length; j++) { 
					baseScore += gammaPrior.eval(Math.log(gammas[i][j]));
				}
			}
			
			double lambda = like.lambda();
			double variance = like.variance();

			double likelihood = like.likelihood();
			double score = likelihood + baseScore;
			//System.out.println(String.format("%f (base) + %f (like) = %f", baseScore, likelihood, score));
			
			EndptsFit eptf = new EndptsFit(cluster, epts, gammas, lambda, variance, score, likelihood);

			return eptf;
		} else { 
			return null;
		}
	} 	
}
