package edu.mit.csail.cgs.sigma.litdata.steinmetz;

import java.io.*;
import java.util.*;

import edu.mit.csail.cgs.datasets.species.Genome;
import edu.mit.csail.cgs.sigma.litdata.*;

public class SteinmetzProperties {

	private LitDataProperties ldProps;
	private ResourceBundle bundle;
	
	public SteinmetzProperties() { 
		this("default", new LitDataProperties());
	}
	
	public SteinmetzProperties(String name, LitDataProperties ps) { 
		ldProps = ps;
		String filename = String.format("edu.mit.csail.cgs.sigma.litdata.steinmetz.%s", name);
		bundle = ResourceBundle.getBundle(filename);
	}
	
	public File getDirectory() { 
		File dir = ldProps.getDirectory();
		String dirname = bundle.getString("directory");
		return new File(dir, dirname);
	}
	
	public File getDataFile() { 
		return new File(getDirectory(), bundle.getString("datafile"));
	}
	
	public Genome getGenome() { 
		return ldProps.getSigmaProperties().getGenome("s288c");
	}
}
