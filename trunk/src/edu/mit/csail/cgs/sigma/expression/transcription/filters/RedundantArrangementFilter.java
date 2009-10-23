/*
 * Author: tdanford
 * Date: Jan 25, 2009
 */
package edu.mit.csail.cgs.sigma.expression.transcription.filters;

import java.util.*;
import edu.mit.csail.cgs.ewok.verbs.Filter;
import edu.mit.csail.cgs.sigma.expression.transcription.Call;
import edu.mit.csail.cgs.sigma.expression.transcription.TranscriptArrangement;

/**
 * TranscriptCalls in an Arrangement have an implicit "sum" -- if we have one call from 
 * (a, b) and another (b, c), then these implicitly sum to a "combined call" of (a, c).  
 * 
 * In our current implementation, we want to avoid testing transcript arrangements where 
 * there exists a call 'c0' which is a sum of N other calls, 'c1 + ... + cN'.  This filter
 * detects arrangements where this is the situation. 
 * 
 * @author tdanford
 *
 */
public class RedundantArrangementFilter 
	implements Filter<TranscriptArrangement,TranscriptArrangement> {
	
	public TranscriptArrangement execute(TranscriptArrangement arr) { 
		CallMap map = new CallMap(arr.calls);
		for(Call call : arr.calls) {
			if(map.hasSum(call)) { 
				return null;
			}
		}
		return arr;
	}

	private static class CallMap { 
		public Map<Integer,Set<Call>> starts, ends;
		
		public CallMap(Call[] calls) { 
			starts = new TreeMap<Integer,Set<Call>>();
			ends = new TreeMap<Integer,Set<Call>>();
			for(Call call : calls) { 
				int start = call.start, end = call.end;
				if(!starts.containsKey(start)) { 
					starts.put(start, new HashSet<Call>());
				}
				if(!ends.containsKey(end)) { 
					ends.put(end, new HashSet<Call>());
				}
				
				starts.get(start).add(call);
				ends.get(end).add(call);
			}
		}
		
		public boolean hasSum(Call call) { 
			for(Call c : starts.get(call.start)) { 
				if(!c.equals(call)) { 
					if(hasSum(c.end, call.end)) { 
						return true;
					}
				}
			}
			return false;
		}
		
		public boolean hasSum(int start, int end) {
			if(start == end) { return true; }
			
			if(starts.containsKey(start)) { 

				for(Call call : starts.get(start)) {
					if(call.end <= end && hasSum(call.end, end)) {
						return true;
					}
				}
			}
			return false;
		}
	}
}
