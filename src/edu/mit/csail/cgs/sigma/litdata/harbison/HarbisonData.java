/*
 * Author: tdanford
 * Date: Mar 4, 2009
 */
package edu.mit.csail.cgs.sigma.litdata.harbison;

import java.util.*;
import java.util.regex.*;
import java.io.*;

import edu.mit.csail.cgs.ewok.verbs.Expander;
import edu.mit.csail.cgs.ewok.verbs.Filter;
import edu.mit.csail.cgs.ewok.verbs.Mapper;
import edu.mit.csail.cgs.sigma.genes.GeneAnnotationProperties;
import edu.mit.csail.cgs.sigma.genes.GeneNameAssociation;
import edu.mit.csail.cgs.sigma.litdata.LitDataProperties;
import edu.mit.csail.cgs.utils.SetTools;
import edu.mit.csail.cgs.utils.graphs.DirectedGraph;
import edu.mit.csail.cgs.utils.iterators.EmptyIterator;

public class HarbisonData {
	
	public static void main(String[] args) { 
		LitDataProperties lps = new LitDataProperties();
		try {
			HarbisonData data = new HarbisonData(lps);
			String name = "AQY2";
			Set<String> urn = data.upstreamRegNetworkName(name, 0.001);
			System.out.println(String.format("%s:", name));
			for(String id : data.assoc.getIDs(name)) { 
				System.out.println(String.format("\tor: %s", id));
				if(data.orfIndices.containsKey(id)) { 
					for(int idx : data.orfIndices.get(id)) { 
						System.out.println(String.format("\t\t=%d", idx));
					}
				}
			}
			for(String u : urn) { 
				System.out.println(String.format("=> %s (%s)", u, data.assoc.getName(u)));
			}
			System.out.println(String.format("#%d", urn.size()));
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static Pattern exptPattern = Pattern.compile("([^_]+)_(.+)");
	private static Pattern probePattern = Pattern.compile("(?:i)?(.+)");
	
	public static String exptFactor(String expt) { 
		Matcher m = exptPattern.matcher(expt);
		if(!m.matches()) { throw new IllegalArgumentException(expt); }
		return m.group(1);
	}
	
	public static String exptCondition(String expt) { 
		Matcher m = exptPattern.matcher(expt);
		if(!m.matches()) { throw new IllegalArgumentException(expt); }
		return m.group(2);
	}
	
	public static String probeORF(String probe) { 
		Matcher m = probePattern.matcher(probe);
		if(!m.matches()) { 
			throw new IllegalArgumentException(probe);
		}
		String p = m.group(1).trim();
		//System.out.println(String.format("\"%s\"", p));
		if(p.charAt(p.length()-2) == '-' && Character.isDigit(p.charAt(p.length()-1))) { 
			p = p.substring(0, p.length()-2);
		}
		return p;
	}
	
	private GeneNameAssociation assoc;
	private Vector<String> probes, expts;
	private Map<String,Set<Integer>> orfIndices, factorIndices;
	private Vector<Double[]> pvalues;
	
	public HarbisonData() {
		assoc = new GeneAnnotationProperties().getGeneNameAssociation("s288c");
		probes = new Vector<String>();
		expts = new Vector<String>();
		orfIndices = new TreeMap<String,Set<Integer>>();
		factorIndices = new TreeMap<String,Set<Integer>>();
		pvalues = new Vector<Double[]>();
	}
	
	public HarbisonData(LitDataProperties lps) throws IOException { 
		this(lps.getHarbisonFile());
	}
	
	public Collection<String> getProbes() { return probes; }
	public Collection<String> getExpts() { return expts; }
	
	private class FactorToORF implements Expander<String,String> {
		public Iterator<String> execute(String factor) {
			if(assoc.containsName(factor)) { 
				return assoc.getIDs(factor).iterator();
			} else { 
				return new EmptyIterator<String>();
			}
		} 
	}
	
	private class ORFToName implements Filter<String,String> {
		public String execute(String orf) {
			if(assoc.containsID(orf)) { 
				return assoc.getName(orf);
			} else { 
				return null;
			}
		} 
	}
	
	public HarbisonData(File f) throws IOException { 
		this();
		BufferedReader br = new BufferedReader(new FileReader(f));
		String line = br.readLine();
		String[] array = line.split(",");
		for(int i = 1; i < array.length; i++) { 
			expts.add(array[i]);
			String factor = exptFactor(array[i]);
			if(!factorIndices.containsKey(factor)) { 
				factorIndices.put(factor, new TreeSet<Integer>());
			}
			factorIndices.get(factor).add(i-1);
		}
		
		while((line = br.readLine()) != null) { 
			array = line.split(","); 
			if(array.length > 2) { 
				String probe = array[0];
				Double[] pvs = new Double[array.length-1];
				for(int i = 0; i < pvs.length; i++) { 
					try { 
						pvs[i] = Double.parseDouble(array[i+1]);
					} catch(NumberFormatException e) { 
						pvs[i] = null;
					}
				}
				probes.add(probe);
				pvalues.add(pvs);
				String orf = probeORF(probe);
				if(!orfIndices.containsKey(orf)) { 
					orfIndices.put(orf, new TreeSet<Integer>());
				}
				orfIndices.get(orf).add(probes.size()-1);
				System.out.println(String.format("%s -> %s (%.4f)", probe, orf, pvs[0]));
			}
		}
		
		br.close();
	}
	
	public Set<String> upstreamRegNetworkName(String name, double thresh) { 
		Set<String> urn = new TreeSet<String>();
		if(assoc.containsName(name)) { 
			for(String id : assoc.getIDs(name)) { 
				urn.addAll(upstreamRegNetwork(id, thresh));
			}
		}
		return urn;
	}
	
	public Set<String> upstreamRegNetwork(String orf, double thresh) { 
		Set<String> urn = new TreeSet<String>();
		SetTools<String> tools = new SetTools<String>();
		
		LinkedList<String> tosearch = new LinkedList<String>();
		tosearch.addAll(boundFactorORFs(orf, thresh));
		
		while(!tosearch.isEmpty()) { 
			String first = tosearch.removeFirst();
			if(!first.equals(orf) && !urn.contains(first)) { 
				urn.add(first);
				tosearch.addAll(tools.subtract(boundFactorORFs(first, thresh), urn));
			}
		}
		
		return urn;
	}
	
	public DirectedGraph upstreamRegulatoryNetwork(String orf, double thresh) { 
		DirectedGraph g = new DirectedGraph();
		return upstreamRegulatoryNetwork(g, orf, thresh);
	}
	
	public DirectedGraph upstreamRegulatoryNetwork(DirectedGraph g, String orf, double thresh) {
		LinkedList<String> toSearch = new LinkedList<String>();
		toSearch.add(orf);
		while(!toSearch.isEmpty()) { 
			String searchOrf = toSearch.removeFirst();
			if(!g.containsVertex(searchOrf)) { 
				g.addVertex(searchOrf);
			}
			
			Set<String> regs = boundFactorORFs(searchOrf, thresh);
			for(String reg : regs) { 
				if(!g.containsVertex(reg)) { 
					g.addVertex(reg);
					toSearch.add(reg);
				}				
				
				if(!g.containsEdge(reg, searchOrf)) {
					System.out.println(String.format("%s ---->> %s", reg, searchOrf));
					g.addEdge(reg, searchOrf);
				}
			}
		}
		return g;
	}
	
	private boolean isBound(int pidx, int eidx, Double thresh) { 
		Double pvalue = pvalues.get(pidx)[eidx];
		boolean bound = pvalue != null && (thresh == null || pvalue <= thresh);
		if(!bound) { 
			String probeOrf = probeORF(probes.get(pidx));
			String exptFactor = exptFactor(expts.get(eidx));
			//System.out.println(String.format("%s@%s=%.3f>%.3f", exptFactor, probeOrf, pvalue, thresh));
		}
		return bound;
	}
	
	public Set<String> boundExpts(String targetOrf, Double thresh) { 
		TreeSet<String> bound = new TreeSet<String>();
		if(orfIndices.containsKey(targetOrf)) { 
			for(int i = 0; i < expts.size(); i++) { 
				for(int pidx : orfIndices.get(targetOrf)) { 
					if(isBound(pidx, i, thresh)) { 
						bound.add(expts.get(i));
					}
				}
			}
		}
		return bound;
	}
	
	public Set<String> boundORFs(String expt, Double thresh) { 
		TreeSet<String> bound = new TreeSet<String>();
		int eidx = expts.contains(expt) ? expts.indexOf(expt) : -1;
		if(eidx != -1) {  
			for(int pidx = 0; pidx < probes.size(); pidx++) { 
				if(isBound(pidx, eidx, thresh)) { 
					bound.addAll(probeORFs(pidx));
				}
			}
		}
		return bound;
	}
	
	public Set<String> probeORFs(String probe) { 
		if(probes.contains(probe)) { 
			return probeORFs(probes.indexOf(probe));
		} else { 
			return new TreeSet<String>();
		}
	}
	
	private Set<String> probeORFs(int pidx) {
		Set<String> orfs = new TreeSet<String>();
		for(String orf : orfIndices.keySet()) { 
			if(orfIndices.get(orf).contains(pidx)) { 
				orfs.add(orf);
			}
		}
		return orfs;
	}
	
	public Set<String> boundFactorORFs(String orf, Double thresh) { 
		TreeSet<String> bound = new TreeSet<String>();
		for(String f : factorIndices.keySet()) {
			//System.out.println(String.format("%s?", f));
			if(isBound(orf, f, thresh)) { 
				if(assoc.containsName(f)) { 
					bound.addAll(assoc.getIDs(f));
				} else { 
					System.out.println(String.format("Unknown name: \"%s\"", f));
				}
			}
		}
		return bound;
	}

	public boolean isBound(String orf, String factor, Double thresh) {
		if(factorIndices.containsKey(factor)) { 
			for(int eidx : factorIndices.get(factor)) {
				if(orfIndices.containsKey(orf)) { 
					for(int pidx : orfIndices.get(orf)) { 
						if(isBound(pidx, eidx, thresh)) { 
							return true;
						}
					}
				} else { 
					//System.out.println(String.format("Unknown ORF: \"%s\"", orf));
					return false; 
				} 
			}
		} else { 
			System.out.println(String.format("Unknown factor: \"%s\"", factor));
		}
		return false;
	}
}
