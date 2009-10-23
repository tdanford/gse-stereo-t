/*
 * Created on Nov 28, 2007
 */
package edu.mit.csail.cgs.sigma.alignments;

import java.sql.SQLException;
import java.util.*;
import java.util.logging.*;
import java.io.*;

import edu.mit.csail.cgs.datasets.general.Region;
import edu.mit.csail.cgs.datasets.species.Gene;
import edu.mit.csail.cgs.datasets.species.Genome;
import edu.mit.csail.cgs.datasets.species.Organism;
import edu.mit.csail.cgs.ewok.verbs.Expander;
import edu.mit.csail.cgs.sigma.SigmaProperties;
import edu.mit.csail.cgs.utils.NotFoundException;
import edu.mit.csail.cgs.utils.preferences.ReferencedProperties;

public class AlignmentProperties {

    private SigmaProperties sigmaProps;
    private ReferencedProperties props;
    
    private String name, path;
    private Logger logger;
    
    public AlignmentProperties() { 
        name = "alignment";
        sigmaProps = new SigmaProperties();
        
        path = String.format("edu.mit.csail.cgs.sigma.alignments.%s", name);
        props = new ReferencedProperties(path);
        logger = sigmaProps.getLogger();
        
        load();        
    }
    
    public AlignmentProperties(SigmaProperties ps, String n) {
        name = n;
        path = String.format("edu.mit.csail.cgs.sigma.alignments.%s", name);
        sigmaProps = ps;
        props = new ReferencedProperties(path);
        logger = sigmaProps.getLogger();
        
        load();
    }
    
    public SigmaProperties getSigmaProperties() { return sigmaProps; }
    
    private void load() { 
    }
    
    public Logger getLogger(String suffix) { 
    	return logger;
    }
    
    public String getAlignmentVersionName() { 
    	return props.getStringProperty("alignment_version");
    }
 
    public Genome getGenome(String genomeTag) { 
    	if(genomeTag.equals("s288c")) { 
    		return sigmaProps.getS288cGenome();
    	} else if (genomeTag.equals("sigma")) { 
    		return sigmaProps.getSigmaGenome();
    	} else { 
    		String value = props.getStringProperty(genomeTag);
    		try {
				return Organism.findGenome(value);
			} catch (NotFoundException e) {
				e.printStackTrace();
				return null;
			}
    	}
    }
}