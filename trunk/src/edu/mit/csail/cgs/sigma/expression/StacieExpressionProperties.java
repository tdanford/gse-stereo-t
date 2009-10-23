package edu.mit.csail.cgs.sigma.expression;

import java.io.File;
import java.util.*;
import java.util.regex.*;

import edu.mit.csail.cgs.datasets.locators.ChipChipLocator;
import edu.mit.csail.cgs.datasets.species.Genome;
import edu.mit.csail.cgs.sigma.SigmaProperties;

public class StacieExpressionProperties extends BaseExpressionProperties {
	
	public static void main(String[] args) { 
		StacieExpressionProperties props = new StacieExpressionProperties();
		for(String str : props.getExptKeys()) { 
			System.out.println(str);
		}
	}

	public StacieExpressionProperties() {
		super(new SigmaProperties(), "stacie");
	}
	
	public Genome getGenome() { return sigmaProps.getSigmaGenome(); }

	@Override
	public Collection<String> getExptKeys() {
		Vector expt_list = (Vector)props.getProperty("expt_list");
		LinkedList<String> keys = new LinkedList<String>();
		
		for(Object expt : expt_list) { 
			Vector exptPair = (Vector)expt;
			String fgCells = (String)exptPair.get(0);
			String bgCells = (String)exptPair.get(1);
			keys.add(createExptKeyFromCells(fgCells, bgCells, fgCells));
			keys.add(createExptKeyFromCells(fgCells, bgCells, bgCells));
		}
		
		return keys;
	}
	
	public String createExptKeyFromCells(String fgCells, String bgCells, String cells) { 
		return String.format("%s_%s_%s", fgCells, bgCells, cells);
	}
	
	public String getPrefix() { return props.getStringProperty("prefix"); } 
	public String getFactor() { return props.getStringProperty("factor"); }
    
	public String getVersion() {
        return props.getBundle().getString("version");
	}
	
	public static Pattern exptKeyPattern = Pattern.compile("^([^_]+)_([^_]+)_([^_]+)$");

	@Override
	public ChipChipLocator getLocator(String key) {
		Genome g = getGenome();
		String pref = getPrefix(), factor = getFactor();
		String ipCells = parseFgCellsFromExptKey(key);
		String wceCells = parseBgCellsFromExptKey(key);
		
		String version = getVersion();
		
		String expt = String.format("%s %s %s vs %s %s", pref, ipCells, factor, wceCells, factor);
		return new ChipChipLocator(g, expt, version);
	}

	@Override
	public boolean isIPData(String exptKey) {
		String fg = parseFgCellsFromExptKey(exptKey);
		String target = parseTargetCellsFromExptKey(exptKey);
		return fg.equals(target);
	}

	public String parseStrainFromExptKey(String key) {
		return "sigma";
	}

	public String parseTargetCellsFromExptKey(String key) { 
		Matcher m = exptKeyPattern.matcher(key);
		if(!m.matches()) { throw new IllegalArgumentException(key); }
		return m.group(3);
	}

	public String parseFgCellsFromExptKey(String key) { 
		Matcher m = exptKeyPattern.matcher(key);
		if(!m.matches()) { throw new IllegalArgumentException(key); }
		return m.group(1);
	}

	public String parseBgCellsFromExptKey(String key) { 
		Matcher m = exptKeyPattern.matcher(key);
		if(!m.matches()) { throw new IllegalArgumentException(key); }
		return m.group(2);
	}

	@Override
	public Set<String> getArrayExptKeys(String exptKey) {
		String fg = parseFgCellsFromExptKey(exptKey);
		String bg = parseBgCellsFromExptKey(exptKey);
		//String target = parseTargetCellsFromExptKey(exptKey);
		
		LinkedHashSet<String> keys = new LinkedHashSet<String>();
		keys.add(createExptKeyFromCells(fg, bg, fg));
		keys.add(createExptKeyFromCells(fg, bg, bg));
		
		return keys;
	}
}
