package edu.mit.csail.cgs.sigma.metabolism;

import java.io.*;
import java.util.*;
import java.util.logging.*;

import edu.mit.csail.cgs.sigma.SigmaProperties;
import edu.mit.csail.cgs.utils.preferences.ReferencedProperties;

public class MetabolismProperties {

	private SigmaProperties sigmaProps;
	private ReferencedProperties props;
	private String path, name, loggerName;
	private Logger logger;
	
	private File baseDir, metabDir;
	private File networkFile, abbrevsFile;
    private Set<String> trivialReactants;
	
	public MetabolismProperties(SigmaProperties sp, String n) {
		sigmaProps = sp;
		name = n;
        trivialReactants = new HashSet<String>();
		load();
	}
	
	public MetabolismProperties() {
		sigmaProps = new SigmaProperties();
		name = "default";
        trivialReactants = new HashSet<String>();
		load();
	}
	
	private void load() { 
		path = String.format("edu.mit.csail.cgs.sigma.metabolism.%s", name);
		props = new ReferencedProperties(path);
		logger = sigmaProps.getLogger(String.format("metabolism.%s", name));
		loggerName = logger.getName();
		
		baseDir = sigmaProps.getBaseDir();
		metabDir = new File(baseDir, props.getStringProperty("directory"));
		networkFile = new File(metabDir, props.getStringProperty("network"));
		abbrevsFile = new File(metabDir, props.getStringProperty("abbrevs"));
        
		String trivialString = props.getBundle().getString("trivial_reactants");
        String[] array = trivialString.split(",");
        for(int i = 0; i < array.length; i++) { 
            trivialReactants.add(array[i].trim());
        }
	}
	
    public Set<String> getTrivialReactants() { return trivialReactants; } 
    
	public File getNetworkFile() { return networkFile; }
	public File getAbbreviationFile() { return abbrevsFile; }
	
	public int getAbbreviationHeaderLines() { 
		return Integer.parseInt(props.getBundle().getString("abbrevs_header"));
	}
	
	public String getPath() { return path; }
	
	public Logger getLogger(String suffix) { 
		String loggerpath = loggerName + (suffix.startsWith(".") ? "" : ".") + suffix;
		return Logger.getLogger(loggerpath);
	}
}
