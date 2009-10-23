package edu.mit.csail.cgs.sigma.giorgos;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import edu.mit.csail.cgs.datasets.alignments.GappedAlignmentString;
import edu.mit.csail.cgs.datasets.alignments.parsing.ClustalParser;
import edu.mit.csail.cgs.sigma.alignments.ClustalMultipleAlignment;

import edu.mit.csail.cgs.utils.stats.StatUtil;


public class ExprDataHandling 
{
	/**
	 * Write the expression file into the <tt>outFile</tt> destination with coordinates corresponding to the <tt>targerSeq</tt>
	 * </br>Note: Valid numbers for serial numbers are [0, number_of_sequences-1]
	 * </br>If a coordinate corresponds to a gap in the second seq, then it is omitted.
	 * @param outFile The destination plus name of the output file
	 * @param exprFile The expression data file
	 * @param alnFiles The alignment files
	 * @param seqName The serial number of the sequence
	 * @param targetSeqName The serial number of the target sequence
	 * @throws IOException
	 */
	public static void convertFileCoords(File outFile, File exprFile, File[] alnFiles, int seqID, int targetSeqID) throws IOException
	{
		// Get the names of the sequence IDs
		File alnFile = alnFiles[0];
		ClustalParser cp = new ClustalParser(alnFile);
		ClustalMultipleAlignment cma = new ClustalMultipleAlignment(cp);
		String seqName = cma.species()[seqID];
		String targetSeqName = cma.species()[targetSeqID];
		
		convertFileCoords(outFile, exprFile, alnFiles, seqName, targetSeqName);
	}//end of convertFileCoords method
	
