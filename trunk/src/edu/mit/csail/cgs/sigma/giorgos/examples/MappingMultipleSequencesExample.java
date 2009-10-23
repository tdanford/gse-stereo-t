package edu.mit.csail.cgs.sigma.giorgos.examples;


/**
 * 
 * @author George Fou
 *
 */

/** java.io package **/
import java.io.File;
import java.io.IOException;
import java.util.Vector;
import java.util.Set;
import java.util.HashMap;

/** Sigma:  cgs.sigma.alignments package **/
import edu.mit.csail.cgs.sigma.alignments.ClustalMultipleAlignment;
import edu.mit.csail.cgs.datasets.alignments.GappedAlignmentString;
import edu.mit.csail.cgs.datasets.alignments.MultipleAlignment;
import edu.mit.csail.cgs.datasets.alignments.parsing.ClustalParser;

/** testing packages **/
import java.util.HashSet;

public class MappingMultipleSequencesExample 
{

	/**
	 * @param args
	 */
	public static void main(String[] args) 
	{
		// TODO Auto-generated method stub
		
		char[] seq1 = "AAT-GCT-TCG".toCharArray();
		char[] seq2 = "G--C-CTCT-C".toCharArray();
		char[] seq3 = "A-T--GT-AG-".toCharArray();
		
		GappedAlignmentString gas1 = new GappedAlignmentString(seq1);
		GappedAlignmentString gas2 = new GappedAlignmentString(seq2);
		GappedAlignmentString gas3 = new GappedAlignmentString(seq3);
		
		MultipleAlignment mal = new MultipleAlignment();
		mal.addGappedAlignment("seq1", gas1);
		mal.addGappedAlignment("seq2", gas2);
		mal.addGappedAlignment("seq3", gas3);
		
		
		Vector<String> ss = new Vector<String>(); ss.add("seq2"); ss.add("seq3");
		mal.mapSeqCoords2SeqsCoords("seq1", ss);
		
		
		
		HashMap<Integer, Integer>[] mpp = new HashMap[2];
		mpp[0] = new HashMap<Integer, Integer>();
		mpp[1] = new HashMap<Integer, Integer>();
		
	/*	String seq1 = "AAT-GCT-TCG";
		String seq2 = "G--C-CTCT-C";
		
		GappedAlignmentString ga1 = new GappedAlignmentString(seq.toCharArray());
	*/	
		
	
		String alnPath = "Y:\\u\\g\\geopapa\\Desktop\\Geopapa\\Research\\Data\\yeast_versions\\NCBI_genome_source\\alignments\\MAVID\\" +
				"chr01\\";
		
		String fName = alnPath + "mavid_chr01.aln";
		File f = new File(fName);
		
		try 
		{
			ClustalParser cp = new ClustalParser(f);
			Set<String> species = cp.getSpecies();
			for(String currSpecies : species)
			{
				String currSeq = cp.getSequence(currSpecies);
			}
			
			ClustalMultipleAlignment cma = new ClustalMultipleAlignment(cp);
			String sss = cma.toString();
			
			Vector<Integer> ss1 = new Vector<Integer>(); ss1.add(0); //ss.add("seq3");
			cma.mapSeqCoords2SeqsCoords(1, ss1);
			
			/*Set<String> ts = new HashSet<String>();
			ts.add("ref1"); ts.add("ref2"); ts.add("ref3"); 
			*/
			
			int foo = 3;
			
		//	cma.mapSeqCoords2SeqsCoords(1, ts);
			
			
		} 
		
		catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}// end of main method

}// end of MappingMultipleSequencesExample class
