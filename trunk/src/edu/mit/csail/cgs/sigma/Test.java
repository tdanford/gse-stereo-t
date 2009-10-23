package edu.mit.csail.cgs.sigma;

import java.util.*;

import edu.mit.csail.cgs.utils.*;
import edu.mit.csail.cgs.datasets.species.*;
import edu.mit.csail.cgs.datasets.general.*;
import edu.mit.csail.cgs.ewok.verbs.*;

public class Test { 

	public static void main(String[] args) { 
		try { 
			Genome g= Organism.findGenome("hg18");
			ChromRegionIterator itr = new ChromRegionIterator(g);
			long length = (long)0;
			while(itr.hasNext()) { 
				NamedRegion r = itr.next();
				int rlen = r.getWidth();
				length += (long)rlen;
			}
			System.out.println("Total Length: " + length);
		} catch(NotFoundException nfe) { 
			System.err.println(nfe.getMessage());
		}
	}
}
