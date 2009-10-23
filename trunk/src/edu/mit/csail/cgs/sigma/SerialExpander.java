/*
 * Author: tdanford
 * Date: Apr 29, 2009
 */
package edu.mit.csail.cgs.sigma;

import java.util.Iterator;

import edu.mit.csail.cgs.ewok.verbs.Expander;
import edu.mit.csail.cgs.utils.iterators.SerialIterator;

public class SerialExpander<X,Y> implements Expander<X,Y> {
	
	private Expander<X,Y> first, second;
	
	public SerialExpander(Expander<X,Y> f, Expander<X,Y> s) { 
		first = f; 
		second = s;
	}

	public Iterator<Y> execute(X a) {
		return new SerialIterator<Y>(first.execute(a), second.execute(a));
	} 
}
