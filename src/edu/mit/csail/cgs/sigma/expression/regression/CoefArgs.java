/*
 * Author: tdanford
 * Date: Nov 11, 2008
 */
package edu.mit.csail.cgs.sigma.expression.regression;

import java.io.File;

import edu.mit.csail.cgs.utils.models.ArgumentModel;
import edu.mit.csail.cgs.utils.models.Arguments;

public class CoefArgs extends ArgumentModel {
	
	public static File coefFile(File dir, String key) { 
		String name = String.format("%s_coef.txt", key);
		return new File(dir, name);
	}
	
	public static String[] required = { "keys" };
	
	public String dir = "C:\\Documents and Settings\\tdanford\\Desktop\\Rtables";
	public String compare = "intercept";  // should be "intercept", "offset", or "fg".  
	public String[] keys; 
	
	public File dir() { 
		return dir == null ? null : new File(dir);
	}
	
	public File[] coefs() { 
		File base = dir();
		File[] farray = new File[keys.length];
		for(int i = 0; i < keys.length; i++) { 
			farray[i] = coefFile(base, keys[i]);
		}
		return farray;
	}
	
	public static CoefArgs parse(String[] args) { 
		Arguments<CoefArgs> a = new Arguments<CoefArgs>(CoefArgs.class, "keys");
		CoefArgs ca = a.parse(args);
		
		if(!ca.checkArgs()) { 
			System.err.println(ca.getArgErrors());
			System.exit(1);
		}
		
		return ca;
	}
}