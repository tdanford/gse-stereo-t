package edu.mit.csail.cgs.sigma.probnetwork;

import java.util.*;
import java.util.logging.*;
import java.util.regex.*;

import java.io.*;

import edu.mit.csail.cgs.sigma.SigmaProperties;
import edu.mit.csail.cgs.utils.preferences.ReferencedProperties;

public class ProbNetworkProperties {
	
	private SigmaProperties sigmaProps;
	private ReferencedProperties props;
	
	private Logger logger;
	private String path, name, loggerName;
	
	public ProbNetworkProperties() { 
		name = "marcotte";
		path = String.format("edu.mit.csail.cgs.sigma.probnetwork.%s", name);
		sigmaProps = new SigmaProperties();
		props = new ReferencedProperties(path);
		logger = sigmaProps.getLogger(String.format("probnetwork.%s", name));
		loggerName = logger.getName();		
	}

	public ProbNetworkProperties(SigmaProperties sp, String n) {
		name = n;
		path = String.format("edu.mit.csail.cgs.sigma.probnetwork.%s", name);
		sigmaProps = sp;
		props = new ReferencedProperties(path);
		logger = sigmaProps.getLogger(String.format("probnetwork.%s", name));
		loggerName = logger.getName();
	}
	
	public Logger getLogger(String suffix) { 
		String loggerpath = loggerName + (suffix.startsWith(".") ? "" : ".") + suffix;
		return Logger.getLogger(loggerpath);
	}
	
	public File getMarcotteDirectory() { 
		String filename = props.getStringProperty("marcotte_directory");
		return new File(sigmaProps.getBaseDir(), filename);
	}
	
	public File getLeeSubdirectory() { 
		File marcotte = getMarcotteDirectory();
		String filename = props.getStringProperty("lee_directory");
		return new File(marcotte, filename);
	}
	
	public File getOrfEdgesFile() { 
		String filename = props.getStringProperty("orf_edges");
		return new File(getLeeSubdirectory(), filename);
	}

	public File getGeneEdgesFile() { 
		String filename = props.getStringProperty("gene_edges");
		return new File(getLeeSubdirectory(), filename);
	}
	
	public File getOrfEvidenceEdgesFile() { 
		String filename = props.getStringProperty("orf_evidence_edges");
		return new File(getLeeSubdirectory(), filename);
	}

	public File getGeneEvidenceEdgesFile() { 
		String filename = props.getStringProperty("gene_evidence_edges");
		return new File(getLeeSubdirectory(), filename);
	}
	
}
