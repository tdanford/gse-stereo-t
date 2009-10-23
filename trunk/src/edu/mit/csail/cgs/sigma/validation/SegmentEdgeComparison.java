/*
 * Author: tdanford
 * Date: May 7, 2009
 */
package edu.mit.csail.cgs.sigma.validation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import edu.mit.csail.cgs.ewok.verbs.Filter;
import edu.mit.csail.cgs.ewok.verbs.Mapper;
import edu.mit.csail.cgs.ewok.verbs.MapperIterator;
import edu.mit.csail.cgs.sigma.expression.workflow.models.DataSegment;
import edu.mit.csail.cgs.utils.Interval;

/**
 * Assesses segmentation fidelity by calculating the mean distance from a breakpoint at the 
 * edge of a real segment to the breakpoint at the end of one of the called segments.
 * 
 * For the purposes of this comparison, given a "real" Segment, when considering the right 
 * breakpoint we only search those "called" Segments either overlapping or to the right of the 
 * real Segment.  
 * 
 * Equivalently, with the real left breakpoint and called Segments that are either overlapping
 * or to the left of the real Segment.  
 * 
 * @author tdanford
 *
 */
public class SegmentEdgeComparison implements SegmentComparison {
	
	public SegmentEdgeComparison() { 
	}

	public Double compare(Collection<Integer> correct, Collection<DataSegment> learned) {

		SegmentIntervalMapper sim = new SegmentIntervalMapper();

		Iterator<Interval<DataSegment>> learnedLineItr =
			new MapperIterator<DataSegment,Interval<DataSegment>>(sim,
			learned.iterator());
		
		ArrayList<Interval<DataSegment>> learnedLines = 
			new ArrayList<Interval<DataSegment>>();
		while(learnedLineItr.hasNext()) { 
			learnedLines.add(learnedLineItr.next());
		}

		int totalDistance = 0;
		int numDistances = 0;
		
		for(Integer s : correct) { 
			Integer leftMin = null, rightMin = null;
			
			for(Interval<DataSegment> call : learnedLines) {
				int rightDist = Math.min(Math.abs(call.start-s), Math.abs(call.end-s));
				int leftDist = Math.min(Math.abs(call.start-s), Math.abs(call.end-s));
				
				/*
				System.out.println(String.format("real:%s -> call:%s, right: %d, left: %d", 
						s.toString(), call.toString(), rightDist, leftDist));
				*/
				
				if(call.end < s) { 
					leftMin = leftMin == null ? leftDist : Math.min(leftDist, leftMin);
					
				} else if (call.start > s) { 
					rightMin = leftMin == null ? rightDist : Math.min(rightDist, leftMin);
					
				} else { 
					leftMin = leftMin == null ? leftDist : Math.min(leftDist, leftMin);
					rightMin = leftMin == null ? rightDist : Math.min(rightDist, leftMin);
				}
			}

			if(leftMin != null) { 
				totalDistance += leftMin; 
				numDistances += 1;
			}

			if(rightMin != null) { 
				totalDistance += rightMin; 
				numDistances += 1;
			}
		}
		
		return (double)totalDistance / (double)Math.max(1, numDistances);
	} 
}

class SegmentTypeFilter implements Filter<DataSegment,DataSegment> {
	
	private int type;
	private Integer[] channels;
	
	public SegmentTypeFilter(int t, Integer[] ch) { 
		type = t;
		channels = ch.clone();
	}
	
	public DataSegment execute(DataSegment s) { 
		return s.hasConsistentType(type, channels) ? s : null;
	}
}

class SegmentIntervalMapper implements Mapper<DataSegment,Interval<DataSegment>> { 
	public Interval<DataSegment> execute(DataSegment s) { 
		return new Interval<DataSegment>(s.start, s.end, s);
	}
}