/*
 * Created on Nov 28, 2007
 */
package edu.mit.csail.cgs.sigma.expression.segmentation;

import java.util.*;
import java.util.regex.*;
import java.util.logging.*;
import java.io.*;
import java.sql.SQLException;

import edu.mit.csail.cgs.datasets.general.Region;
import edu.mit.csail.cgs.datasets.species.Gene;
import edu.mit.csail.cgs.datasets.species.Genome;
import edu.mit.csail.cgs.ewok.verbs.Expander;
import edu.mit.csail.cgs.sigma.SigmaProperties;
import edu.mit.csail.cgs.sigma.expression.segmentation.SegmentationParameters;
import edu.mit.csail.cgs.sigma.expression.segmentation.sharing.AllOrNothingSharingFactory;
import edu.mit.csail.cgs.sigma.expression.segmentation.sharing.DefaultSharingFactory;
import edu.mit.csail.cgs.sigma.expression.segmentation.sharing.GroupedSharingFactory;
import edu.mit.csail.cgs.sigma.expression.segmentation.sharing.NoSharingFactory;
import edu.mit.csail.cgs.sigma.expression.segmentation.sharing.ParameterSharing;
import edu.mit.csail.cgs.sigma.expression.segmentation.sharing.ParameterSharingFactory;
import edu.mit.csail.cgs.utils.preferences.ReferencedProperties;

public class SegmentationProperties {
	
    private SigmaProperties sigmaProps;
    private ReferencedProperties props;
    
    private String name, path, loggerName;
    private Logger logger;
    
    public SegmentationProperties() { 
        name = "default";
        sigmaProps = new SigmaProperties();
        
        path = String.format("edu.mit.csail.cgs.sigma.expression.segmentation.%s", name);
        props = new ReferencedProperties(path);
        
        setupLogger();
        load();        
    }
    
    public SegmentationProperties(SigmaProperties ps, String n) {
        name = n;
        path = String.format("edu.mit.csail.cgs.sigma.expression.segmentation.%s", name);
        sigmaProps = ps;
        props = new ReferencedProperties(path);
        
        setupLogger();
        load();
    }
    
    public SigmaProperties getSigmaProperties() { return sigmaProps; }
    
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
    
    public File getDirectory() { 
    	String dirname = props.getStringProperty("directory");
    	return new File(sigmaProps.getBaseDir(), dirname);
    }

    public Double getProbSplit() { 
    	return Double.parseDouble(props.getStringProperty("probSplit"));
    }
    public Double getProbShare() { 
    	return Double.parseDouble(props.getStringProperty("probShare"));
    }
    public Integer getMinSegmentLength() { 
    	return Integer.parseInt(props.getStringProperty("minSegmentLength"));
    }
    public Double getProbLine() { 
    	return Double.parseDouble(props.getStringProperty("probLine"));
    }
    public Double getLineVarPenalty() { 
    	return Double.parseDouble(props.getStringProperty("lineVarPenalty"));
    }
    public Double getFlatVarPenalty() { 
    	return Double.parseDouble(props.getStringProperty("flatVarPenalty"));
    }
    public Double getFlatIntensityPenalty() { 
    	return Double.parseDouble(props.getStringProperty("flatIntensityPenalty"));
    }
    
    public ParameterSharingFactory getParameterSharingFactory() { 
    	//return new NoSharingFactory();
    	
    	String shareFalse = props.getStringProperty("share_false");
    	String shareTrue = props.getStringProperty("share_true");
    	
    	DefaultSharingFactory factory = new DefaultSharingFactory();
    	
    	factory.addSharing(new ParameterSharing(Math.log(1.0 - getProbShare()), shareFalse));
    	factory.addSharing(new ParameterSharing(Math.log(getProbShare()), shareTrue));
    	
    	return factory;
    	
    	/*
    	if(key.equals("default")) { 
    		return new AllOrNothingSharingFactory(this);
    	} else { 
    		String[] array = key.split("\\s+");
    		if(array[0].equals("grouped")) {
    			int groups = Integer.parseInt(array[1]);
    			return new GroupedSharingFactory(this, groups);
    		}
    	}
    	throw new IllegalArgumentException(String.format("Unknown sharing parameter \"%s\"", 
    			key));
    	*/
    }
    
    public SegmentationParameters getDefaultSegmentationParameters() { 
    	return new SegmentationParameters(this);
    }
}
