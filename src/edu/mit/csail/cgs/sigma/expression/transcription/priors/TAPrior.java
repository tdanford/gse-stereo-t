/*
 * Author: tdanford
 * Date: Dec 16, 2008
 */
package edu.mit.csail.cgs.sigma.expression.transcription.priors;

import edu.mit.csail.cgs.sigma.expression.transcription.TranscriptArrangement;

/**
 * We need to implement a "prior," which weights different TranscriptArrangements 
 * differently, according to how they conform to our understanding what the transcripts
 * "should" look like.  
 * 
 * In practice, this is mainly going to be a prior which weights TranscriptArrangements
 * with fewer Calls higher than those with more -- this is a bias towards simpler 
 * explanations, Occam's-Razor-style, a form of "capacity control."  
 * 
 * @author tdanford
 */
public interface TAPrior {
	public Double prior(TranscriptArrangement arr);
}
