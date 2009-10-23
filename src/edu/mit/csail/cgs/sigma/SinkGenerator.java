package edu.mit.csail.cgs.sigma;

import java.util.*;
import edu.mit.csail.cgs.ewok.verbs.*;

public class SinkGenerator<X> implements Generator<X> {
	
	private LinkedList<X> values;
	
	public SinkGenerator(Iterator<X> itr) { 
		values = new LinkedList<X>();
		while(itr.hasNext()) { 
			values.addLast(itr.next());
		}
	}
	
	public Iterator<X> execute() { return values.iterator(); }
}
