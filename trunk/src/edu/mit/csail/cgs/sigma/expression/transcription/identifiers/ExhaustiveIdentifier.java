/*
 * Author: tdanford
 * Date: Dec 16, 2008
 */
package edu.mit.csail.cgs.sigma.expression.transcription.identifiers;

import java.util.*;

import edu.mit.csail.cgs.ewok.verbs.FilterIterator;
import edu.mit.csail.cgs.sigma.expression.transcription.Arrangements;
import edu.mit.csail.cgs.sigma.expression.transcription.Cluster;
import edu.mit.csail.cgs.sigma.expression.transcription.TranscriptArrangement;
import edu.mit.csail.cgs.sigma.expression.transcription.TranscriptionParameters;
import edu.mit.csail.cgs.sigma.expression.transcription.filters.MaxOverlapArrangementFilter;
import edu.mit.csail.cgs.sigma.expression.transcription.filters.RedundantArrangementFilter;
import edu.mit.csail.cgs.sigma.expression.transcription.fitters.LinearTAFitter;
import edu.mit.csail.cgs.sigma.expression.transcription.fitters.MaxLikeTAFitter;
import edu.mit.csail.cgs.sigma.expression.transcription.fitters.SamplingTAFitter;
import edu.mit.csail.cgs.sigma.expression.transcription.fitters.TAFit;
import edu.mit.csail.cgs.sigma.expression.transcription.fitters.TAFitter;
import edu.mit.csail.cgs.sigma.expression.transcription.priors.TAPerProbePenalty;
import edu.mit.csail.cgs.sigma.expression.transcription.priors.TAPrior;
import edu.mit.csail.cgs.sigma.expression.workflow.WorkflowProperties;
import edu.mit.csail.cgs.sigma.expression.workflow.models.DataSegment;
import edu.mit.csail.cgs.utils.numeric.Numerical;

public class ExhaustiveIdentifier implements TranscriptIdentifier {
	
	private TranscriptionParameters params;
	private TAFitter fitter;
	private TAPrior prior;
	private SortedSet<TAFit> fits;
	private Integer maxTranscripts;
	
	public ExhaustiveIdentifier() { 
		this(new WorkflowProperties());
	}
	
	public ExhaustiveIdentifier(WorkflowProperties ps) { 
		this(ps, ps.getDefaultTranscriptionParameters());
	}
	
	public ExhaustiveIdentifier(WorkflowProperties ps, TranscriptionParameters cps) { 
		this(ps, new TAPerProbePenalty(cps.extraTranscripts, cps.overlapPenalty, cps.meanIntensity));
		params = cps;
		maxTranscripts = params.maxCallsPerCluster;
	}

	public ExhaustiveIdentifier(WorkflowProperties ps, TAPrior p) {
		this(p, new MaxLikeTAFitter(0));
		//this(p, new SamplingTAFitter(ps, p));
		//this(p, new LinearTAFitter(p));
	}
	
	public ExhaustiveIdentifier(TAPrior p, TAFitter f) {
		params = new TranscriptionParameters();
		fitter = f;
		prior = p;
		fits = new TreeSet<TAFit>();
		maxTranscripts = null;
	}
	
	public TAFit identify(Cluster c) {
		fits.clear();
		Double maxScore = null;
		Double minError = null;
		TAFit maxFit = null;
		System.out.println(String.format("Identifying Transcripts from Cluster (size: %d)", 
				c.segments.length));

		for(int k = 1; k <= c.segments.length && 
			(maxTranscripts == null || k <= maxTranscripts); k++) {
			
			System.out.println(String.format("\t#Calls: %d", k));
			Iterator<TranscriptArrangement> arrItr = new Arrangements(c, k);
			
			if(params.filterRedundancies) { 
				System.out.println("\tFiltering Redundant arrangements...");
				
				arrItr = new FilterIterator<TranscriptArrangement,TranscriptArrangement>(
						new RedundantArrangementFilter(), arrItr);
				
				/*
				arrItr = new FilterIterator<TranscriptArrangement,TranscriptArrangement>(
						new ContinuousArrangementFilter(), arrItr);
				*/
				
				arrItr = new FilterIterator<TranscriptArrangement,TranscriptArrangement>(
						new MaxOverlapArrangementFilter(2), arrItr);
			}
			
			while(arrItr.hasNext()) { 
				TranscriptArrangement arr = arrItr.next();
				System.out.print(arr.diagram());
				
				//System.out.println(String.format("\t%s", arr.toString()));
				
				TAFit fit = fitter.fitTranscripts(arr);
				
				if(fit != null) {
					//System.out.println(String.format("\t\t%s", fit.toString()));
					if(fit.isLegal()) { 
						fits.add(fit);

						//System.out.println(String.format("\t\t\tLegal Fit."));
						
						double score = fit.getScore();
						if(maxScore == null || score > maxScore) { 
							maxScore = score;
							minError = fit.error;
							maxFit = fit;

							System.out.println(String.format(
									"Best Fit: %s (Prior: %.2f)", 
									maxFit.toString(), maxFit.logPrior));

						} else {
							System.out.println(String.format(
									"Sub-optimal Fit: %s (Prior: %.2f)", 
									fit.toString(), fit.logPrior));
						}
					} else {
						System.out.println(String.format("Illegal Fit."));
					}
				} else { 
					System.out.println("Null fit.");
				}
			}
		}
		
		return maxFit;
	}


	/*
	 * These two methods only "work" when the identify() method, above, has 
	 * already been called -- they rely on the internal state set up by that 
	 * method, and so their results will change after successive calls to 
	 * identify().
	 */
	
	public SortedSet<TAFit> getRankedFits() { return fits; }
	
	public boolean topFitsOverlap(Double diff) {
		TAFit first = null;
		allFits: for(TAFit f : fits) { 
			if(first == null) { 
				first = f;
			}
			
			double scored = first.getScore() - f.getScore();
			if(scored > diff) { break allFits; }
			
			if(!f.arrangement.containsOverlap()) { return false; }
		}
		return true;
	}
}
