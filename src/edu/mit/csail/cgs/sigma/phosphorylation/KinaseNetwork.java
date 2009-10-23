/*
 * Author: tdanford
 * Date: May 1, 2008
 */
package edu.mit.csail.cgs.sigma.phosphorylation;

import java.util.*;
import java.io.*;

import edu.mit.csail.cgs.sigma.Parser;

public class KinaseNetwork {

	private PhosProperties props;
	private KinaseList klist;
	private Map<String,Vector<PhosEntry>> entryMap;
	
	public KinaseNetwork(PhosProperties pps) { 
		props = pps;
		klist = new KinaseList(props);
		entryMap = new HashMap<String,Vector<PhosEntry>>();
	}
	
	public Set<String> getKinases() { return entryMap.keySet(); }
	
	public Collection<PhosEntry> getPhosphorylationEntries(String kinase) { 
		return entryMap.get(kinase); 
	}
	
	public void loadData() { 
		for(String kinase : klist.getKinaseNames()) { 
			File kinaseFile = klist.getKinaseFile(kinase);
			try {
				entryMap.put(kinase, new Vector<PhosEntry>());
				Parser<PhosEntry> entries = new Parser<PhosEntry>(kinaseFile, new PhosEntry.Decoder(), 1);
				while(entries.hasNext()) { 
					PhosEntry e = entries.next();
					entryMap.get(kinase).add(e);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void closeData() { 
		entryMap.clear();
	}
}
