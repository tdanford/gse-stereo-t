package edu.mit.csail.cgs.sigma.expression.workflow.assessment.differential;

import edu.mit.csail.cgs.sigma.expression.workflow.models.DataSegment;
import edu.mit.csail.cgs.utils.models.Model;
import java.util.*;

public class DifferentialSpec {

	private Map<String,DifferentialTest> tests;
	
	public DifferentialSpec() { 
		tests = new TreeMap<String,DifferentialTest>();
	}
	
	public void addTest(String t, DifferentialTest dt) { 
		tests.put(t, dt);
	}
	
	public String[] differential(DataSegment segment) { 
		Set<String> tags = new TreeSet<String>();
		for(String tk : tests.keySet()) { 
			if(tests.get(tk).isDifferent(segment)) { 
				tags.add(tk);
			}
		}
		return tags.toArray(new String[0]);
	}
}
