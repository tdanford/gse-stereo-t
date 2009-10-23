/*
 * Author: tdanford
 * Date: Jan 25, 2009
 */
package edu.mit.csail.cgs.sigma.expression.transcription.filters;

import java.util.*;
import edu.mit.csail.cgs.ewok.verbs.Filter;
import edu.mit.csail.cgs.sigma.expression.transcription.TranscriptArrangement;

/**
 * @author tdanford
 *
 */
public class MaxOverlapArrangementFilter 
	implements Filter<TranscriptArrangement,TranscriptArrangement> {
	
	private int thresh;
	public MaxOverlapArrangementFilter(int t) { 
		thresh = t;
	}
	
	public TranscriptArrangement execute(TranscriptArrangement arr) { 
		Integer[] counts = arr.overlapCount();
		for(int i = 0; i < counts.length; i++) { 
			if(counts[i] > thresh) { 
				return null;
			}
		}
		
		return arr;
	}
}
