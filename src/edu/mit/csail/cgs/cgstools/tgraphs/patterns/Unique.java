package edu.mit.csail.cgs.cgstools.tgraphs.patterns;

import java.util.HashSet;
import java.util.TreeSet;

import edu.mit.csail.cgs.cgstools.tgraphs.GraphContext;
import edu.mit.csail.cgs.cgstools.tgraphs.GraphQuery;
import edu.mit.csail.cgs.ewok.verbs.Filter;

public class Unique extends ContextFilterPattern {

	public Unique(String... n) {
		super(new UniqueFilter(n), n);
	}

	public Unique(Node... n) {
		this(nodePatternNames(n));
	}
	
	private static String[] nodePatternNames(Node... ns) { 
		int nns = ns.length;
		String[] array = new String[nns];
		for(int i = 0; i < ns.length; i++) { array[i] = ns[i].nodeName(); }
		return array;
	}
	
	private static class Values { 
		public String[] array;
		
		public Values(String[] a) { 
			array = a;
		}
		
		public int hashCode() { 
			int code = 17;
			for(int i = 0; i < array.length; i++) {  
				code += array[i].hashCode(); code *= 37;
			}
			return code;
		}
		
		public boolean equals(Object o) { 
			if(!(o instanceof Values)) { return false; }
			Values v = (Values)o;
			return GraphQuery.stringArrayEquals(array, v.array);
		}
	}

	private static class UniqueFilter implements Filter<GraphContext,GraphContext> {

		private String[] names;
		private HashSet<Values> seen;
		
		public UniqueFilter(String... ns) { 
			names = ns;
			seen = new HashSet<Values>();
		}

		public GraphContext execute(GraphContext a) {
			Values value = new Values(a.lookupAll(names));
			boolean seenValue = seen.contains(value);
			seen.add(value);
			return seenValue ? null : a; 
		} 
		
	}
}
