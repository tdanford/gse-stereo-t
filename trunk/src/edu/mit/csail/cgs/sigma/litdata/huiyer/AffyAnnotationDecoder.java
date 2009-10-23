/*
 * Author: tdanford
 * Date: Mar 3, 2009
 */
package edu.mit.csail.cgs.sigma.litdata.huiyer;

import java.io.*;
import java.util.*;
import java.util.regex.*;
import edu.mit.csail.cgs.utils.parsing.*;

public class AffyAnnotationDecoder {
	
	public static void main(String[] args) { 
		File desktop = new File("C:\\Documents and Settings\\tdanford\\Desktop");
		File affydir = new File(desktop, "Microarray_Platforms\\Affymetrix\\YG_S98");
		try {
			AffyAnnotationDecoder decoder = 
				new AffyAnnotationDecoder(new File(affydir, "YG_S98.na27.annot.csv"));
			decoder.printMapping(System.out);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static Pattern quoted = Pattern.compile("\"([^\"]+)\"");
	
	public static String unquote(String str) { 
		Matcher m = quoted.matcher(str);
		if(m.matches()) { 
			return m.group(1);
		} else { 
			return str;
		}
	}
	
	private Map<String,Integer> header;
	private Map<String,Set<String>> probeSets, ids;
	
	private String probeSetKey = "Probe Set ID";
	private String geneIDKey = "Representative Public ID";
	
	public AffyAnnotationDecoder() {
		header = new TreeMap<String,Integer>();
		probeSets = new TreeMap<String,Set<String>>();
		ids = new TreeMap<String,Set<String>>();
	}
	
	public AffyAnnotationDecoder(File f) throws IOException { 
		this();
		loadFile(f);
	} 
	
	public Collection<String> probes(String id) { 
		return ids.containsKey(id) ? ids.get(id) : new LinkedList<String>(); 
	}
	
	public Collection<String> ids(String probe) { return probeSets.get(probe); }
	
	public void printMapping(File f) throws IOException { 
		PrintStream ps = new PrintStream(new FileOutputStream(f));
		printMapping(ps);
		ps.close();
	}
	
	public void printMapping(PrintStream ps) { 
		for(String id : ids.keySet()) {
			ps.print(id);
			for(String probe : ids.get(id)) { 
				ps.print("\t" + probe);
			}
			ps.println();
		}
	}
	
	public void loadFile(File f) throws IOException { 
		CSVFileParser parser = new CSVFileParser();
		Iterator<String[]> itr = parser.parseFile(f);
		
		int i1 = 0, i2 = 0;

		while(itr.hasNext()) { 
			String[] array = itr.next();
			if(array.length > 0 && !array[0].startsWith("#")) { 
				if(header.isEmpty()) { 
					for(int i = 0; i < array.length; i++) {
						String key = unquote(array[i]);
						header.put(key, i);
						System.out.println(String.format("\"%s\"", key));
					}
					i1 = header.get(probeSetKey);
					i2 = header.get(geneIDKey);
				} else { 
					String probeSet = unquote(array[i1]);
					String targetID = unquote(array[i2]);
					if(!probeSets.containsKey(probeSet)) { 
						probeSets.put(probeSet, new TreeSet<String>());
					}
					probeSets.get(probeSet).add(targetID);
					
					if(!ids.containsKey(targetID)) { 
						ids.put(targetID, new TreeSet<String>());
					}
					ids.get(targetID).add(probeSet);
				}
			}
		}
	}
}
