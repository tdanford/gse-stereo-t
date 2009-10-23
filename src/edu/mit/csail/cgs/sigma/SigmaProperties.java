/*
 * Created on Dec 17, 2007
 *
 * TODO 
 * 
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package edu.mit.csail.cgs.sigma;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.*;

import edu.mit.csail.cgs.datasets.species.*;
import edu.mit.csail.cgs.ewok.verbs.*;
import edu.mit.csail.cgs.sigma.genes.GeneAnnotationGenerator;
import edu.mit.csail.cgs.sigma.genes.GeneAnnotationProperties;
import edu.mit.csail.cgs.utils.NotFoundException;
import edu.mit.csail.cgs.utils.preferences.ReferencedProperties;

public class SigmaProperties {
    
	private String path;
    private ReferencedProperties props;
    private Logger logger;
    
    public SigmaProperties() {
        path = "edu.mit.csail.cgs.sigma.default";
        load(path);
        setupLogger();
    }

    public SigmaProperties(String name) {
    	path = name;
        load(name);
        setupLogger();
    }
    
    public String getPath() { return path; }
    
    private void setupLogger() { 
    	logger = Logger.getLogger(path);
    }
    
    public Logger getLogger() { return logger; }
    
    public Logger getLogger(String suffix) { 
    	String loggerName = path + (suffix.startsWith(".") ? "" : ".") + suffix;
    	return Logger.getLogger(loggerName);
    }
    
    private void load(String name) { 
        props = new ReferencedProperties(name);
    }

    public File getBaseDir() { 
    	return new File(props.getStringProperty("base_dir"));
    }
    
    public Collection<String> getStrains() { 
    	return props.getVectorProperty("strains");
    }
    
    public Genome getSigmaGenome() { 
        try {
            Organism org = Organism.getOrganism(props.getStringProperty("organism"));
            Genome genome = org.getGenome(props.getStringProperty("sigma_genome"));
            return genome;
        } catch (NotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public Genome getS288cGenome() { 
        try {
            Organism org = Organism.getOrganism(props.getStringProperty("organism"));
            Genome genome = org.getGenome(props.getStringProperty("s288c_genome"));
            return genome;
        } catch (NotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public Genome getGenome(String strain) { 
        try {
        	String genomeName = props.getBundle().getString(String.format("%s_genome", strain));
            return Organism.findGenome(genomeName);
        } catch(Exception nfe) { 
        	File f = new File(getBaseDir(), String.format("%s.genome", strain));
        	if(!f.exists()) { 
                throw new IllegalArgumentException(String.format("Unknown strain: %s", strain));
        	} else { 
        		try {
					Genome g = GenomeModel.loadGenome(f);
					return g;
				} catch (IOException e) {
	                throw new IllegalArgumentException(String.format("I/O Exceptoin: %s", e.getMessage()), e);
				}
        	}
        }
    }
    
    public GeneGenerator getSigmaGeneGenerator() { 
        return getGeneGenerator("sigma");
    }

    public GeneGenerator getS288cGeneGenerator() { 
        return getGeneGenerator("s288c");
    }

    public GeneGenerator getGeneGenerator(String strain) { 
        Genome g = getGenome(strain);
        String gene_annotations = props.getStringProperty(String.format("%s_gene_annotations", strain));
        try { 
        	GeneGenerator gg = new GeneGenerator.RefGeneWrapper(new RefGeneGenerator(g, gene_annotations));
        	return gg;
        } catch(Exception e) { 
        	File geneFile = new File(getBaseDir(), String.format("%s_genes.txt", strain));
        	try {
				return new GeneAnnotationGenerator(getGenome(strain), geneFile);
			} catch (IOException e1) {
				e1.printStackTrace();
				throw new IllegalArgumentException(geneFile.getName());
			}
        }
    }

	public Organism getOrganism() {
		String orgName = props.getStringProperty("organism");
		try {
			Organism org = Organism.getOrganism(orgName);
			return org;
		} catch (NotFoundException e) {
			logger.log(Level.SEVERE, String.format("Error: %s", e.getMessage()));
			return null;
		}
	}
    

}
