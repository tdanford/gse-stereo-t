package edu.mit.csail.cgs.sigma.litdata.huiyer;

import java.util.*;
import java.io.*;

public class TFKOParser {
	
	private String[] headers;
	private Map<String,String> headerMap;
	private HashMap<String, KOLine> lines;
	
	public TFKOParser() throws IOException { 
		this(new HuIyerProperties());
	}
	
	public TFKOParser(HuIyerProperties ps) throws IOException { 
		this(ps.getTFKOFile());
	}
	
	public TFKOParser(File f) throws IOException { 
		BufferedReader br = new BufferedReader(new FileReader(f));
		String line;
		lines = new HashMap<String,KOLine>();
		headerMap = new HashMap<String,String>();
		
		while((line = br.readLine()) != null) { 
			if(headers == null) { 
				headers = line.split("\\t");
				for(int i = 1; i < headers.length; i++) { 
					String[] a = headers[i].split("\\s+");
					String tag = a[0].toUpperCase();
					headerMap.put(tag, headers[i]);
				}
			} else { 
				KOLine kol = new KOLine(line, headers);
				lines.put(kol.id, kol);
			}
		}
		
		br.close();
	}
	
	public Set<String> getExpts() { return headerMap.keySet(); }
	public Set<String> getIDs() { return lines.keySet(); }
	
	public Map<String,Double> getExptValues(String expt) { 
		Map<String,Double> values = new TreeMap<String,Double>();
		String header = headerMap.get(expt);
		for(String id : lines.keySet()) { 
			KOLine line = lines.get(id);
			Double value = line.values.get(header);
			values.put(id, value);
		}
		return values;
	}

	public Map<String,Double> getIDValues(String id) { 
		KOLine line = lines.get(id);
		Map<String,Double> values = new TreeMap<String,Double>();
		for(String tag : headerMap.keySet()) { 
			String header = headerMap.get(tag);
			Double value = line.values.get(header);
			values.put(tag, value);
		}
		return values;
	}
	
}

class KOLine { 
	public String id;
	public Map<String,Double> values;
	
	public KOLine(String line, String[] headers) { 
		String[] array = line.split("\\t");
		id = array[0];
		values = new HashMap<String,Double>();
		for(int i = 1; i < array.length; i++) { 
			Double v = null;
			try { 
				v = Double.parseDouble(array[i]);
			} catch(NumberFormatException nfe) { 
				v = null;
			}
			
			values.put(headers[i], v);
		}
	}
}