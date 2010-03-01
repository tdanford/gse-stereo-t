package edu.mit.csail.cgs.cgstools.oldregression;

import java.util.*;

public class Datapoints {

	private Vector<String> names;
	private Vector<String> descriptions;

	public Datapoints(int n) { 
		names = new Vector<String>();
		descriptions = new Vector<String>();
		for(int i = 0; i < n; i++) { 
			names.add(String.format("point%d", i));
			descriptions.add("");
		}
	}
	
	public Datapoints() { 
		names = new Vector<String>();
		descriptions = new Vector<String>();
	}
	
	public int size() { return names.size(); }
	public String getName(int i) { return names.get(i); }
	public String getDescription(int i) { return descriptions.get(i); }
	public int findDatapoint(String n) { return names.indexOf(n); }
	
	public void addDatapoint(String n) { addDatapoint(n, ""); }
	public void addDatapoint(String n, String d) { names.add(n); descriptions.add(d); }
	
	public String toString() { 
		return String.format(
				"%d Datapoints", 
				size());
	}

    public void clear() {
        names.clear();
        descriptions.clear();
    }
}
