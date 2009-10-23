/*
 * Created on Feb 14, 2008
 */
package edu.mit.csail.cgs.sigma.lethality;

import java.util.*;

import java.io.*;

import edu.mit.csail.cgs.ewok.verbs.*;
import edu.mit.csail.cgs.utils.models.*;
import edu.mit.csail.cgs.utils.models.data.DataFrame;

public class LethalityData {

	private LethalityProperties props;
    private Map<String,StrainLethalityData> strainLethals;
    
    public LethalityData(LethalityProperties ps) {
    	props = ps;
    	strainLethals = new TreeMap<String,StrainLethalityData>();
    }
    
    public void loadData() { 
    	for(String strain : props.strains()) { 
    		try { 
    			File f = props.getLethalityFile(strain);
    			StrainLethalityData data = new StrainLethalityData(strain, f);
    			strainLethals.put(strain, data);
    		} catch(IOException e) { 
    			System.err.println(String.format("Couldn't load lethality data for strain %s: %s",
    					strain, e.getMessage()));
    		}
    	}
    }

	public Set<String> getS288CLethalORFs() { return getLethalORFs("s288c"); }
	public Set<String> getSigmaLethalORFs() { return getLethalORFs("sigma"); }
	
	public Set<String> getS288cORFs() { return getORFs("s288c"); }
	public Set<String> getSigmaORFs() { return getORFs("sigma"); }

	public Set<String> getORFs(String strain) {
		return strainLethals.get(strain).allGenes();
	}

	public Set<String> getLethalORFs(String strain) {
		if(!strainLethals.containsKey(strain)) { 
			throw new IllegalStateException();
		}
		
		StrainLethalityData data = strainLethals.get(strain);
		Iterator<String> itr = data.findGenes(StrainLethalityData.Tag.LETHAL);
		Set<String> orfs = new TreeSet<String>();
		while(itr.hasNext()) { 
			orfs.add(itr.next());
		}
		
		return orfs;
	}
}

class StrainLethalityData {
	
	public static enum Tag { LETHAL, VIABLE };
	
	public static Tag findTag(String str) { 
		return Tag.valueOf(str.toUpperCase());
	}
	
	private String strain;
	private Map<String,Tag> tags;
	
	public StrainLethalityData(String str, File f) throws IOException { 
		DataFrame<GeneLethality> frame = 
			new DataFrame<GeneLethality>(GeneLethality.class, f, false, "gene", "tag");
		strain = str;
		tags = new TreeMap<String,Tag>();
		
		for(int i = 0; i < frame.size(); i++) { 
			GeneLethality gt = frame.object(i);
			tags.put(gt.gene, findTag(gt.tag));
		}
	}

	public String getStrain() { return strain; }
	public Set<String> allGenes() { return tags.keySet(); }
	public Tag getTag(String gene) { return tags.get(gene); }
	
	public Iterator<String> findGenes(Tag t) { 
		return new FilterIterator<String,String>(
				new TagFilter(t), tags.keySet().iterator());
	}
	
	private class TagFilter implements Filter<String,String> { 
		private Tag tag;
		public TagFilter(Tag t) { tag = t; }
		public String execute(String g) { 
			return tags.containsKey(g) && tags.get(g).equals(tag) ? g : null;
		}
	}
	
	public static class GeneLethality extends Model { 
		public String gene; 
		public String tag;
	}
}

