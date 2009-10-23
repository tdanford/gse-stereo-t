/*
 * Created on Dec 17, 2007
 *
 */
package edu.mit.csail.cgs.sigma.expression;

import java.io.File;
import java.util.*;
import java.util.logging.*;
import java.util.regex.*;

import edu.mit.csail.cgs.datasets.general.*;
import edu.mit.csail.cgs.datasets.locators.ChipChipLocator;
import edu.mit.csail.cgs.datasets.species.*;
import edu.mit.csail.cgs.sigma.JLogHandlerPanel;
import edu.mit.csail.cgs.sigma.SigmaProperties;
import edu.mit.csail.cgs.utils.NotFoundException;
import edu.mit.csail.cgs.utils.preferences.ReferencedProperties;
import edu.mit.csail.cgs.ewok.verbs.*;

public class SudeepExpressionProperties extends BaseExpressionProperties {

	public static Pattern strainOriginalCellsPattern = Pattern.compile("^original_([^_]+)_(.*)$");
	public static Pattern strainReplicateCellsPattern = Pattern.compile("^replicate_([^_]+)_(.*)$");
	
	public static String ime4key = "sigma_IME4";

    public static void main(String[] args) {
        SudeepExpressionProperties props = new SudeepExpressionProperties();
        
        //ChipChipLocator loc = props.getLocator("s288c", "mat_a");
        //System.out.println("Found locator: " + loc);
    }
    
    public SudeepExpressionProperties() {
    	super(new SigmaProperties(), "sudeep");
    }

    public SudeepExpressionProperties(SigmaProperties sp, String n) {
    	super(sp, n);
    }
    
    public File getDivergentSiteFile(String strain, String cells, int bp) { 
    	return super.getDivergentSiteFile(createExptKey(strain, cells), bp);
    }
    
    public File getExpressionSegmentFile(String strain, String cells) {
    	return super.getExpressionSegmentFile(createExptKey(strain, cells));
    }

    public File getExpressionTranscriptFile(String strain, String cells) {
    	return super.getExpressionTranscriptFile(createExptKey(strain, cells));
    }
    
    public File getDifferentialRegressionFile(String strain, String cells) { 
    	return super.getDifferentialRegressionFile(createExptKey(strain, cells));
    }
    
    public File getRankedDifferentialFile(String strain, String cells) { 
    	return super.getRankedDifferentialFile(createExptKey(strain, cells));
    }
                
    /**
     * @param strain {sigma, s288c}
     * @param cells {diploid, mat_a, mat_alpha}
     * @return
     */
    public ChipChipLocator getLocator(String strain, String cells) {
        ResourceBundle bundle = props.getBundle();
        
        String version = bundle.getString("version");
        String prefix = bundle.getString("prefix");
        String factor = bundle.getString("factor");
        String condition = bundle.getString("condition");
        String cells1 = bundle.getString(String.format("sigma_%s", cells)); 
        String cells2 = bundle.getString(String.format("s288c_%s", cells)); 
        String replicate = bundle.getString(String.format("%s_array", strain));
        
        String expt = String.format("%s %s:%s:%s vs %s:%s:%s", 
                prefix, factor, cells1, condition, factor, cells2, condition);
        
        return createLocator(strain, expt, version, replicate);
    }

    public ChipChipLocator getReplicateLocator(String strain, String cells) {
        ResourceBundle bundle = props.getBundle();
        
        String version = bundle.getString("replicate_version");
        String prefix = bundle.getString("prefix");
        String factor = bundle.getString("factor");
        String condition = bundle.getString("condition");
        String cells1 = bundle.getString(String.format("sigma_%s", cells)); 
        String cells2 = bundle.getString(String.format("s288c_%s", cells)); 
        String replicate = bundle.getString(String.format("%s_replicate_array", strain));
        
        String expt = String.format("%s %s:%s:%s vs %s:%s:%s", 
                prefix, factor, cells1, condition, factor, cells2, condition);
        
        return createLocator(strain, expt, version, replicate);
    }
    
    public ChipChipLocator getIME4Locator() { 
    	ResourceBundle bundle = props.getBundle();
    	String name = bundle.getString("ime4_name");
    	String version = bundle.getString("ime4_version");
    	String replicate = bundle.getString("ime4_replicate");
    	
    	return createLocator("sigma", name, version, replicate);
    }

    /**
     * @param strain {sigma, s288c}
     * @param cells {diploid, mat_a, mat_alpha}
     * @param replicateKey {s288c_array, sigma_array}
     * @return
     */
    public ChipChipLocator getLocator(String strain, String cells, String replicateKey) {
        ResourceBundle bundle = props.getBundle();
        
        String version = bundle.getString("version");
        String prefix = bundle.getString("prefix");
        String factor = bundle.getString("factor");
        String condition = bundle.getString("condition");
        String cells1 = bundle.getString(String.format("sigma_%s", cells)); 
        String cells2 = bundle.getString(String.format("s288c_%s", cells)); 
        String replicate = bundle.getString(replicateKey);
        
        String expt = String.format("%s %s:%s:%s vs %s:%s:%s", 
                prefix, factor, cells1, condition, factor, cells2, condition);
        
        return createLocator(strain, expt, version, replicate);
    }

