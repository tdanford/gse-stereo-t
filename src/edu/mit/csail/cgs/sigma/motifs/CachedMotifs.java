/*
 * Author: tdanford
 * Date: Mar 23, 2009
 */
package edu.mit.csail.cgs.sigma.motifs;

import java.util.*;
import java.io.*;

import edu.mit.csail.cgs.datasets.general.Region;
import edu.mit.csail.cgs.ewok.verbs.Expander;
import edu.mit.csail.cgs.utils.models.*;

/**
 * Data structure for stroing, loading, and manipulating a compact version 
 * of all the motifs hits in the genome.
 * 
 * @author tdanford
 */
public class CachedMotifs extends Model {
	
	public static void main(String[] args) { 
		//createCached("s288c");
		//createCached("sigma");
		
		try {
			CachedMotifs cms = new CachedMotifs(new MotifProperties(), "s288c");
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void createCached(String strain) { 
		MotifProperties ps = new MotifProperties();
		try {
			MotifHitLoader loader = new MotifHitLoader(ps, strain);
			CachedMotifs cms = new CachedMotifs(ps, strain, loader);
			cms.save();
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}
	
	private MotifProperties props;
	private String strain; 
	private Map<String,Map<String,ChromStrandCachedMotifs>> motifChromMap;
	
	public CachedMotifs(MotifProperties mps, String str) throws IOException {
		props = mps;
		strain = str;
		motifChromMap = new TreeMap<String,Map<String,ChromStrandCachedMotifs>>();

		ModelInput<ChromStrandCachedMotifs> input = 
			new ModelInput.LineReader<ChromStrandCachedMotifs>(
					ChromStrandCachedMotifs.class, new FileReader(getFile()));
		ModelInputIterator<ChromStrandCachedMotifs> itr = 
			new ModelInputIterator<ChromStrandCachedMotifs>(input);
		
		while(itr.hasNext()) { 
			ChromStrandCachedMotifs cm = itr.next();
			if(!motifChromMap.containsKey(cm.motif)) { 
				motifChromMap.put(cm.motif, new TreeMap<String,ChromStrandCachedMotifs>());
			}
			motifChromMap.get(cm.motif).put(cm.chrom, cm);
		}
	}
	
	public File getFile() { 
		File f = new File(props.getMotifDirectory(), 
				String.format("%s_cached_motifs.txt", strain));
		return f;
	}
	
	public void save() throws IOException { 
		File f = getFile();
		PrintStream ps = new PrintStream(new FileOutputStream(f));
		for(String motif : motifChromMap.keySet()) { 
			for(String chrom : motifChromMap.get(motif).keySet()) { 
				ps.println(motifChromMap.get(motif).get(chrom).asJSON().toString());
			}
		}
		ps.close();
	}

	public CachedMotifs(MotifProperties ps, String str, MotifHitLoader ldr) {
		props = ps;
		strain = str;
		motifChromMap = new TreeMap<String,Map<String,ChromStrandCachedMotifs>>();
		
		Map<String,Map<String,ArrayList<MotifHit>>> premap = 
			new TreeMap<String,Map<String,ArrayList<MotifHit>>>();

		int i = 0;
		
		while(ldr.hasNext()) { 
			MotifHit h = ldr.next();
			if(!premap.containsKey(h.motif)) { 
				premap.put(h.motif, new TreeMap<String,ArrayList<MotifHit>>());
			}
			
			if(!premap.get(h.motif).containsKey(h.chrom)) { 
				premap.get(h.motif).put(h.chrom, new ArrayList<MotifHit>());
			}
			
			premap.get(h.motif).get(h.chrom).add(h);
			i += 1;
			
			if(i % 10000 == 0) { System.out.print("."); System.out.flush(); }
			if(i % 100000 == 0) { 
				System.out.println(String.format("%dk", (i/1000)));
			}
		}
		
		System.out.println(String.format("\nLoaded %d motif hits.", i));
		
		for(String motif : premap.keySet()) { 
			motifChromMap.put(motif, new TreeMap<String,ChromStrandCachedMotifs>());
			for(String chrom : premap.get(motif).keySet()) { 
				motifChromMap.get(motif).put(chrom,
						new ChromStrandCachedMotifs(premap.get(motif).get(chrom)));
			}
		}
	}
	
	public static int binaryLeft(Integer value, Integer[] array) { 
		int lower = 0, upper = array.length-1;
		
		if(array[lower] > value) { 
			return -1; 
		}
		
		if(array[upper] < value) { 
			return upper;
		}
		
		while(upper - lower > 1) { 
			int middle = (upper+lower)/2;
			if(array[middle] == value) { 
				return middle; 
			} else if (array[middle] < value) { 
				lower = middle;
			} else { 
				upper = middle;
			}
		}
		
		while(lower < array.length-1 && array[lower+1] == array[lower]) { 
			lower += 1;
		}
		
		return lower;
	}

	public static class ChromStrandCachedMotifs extends Model {
		public String motif, chrom;
		public Integer motifWidth;
		public Integer[] watsonOffsets;
		public Integer[] crickOffsets;
		
		public Collection<MotifHit> hits(int start, int end) { 
			ArrayList<MotifHit> hits = new ArrayList<MotifHit>();
			int shiftedStart = start-motifWidth;
			
			int ls = binaryLeft(shiftedStart, watsonOffsets);
			int le = binaryLeft(end, watsonOffsets); 
			for(int i = ls; i <= le; i++) { 
				if(watsonOffsets[i] >= start) { 
					
				}
			}
			return hits;
		}
		
		public ChromStrandCachedMotifs(Collection<MotifHit> hits) { 
			Iterator<MotifHit> itr = hits.iterator();
			MotifHit first = itr.next();
			motif = first.motif;
			chrom = first.chrom;
			motifWidth = first.end-first.start+1;

			int watson = 0, crick = 0;
			for(MotifHit h : hits) { 
				if(h.strand.equals("+")) { 
					watson += 1;
				} else { 
					crick += 1;
				}
			}
			
			watsonOffsets = new Integer[watson];
			crickOffsets = new Integer[crick];
			
			int i = 0, j = 0;
			for(MotifHit h : hits) { 
				if(h.strand.equals("+")) {
					watsonOffsets[i++] = h.start;
				} else { 
					crickOffsets[j++] = h.start;
				}
			}
			
			Arrays.sort(watsonOffsets);
			Arrays.sort(crickOffsets);
		}
	}
}
