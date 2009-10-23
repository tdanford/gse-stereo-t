package edu.mit.csail.cgs.sigma.litdata.miura;

import java.util.*;
import java.util.regex.*;
import java.io.*;

import edu.mit.csail.cgs.sigma.expression.workflow.WorkflowProperties;
import edu.mit.csail.cgs.sigma.expression.workflow.models.RegionKey;
import edu.mit.csail.cgs.utils.iterators.EmptyIterator;
import edu.mit.csail.cgs.utils.parsing.alignment.*;
import edu.mit.csail.cgs.ewok.verbs.*;

public class MiuraPSLParser {
	
	public static void main(String[] args) { 
		WorkflowProperties props = new WorkflowProperties();
		File miura = new File(props.getDirectory(), "Miura_ESTs.psl");
		try {
			MiuraPSLParser parser = new MiuraPSLParser(miura);
			Iterator<String> keys = parser.uniquelyMappedKeys();
			while(keys.hasNext()) { 
				String key = keys.next();
				Iterator<MiuraHit> hits = parser.getHits(key);
				while(hits.hasNext()) { 
					System.out.println(hits.next().asJSON().toString());
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static Pattern keyPattern = Pattern.compile("^([^|]+).*");
	private static Pattern chromPattern = Pattern.compile("chr(.*)");
	
	private Map<String,Set<BlatPSLEntry>> hits;

	public MiuraPSLParser(File f) throws IOException { 
		BlatPSLParser parser = new BlatPSLParser();
		
		hits = new TreeMap<String,Set<BlatPSLEntry>>();
		
		Iterator<BlatPSLEntry> entries = parser.parse(f);
		entries = new FilterIterator<BlatPSLEntry,BlatPSLEntry>(new HitFilter(), entries);
		
		int n = 0, h = 0;
		
		while(entries.hasNext()) { 
			BlatPSLEntry e = entries.next();
			n += 1;
			
			String queryName = e.getQname();
			Matcher matcher = keyPattern.matcher(queryName);
			
			if(matcher.matches()) { 
				String estName = matcher.group(1);
				addHit(estName, e);
				h+= 1;
				
			} else { 
				throw new IllegalArgumentException(queryName);
			}
		}
		
		System.out.println(String.format("Parsed %d lines -> %d hits.", n, h));
	}
	
	public static class HitMapper implements Mapper<BlatPSLEntry,MiuraHit> { 
		public MiuraHit execute(BlatPSLEntry e) { 
			Matcher m = chromPattern.matcher(e.getTname());
			if(!m.matches()) { throw new IllegalArgumentException(e.getTname()); }
			String chrom = m.group(1);
			int start = e.getTstart(), end = e.getTend();
			String strand = String.valueOf(e.getStrand());
			m = keyPattern.matcher(e.getQname());
			if(!m.matches()) { throw new IllegalArgumentException(e.getQname()); }
			String keyName = m.group(1);
			return new MiuraHit(keyName, chrom, start, end, strand);
		}
	}
	
	public int size() { return hits.size(); }
	
	public Iterator<String> keys() { return hits.keySet().iterator(); }
	
	public int getNumHits(String key) { return hits.get(key).size(); }
	
	public Iterator<MiuraHit> getHits(String key) { 
		return new MapperIterator<BlatPSLEntry,MiuraHit>(new HitMapper(), hits.containsKey(key) ? hits.get(key).iterator() : new EmptyIterator<BlatPSLEntry>());
	}
	
	public Iterator<String> uniquelyMappedKeys() { 
		return new FilterIterator<String,String>(new Filter<String,String>() { 
			public String execute(String key) { 
				return hits.containsKey(key) && hits.get(key).size() == 1 ? key : null;
			}
		}, keys());
	}
	
	public static class HitFilter implements Filter<BlatPSLEntry,BlatPSLEntry> { 
	
		public BlatPSLEntry execute(BlatPSLEntry e) { 
			int qlength = e.getQsize();
			int match = e.getMatch();
			int mismatch = e.getMismatch();
			int qgapBases = e.getQgapBases();
			int ns = e.getNs();
			
			double f = (double)match / (double)qlength;
			int tgapbases = e.getTgapBases();
			int qstart = e.getQstart(), qend = e.getQend();
			
			if(qstart == 0 && qend == qlength && f >= 0.95 && tgapbases <= 10) {
				return e;
			} else { 
				return null;
			}
		}
	}
	
	public void addHit(String estName, BlatPSLEntry entry) { 
		if(!hits.containsKey(estName)) { 
			hits.put(estName, new HashSet<BlatPSLEntry>());
		}
		hits.get(estName).add(entry);
	}
}
