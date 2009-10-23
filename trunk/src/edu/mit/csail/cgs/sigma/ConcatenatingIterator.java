/*
 * Author: tdanford
 * Date: May 4, 2008
 */
package edu.mit.csail.cgs.sigma;

import java.util.*;

public class ConcatenatingIterator<X> implements Iterator<X> { 
	
	private Iterator<X> current;
	private LinkedList<Iterator<X>> remaining;
	
	public ConcatenatingIterator(Collection<Iterator<X>> itrs) { 
		remaining = new LinkedList<Iterator<X>>(itrs);
		current = null;
		findNextIterator();
	}

	public ConcatenatingIterator(Iterator<X> i1, Iterator<X> i2) { 
		remaining = new LinkedList<Iterator<X>>();
		remaining.add(i1);
		remaining.add(i2);
		current = null;
		findNextIterator();
	}

	public ConcatenatingIterator(Iterator<X> i1) { 
		remaining = new LinkedList<Iterator<X>>();
		remaining.add(i1);
		current = null;
		findNextIterator();
	}

	public boolean hasNext() {
		return current != null && current.hasNext();
	}
	
	private void findNextIterator() { 
		Iterator<X> next = null;
		do { 
			next = remaining.isEmpty() ? null : remaining.removeFirst();			
		} while(next != null && !next.hasNext());
		current = next;
	}

	public X next() {
		X next = current.next();
		if(!current.hasNext()) { 
			current = null;
			findNextIterator();
		}
		return next;
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}
}


