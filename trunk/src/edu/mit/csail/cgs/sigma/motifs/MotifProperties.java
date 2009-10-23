/*
 * Created on Nov 28, 2007
 */
package edu.mit.csail.cgs.sigma.motifs;

import java.util.*;
import java.util.logging.*;
import java.io.*;

import edu.mit.csail.cgs.datasets.motifs.WeightMatrixLoader;
import edu.mit.csail.cgs.sigma.SigmaProperties;
import edu.mit.csail.cgs.utils.preferences.ReferencedProperties;

public class MotifProperties {

    private SigmaProperties sigmaProps;
    private ReferencedProperties props;
    
    private String name, path, loggerName;
    private Logger logger;
    
    public MotifProperties() { 
        name = "default";
        sigmaProps = new SigmaProperties();
        
        path = String.format("edu.mit.csail.cgs.sigma.motifs.%s", name);
        props = new ReferencedProperties(path);
        
        setupLogger();
        load();        
    }
    
    public MotifProperties(SigmaProperties ps, String n) {
        name = n;
        path = String.format("edu.mit.csail.cgs.sigma.motifs.%s", name);
        sigmaProps = ps;
        props = new ReferencedProperties(path);
        
        setupLogger();
        load();
    }
    
    public SigmaProperties getSigmaProperties() { return sigmaProps; }
    
    public String getMotifType() { return props.getStringProperty("type"); }
    public String getMotifVersion() { return props.getStringProperty("version"); }
    
    public File getMotifDirectory() {
    	String filename = props.getStringProperty("directory");
    	return new File(sigmaProps.getBaseDir(), filename);
    }
    
    public File getTFListFile() { 
    	String filename = props.getStringProperty("tf_list");
    	return new File(getMotifDirectory(), filename);
    }
    
    public double getStandardCutoffFraction() { 
        return Double.parseDouble(props.getStringProperty("standard_cutoff_fraction"));
    }
    
    private void setupLogger() { 
        logger = sigmaProps.getLogger(name);
        loggerName = logger.getName();
    }
    
    private void load() { 
    }
    
    public Logger getLogger(String suffix) { 
        String loggerpath = loggerName + (suffix.startsWith(".") ? "" : ".") + suffix;
        return Logger.getLogger(loggerpath);
    }
    
    public WeightMatrixLoader createLoader() { 
    	return new WeightMatrixLoader();
    }

	public int getPromoterWidth() {
		return Integer.parseInt(props.getStringProperty("promoter_width"));
	}
}
