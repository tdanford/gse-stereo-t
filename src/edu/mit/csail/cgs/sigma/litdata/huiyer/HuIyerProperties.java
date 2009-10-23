package edu.mit.csail.cgs.sigma.litdata.huiyer;

import java.io.*;
import java.util.*;

import edu.mit.csail.cgs.sigma.litdata.*;

public class HuIyerProperties {

	private LitDataProperties ldProps;
	private ResourceBundle bundle;
	
	public HuIyerProperties() { 
		this("default", new LitDataProperties());
	}
	
	public HuIyerProperties(String name, LitDataProperties ps) { 
		ldProps = ps;
		String filename = String.format("edu.mit.csail.cgs.sigma.litdata.huiyer.%s", name);
		bundle = ResourceBundle.getBundle(filename);
	}
	
	public File getDirectory() { 
		File dir = ldProps.getDirectory();
		String dirname = bundle.getString("directory");
		return new File(dir, dirname);
	}
	
	public File getTFKOFile() { 
		return new File(getDirectory(), bundle.getString("tfko_expression_file"));
	}
}
