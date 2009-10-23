package edu.mit.csail.cgs.sigma;

import java.util.*;

import edu.mit.csail.cgs.ewok.verbs.Expander;
import edu.mit.csail.cgs.ewok.verbs.Mapper;

public class MultiClassifier<X> {

	private Map<String,Set<X>> sets;
	private Expander<X,String> expander;
	
	public MultiClassifier(Expander<X,String> m) { 
		sets = new TreeMap<String,Set<X>>();
		expander = m;
	}
	
	public void addValue(X v) {
		Iterator<String> ks = expander.execute(v);
        while(ks.hasNext()) {
            String k = ks.next();
            if(!sets.containsKey(k)) { sets.put(k, new HashSet<X>()); }
            sets.get(k).add(v);
        }
	}
	
	public Set<String> getKeys() { return sets.keySet(); }
	public Set<X> getValues(String k) { return sets.get(k); }
	
	public void addValues(Collection<X> vs) { 
		for(X v : vs) { addValue(v); }
	}
	
	public void addValues(Iterator<X> vs) { 
		while(vs.hasNext()) { addValue(vs.next()); }
	}
}
