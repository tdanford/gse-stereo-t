package edu.mit.csail.cgs.sigma.biogrid;

import java.util.*;
import java.io.*;

import edu.mit.csail.cgs.utils.preferences.ReferencedProperties;
import edu.mit.csail.cgs.sigma.SigmaProperties;

public class BiogridProperties {
	
	private SigmaProperties sigmaProps;
	private ReferencedProperties props;
	private String name, path;

	public BiogridProperties() {
		name = "default";
		path = String.format("edu.mit.csail.cgs.sigma.biogrid.%s", name);
		sigmaProps = new SigmaProperties();
		props = new ReferencedProperties(path);
	}

	public BiogridProperties(SigmaProperties sp, String n) {
		name = n;
		path = String.format("edu.mit.csail.cgs.sigma.biogrid.%s", name);
		sigmaProps = sp;
		props = new ReferencedProperties(path);
	}
    
    public File getDirectory() { 
        File sigma = sigmaProps.getBaseDir();
        String filename = props.getStringProperty("directory");
        return new File(sigma, filename);
    }
    
    public File getBiogridFile() { 
        File directory = getDirectory();
        String filename = props.getStringProperty("datafile");
        return new File(directory, filename);
    }
}
