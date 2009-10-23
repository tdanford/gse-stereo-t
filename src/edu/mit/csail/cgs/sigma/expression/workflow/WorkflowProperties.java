/*
 * Created on Nov 28, 2007
 */
package edu.mit.csail.cgs.sigma.expression.workflow;

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
import edu.mit.csail.cgs.sigma.expression.segmentation.SegmentationProperties;
import edu.mit.csail.cgs.sigma.expression.segmentation.sharing.ParameterSharingFactory;
import edu.mit.csail.cgs.sigma.expression.transcription.TranscriptionParameters;
import edu.mit.csail.cgs.sigma.expression.transcription.TranscriptionProperties;
import edu.mit.csail.cgs.sigma.expression.transcription.identifiers.ExhaustiveIdentifier;
import edu.mit.csail.cgs.sigma.expression.transcription.identifiers.TranscriptIdentifier;
import edu.mit.csail.cgs.sigma.expression.workflow.assessment.differential.AndDifferential;
import edu.mit.csail.cgs.sigma.expression.workflow.assessment.differential.DifferentialSpec;
import edu.mit.csail.cgs.sigma.expression.workflow.assessment.differential.DifferentialTest;
import edu.mit.csail.cgs.sigma.expression.workflow.assessment.differential.PairwiseDifferential;
import edu.mit.csail.cgs.utils.preferences.ReferencedProperties;

public class WorkflowProperties {
	
	public static void main(String[] args) { 
		WorkflowProperties props = new WorkflowProperties();
		File dir = props.getDirectory();
		String name = "5plus.data";
		File input = new File(dir, name);
		File output = props.getMostRecentWorkflowFile(input);
		
		System.out.println("-> " + output.getName());
	}
	
	public static String[] types = 
		new String[] { "data", "chunks", "packages", "segments", "clusters", "transcripts" };
	private static Pattern pr = Pattern.compile("([^\\.]+)\\.(.*)");
	private static Pattern keyPattern = Pattern.compile("([^_]+)_(.*)");

    private SigmaProperties sigmaProps;
    private ReferencedProperties props;
    private SegmentationProperties segProps;
    private TranscriptionProperties transProps;
    private Map<String,String> keyToStrainMap;
    
    private String name, path, loggerName;
    private Logger logger;
    
    public WorkflowProperties() { 
    	this(new SigmaProperties(), "default");
    }
    
    public WorkflowProperties(SigmaProperties ps, String n) {
        name = n;
        path = String.format("edu.mit.csail.cgs.sigma.expression.workflow.%s", name);
        sigmaProps = ps;
        props = new ReferencedProperties(path);
        segProps = new SegmentationProperties(sigmaProps, "default");
        transProps = new TranscriptionProperties(sigmaProps, "default");
        keyToStrainMap = new TreeMap<String,String>();
        
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
    
    public WorkflowIndexing getIndexing(String key) { 
    	return new WorkflowIndexing(this, key); 
    }
    
    public Logger getLogger(String suffix) { 
        String loggerpath = loggerName + (suffix.startsWith(".") ? "" : ".") + suffix;
        return Logger.getLogger(loggerpath);
    }
    
    public File getDirectory() { 
    	String dirname = props.getStringProperty("directory");
    	return new File(sigmaProps.getBaseDir(), dirname);
    }
    
    public Integer getNumTranscriptionSamples() { 
    	return Integer.parseInt(props.getStringProperty("transcription_samples"));
    }
    
    public SegmentationParameters getDefaultSegmentationParameters() { 
    	return segProps.getDefaultSegmentationParameters();
    }
    
    public TranscriptionParameters getDefaultTranscriptionParameters() { 
    	return transProps.getDefaultTranscriptionParameters();
    }
    
    public Integer getDefaultSplit() { 
    	return Integer.parseInt(props.getStringProperty("default_split"));
    }
    
    public Double getDifferentialPValue() { 
    	return Double.parseDouble(props.getStringProperty("differential_pvalue"));
    }
    
    public Integer getDefaultMinimum() { 
    	return Integer.parseInt(props.getStringProperty("default_minimum"));
    }
    
    public Integer getHRMADifferentialSamples() { 
    	return Integer.parseInt(props.getStringProperty("hrma_differential_samples"));
    }
    
    public Double getDefaultThreshold() { 
    	return Double.parseDouble(props.getStringProperty("default_threshold"));
    }
    
    public ParameterSharingFactory getDefaultParameterSharingFactory() { 
    	return segProps.getParameterSharingFactory();
    }
    
    public Integer[] getInputChannels(String key) { 
    	String str = props.getStringProperty(String.format("%s_input_channels", key));
    	Integer[] ch = new Integer[Integer.parseInt(str)];
    	for(int i = 0; i < ch.length; i++) { ch[i] = i; } 
    	return ch;
    }

	public TranscriptIdentifier getDefaultTranscriptIdentifier() {
		ExhaustiveIdentifier ident = new ExhaustiveIdentifier(this);
		return ident;
	}

    public File getMostRecentWorkflowFile(File f) {
    	File dir = f.getParentFile(); 
    	String filename = f.getName();
    	String name = parseWorkflowName(filename);
    	int type = findWorkflowTypeIndex(parseWorkflowType(filename));
    	File pf = f;
    	
    	if(type==-1) { return null; }
    	
    	while(type < types.length-1 && f.exists()) {
    		pf = f;
    		type += 1;
    		f = new File(dir, String.format("%s.%s", name, types[type]));
    	}

    	return pf;
    }
    
    private int findWorkflowTypeIndex(String type) {
    	if(type == null) { return 0; }
    	for(int i = 0; i < types.length; i++) { 
    		if(types[i].equals(type)) { 
    			return i;
    		}
    	}
    	return -1;
    }
    
    public String parseWorkflowType(File f) { 
    	return parseWorkflowType(f.getName());
    }
    
    public String parseWorkflowName(File f) { 
    	return parseWorkflowName(f.getName());
    }
    
    public String keyFromWorkflowName(String workflowName) {
    	Matcher m = keyPattern.matcher(workflowName);
    	String key = workflowName;
    	if(m.matches()) { key = m.group(1); } 
    	return key;
    }
    
    public String parseStrainFromKey(String key) {
    	if(key.startsWith("test")) { return "s288c"; }

    	key = keyFromWorkflowName(key);
    	
    	if(keyToStrainMap.containsKey(key)) { return keyToStrainMap.get(key); }
    	
    	String propsKey = String.format("%s_strain", key);
    	String propsValue = props.getStringProperty(propsKey);
    	if(propsValue != null) { 
    		keyToStrainMap.put(key, propsValue.trim());
    		return keyToStrainMap.get(key);
    	} else { 
    		throw new IllegalArgumentException(key);
    	}
    }
    
    public String parseWorkflowType(String filename) { 
    	Matcher m = pr.matcher(filename);
    	if(!m.matches()) { 
    		return null;
    	} else { 
    		return m.group(2);
    	}
    }

    public String parseWorkflowName(String filename) { 
    	Matcher m = pr.matcher(filename);
    	if(!m.matches()) { 
    		return null;
    	} else { 
    		return m.group(1);
    	}
    }

	public ResourceBundle getBundle() {
		return props.getBundle();
	}

}