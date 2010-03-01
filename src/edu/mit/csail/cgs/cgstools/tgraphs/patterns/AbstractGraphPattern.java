/*
 * Author: tdanford
 * Date: Apr 21, 2009
 */
package edu.mit.csail.cgs.cgstools.tgraphs.patterns;

import java.util.Collection;
import java.util.Iterator;

import edu.mit.csail.cgs.cgstools.tgraphs.GraphContext;
import edu.mit.csail.cgs.ewok.verbs.Expander;
import edu.mit.csail.cgs.ewok.verbs.Mapper;
import edu.mit.csail.cgs.ewok.verbs.MapperIterator;

public abstract class AbstractGraphPattern implements GraphPattern {
	
	private String[] dependentNames, outputNames;  
	
	public AbstractGraphPattern(String n) {
		dependentNames = new String[] {};
		outputNames = new String[] { n };
	}
	
	public AbstractGraphPattern(String dn, String on) { 
		dependentNames = new String[] { dn };
		outputNames = new String[] { on };		
	}
	
	public AbstractGraphPattern(String[] dns, String[] ons) { 
		dependentNames = dns.clone();
		outputNames = ons.clone();
	}
	
	public AbstractGraphPattern(Collection<String> dns, Collection<String> ons) { 
		this(dns.toArray(new String[0]), ons.toArray(new String[0])); 
	}
	
	public String[] dependencies() { return dependentNames; }
	public String[] names() { return outputNames; }
	
	protected Iterator<GraphContext> makeContexts(Iterator<String> nodes, GraphContext root, String name) { 
		return new MapperIterator<String,GraphContext>(new ContextMapper(root, name), nodes);
	}
		
	protected static class ContextMapper implements Mapper<String,GraphContext> { 
		private GraphContext root;
		private String name;
		
		public ContextMapper(GraphContext r, String n) { 
			root = r; 
			name = n;
		}
		
		public GraphContext execute(String value) { 
			return new GraphContext(name, value, root);
		}
	}
	
	public static String namesToString(String[] strings) {
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < strings.length; i++) { 
			if(sb.length() > 0) { sb.append(","); }
			sb.append(strings[i]);
		}
		return sb.toString();
	}
}