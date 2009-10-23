/*
 * Author: tdanford
 * Date: May 1, 2008
 */
package edu.mit.csail.cgs.sigma.phosphorylation;

import java.io.*;
import java.util.*;

import edu.mit.csail.cgs.ewok.verbs.Mapper;
import edu.mit.csail.cgs.sigma.Parser;

/*
0: ORF name        
1: Slide_Ave_Signal(before GST)    
2: Pro_pro interaction     
3: Known substrate localization    
4: Essentiality
5: MIPSannotation     
6: Common name     
7: SGD annotation
 */

/**
 * @author tdanford
 *
 */
public class PhosEntry {
	
	public static void main(String[] args) {
		PhosProperties props = new PhosProperties();
		try {
			File kinase_file = new File(props.getDirectory(), "AKL1.txt");
			Parser<PhosEntry> p = new Parser<PhosEntry>(kinase_file, new Decoder(), 1);
			while(p.hasNext()) { 
				PhosEntry e = p.next();
				e.printEntry(System.out);
				System.out.println();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private String orf;
	private double[] slideAvgSignal;
	private String proPro;
	private String knownSubstrateLoc;
	private String essentiality;
	private String MIPSAnnotation;
	private String commonName;
	private Set<String> sgdAnnotations;
	
	public PhosEntry(String line) { 
		String[] a = line.split("\t");
		int k = 0;
		
		orf = a[k++];
		slideAvgSignal = new double[2];
		slideAvgSignal[0] = Double.parseDouble(a[k++]);
		slideAvgSignal[1] = Double.parseDouble(a[k++]);
		
		proPro = a[k++];
		knownSubstrateLoc = a[k++];
		essentiality = a[k++];
		MIPSAnnotation = a[k++];
		commonName = a[k++];
		sgdAnnotations = new LinkedHashSet<String>();
		for(int i = k; i < a.length; i++) {
			sgdAnnotations.add(a[i]);
		}
	}
	
	public String getORF() { return orf; }
	public double[] getSlideAvgSignal() { return slideAvgSignal; }
	public String getProteinProteinInteractions() { return proPro; }
	public String getKnownSubstrateLocalization() { return knownSubstrateLoc; }
	public String getEssentiality() { return essentiality; }
	public String getMIPSAnnotation() { return MIPSAnnotation; }
	public String getCommonName() { return commonName; }
	public Set<String> getSGDAnnotations() { return sgdAnnotations; }
	
	public void printEntry(PrintStream ps) { 
		ps.println(String.format("ORF: %s", orf));
		ps.println(String.format("SlideAvgSignal: %s", slideAvgSignal));
		ps.println(String.format("ProPro: %s", proPro));
		ps.println(String.format("KnownSubstractLocalization: %s", knownSubstrateLoc));
		ps.println(String.format("Essentiality: %s", essentiality));
		ps.println(String.format("MIPS Annotation: %s", MIPSAnnotation));
		ps.println(String.format("CommonName: %s", commonName));
		ps.println(String.format("SGDAnnotation: %s", sgdAnnotations));
	}
	
	public static class Decoder implements Mapper<String,PhosEntry> {
		public PhosEntry execute(String a) {
			return new PhosEntry(a);
		} 
	}
}
