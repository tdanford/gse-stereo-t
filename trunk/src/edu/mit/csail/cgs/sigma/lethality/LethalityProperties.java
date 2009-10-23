/*
 * Created on Nov 28, 2007
 *
 * TODO 
 * 
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package edu.mit.csail.cgs.sigma.lethality;

import java.util.*;
import java.util.logging.*;
import java.io.*;

import edu.mit.csail.cgs.sigma.SigmaProperties;
import edu.mit.csail.cgs.utils.preferences.ReferencedProperties;

public class LethalityProperties {

    private SigmaProperties sigmaProps;
    private ReferencedProperties props;
    
    private String name, path, loggerName;
    private Logger logger;
    
    private File baseDir, lethalityDir;
    private Set<String> strains;
    private Map<String,File> strainFiles;
    
    public LethalityProperties() { 
        name = "default";
        sigmaProps = new SigmaProperties();
        
        path = String.format("edu.mit.csail.cgs.sigma.lethality.%s", name);
        props = new ReferencedProperties(path);
        
        setupLogger();
        load();        
    }
    
    public LethalityProperties(SigmaProperties ps, String n) {
        name = n;
        path = String.format("edu.mit.csail.cgs.sigma.lethality.%s", name);
        sigmaProps = ps;
        props = new ReferencedProperties(path);
        
        setupLogger();
        load();
    }
    
    private void setupLogger() { 
        logger = sigmaProps.getLogger(name);
        loggerName = logger.getName();
    }
    
    private void load() { 
        baseDir = sigmaProps.getBaseDir();
        lethalityDir = new File(baseDir, props.getStringProperty("directory"));
        
        ResourceBundle bundle = props.getBundle();
        
        String strainstring = bundle.getString("strains");
        String[] strainarray = strainstring.split(",");
        strains = new TreeSet<String>();
        strainFiles = new TreeMap<String,File>();
        
        for(int i = 0; i < strainarray.length; i++) {
        	String strain = strainarray[i].trim();
        	strains.add(strain);
        	String tag = String.format("%s_lethal", strain);
        	String filename = bundle.getString(tag);
        	File file = new File(lethalityDir, filename);
        	strainFiles.put(strain, file);
        }
    }
    
    public Logger getLogger(String suffix) { 
        String loggerpath = loggerName + (suffix.startsWith(".") ? "" : ".") + suffix;
        return Logger.getLogger(loggerpath);
    }
    
    public Set<String> strains() { return strains; }
    
    public File getLethalityFile(String strain) { 
    	return strainFiles.get(strain);
    }
    
    public File getSigmaLethalFile() { return strainFiles.get("sigma"); }
    public File getS288cLethalFile() { return strainFiles.get("s288c"); }
}
