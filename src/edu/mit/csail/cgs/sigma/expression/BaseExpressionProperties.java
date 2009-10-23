/*
 * Created on Dec 17, 2007
 *
 */
package edu.mit.csail.cgs.sigma.expression;

import java.io.File;
import java.util.*;
import java.util.logging.*;

import edu.mit.csail.cgs.datasets.general.*;
import edu.mit.csail.cgs.datasets.locators.ChipChipLocator;
import edu.mit.csail.cgs.datasets.species.*;
import edu.mit.csail.cgs.sigma.GeneGenerator;
import edu.mit.csail.cgs.sigma.JLogHandlerPanel;
import edu.mit.csail.cgs.sigma.SigmaProperties;
import edu.mit.csail.cgs.utils.NotFoundException;
import edu.mit.csail.cgs.utils.preferences.ReferencedProperties;
import edu.mit.csail.cgs.ewok.verbs.*;

public abstract class BaseExpressionProperties {
    
    protected String name, path, loggerName;
    private Logger propsLogger;
    private Level logLevel;
    protected SigmaProperties sigmaProps;
    protected ReferencedProperties props;
    
    public BaseExpressionProperties(SigmaProperties sp, String n) {
        sigmaProps = sp;
        name = n;
        path = String.format("edu.mit.csail.cgs.sigma.expression.%s", name);
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
        
        logLevel = parseLogLevel(props.getBundle().getString("log_level"));
        boolean popupLogger = props.getBundle().getString("popup_logging").equals("true");
        boolean consoleLogger = props.getBundle().getString("console_logging").equals("true");
        
        propsLogger.setLevel(logLevel);
        propsLogger.setUseParentHandlers(false);

        if(popupLogger) { 
        	JLogHandlerPanel.StandaloneFrame sf = new JLogHandlerPanel.StandaloneFrame();
        	propsLogger.addHandler(sf.getHandlerPanel().createHandler());
        	sf.showFrame();
        	
        } 
        
        if(consoleLogger) { 
        	propsLogger.addHandler(new Handler() {
        		public void close() throws SecurityException {
        		}

        		public void flush() {
        		}

        		public void publish(LogRecord rec) {
        			String msg = String.format("%s: %s (%s)", rec.getLevel(), rec.getMessage(), 
        					rec.getLoggerName());
        			System.err.println(msg);
        		} 
        	});
        }
    }
    
    public Logger getLogger(String suffix) { 
    	String loggerpath = loggerName + (suffix.startsWith(".") ? "" : ".") + suffix;
    	return Logger.getLogger(loggerpath);
    }
    
    public int getMinSegmentProbes() { 
        return Integer.parseInt(props.getBundle().getString("min_segment_probes"));
    }
    
    public double getZScoreThreshold() { 
        return Double.parseDouble(props.getBundle().getString("zscore_threshold"));
    }

    public int getGeneExpressionStartWindow() { 
        return Integer.parseInt(props.getStringProperty("gene_expression_start_window"));
    }

    public double[] getBackgroundParameters() { 
        String str = props.getBundle().getString("background_params");
        String[] array = str.split(",");
        double[] params = new double[array.length];
        for(int i =0; i < array.length; i++) { 
            params[i] = Double.parseDouble(array[i]);
        }
        return params;
    }
    
    public File getDivergentSiteFile(String key, int bpwidth, boolean fg) { 
        String prop = props.getBundle().getString("divergence_file");
        String fgkey = fg ? "fg" : "bg";
        String filename = String.format(prop, key, fgkey, bpwidth);
        File baseDir = sigmaProps.getBaseDir();
        return new File(baseDir, filename);
    }
    
    public File getDivergentSiteFile(String key, int bpwidth) {
    	return getDivergentSiteFile(key, bpwidth, true);
    }
    
    public File getExpressionSegmentFile(String key) { 
        String prop = props.getBundle().getString("segment_file");
        String filename = String.format(prop, key);
        File baseDir = sigmaProps.getBaseDir();
        return new File(baseDir, filename);
    }

    public File getExpressionTranscriptFile(String key, boolean fg) { 
        String prop = props.getBundle().getString("transcript_file");
        String fgkey = fg ? "fg" : "bg";
        String filename = String.format(prop, key, fgkey);
        File baseDir = sigmaProps.getBaseDir();
        return new File(baseDir, filename);
    }
    
    public File getExpressionTranscriptFile(String key) { 
    	return getExpressionTranscriptFile(key, true);
    }
    
    public File getDifferentialRegressionFile(String key) { 
        String prop = props.getBundle().getString("differential_regression_file");
        String filename = String.format(prop, key);
        File baseDir = sigmaProps.getBaseDir();
        return new File(baseDir, filename);        
    }
    
    public File getRankedDifferentialFile(String key) { 
        String prop = props.getBundle().getString("ranked_differential_file");
        String filename = String.format(prop, key);
        File baseDir = sigmaProps.getBaseDir();
        return new File(baseDir, filename);        
    }
    
    public File getDir() { 
        File baseDir = sigmaProps.getBaseDir();
        return baseDir;
    }
    
    public ChipChipLocator createLocator(String strain, String expt, String version, String replicate) { 
        Genome genome = sigmaProps.getGenome(strain);
        return new ChipChipLocator(genome, expt, version, replicate);    	
    }
        
    public int getDivergenceWindow() { 
    	return Integer.parseInt(props.getBundle().getString("divergence_window"));
    }

	public int getSeedWidth() { 
		return Integer.parseInt(props.getBundle().getString("seed_width"));
	}
    
    public Genome getGenome(String strain) { 
    	return sigmaProps.getGenome(strain);
    }
    
    public GeneGenerator getGeneGenerator(String strain) { 
    	return sigmaProps.getGeneGenerator(strain);
    }    
    
    public abstract Collection<String> getExptKeys();
    public abstract ChipChipLocator getLocator(String key);
    public abstract String parseStrainFromExptKey(String key);
    public abstract boolean isIPData(String exptKey);
    public abstract Set<String> getArrayExptKeys(String exptKey);
}
