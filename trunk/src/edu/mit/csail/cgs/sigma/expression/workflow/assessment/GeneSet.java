/*
 * Author: tdanford
 * Date: Jun 5, 2009
 */
package edu.mit.csail.cgs.sigma.expression.workflow.assessment;

import java.io.*;
import java.util.*;

import edu.mit.csail.cgs.sigma.genes.*;

public class GeneSet {

	private Set<String> ids, names;
	
	public GeneSet(GeneNameAssociation assoc, File input) throws IOException { 
		ids = new TreeSet<String>();
		names = new TreeSet<String>();
		
		String line;
		BufferedReader br = new BufferedReader(new FileReader(input));
		while((line = br.readLine()) != null) { 
			line = line.trim();
			if(line.length() > 0) {
				if(assoc.containsID(line)) { 
					ids.add(line);
					names.add(assoc.getName(line));
				}
				if(assoc.containsName(line)) { 
					names.add(line);
					ids.addAll(assoc.getIDs(line));
				}
			}
		}
		br.close();
	}
	
	public int size() { return Math.max(names.size(), ids.size()); }
	
	public boolean containsName(String name) { return names.contains(name); }
	public boolean containsID(String id) { return ids.contains(id); }
	
	public boolean containsKey(String nameOrID) { 
		return containsName(nameOrID) || containsID(nameOrID);
	}
	
	public Set<String> idSubset(Collection<String> ks) { 
		TreeSet<String> keys = new TreeSet<String>();
		for(String k : ks) { 
			if(containsID(k)) { 
				keys.add(k);
			}
		}
		return keys;		
	}
	
	public Set<String> nameSubset(Collection<String> ks) { 
		TreeSet<String> keys = new TreeSet<String>();
		for(String k : ks) { 
			if(containsName(k)) { 
				keys.add(k);
			}
		}
		return keys;		
	}
	
	public Set<String> keySubset(Collection<String> ks) { 
		TreeSet<String> keys = new TreeSet<String>();
		for(String k : ks) { 
			if(containsKey(k)) { 
				keys.add(k);
			}
		}
		return keys;
	}
}
