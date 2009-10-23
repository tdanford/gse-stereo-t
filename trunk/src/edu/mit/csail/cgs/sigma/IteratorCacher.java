/*
 * Author: tdanford
 * Date: Apr 4, 2009
 */
package edu.mit.csail.cgs.sigma;

import java.util.ArrayList;
import java.util.Iterator;

public class IteratorCacher<X> { 
	
	private ArrayList<X> values;

	public IteratorCacher(Iterator<X> vs) { 
		values = new ArrayList<X>();
		while(vs.hasNext()) {
			values.add(vs.next());
		}
	}
	
	public Iterator<X> iterator() { return values.iterator(); }

	public int size() {
		return values.size();
	}
}