	/**
	 * Writes the expression file into the <tt>outFile</tt> destination with coordinates corresponding to the <tt>targerSeq</tt>
	 * </br>If a coordinate corresponds to a gap in the second seq, then it is omitted.
	 * @param outFile The destination plus name of the output file
	 * @param exprFile The expression data file
	 * @param alnFiles The alignment files
	 * @param seqName The name of the sequence
	 * @param targetSeqName The name of the target sequence
	 * @throws IOException
	 */
	public static void convertFileCoords(File outFile, File exprFile, File[] alnFiles, String seqName, String targetSeqName) throws IOException
	{
		FileWriter fw = new FileWriter(outFile);
		
		//Read expression data file
		BufferedReader br = new BufferedReader(new FileReader(exprFile));
		String headLine = br.readLine();
		fw.write(headLine + "\n");
		
		int previous_chr_idx = 0;
		String line;
		ClustalMultipleAlignment cma = new ClustalMultipleAlignment(new ClustalParser());
		int count = 0;
		while((line = br.readLine()) != null)
		{
			String[] tokens = line.trim().split("\\s");
			//Get the chromosome #
			int chr_idx = Integer.parseInt(tokens[0]);
			
			if( chr_idx != previous_chr_idx )
			{		
				System.out.println("Chromosome #" + chr_idx + " is parsed...");
				
				// subtract 1 since indexing starts from 0 
				File alnFile = alnFiles[chr_idx -1];
				// Find the (genomic) coordinate mappings of the version seqName to the version targetSeqName
				ClustalParser cp = new ClustalParser(alnFile);
				cma = new ClustalMultipleAlignment(cp);
			}
			
			//Get the position
			int pos = Integer.parseInt(tokens[2]);
			pos -= 1;  // subtract 1 because in expression file indexing starts from 1 while in my notation from 0
			
			//Get the strand
			String strand = tokens[1];
			if( strand.equals("+") )
			{
				int pos2 = cma.mapUngapped(seqName, targetSeqName, pos);
				if(pos2 != -1)
					writeNewLine(tokens, pos2, fw);
				else
				{
					count++;
					System.out.println(count + ") " + line + "line(101)");
				}
			}
			else if( strand.equals("-") )
			{
				int ungappedLength_seq = cma.getGappedAlignment(seqName).ungappedLength();
				int ungappedLength_targetSeq = cma.getGappedAlignment(targetSeqName).ungappedLength();
				
				pos = ungappedLength_seq -1 - pos;
				int pos2 = cma.mapUngapped(seqName, targetSeqName, pos);
				if(pos2 != -1)
				{
					pos2 = ungappedLength_targetSeq -1 - pos2;
					writeNewLine(tokens, pos2, fw);
				}
				else
				{
					count++;
					System.out.println(count + ") " + line + "line(119)");
				}

			}
			else
			{
				throw new IllegalArgumentException("There is an entry in the expression file that does not have" +
						                           " a valid symbol for strand. Strand symbol should be either " +
						                           "+ for Watson or - for Crick strand.");
			}
			
			previous_chr_idx = Integer.parseInt(tokens[0]);
		}
		fw.close();
		
	}//end of convertFileCoords method
	
	
	/**
	 * Writes the expression file into the <tt>outFile</tt> destination with coordinates corresponding to the <tt>targerSeq</tt></br>
	 * However, in this function there is a window searching where for a given coordinate, a search for a gap in the other sequence is performed
	 * within the specified range.</br>
	 * </br>If the window [coordinate-leftWin, coordinate+rightWin] contains a gap in the second seq (<tt>targetSeqName</tt>), then the entry in the expression
	 * file having that coordinate is omitted.</br>
	 * The window length is: <i>rightWinLength + leftWinLength + 1</i>
	 * @param outFile The destination plus name of the output file
	 * @param exprFile The expression data file
	 * @param alnFiles The alignment files
	 * @param seqName The name of the sequence
	 * @param targetSeqName The name of the target sequence
	 * @param leftWinLength The length of the left window of the search space
	 * @param rightWinLength The length of the right window of the search space
	 * @throws IOException
	 */
	public static void convertFileCoords(File outFile, File exprFile, File[] alnFiles, String seqName, String targetSeqName, int leftWinLength, int rightWinLength) throws IOException
	{
		FileWriter fw = new FileWriter(outFile);
		
		//Read expression data file
		BufferedReader br = new BufferedReader(new FileReader(exprFile));
		String headLine = br.readLine();
		fw.write(headLine + "\n");
		
		int previous_chr_idx = 0;
		String line;
		ClustalMultipleAlignment cma = new ClustalMultipleAlignment(new ClustalParser());
		int count = 0;
		while((line = br.readLine()) != null)
		{		
			String[] tokens = line.trim().split("\\s");
			//Get the chromosome #
			int chr_idx = Integer.parseInt(tokens[0]);
			
			if( chr_idx != previous_chr_idx )
			{		
				System.out.println("Chromosome #" + chr_idx + " is parsed...");
				
				// subtract 1 since indexing starts from 0 
				File alnFile = alnFiles[chr_idx -1];
				// Find the (genomic) coordinate mappings of the version seqName to the version targetSeqName
				ClustalParser cp = new ClustalParser(alnFile);
				cma = new ClustalMultipleAlignment(cp);
			}
			
			//Get the position
			int pos = Integer.parseInt(tokens[2]);
			pos -= 1;  // subtract 1 because in expression file indexing starts from 1 while in my notation from 0
			
			//Get the strand
			String strand = tokens[1];
			if( strand.equals("+") )
			{
				ArrayList<Integer> pos2 = windowMapping(pos, leftWinLength, rightWinLength, cma, seqName, targetSeqName);
				if(!pos2.contains(-1))
				{
					Integer[] positions2 = pos2.toArray(new Integer[pos2.size()]);
					int pos2_of_pos = positions2[leftWinLength];
					writeNewLine(tokens, pos2_of_pos, fw);
				}
				else
				{
					count++;
					System.out.println(count + ") " + line + "line(201)");
				}
			}
			else if( strand.equals("-") )
			{
				int ungappedLength_seq = cma.getGappedAlignment(seqName).ungappedLength();
				int ungappedLength_targetSeq = cma.getGappedAlignment(targetSeqName).ungappedLength();
				
				pos = ungappedLength_seq -1 - pos;
				ArrayList<Integer> pos2 = windowMapping(pos, leftWinLength, rightWinLength, cma, seqName, targetSeqName);
				if(!pos2.contains(-1))
				{
					Integer[] positions2 = pos2.toArray(new Integer[pos2.size()]);
					int pos2_of_pos = positions2[leftWinLength];
					pos2_of_pos = ungappedLength_targetSeq -1 - pos2_of_pos;
					writeNewLine(tokens, pos2_of_pos, fw);
				}
				else
				{
					count++;
					System.out.println(count + ") " + line + "line(221)");
				}

			}
			else
			{
				throw new IllegalArgumentException("There is an entry in the expression file that does not have" +
						                           " a valid symbol for strand. Strand symbol should be either " +
						                           "+ for Watson or - for Crick strand.");
			}
			
			previous_chr_idx = Integer.parseInt(tokens[0]);
		}
		fw.close();
		
	}//end of convertFileCoords method
	
