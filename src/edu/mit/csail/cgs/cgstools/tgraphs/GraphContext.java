/*
 * Author: tdanford
 * Date: Apr 17, 2009
 */
package edu.mit.csail.cgs.cgstools.tgraphs;

import java.util.*;
import edu.mit.csail.cgs.utils.models.Model;

public class GraphContext {
	
	private TreeMap<String,String> values;
	
	public GraphContext() { 
		values = new TreeMap<String,String>();
	}
	
	public GraphContext(String n, String v) { 
		this();
		values.put(n, v);
	}
	
	public GraphContext(GraphContext c) { 
		values = new TreeMap<String,String>(c.values);
	}
	
	public GraphContext(String n, String v, GraphContext c) {
		this(c);
		values.put(n, v);
	}
	
	public boolean hasName(String n) { return values.containsKey(n); }
	
	public String lookup(String n) { return values.get(n); }
	
	public String[] lookupAll(String... ns) { 
		String[] array = new String[ns.length];
		for(int i = 0; i < array.length; i++) { 
			array[i] = lookup(ns[i]);
		}
		return array;
	}
	
	public String toString() { 
		StringBuilder sb = new StringBuilder();
		for(String name : values.keySet()) { 
			if(sb.length() > 0) { sb.append(" "); }
			sb.append(String.format("%s=%s", name, values.get(name)));
		}
		return sb.toString();
	}
	
	public int hashCode() { 
		int code = 17;
		for(String name : values.keySet()) { 
			code += name.hashCode(); code *= 37;
			code += values.get(name).hashCode(); code *= 37;
		}
		return code; 
	}
	
	public boolean equals(Object o) {
		if(o == null) { return false; }
		if(!(o instanceof GraphContext)) { return false; }
		GraphContext c = (GraphContext)o;
		if(values.size() != c.values.size()) { return false; }
		for(String n : values.keySet()) { 
			if(!c.hasName(n) || !c.lookup(n).equals(values.get(n))) { 
				return false;
			}
		}
		return true;
	}

	public static GraphContext replace(String key, String newValue, GraphContext c) { 
		GraphContext cc = new GraphContext(c);
		cc.values.put(key, newValue);
		return cc;
	}
}
