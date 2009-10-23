/*
 * Author: tdanford
 * Date: Mar 1, 2009
 */
package edu.mit.csail.cgs.sigma.motifs.analysis;

import java.util.*;
import edu.mit.csail.cgs.sigma.*;
import edu.mit.csail.cgs.sigma.genes.*;
import edu.mit.csail.cgs.sigma.motifs.*;
import edu.mit.csail.cgs.sigma.networks.*;

public class CoreRegulatoryNetwork {

	public static void main(String[] args) { 
		
		MotifProperties mprops = new MotifProperties();
		GeneAnnotationProperties gprops = new GeneAnnotationProperties();
		TFList tfs = new TFList(mprops);
		String strain = "s288c";
		
		RegulatoryNetwork network = new RegulatoryNetwork(
				mprops.getSigmaProperties(), mprops, gprops, strain);
		network.loadData();
		
		for(String tf : tfs.getTFs()) { 
		}
	}
}
