/*
 * Created on Dec 17, 2007
 *
 */
package edu.mit.csail.cgs.sigma.phosphorylation;

import java.io.File;
import java.util.*;
import java.util.logging.*;

import edu.mit.csail.cgs.datasets.general.*;
import edu.mit.csail.cgs.datasets.locators.ChipChipLocator;
import edu.mit.csail.cgs.datasets.species.*;
import edu.mit.csail.cgs.sigma.JLogHandlerPanel;
import edu.mit.csail.cgs.sigma.SigmaProperties;
import edu.mit.csail.cgs.sigma.biogrid.BiogridProperties;
import edu.mit.csail.cgs.sigma.genes.GeneAnnotationProperties;
import edu.mit.csail.cgs.sigma.lethality.LethalityProperties;
import edu.mit.csail.cgs.sigma.localization.LocalizationProperties;
import edu.mit.csail.cgs.sigma.motifs.MotifProperties;
import edu.mit.csail.cgs.utils.NotFoundException;
import edu.mit.csail.cgs.utils.preferences.ReferencedProperties;
import edu.mit.csail.cgs.ewok.verbs.*;

public class PhosProperties {
    
    protected String name, path, loggerName;
    private Logger propsLogger;
    protected SigmaProperties sigmaProps;
    protected ReferencedProperties props;
    
    public PhosProperties() {
        sigmaProps = new SigmaProperties();
        name = "default";
        path = String.format("edu.mit.csail.cgs.sigma.phosphorylation.%s", name);
        props = new ReferencedProperties(path);
        setupLogger();
        
    }
    
    public PhosProperties(SigmaProperties sp, String n) {
        sigmaProps = sp;
        name = n;
        path = String.format("edu.mit.csail.cgs.sigma.phosphorylation.%s", name);
        props = new ReferencedProperties(path);
        setupLogger();
        
    }
    
    public SigmaProperties getSigmaProperties() { return sigmaProps; }

    public Level parseLogLevel(String t) { 
    	t = t.toUpperCase();
    	return Level.parse(t);
    }
    
    private void setupLogger() {
    	propsLogger = sigmaProps.getLogger(String.format("phosphorylation.%s", name));
        loggerName = propsLogger.getName();
    }
    
    public Logger getLogger(String suffix) { 
    	String loggerpath = loggerName + (suffix.startsWith(".") ? "" : ".") + suffix;
    	return Logger.getLogger(loggerpath);
    }

    public File getDirectory() { 
    	String dirname = props.getStringProperty("directory");
    	File networkDir = sigmaProps.getBaseDir();
    	return new File(networkDir, dirname);
    }
    
    public File getKinaseListFile() { 
    	File dir = getDirectory();
    	String filename = props.getStringProperty("kinase_list");
    	return new File(dir, filename);
    }
}
