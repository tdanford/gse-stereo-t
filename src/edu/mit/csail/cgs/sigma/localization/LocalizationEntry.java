/*
 * Author: tdanford
 * Date: Apr 28, 2008
 */
package edu.mit.csail.cgs.sigma.localization;

import java.util.*;
import java.io.*;

import edu.mit.csail.cgs.ewok.verbs.Mapper;
import edu.mit.csail.cgs.sigma.Parser;

/*
 * 
0: orfid
1: yORF
2: gene name
3: GFP tagged?
4: GFP visualized?
5: TAP visualized?
6: abundance
7: error
8: localization summary
9: ambiguous
10: mitochondrion
11: vacuole
12: spindle pole
13: cell periphery
14: punctate composite
15: vacuolar membrane
16: ER
17: nuclear periphery
18: endosome
19: bud neck
20: microtubule
21: Golgi
22: late Golgi
23: peroxisome
24: actin
25: nucleolus
26: cytoplasm
27: ER to Golgi
28: early Golgi
29: lipid particle
30: nucleus
31: bud
 */

public class LocalizationEntry {
	
	public static void main(String[] args) {
		print_headers(args);
		LocalizationProperties props = new LocalizationProperties();
		File data = props.getLocalizationDataFile();
		try {
			Parser<LocalizationEntry> parser = new Parser<LocalizationEntry>(data, new Decoder(), 1);
			while(parser.hasNext()) { 
				LocalizationEntry e = parser.next();
				System.out.println(String.format("%s:%s:%s\t%s\t%s",
						e.getOrfID(), e.getOrf(), e.getGeneName(), e.getError(),
						e.getSummary()));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void print_headers(String[] args) { 
		LocalizationProperties props = new LocalizationProperties();
		File data = props.getLocalizationDataFile();
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(data));
			String line = br.readLine();
			br.close();
			String[] a = line.split("\t");
			for(int i = 0; i < a.length; i++) { 
				System.out.print(String.format("\"%s\",", a[i]));
			}
			System.out.println();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static String[] localizationTags = new String[] { 
		"mitochondrion","vacuole","spindle pole","cell periphery","punctate composite",
		"vacuolar membrane","ER","nuclear periphery","endosome","bud neck","microtubule",
		"Golgi","late Golgi","peroxisome","actin","nucleolus","cytoplasm","ER to Golgi",
		"early Golgi","lipid particle","nucleus","bud",
	};

	private String orfID, orf, geneName;
	private boolean gfpTagged, gfpVisualized, tapVisualized;
	private Double abundanceValue;
	private String abundance;
	private String error, summary;
	private boolean ambiguous;
	private Vector<Boolean> localization;
	
	public LocalizationEntry(String line) { 
		String[] a = line.split("\t");
		orfID = a[0];
		orf = a[1];
		geneName = a[2];
		gfpTagged = a[3].equals("tagged");
		gfpVisualized = a[4].equals("visualized");
		tapVisualized = a[5].equals("TAP visualized");
		try { 
			abundance = a[6];
			abundanceValue = Double.parseDouble(abundance);
		} catch(NumberFormatException nfe) { 
			abundanceValue = null;
		}
		
		error = a[7];
		summary = a[8];
		ambiguous = !a[9].equals("F");
		localization = new Vector<Boolean>();
		
		for(int i = 10; i < a.length; i++) { 
			localization.add(a[i].equals("T"));
		}
	}
	
	public Set<String> getLocalizations() { 
		Set<String> ls = new LinkedHashSet<String>();
		for(int i = 0; i < localization.size(); i++) { 
			if(localization.get(i)) { 
				ls.add(localizationTags[i]);
			}
		}
		return ls;
	}

	public String getOrfID() { return orfID; }
	public String getOrf() { return orf; }
	public String getGeneName() { return geneName; }
	public boolean isGFPTagged() { return gfpTagged; }
	public boolean isGFPVisualized() { return gfpVisualized; }
	public boolean isTAPVisualized() { return tapVisualized; }
	
	public String getAbundance() { return abundance; }
	public Double getAbundanceValue() { return abundanceValue; }
	
	public String getError() { return error; }
	public String getSummary() { return summary; }
	public boolean isAmbiguous() { return ambiguous; }
	
	public int size() { return localization.size(); }
	public boolean isLocalized(int i) { 
		return localization.get(i);
	}

	public static class Decoder implements Mapper<String,LocalizationEntry> {
		public LocalizationEntry execute(String a) {
			return new LocalizationEntry(a);
		} 
	}
}
