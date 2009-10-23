/*
 * Author: tdanford
 * Date: May 8, 2008
 */
package edu.mit.csail.cgs.sigma;

import java.util.*;

public interface Tuple<X> extends Collection<X> {
	public X get(int i);
	public Tuple<X> subTuple(int i1, int i2);
	
	public static abstract class Immutable<Y> implements Tuple<Y> {

		public boolean add(Y arg0) {
			throw new UnsupportedOperationException(arg0.toString());
		}

		public boolean addAll(Collection<? extends Y> arg0) {
			throw new UnsupportedOperationException(arg0.toString());
		}

		public void clear() {
			throw new UnsupportedOperationException();
		}

		public boolean containsAll(Collection<?> vals) {
			for(Object v : vals) { 
				if(!contains(v)) { return false; }
			}
			return true;
		}

		public boolean remove(Object arg0) {
			throw new UnsupportedOperationException(arg0.toString());
		}

		public boolean removeAll(Collection<?> arg0) {
			throw new UnsupportedOperationException(arg0.toString());
		}

		public boolean retainAll(Collection<?> arg0) {
			throw new UnsupportedOperationException(arg0.toString());
		}
	}
}
