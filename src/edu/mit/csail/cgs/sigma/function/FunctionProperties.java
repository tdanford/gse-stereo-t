/*
 * Created on Nov 28, 2007
 */
package edu.mit.csail.cgs.sigma.function;

import java.sql.SQLException;
import java.util.*;
import java.util.logging.*;
import java.io.*;

import edu.mit.csail.cgs.datasets.general.Region;
import edu.mit.csail.cgs.datasets.species.Gene;
import edu.mit.csail.cgs.datasets.species.Genome;
import edu.mit.csail.cgs.datasets.function.*;
import edu.mit.csail.cgs.ewok.verbs.Expander;
import edu.mit.csail.cgs.sigma.SigmaProperties;
import edu.mit.csail.cgs.utils.preferences.ReferencedProperties;

public class FunctionProperties {
    
    public static void main(String[] args) { 
        FunctionProperties fps = new FunctionProperties();
        FunctionLoader loader = fps.getFunctionLoader("s288c");
        try {
            LinkedList<FunctionVersion> vs = new LinkedList<FunctionVersion>(loader.getAllVersions());
            FunctionVersion v = vs.getFirst();
            String obj = "FKH2";
            Collection<Assignment> assigns = loader.getAssignments(obj, v);
            for(Assignment assign : assigns) { 
                System.out.println(assign.getCategory().toString());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private SigmaProperties sigmaProps;
    private ReferencedProperties props;
    
    private String name, path, loggerName;
    private Logger logger;
    
    public FunctionProperties() { 
        name = "default";
        sigmaProps = new SigmaProperties();
        
        path = String.format("edu.mit.csail.cgs.sigma.function.%s", name);
        props = new ReferencedProperties(path);
        
        setupLogger();
        load();        
    }
    
    public FunctionProperties(SigmaProperties ps, String n) {
        name = n;
        path = String.format("edu.mit.csail.cgs.sigma.function.%s", name);
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
    
    public FunctionLoader getFunctionLoader(String strain) { 
        try {
            TextFunctionLoader loader = new TextFunctionLoader(getOBOFile());
            loader.addGOAFile(getAssociationFile());
            return loader;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public File getOBOFile() { 
        String filename = props.getStringProperty("obo_file");
        File dir = getDirectory();
        return new File(dir, filename);
    }

    public File getAssociationFile() { 
        String filename = props.getStringProperty("association_file");
        File dir = getDirectory();
        return new File(dir, filename);
    }
}