	/**
	 * This function returns an ArrayList containing the maps of the positions of <tt>seqName</tt> to the positions of <tt>targetSeqName</tt>.
	 * Map's entries are in this order:<pre> 
	 *  pos-leftWin        pos-leftWin+1 ...   pos ...    pos+rightWin-1          pos+rightWin
	 *      |                     |             |               |                      |
	 *      V                     V             V               V                      V
	 * map(pos-leftWin)   map(pos-leftWin+1)   map(pos)     map(pos+rightWin-1)   map(pos+rightWin)</pre>
	 * @param pos The coordinate which will be mapped
	 * @param leftWin The length of the left window
	 * @param rightWin The length of the right window
	 * @param cma The Multiple Alignment
	 * @param seqName The name of the sequence
	 * @param targetSeqName The name of the target sequence
	 * @return
	 */
	private static ArrayList<Integer> windowMapping(int pos, int leftWinLength, int rightWinLength, ClustalMultipleAlignment cma, String seqName, String targetSeqName)
	{
		ArrayList<Integer> map = new ArrayList<Integer>();
		
		int ungappedLength_seq = cma.getGappedAlignment(seqName).ungappedLength();
		int leftStart = pos - leftWinLength; leftStart = leftStart < 0 ? 0 : leftStart;
		int rightEnd = pos + rightWinLength; rightEnd = rightEnd >  ungappedLength_seq-1 ? ungappedLength_seq-1 : rightEnd;
		
		for(int i = leftStart; i <= rightEnd; i++)
		{
			int pos2 = cma.mapUngapped(seqName, targetSeqName, i);
			map.add(pos2);
		}
		
		return map;
	}//end of windowMapping method
	
	
	private static void writeNewLine(String[] tokens, int newPos, FileWriter fw) throws IOException
	{
		newPos += 1;  //add 1 because in expression file indexing starts from 1 while in my notation from 0
		tokens[2] = Integer.toString(newPos);
		String str = "";
		for(String tok:tokens)
			str += tok + "\t";
		
		str = str.trim() + "\n";
		//System.out.print(str);
		fw.write(str);
	}
	
	
	/**
	 * This function takes the expression data file and the alignment of the source to the target
	 * sequence and returns an expression data file which now has the coordinates of the target sequence
	 * in the position of the coordinates of the source file.</br>
	 * Note: Each entry of the expression file contains info in this order: <pre>chr	strand	position	level</pre>
	 * @param <T> It can be either an integer representing the serial number of the sequence or 
	 * a String representing the name of the sequence
	 * @param outFile The destination plus name of the output file
	 * @param exprFile The expression data file
 	 * @param alnFile The alignment file
	 * @param generic_seqID The id of the sequence
	 * @param generic_targetSeqIDs The id of the target sequence
	 * @throws IOException
	 * @deprecated Avoid using it - Use convertFileCoords instead (without generics)
	 */
	@Deprecated
	public static <T>   void convertFileCoords(File outFile, File exprFile, File[] alnFiles, T generic_seqID, Vector<T> generic_targetSeqIDs) throws IOException
	{	
		FileWriter fw = new FileWriter(outFile);
		
		//Read expression data file
		BufferedReader br = new BufferedReader(new FileReader(exprFile));
		String headLine = br.readLine();
		fw.write(headLine + "\n");
		
		int previous_chr_idx = 0;
		String line;
		Map seqCoords2SeqsCoords = new HashMap();
		Map seqCoords2SeqsCoords_reverseStrand = new HashMap();
		while((line = br.readLine()) != null)
		{
			String[] tokens = line.trim().split("\\s");
			int chr_idx = Integer.parseInt(tokens[0]);
			if( chr_idx != previous_chr_idx )
			{
				// add -1 since indexing starts from 0 
				File alnFile = alnFiles[chr_idx -1];
				
				// Find the (genomic) coordinate mappings of the version generic_seqID to the version(s) generic_targetSeqIDs
				ClustalParser cp = new ClustalParser(alnFile);
				ClustalMultipleAlignment cma = new ClustalMultipleAlignment(cp);
				seqCoords2SeqsCoords = cma.mapSeqCoords2SeqsCoords(generic_seqID, generic_targetSeqIDs);
				
				int ungappedLength_seq;
				int[] ungappedLength_targetSeqs;
				
				String[] speciesNames = cma.species();
				String seqsClassName = generic_seqID.getClass().getSimpleName();
				if( seqsClassName.equals("Integer") )
				{
					Integer seqID = (Integer)generic_seqID;
					Integer[] seqIDs = generic_targetSeqIDs.toArray(new Integer[generic_targetSeqIDs.size()]);
					ungappedLength_seq = cma.ungappedLength(speciesNames[seqID]);
					ungappedLength_targetSeqs = new int[seqIDs.length];
					for(int i = 0; i < seqIDs.length; i++)
						ungappedLength_targetSeqs[i] = cma.ungappedLength(speciesNames[seqIDs[i]]);
				}
				else if( seqsClassName.equals("String") )
				{
					String seqID = (String)generic_seqID;
					String[] seqIDs = generic_targetSeqIDs.toArray(new String[generic_targetSeqIDs.size()]);
					ungappedLength_seq = cma.ungappedLength(seqID);
					ungappedLength_targetSeqs = new int[seqIDs.length];
					for(int i = 0; i < seqIDs.length; i++)
						ungappedLength_targetSeqs[i] = cma.ungappedLength(seqIDs[i]);
				}
				else
				{
					throw new IllegalArgumentException("Sequence IDs should be either of type Integer or String");
				}
				
				int ungappedLength_targetSeq = 0;
				if( generic_targetSeqIDs.size() == 1 )
					ungappedLength_targetSeq = ungappedLength_targetSeqs[0];
				seqCoords2SeqsCoords_reverseStrand = reverseCoords(seqCoords2SeqsCoords, ungappedLength_seq, ungappedLength_targetSeq); 

			}
						
			int pos = Integer.parseInt(tokens[2]);
			pos -= 1;  // subtract one because in expression file indexing starts from 1 while in my notation from 0
			String strand = tokens[1];
			if( strand.equals("+") )
			{
				writeNewLine(tokens, pos, seqCoords2SeqsCoords, fw);
			}
			else if( strand.equals("-") )
			{
				writeNewLine(tokens, pos, seqCoords2SeqsCoords_reverseStrand, fw);
			}
			else
			{
				throw new IllegalArgumentException("There is an entry in the expression file that does not have" + 
						                           " a valid symbol for strand. Strand symbol should be either " +
						                            "+ for Watson or - for Crick strand.");
			}
			
			previous_chr_idx = Integer.parseInt(tokens[0]);
		}
		
		fw.close();
	}//end of convertFileCoords method
	
