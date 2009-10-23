/*
 * Created on Dec 17, 2007
 *
 */
package edu.mit.csail.cgs.sigma.localization;

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
import edu.mit.csail.cgs.sigma.motifs.MotifProperties;
import edu.mit.csail.cgs.utils.NotFoundException;
import edu.mit.csail.cgs.utils.preferences.ReferencedProperties;
import edu.mit.csail.cgs.ewok.verbs.*;

public class LocalizationProperties {
    
    protected String name, path, loggerName;
    private Logger propsLogger;
    protected SigmaProperties sigmaProps;
    protected ReferencedProperties props;
    
    public LocalizationProperties() {
        sigmaProps = new SigmaProperties();
        name = "default";
        path = String.format("edu.mit.csail.cgs.sigma.localization.%s", name);
        props = new ReferencedProperties(path);
        setupLogger();
    }
    
    public LocalizationProperties(SigmaProperties sp, String n) {
        sigmaProps = sp;
        name = n;
        path = String.format("edu.mit.csail.cgs.sigma.localization.%s", name);
        props = new ReferencedProperties(path);
        setupLogger();
    }
    
    public SigmaProperties getSigmaProperties() { return sigmaProps; }
    
    public Level parseLogLevel(String t) { 
    	t = t.toUpperCase();
    	return Level.parse(t);
    }
    
    private void setupLogger() {
    	propsLogger = sigmaProps.getLogger(String.format("expression.%s", name));
        loggerName = propsLogger.getName();
    }
    
    public Logger getLogger(String suffix) { 
    	String loggerpath = loggerName + (suffix.startsWith(".") ? "" : ".") + suffix;
    	return Logger.getLogger(loggerpath);
    }
    
    public Collection<String> getLocales() { 
        LinkedList<String> locales = new LinkedList<String>();
        for(int i = 0; i < LocalizationEntry.localizationTags.length; i++) { 
            locales.addLast(LocalizationEntry.localizationTags[i]);
        }
        return locales;
    }

    public File getDirectory() { 
    	String dirname = props.getStringProperty("directory");
    	File networkDir = sigmaProps.getBaseDir();
    	return new File(networkDir, dirname);
    }
    
    public File getLocalizationDataFile() { 
    	File dir = getDirectory();
    	String filename = props.getStringProperty("datafile");
    	return new File(dir, filename);
    }
}
