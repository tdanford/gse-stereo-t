/*
 * Author: tdanford
 * Date: Nov 21, 2008
 */
package edu.mit.csail.cgs.sigma.expression.segmentation;

import edu.mit.csail.cgs.utils.Predicate;
import edu.mit.csail.cgs.utils.models.Model;

public class RPModel extends Model {  
	public Integer index, offset;
	public Integer channel;
	public Double value;
	
	public RPModel(int idx, int off, int chan, double v) { 
		index = idx;
		offset = off;
		channel = chan;
		value = v;
	}
}

class RPModelChannelPredicate implements Predicate<RPModel> { 
	private int channel;

	public RPModelChannelPredicate(int k) { 
		channel = k;
	}
	public boolean accepts(RPModel v) {
		return v.channel == channel;
	}
}

class RPModelIndexPredicate implements Predicate<RPModel> { 
	private int start, end;

	public RPModelIndexPredicate(int s, int e) { 
		start = s; end = e;
	}
	public boolean accepts(RPModel v) {
		return v.index >= start && v.index <= end;
	}
}