	/**
	 * 
	 * @param tokens
	 * @param pos
	 * @param coordsMap
	 * @param fw
	 * @throws IOException
	 * @deprecated Avoid using it
	 */
	@Deprecated
	private static void writeNewLine(String[] tokens, int pos, Map coordsMap, FileWriter fw) throws IOException
	{
		int transPos = mapPos(pos, coordsMap);
		transPos += 1;  //add one because in expression file indexing starts from 1 while in my notation from 0
		tokens[2] = Integer.toString(transPos);
		String str = "";
		for(String tok:tokens)
			str += tok + "\t";
		
		str = str.trim() + "\n";
		//System.out.print(str);
		fw.write(str);
	}//end of writeNewLine method
	
	/**
	 * 
	 * @param seqCoords2SeqsCoords
	 * @param ungappedLength_seq1
	 * @param ungappedLength_seq2
	 * @return
	 * @deprecated Avoid using it
	 */
	@Deprecated
	private static Map<Integer, Integer> reverseCoords(Map<Integer, Integer> seqCoords2SeqsCoords, int ungappedLength_seq1, int ungappedLength_seq2)
	{
		Map<Integer, Integer> reverseMap = new HashMap<Integer, Integer>(seqCoords2SeqsCoords.size());
		Integer[] coords_seq1 = seqCoords2SeqsCoords.keySet().toArray(new Integer[seqCoords2SeqsCoords.size()]);
		
		for(Integer coord_seq1:coords_seq1)
		{
			Integer newCoord_seq1 = ungappedLength_seq1 -1 - coord_seq1;
			
			Integer coord_seq2 = seqCoords2SeqsCoords.get(coord_seq1);
			Integer newCoord_seq2 = coord_seq2 != -1? ungappedLength_seq2 -1 - coord_seq2 : -1;
			
			reverseMap.put(newCoord_seq1, newCoord_seq2);
		}
		
		return reverseMap;
	}//end of reverseCoords method
	
