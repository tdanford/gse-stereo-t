/*
 * Author: tdanford
 * Date: Jan 20, 2009
 */
package edu.mit.csail.cgs.sigma.expression.workflow;

import java.io.*;
import java.util.*;

import edu.mit.csail.cgs.sigma.expression.workflow.models.FileInputData;
import edu.mit.csail.cgs.sigma.expression.workflow.models.InputSegmentation;
import edu.mit.csail.cgs.sigma.expression.workflow.models.RegionKey;

/**
 * This is a PackageLoader, in that it is an Iterator<FileInputData>.  However, 
 * the trick here is that we want, in some cases, to be able to *restart* segmentation
 * runs which were prematurely aborted or stopped.  In this case, the .segments file
 * will contain segmentations for some, but not all, of the packages in a .packages file.
 * 
 * This class, therefore, will read in *both* the .packages and .segments files, and it 
 * will be an iterator over *only* the packages for which there is not a segmentation 
 * given in the .segments file.  
 * 
 * This is used by Worfklow to "complete" incomplete segmentation runs. 
 * 
 * @author tdanford
 */
public class IncompletePackageLoader implements Iterator<FileInputData> {
	
	private LinkedHashMap<RegionKey,FileInputData> packages;
	private Iterator<RegionKey> keys;
	private int size;
	
	public IncompletePackageLoader(File pkgs, File segs) throws IOException {
		packages = new LinkedHashMap<RegionKey,FileInputData>();
		
		WorkflowPackageReader pkgReader = new WorkflowPackageReader(pkgs);
		while(pkgReader.hasNext()) { 
			FileInputData pkg = pkgReader.next();
			int len = pkg.length();
			int start = pkg.locations()[0], end = pkg.locations()[len-1];
			RegionKey key = new RegionKey(pkg.chrom(), start, end, pkg.strand());
			packages.put(key, pkg);
		}
		pkgReader.close();
		System.out.println(String.format("Loaded %d packages...", packages.size()));
		
		WorkflowSegmentationReader segReader = new WorkflowSegmentationReader(segs);
		int numRemoved = 0;
		while(segReader.hasNext()) { 
			InputSegmentation seg = segReader.next();
			FileInputData pkg = seg.input;
			int len = pkg.length();
			int start = pkg.locations()[0], end = pkg.locations()[len-1];
			RegionKey key = new RegionKey(pkg.chrom(), start, end, pkg.strand());
			if(packages.containsKey(key)) { 
				packages.remove(key);
				numRemoved += 1;
			}
		}
		size = packages.size();
		System.out.println(String.format("\t%d Removed.", numRemoved));
		System.out.println(String.format("\t# Packages Remaining: %d", size));
		segReader.close();
		
		keys = packages.keySet().iterator();
	}
	
	public int size() { return size; }
	
	public boolean hasNext() {
		return keys.hasNext();
	}

	public FileInputData next() {
		return packages.get(keys.next());
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}
}
