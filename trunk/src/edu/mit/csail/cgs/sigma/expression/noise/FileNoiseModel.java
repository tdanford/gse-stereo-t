/*
 * Author: tdanford
 * Date: Nov 25, 2008
 */
package edu.mit.csail.cgs.sigma.expression.noise;

import edu.mit.csail.cgs.datasets.general.Point;
import edu.mit.csail.cgs.datasets.general.Region;
import edu.mit.csail.cgs.datasets.species.Genome;
import edu.mit.csail.cgs.sigma.OverlappingRegionFinder;
import java.io.*;
import java.util.*;
import java.util.regex.*;

/**
 * Reads a set of regions from a file, and indicates as noise any probe that is 
 * *not* in those regions.  This is a hack, for getting a few figures working 
 * (such that the transcript probes are drawn as dark, and every other probe is grayed out).  
 * @author tdanford
 */
public class FileNoiseModel implements NoiseModel {
	
	private static Pattern rpatt = Pattern.compile("([^:]+):(\\d+)-(\\d+)");
	private OverlappingRegionFinder regions;
	
	public FileNoiseModel(Genome g, File f) {
		ArrayList<Region> rlist = new ArrayList<Region>();
		try { 
			BufferedReader br = new BufferedReader(new FileReader(f));
			String line;
			while((line = br.readLine()) != null) { 
				line = line.trim();
				if(line.length() != 0) { 
					Matcher m = rpatt.matcher(line);
					if(m.matches()) { 
						String chrom = m.group(1);
						int start = Integer.parseInt(m.group(2));
						int end = Integer.parseInt(m.group(3));
						Region r = new Region(g, chrom, start, end);
						rlist.add(r);
					}
				}
			}
			br.close();
		} catch(IOException e) { 
			System.err.println(String.format("Couldn't load file: %s", f.getName()));
		}
		regions = new OverlappingRegionFinder(rlist);
	}

	public boolean isNoise(Point p, double value) {
		if(regions.findOverlapping(p).isEmpty()) { 
			return true;
		} else { 
			return false;
		}
	}

	public double noiseScore(Point p, double value) {
		if(isNoise(p, value)) { 
			return 1.0; 
		} else { 
			return 0.0;
		}
	}
}
