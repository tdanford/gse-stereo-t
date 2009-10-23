/*
 * Author: tdanford
 * Date: Jun 12, 2009
 */
package edu.mit.csail.cgs.sigma.expression.transcription.recursive;

public class Coverage extends Endpts {

	public int calls; 
	
	public Coverage(int s, int e, int c) { 
		super(s, e);
		calls = c;
	}
	
	public int hashCode() { 
		int c = super.hashCode(); 
		c += calls; c *= 37;
		return c;
	}
	
	public boolean equals(Object o) { 
		if(!(o instanceof Coverage)) { return false; }
		Coverage c = (Coverage)o;
		if(c.calls != calls) { return false; }
		return super.equals(c);
	}
	
	public int compareTo(Endpts c) { 
		int cc = super.compareTo(c);
		if(cc != 0) { return cc; }
		if(c instanceof Coverage) {
			Coverage cov = (Coverage)c;
			if(calls < cov.calls) { return -1; }
			if(calls > cov.calls) { return 1; }
		}
		return 0;
	}
	
}