    public ChipChipLocator getReplicateLocator(String strain, String cells, String replicateKey) {
        ResourceBundle bundle = props.getBundle();
        
        String version = bundle.getString("replicate_version");
        String prefix = bundle.getString("prefix");
        String factor = bundle.getString("factor");
        String condition = bundle.getString("condition");
        String cells1 = bundle.getString(String.format("sigma_%s", cells)); 
        String cells2 = bundle.getString(String.format("s288c_%s", cells)); 
        String replicate = bundle.getString(replicateKey);
        
        String expt = String.format("%s %s:%s:%s vs %s:%s:%s", 
                prefix, factor, cells1, condition, factor, cells2, condition);
        
        return createLocator(strain, expt, version, replicate);
    }
    
    private boolean isIPStrain(String strain) { 
    	return strain.equals(props.getBundle().getString("ip_strain"));
    }
    
    public boolean isIPData(String exptKey) { 
    	if(exptKey.equals(ime4key)) { return false; }
    	return isIPStrain(parseStrainFromExptKey(exptKey));
    }
    
    public Collection<String> getStrainCells(String strain) { 
        String key = String.format("%s_cells", strain);
        return props.getVectorProperty(key);
    }

    public Collection<String> getStrainReplicateCells(String strain) { 
        String key = String.format("%s_replicate_cells", strain);
        return props.getVectorProperty(key);
    }

    public Collection<String> getStrainCellKeys(String strain) { 
        String key = String.format("%s_cell_keys", strain);
        return props.getVectorProperty(key);
    }
    
    public Collection<String> getStrainReplicateCellKeys(String strain) { 
        String key = String.format("%s_replicate_cell_keys", strain);
        return props.getVectorProperty(key);
    }
    
    @Override
    public String parseStrainFromExptKey(String key) { 
    	if(ime4key.matches(key)) { return "sigma"; }
		Matcher mo = strainOriginalCellsPattern.matcher(key);
		Matcher mr = strainReplicateCellsPattern.matcher(key);
		if(!mo.matches() && !mr.matches()) {
			throw new IllegalArgumentException(key);
		}
		return mo.matches() ? mo.group(1) : mr.group(1);
    }
    
    public String parseCellsFromExptKey(String key) { 
		Matcher mo = strainOriginalCellsPattern.matcher(key);
		Matcher mr = strainReplicateCellsPattern.matcher(key);
		if(!mo.matches() && !mr.matches()) {
			throw new IllegalArgumentException(key);
		}
		return mo.matches() ? mo.group(2) : mr.group(2);
    }
    
    public boolean isReplicateKey(String key) { 
    	Matcher m = strainReplicateCellsPattern.matcher(key);
    	return m.matches();
    }

    public boolean isOriginalKey(String key) { 
    	Matcher m = strainOriginalCellsPattern.matcher(key);
    	return m.matches();
    }
    
    public boolean isIME4Key(String key) { 
    	return ime4key.equals(key);
    }

	public String createExptKey(String strain, String cells) { 
		return String.format("original_%s_%s", strain, cells);
	}
	
	public String createReplicateExptKey(String strain, String cells) { 
		return String.format("replicate_%s_%s", strain, cells);
	}
	
	public Collection<String> getExptKeys() {
		LinkedList<String> keys = new LinkedList<String>();
		for(String strain : sigmaProps.getStrains()) { 
			for(String cells : getStrainCellKeys(strain)) { 
				keys.addLast(createExptKey(strain, cells));
			}
		}
		for(String strain : sigmaProps.getStrains()) { 
			for(String cells : getStrainReplicateCellKeys(strain)) { 
				keys.addLast(createReplicateExptKey(strain, cells));
			}
			keys.addLast(ime4key);
		}
		return keys;
	}
	
	public Collection<String> getStrainExptKeys(String strain) {
		LinkedList<String> keys = new LinkedList<String>();
		for(String cells : getStrainCellKeys(strain)) { 
			keys.addLast(createExptKey(strain, cells));
		}
		if(strain.equals("sigma")) { keys.addLast(ime4key); }
		return keys;
	}	
	@Override
	public ChipChipLocator getLocator(String key) {
		if(isIME4Key(key)) { 
			return getIME4Locator();
		} else if(isOriginalKey(key)) { 
			return getLocator(
					parseStrainFromExptKey(key),
					parseCellsFromExptKey(key));
		} else { 
			return getReplicateLocator(
					parseStrainFromExptKey(key),
					parseCellsFromExptKey(key));			
		}
	}

	@Override
	public Set<String> getArrayExptKeys(String exptKey) {
		TreeSet<String> keys = new TreeSet<String>();
		keys.add(exptKey);
		return keys;
	}    
}