	/**
	 * 
	 * do a window-searching
     * If there is no match in the specified position, take the closest position giving priority to the right
	 * @param pos
	 * @param seqCoords2SeqsCoords
	 * @return
	 * @deprecated Avoid using it
	 */
	@Deprecated
	private static int mapPos(int pos, Map<Integer, Integer> seqCoords2SeqsCoords)
	{
		//System.out.println("current pos is: " + pos);
		
		Integer transPos;
		
		Integer[] seq_coords = seqCoords2SeqsCoords.keySet().toArray(new Integer[seqCoords2SeqsCoords.size()]);
		Integer min_coord = StatUtil.findMin(seq_coords).getFirst();
		Integer max_coord = StatUtil.findMax(seq_coords).getFirst();
		
		Integer step = 0;
		while(true)
		{
			if(pos + step <= max_coord)
			{
				Object transPosObj = seqCoords2SeqsCoords.get(pos + step);
				//System.out.println(transPosObj.toString());
				if( !transPosObj.equals(-1))
				{
					transPos = (Integer) transPosObj;
					break;
				}
			}
			if(pos - step >= min_coord)
			{
				Object transPosObj = seqCoords2SeqsCoords.get(pos - step);
				if( !transPosObj.equals(-1))
				{
					transPos = (Integer) transPosObj;
					break;
				}
			}
			if( (pos + step > max_coord) && (pos - step < min_coord))
			{
				transPos = max_coord;
				break;
			}
			
			step++;
		}
		
		return transPos;
	}//end of mapPos method
	
	public static void main(String[] args)
	{
		// String path = "Y:\\u\\g\\geopapa\\Desktop\\Geopapa\\Research\\Data\\Simulation\\";
		String path = "Y:\\u\\g\\geopapa\\Desktop\\Geopapa\\Research\\Data\\SimulationBig\\";
		String outFileS = path + "new_expr.txt";
		String exprFileS = path + "polyA_normalized.txt";  // total_normalized.txt
		String[] alnFilesS = {path + "mavid_chr01.aln", path + "mavid_chr02.aln", path + "mavid_chr03.aln", path + "mavid_chr04.aln",
				              path + "mavid_chr05.aln", path + "mavid_chr06.aln", path + "mavid_chr07.aln", path + "mavid_chr08.aln",
				              path + "mavid_chr09.aln", path + "mavid_chr10.aln", path + "mavid_chr11.aln", path + "mavid_chr12.aln", 
				              path + "mavid_chr13.aln", path + "mavid_chr14.aln", path + "mavid_chr15.aln", path + "mavid_chr16.aln", 
				              path + "mavid_chrmt.aln"};
		
		File outFile = new File(outFileS);
		File exprFile = new File(exprFileS);
		File[] alnFiles = new File[alnFilesS.length];
		for(int i = 0; i < alnFiles.length; i++)
			alnFiles[i] = new File(alnFilesS[i]);
		
		String generic_seqID = "Saccharomyces";
		Vector<String>  generic_targetSeqIDs = new Vector<String>();
		generic_targetSeqIDs.add("refNC001133");
			
		//ExprDataHandling edh = new ExprDataHandling();
		try {
			ExprDataHandling.convertFileCoords(outFile, exprFile, alnFiles, "S288chuber", "S288cour", 0, 24);
			
			System.out.println("\n\n THE END");
			//edh.convertFileCoords(outFile, exprFile, alnFiles, generic_seqID, generic_targetSeqIDs);
		} 
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
		
}//end of ExprDataHandling class
