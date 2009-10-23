/*
 * Author: tdanford
 * Date: Mar 4, 2009
 */
package edu.mit.csail.cgs.sigma.litdata.huiyer;

import java.util.*;
import java.io.*;

import edu.mit.csail.cgs.sigma.genes.GeneAnnotationProperties;
import edu.mit.csail.cgs.sigma.genes.GeneNameAssociation;
import edu.mit.csail.cgs.sigma.litdata.*;

public class IyerData {
	
	public static void main(String[] args) { 
		LitDataProperties ps = new LitDataProperties();
		String reb1 = "YBR049C";
		
		String tf = reb1;
		String[] targets = new String[] { "YNL237W" };
		try {
			IyerData data = new IyerData(ps);
			
			for(int i = 0; i < targets.length; i++) { 
				System.out.println(String.format("%s -> %.3f (%f)",
						targets[i], data.intensity(tf, targets[i]),
						data.pvalue(tf, targets[i])));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private GeneNameAssociation assoc;
	private TableParser intensities, pvalues;
	private Map<String,String> orfToSampleMap, sampleToOrfMap;
	private double pvalueThreshold;
	
	public IyerData(LitDataProperties lps) throws IOException { 
		this(lps, 0.01);
	}
	
	public IyerData(LitDataProperties lps, double pvaluethresh) throws IOException { 
		intensities = new TableParser(lps.getIyerFile());
		pvalues = new TableParser(lps.getIyerPvaluesFile());
		orfToSampleMap = new TreeMap<String,String>();
		sampleToOrfMap = new TreeMap<String,String>();
		
		pvalueThreshold = pvaluethresh;
		
		BufferedReader br = new BufferedReader(new FileReader(lps.getIyerMapFile()));
		String line = null;
		while((line = br.readLine()) != null) { 
			line = line.trim();
			String[] array = line.split("\\s+");
			String orf = array[0], sample = array[1];
			orfToSampleMap.put(orf, sample);
			sampleToOrfMap.put(sample, orf);
		}
		br.close();
		
		assoc = new GeneAnnotationProperties().getGeneNameAssociation("s288c");
	}
	
	public Collection<String> findRepressors(String orf) { 
		MicroarrayProbe iprobe = intensities.expression(orf);
		MicroarrayProbe pprobe = pvalues.expression(orf);
		Set<String> reporfs = new TreeSet<String>();
		
		for(int i = 0; i < iprobe.samples.length; i++) { 
			String s = iprobe.samples[i];
			Double its = iprobe.values[i];
			Double pvalue = pprobe.values[i];
			
			if(its != null && pvalue != null) { 
				if(pvalue <= pvalueThreshold && its < 0.0) { 
					reporfs.add(sampleToOrfMap.get(s));
				}
			}
		}
		return reporfs;
	}
	
	public Double intensity(String tfOrf, String orf) { 
		MicroarrayProbe iprobe = intensities.expression(orf);
		MicroarrayProbe pprobe = pvalues.expression(orf);
		String tfSample = orfToSampleMap.get(tfOrf);
		int tfIdx = intensities.findSampleIndex(tfSample);
		
		for(int i = 0; i < iprobe.values.length; i++) {
			if(!Double.isNaN(iprobe.values[i])) { 
				if(i == tfIdx) { 
					System.out.print("\t*"); 
				} else { 
					System.out.print("\t ");
				}
				String name = sampleToOrfMap.get(iprobe.samples[i]);
				if(assoc.containsID(name)) { 
					name = assoc.getName(name);
				}
				System.out.println(String.format(" %.3f \t(%.4f)\t%s", iprobe.values[i], 
						pprobe.values[i], name));
			}
		}
		
		return iprobe.values[tfIdx];
	}

	public Double pvalue(String tfOrf, String orf) { 
		MicroarrayProbe iprobe = pvalues.expression(orf);
		String tfSample = orfToSampleMap.get(tfOrf);
		int tfIdx = pvalues.findSampleIndex(tfSample);
		return iprobe.values[tfIdx];
	}

	public Collection<String> findActivators(String orf) { 
		MicroarrayProbe iprobe = intensities.expression(orf);
		MicroarrayProbe pprobe = pvalues.expression(orf);
		Set<String> reporfs = new TreeSet<String>();
		
		for(int i = 0; i < iprobe.samples.length; i++) { 
			String s = iprobe.samples[i];
			Double its = iprobe.values[i];
			Double pvalue = pprobe.values[i];
			
			if(its != null && pvalue != null) { 
				if(pvalue <= pvalueThreshold && its > 0.0) { 
					reporfs.add(sampleToOrfMap.get(s));
				}
			}
		}
		return reporfs;
	}
}
