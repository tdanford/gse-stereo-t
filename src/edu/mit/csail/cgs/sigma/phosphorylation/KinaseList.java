/*
 * Author: tdanford
 * Date: May 1, 2008
 */
package edu.mit.csail.cgs.sigma.phosphorylation;

import java.io.*;
import java.util.*;

import edu.mit.csail.cgs.ewok.verbs.Mapper;
import edu.mit.csail.cgs.sigma.Parser;

public class KinaseList {
	
	private PhosProperties props;
	private File kinaseListFile; 

	public KinaseList(PhosProperties pp) { 
		props = pp;
		kinaseListFile = props.getKinaseListFile();
		if(!kinaseListFile.exists()) { 
			throw new IllegalArgumentException(kinaseListFile.getAbsolutePath());
		}
	}
	
	public File getKinaseFile(String kinaseName) { 
		return new File(props.getDirectory(), String.format("%s.txt", kinaseName.toUpperCase()));
	}
	
	public LinkedList<String> getKinaseNames() {
		LinkedList<String> names= new LinkedList<String>();

		Mapper<String,String> identity = new Mapper<String,String>() { 
			public String execute(String a) { return a; }
		};
		try {
			Parser<String> kinaseList = new Parser<String>(kinaseListFile, identity);
			while(kinaseList.hasNext()) { 
				String kinaseName = kinaseList.next();
				names.add(kinaseName);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return names;
	}
}
