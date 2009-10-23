/*
 * Author: tdanford
 * Date: Aug 24, 2008
 */
package edu.mit.csail.cgs.sigma.alignments;


import java.util.regex.*;
import java.io.*;

/** java.util package **/
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/********************/
/** Sigma Project **/
/******************/
/** cgs.sigma.alignments **/


/******************/
/** Gse Project **/
/****************/
/** cgs.datasets.alignments **/
import edu.mit.csail.cgs.datasets.alignments.GappedAlignmentString;
import edu.mit.csail.cgs.datasets.alignments.MultipleAlignment;
import edu.mit.csail.cgs.datasets.alignments.parsing.ClustalParser;

/** cgs.utils.stats **/
import edu.mit.csail.cgs.utils.stats.StatUtil;


/**
 * The <tt>ClustalMultipleAlignment</tt> class provides essentially a <tt>MultipleAlignment</tt> 
 * representation of a multiple alignment from a file of ALN/CLUSTALW format.</br>
 * It stores all sequences in a map whose keys are the names of the sequences and values the sequences 
 * in <tt>GappedAlignmentString</tt> format.
 * @see MultipleAlignment
 * @see GappedAlignmentString
 * @author Tim Danford
 *
 */
public class ClustalMultipleAlignment extends MultipleAlignment {

	public ClustalMultipleAlignment(ClustalParser cp) {
		super();
		for(String species : cp.getSpecies()) { 
			String seq = cp.getSequence(species);
			char[] gapped = seq.toCharArray();
			addGappedAlignment(species, new GappedAlignmentString(gapped));
		}
	}
}//end of ClustalMultipleAlignment class