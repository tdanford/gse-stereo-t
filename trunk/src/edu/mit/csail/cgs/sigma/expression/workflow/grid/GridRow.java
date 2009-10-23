/*
 * Author: tdanford
 * Date: May 20, 2009
 */
package edu.mit.csail.cgs.sigma.expression.workflow.grid;

import java.util.*;

import edu.mit.csail.cgs.utils.Enrichment;
import edu.mit.csail.cgs.utils.SetTools;
import edu.mit.csail.cgs.utils.models.*;
import edu.mit.csail.cgs.utils.probability.Hypergeometric;

public class GridRow extends Model {
	
	private static SetTools<String> tools = new SetTools<String>();
	private static Hypergeometric hypgeom = new Hypergeometric();
	
	private Set<String> itemset;
	private Map<String,Set<String>> subsets;
	private Map<String,Integer> subsetSizes;
	
	public String name;
	public String[] items; 
	
	public GridRow() {}
	
	public GridRow(String n, Iterator<String> its) { 
		name = n; 
		itemset = new TreeSet<String>();
		while(its.hasNext()) { itemset.add(its.next()); }
		items = itemset.toArray(new String[0]);
	}

    public void subsetItems(Set<String> total) { 
        itemset = tools.intersection(total, itemset);
    }
	
	public int size() { return items.length; }
	
	public boolean containsItem(String it) { return itemset.contains(it); }
	
	public void addSubset(String tag, Set<String> subs) { 
		if(subsets==null) { 
			subsets = new TreeMap<String,Set<String>>();
			subsetSizes = new TreeMap<String,Integer>();
		}
		if(subsets.containsKey(tag)) { throw new IllegalArgumentException(tag); }
		subsets.put(tag, tools.intersection(subs, itemset));
		subsetSizes.put(tag, subs.size());
	}
	
	public boolean hasSubset(String tag) { 
		return subsets != null && subsets.containsKey(tag);
	}
	
	public Set<String> getSubset(String tag) { 
		return hasSubset(tag) ? subsets.get(tag) : null;
	}
	
	public int subsetSize(String tag) { 
		return hasSubset(tag) ? subsets.get(tag).size() : 0;
	}
	
	public int indexOfItem(String item) {
		if(!itemset.contains(item)) { return -1; }
		for(int i = 0; i < items.length; i++) { 
			if(items[i].equals(item)) { 
				return i;
			}
		}
		return -1;
	}
	
	/**
	 * 
	 * @param N the size of the total tested set (= # of genes, usually)
	 * @param pvalue  the threshold (in *non*-log-scale, i.e. 0.001)
	 * @return
	 */
	public Set<Enrichment> enrichedSubsets(int N, double pvalue) { 
		TreeSet<Enrichment> enrich = new TreeSet<Enrichment>();
		double logpv = Math.log(pvalue);
		
		int theta = items.length;
		for(String tag : subsets.keySet()) {
			int n = subsetSizes.get(tag);
			int x = subsets.get(tag).size();
			double tagPValue = hypgeom.log_hypgeomPValue(N, theta, n, x);
			if(tagPValue <= logpv) { 
				enrich.add(new Enrichment(tag, N, theta, n, x, tagPValue));
			}
		}
		return enrich;
	}
	
	public boolean isEnriched(String tag, int N, double pvalue) { 
		double logpv = Math.log(pvalue);
		int theta = items.length;
		int n = subsetSizes.get(tag);
		int x = subsets.get(tag).size();
		double tagPValue = hypgeom.log_hypgeomPValue(N, theta, n, x);

        if(tagPValue <= logpv) { 
            System.out.println(String.format("%s (n:%d, theta:%d, x:%d) -> %f", 
                                             tag, n, theta, x, Math.exp(tagPValue)));
        } else { 
            System.out.println(String.format("%s (n:%d, theta:%d, x:%d) -> %f", 
                                             tag, n, theta, x, Math.exp(tagPValue)));
            //System.out.print(String.format("%.4f ", Math.exp(tagPValue)));
        }

		return tagPValue <= logpv;
	}
	
	public void update() { 
		itemset = new TreeSet<String>();
		for(int i = 0; i < items.length; i++) { 
			itemset.add(items[i]);
		}
		items = itemset.toArray(new String[0]);
		subsets = new TreeMap<String,Set<String>>();
		subsetSizes = new TreeMap<String,Integer>();
	}
	
	public String toString() { return name; }
	
	public int hashCode() { 
		int code = 17;
		code += name.hashCode(); code *= 37;
		return code;
	}
	
	public boolean equals(Object o) { 
		if(!(o instanceof GridRow)) { return false; }
		GridRow r = (GridRow)o;
		return r.name.equals(name);
	}
}
