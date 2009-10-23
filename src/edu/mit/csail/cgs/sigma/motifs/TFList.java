package edu.mit.csail.cgs.sigma.motifs;

import java.util.*;
import java.util.logging.*;
import java.io.*;

import edu.mit.csail.cgs.datasets.species.Gene;
import edu.mit.csail.cgs.ewok.verbs.Mapper;
import edu.mit.csail.cgs.sigma.*;

public class TFList {
	
	private MotifProperties props;
	private Vector<String> tfNames;

	public TFList(MotifProperties mps) { 
		props = mps;
		tfNames = new Vector<String>();
		File tfFile = props.getTFListFile();
		try {
			Parser<String> tfParser = new Parser<String>(tfFile, new Mapper<String,String>() { 
				public String execute(String a) { return a; }
			});
			while(tfParser.hasNext()) { 
				tfNames.add(tfParser.next());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public int size() { return tfNames.size(); }
	
	public Collection<String> getTFs() { return tfNames; }
	
	public String getTF(int i) { return tfNames.get(i); }
	
	public boolean containsTF(String t) { return tfNames.contains(t); }
	
	public boolean containsTF(Collection<String> ts) { 
		for(String t : ts) { if(containsTF(t)) { return true; } }
		return false;
	}
	
	public boolean isTF(Gene g) { return containsTF(g.getName()) || containsTF(g.getID()); }
}
