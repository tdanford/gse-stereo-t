/*
 * Author: tdanford
 * Date: May 6, 2009
 */
package edu.mit.csail.cgs.sigma.expression.transcription;

import java.io.File;
import java.util.logging.Logger;

import edu.mit.csail.cgs.sigma.SigmaProperties;
import edu.mit.csail.cgs.utils.preferences.ReferencedProperties;

public class TranscriptionProperties {
	
    private SigmaProperties sigmaProps;
    private ReferencedProperties props;
    
    private String name, path, loggerName;
    private Logger logger;
    
    public TranscriptionProperties() { 
        name = "default";
        sigmaProps = new SigmaProperties();
        
        path = String.format("edu.mit.csail.cgs.sigma.expression.transcription.%s", name);
        props = new ReferencedProperties(path);
        
        setupLogger();
        load();        
    }
    
    public TranscriptionProperties(SigmaProperties ps, String n) {
        name = n;
        path = String.format("edu.mit.csail.cgs.sigma.expression.transcription.%s", name);
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
    
    public Integer getMaxTranscripts() { 
    	return Integer.parseInt(props.getStringProperty("max_transcripts"));    	
    }

    public Double getExtraTranscripts() { 
    	return Double.parseDouble(props.getStringProperty("extra_transcripts"));    	
    }

    public Double getOverlapPenalty() { 
    	return Double.parseDouble(props.getStringProperty("overlap_penalty"));    	
    }

    public Double getAverageIntensity() { 
    	return Double.parseDouble(props.getStringProperty("average_intensity"));    	
    }

	public TranscriptionParameters getDefaultTranscriptionParameters() {
		return new TranscriptionParameters(this);
	}

	public Integer getMaxOverlap() {
    	return Integer.parseInt(props.getStringProperty("max_overlap"));    	
	}

	public Boolean isFilteringBreaks() {
    	return props.getStringProperty("filter_breaks").equals("true");    	
	}    
}