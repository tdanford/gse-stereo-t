/*
 * Created on Nov 28, 2007
 */
package edu.mit.csail.cgs.sigma.genes;

import java.sql.SQLException;
import java.util.*;
import java.util.logging.*;
import java.io.*;

import edu.mit.csail.cgs.datasets.general.Region;
import edu.mit.csail.cgs.datasets.species.Gene;
import edu.mit.csail.cgs.datasets.species.Genome;
import edu.mit.csail.cgs.ewok.verbs.Expander;
import edu.mit.csail.cgs.sigma.SigmaProperties;
import edu.mit.csail.cgs.utils.preferences.ReferencedProperties;

public class GeneAnnotationProperties {

    private SigmaProperties sigmaProps;
    private ReferencedProperties props;
    
    private String name, path, loggerName;
    private Logger logger;
    
    public GeneAnnotationProperties() { 
        name = "default";
        sigmaProps = new SigmaProperties();
        
        path = String.format("edu.mit.csail.cgs.sigma.genes.%s", name);
        props = new ReferencedProperties(path);
        
        setupLogger();
        load();        
    }
    
    public GeneAnnotationProperties(SigmaProperties ps, String n) {
        name = n;
        path = String.format("edu.mit.csail.cgs.sigma.genes.%s", name);
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
    
    public GeneNameAssociation getGeneNameAssociation(String strain) {
    	/*
        try {
            Genome g = sigmaProps.getGenome(strain);
            return new GeneNameDBAssociation(g);
        } catch (Exception e) {
        	System.err.println(String.format(
        			"Couldn't get SQL association (%s), loading file instead...", e.getMessage()));
            return getGeneNameFileAssociation(strain);
        }
        */
        return getGeneNameFileAssociation(strain);
    }
    
    public GeneNameAssociation getGeneNameFileAssociation(String strain) {
    	String key = String.format("%s_gene_names", strain);
    	String filename = props.getStringProperty(key);
    	File f = new File(getDirectory(), filename);
    	try {
			return new GeneNameFileAssociation(f);
		} catch (IOException e) {
			logger.log(Level.SEVERE, String.format("getGeneNameAssociation() IOException: %s", e.getMessage()));
			return null;
		}
    }
    
    public File getPromoterFile(String strain) { 
    	String filename = String.format(props.getStringProperty("promoter_filename"), strain);
    	return new File(getDirectory(), filename);
    }
    
    public File getPromoterArchitectureFile(String strain) { 
    	String filename = String.format(props.getStringProperty("promoter_architecture_filename"), strain);
    	return new File(getDirectory(), filename);
    }
    
    public File getOrthologousGenesFile() { 
    	String filename = props.getStringProperty("orthologous_genes_file");
    	return new File(getDirectory(), filename);
    }

    public File getMergedGenesFile() { 
    	String filename = props.getStringProperty("merged_genes_file");
    	return new File(getDirectory(), filename);
    }
    
    public int getGeneAssignmentDistance() { 
    	return Integer.parseInt(props.getStringProperty("gene_assignment_distance"));
    }
    
    public Expander<Region,Gene> getGeneAssigner(String strain) { 
    	return new GeneAssignmentExpander<Region>(this, strain);
    }
}