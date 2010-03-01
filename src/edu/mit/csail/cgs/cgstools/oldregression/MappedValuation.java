package edu.mit.csail.cgs.cgstools.oldregression;

import java.util.*;

public class MappedValuation<ValueType> implements Valuation<ValueType> {
	
	private String name;
	private Map<String,ValueType> values;
	
	public MappedValuation(String n) { 
		name = n;
		values = new TreeMap<String,ValueType>();
	}
	
	public MappedValuation(String n, Map<String,ValueType> vs) { 
		name =n;
		values = new TreeMap<String,ValueType>(vs);
	}
	
	public void addValue(String n, ValueType v) { 
		values.put(n, v);
	}
	
	public void addAllValues(Map<String,ValueType> vs) { 
		for(String k : vs.keySet()) { 
			addValue(k, vs.get(k));
		}
	}

	public String getName() {
		return name;
	}

	public ValueType getValue(Datapoints dp, int index) {
		return values.get(dp.getName(index));
	}

    public void clear() {
        values.clear();
    }

}
