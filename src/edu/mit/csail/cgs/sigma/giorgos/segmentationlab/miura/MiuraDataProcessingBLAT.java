package edu.mit.csail.cgs.sigma.giorgos.segmentationlab.miura;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;

import edu.mit.csail.cgs.tools.sequence.GenbankToFasta;
import edu.mit.csail.cgs.utils.Pair;
import edu.mit.csail.cgs.utils.stats.StatUtil;

public class MiuraDataProcessingBLAT extends MiuraDataProcessing {
	
	private String seqID, chrom, strand, strain, queryName;
	private int querySize, queryStart, queryEnd, start, end;
	
	private int numUniqueESTs = 0, numUniqueHits = 0, numMultiHits = 0;
	
	private double percentThreshold;
	
	private String multiHitsFile;
	private BufferedWriter bw_multi;
	
	private StringBuilder sb = new StringBuilder();
	private boolean isNewSeqEncountered = false;
	private boolean isFirstLine = true;
	
	private Map<Long, ArrayList<String>> mapTSSs = new TreeMap<Long, ArrayList<String>>();
	private int numProperCapped = 0, numImproperCapped = 0;

	private BufferedWriter bw_rej;
	private String rejectedFile;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		String path = "/afs/csail.mit.edu/group/psrg/projects/sigma/LitData/Miura/Data/DDBJ/" + "/BLAT/";
		String input = path + "unique_s288c_sk1_31847_against_BLAT"; // "ST2_TSSs_97_ours_BLAT"; //"unique_s288c_sk1_31847_against_BLAT"; //"s288c_sk1_31847_against_BLAT.psl.txt";
		String output = path + "ST2_TSSs_ours_BLAT";  //"ST2_TSSs_ours_BLAT_counts"; //"unique_s288c_sk1_31847_against_BLAT";
		
		String ESTinput = path + "ESTs_s288c_sk1_31847.fasta";
		
		try {
			/*
			InputStream hits = new FileInputStream(new File(input));
			OutputStream uniqueHits = new FileOutputStream(new File(output));
			*/
			
			
			InputStream uniqueHits = new FileInputStream(new File(input));
			InputStream ESTsFasta = new FileInputStream(new File(ESTinput));
			OutputStream TSSs = new FileOutputStream(new File(output));
			int numHeaderLines_hits = 1;
			
			
			/*
			InputStream TSSs =  new FileInputStream(new File(input));
			OutputStream uniqueTSSs = new FileOutputStream(new File(output));
			int numHeaderLines = 1;
			*/
	   		MiuraDataProcessingBLAT mdp = new MiuraDataProcessingBLAT();
			//mdp.printUniqueHits(hits, uniqueHits, 5);
			//
			
			mdp.printTSSs(uniqueHits, ESTsFasta, TSSs, numHeaderLines_hits);
			//mdp.printUniqueTSSs(TSSs, uniqueTSSs, numHeaderLines);
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}//end of main method
	
	public MiuraDataProcessingBLAT()
	{
		multiHitsFile = "/afs/csail.mit.edu/group/psrg/projects/sigma/LitData/Miura/Data/DDBJ/" + "multiHitsBLAT";
		rejectedFile = "/afs/csail.mit.edu/group/psrg/projects/sigma/LitData/Miura/Data/DDBJ/" + "rejectedBLAT.txt";
		percentThreshold = 97.0;
	}//default constructor
	
	public MiuraDataProcessingBLAT(String multiHitsF, String rejF, double percThres)
	{
		multiHitsFile = multiHitsF;
		rejectedFile = rejF;
		percentThreshold = percThres;
		
		if(percentThreshold < 0 || percentThreshold > 100.0)
			throw new IllegalArgumentException("percentThreshold must be a number between 0 and 100");
	}//customizable constructor
	
	
	/**
	 * It prints the unique hits of the input stream <tt>hits</tt> (after BLATing the ESTs) w.r.t. the <tt>percentIdentity</tt>. 
	 * Hits of the same EST (cDNA) with equal maximum <tt>percentIdentity</tt> corresponding to multiple loci are discarded.
	 * @param hits input stream containing the hits of ESTs against a genome with <tt>BLAT</tt> tool<br>
	 * The input file <tt>hits</tt> should be of the form:
	 * <pre>
	 * match	mismatch	repmatch	Ns	queryGapCount	queryGapBases	subjectGapCount	subjectGapBases	strand	queryName	querySize	queryStart	queryEnd	subjectName	subjectSize	subjectStart	subjectEnd	blockCount	blockSizes	queryStarts	subjectStarts
	 * 
	 * E.g. 
	 * 330	24	0	0	4	22	5	384	-	DB668620	431	17	393	chr12	1078175	931019	931757	6	8,9,17,6,254,60,	38,49,67,84,94,354,	
	 * </pre> 
	 * Note also that the starts and ends are not based on an absolute measurement. Aka, they depend on the strand 
	 * they are on. So, the start of a region will be always less than the end of a region. 
	 * @param uniqueHits output stream with unique hits<br>
	 * The output file will be of the form:
	 * <pre>
	 * EST	querySize	queryStart	queryEnd	chr	strand	start	end	blockSizes
     * 
     * E.g.
     * DB636785	491	0	490	09	-	348520	347900	19,441,30,
	 * </pre>
	 * Here the starts and ends are based on an absolute measurement. Aka, when we are on the Watson (+)
	 * strand, start is less than end but when we are on the Crick (-) strand, start is greater than end.
	 * @param numHeaderLines number of lines corresponding to the header
	 */
	public void printUniqueHits(InputStream hits, OutputStream uniqueHits, int numHeaderLines)
	{
		BufferedReader br = null;
		BufferedWriter bw = null;
		bw_multi = null; 
		try 
		{
			InputStreamReader isr = new InputStreamReader(hits); 
			br = new BufferedReader(isr);
			
			OutputStreamWriter osw = new OutputStreamWriter(uniqueHits);
			bw = new BufferedWriter(osw);
			bw.write(String.format("EST\tquerySize\tqueryStart\tqueryEnd\tchr\tstrand\tstart\tend\tblockSizes%n"));
			
			bw_multi = new BufferedWriter(new FileWriter(new File(multiHitsFile)));
			
			ArrayList<Double> percentIDs = new ArrayList<Double>();
			ArrayList<String> records = new ArrayList<String>();
			
			int count = 0;
			String str;
			String prevQueryName = "";
			boolean isFirst = true;
			
			for(int i = 0; i < numHeaderLines; i++)
				str = br.readLine();
				
			while( (str = br.readLine()) != null )
			{	
				String[] tokens = str.split("\\s+");
				
				queryName = tokens[9];
				querySize = Integer.parseInt(tokens[10]);
				queryStart = Integer.parseInt(tokens[11]);  // indexing (already) starts at 0
				queryEnd = Integer.parseInt(tokens[12]) -1;  // indexing starts at 0
				
				chrom = tokens[13];
				strand = tokens[8];
				int targetStart = Integer.parseInt(tokens[15]);  // indexing (already) starts at 0
				int targetEnd = Integer.parseInt(tokens[16])-1;  // indexing starts at 0
				
				if( strand.matches("[Ww+Cc-]") )  // does the strand column have a valid value?
				{
					if(strand.matches("[Ww+]"))
					{
						start = targetStart; end = targetEnd;
					}
					else
					{
						start = targetEnd; end = targetStart;
					}
					
					String blockSizesStr = tokens[18];
					String[] blockSizesTokens = blockSizesStr.split(",");
					int[] blockSizes = new int[blockSizesTokens.length];
					for(int i = 0; i < blockSizes.length; i++)
						blockSizes[i] = Integer.parseInt(blockSizesTokens[i]);
					
					// Isolate chromosome number
					Matcher chrMatcher = chrPattern.matcher(chrom);
					if(chrMatcher.matches())
					{
						String usefulPart = chrMatcher.group(1);
						chrom = usefulPart.length() == 1 ? "0" + usefulPart : usefulPart;
					}				
					
					String record = String.format("%s\t%d\t%d\t%d\t%s\t%s\t%d\t%d\t%s", queryName, querySize, queryStart, queryEnd, chrom, strand, start, end, blockSizesStr);
					
					int alignedPart = 0;  for(Integer blockSize:blockSizes) { alignedPart += blockSize; }
					Double percentID = ((double) alignedPart/querySize)*100.0;
					
					if( !( queryName.equals(prevQueryName) || isFirst ) )
					{
						printUniqueRecord(percentIDs, records, bw);
						percentIDs.clear(); records.clear();
						numUniqueESTs++;
					}
					
					percentIDs.add(percentID);  records.add(record);
					prevQueryName = queryName;  isFirst = false;
					
					count++;
					if( count%10000 == 0 )
						System.out.println("entry " + count + " has been parsed...");
					
				}//end of if( strand.matches("[Ww+Cc-]") ) condition
					
			}//end of while condition
			
			printUniqueRecord(percentIDs, records, bw);
			
			System.out.println("Total Hits: " + count);
			System.out.println("Number of unique ESTs: " + (numUniqueESTs+1));
			System.out.println("Unique Loci Hits: " + numUniqueHits);
			System.out.println("Multi Loci Hits: " + numMultiHits);
		}
		
		catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(-1);
		} 
		
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(-1);
		}
		finally {
			try {
				br.close();
				bw.close();
				bw_multi.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}//end of printUniqueHits method
	
	/**
	 * It prints the best hit of this EST (cDNA) corresponding to a unique locus. If more than one best hits
	 * (corresponding to multiple loci) exist, then this record is rejected.
	 * @param percentIDs percent identities of all hits for this EST (cDNA)
	 * @param records corresponding records
	 * @param bw output stream where the best hits will be writen
	 */
	private void printUniqueRecord(ArrayList<Double> percentIDs, ArrayList<String> records, BufferedWriter bw)
	{
		try 
		{
			Double[] percentIDsD = percentIDs.toArray(new Double[0]);
			Pair<Double, TreeSet<Integer>> max_maxIndexes = StatUtil.findMax(percentIDsD);
			TreeSet<Integer> maxIndexes = max_maxIndexes.getLast();

			// Discard more than one best hits: EST refers to multiple loci
			if(maxIndexes.size() == 1)
			{
				numUniqueHits++;
				Integer index = maxIndexes.toArray(new Integer[0])[0];
				String bestRecord = records.get(index);
				bw.write(String.format("%s%n", bestRecord));
			}
			
			else
			{
				numMultiHits++;
				String queryInfo = records.get(0).split("\\s+")[0];
				String qName = queryInfo.split("\\|")[0];
				bw_multi.write(String.format("%d\t%s%n", numMultiHits, qName)); //(numMultiHits + "\t" + qName + "%n"); 
			}
		} 
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(-1);
		}
	}// end of printUniqueRecord method

	
	/**
	 * Prints the TSSs corresponding to the unique hits. If an EST is not fully aligned 
	 * and its percent identity is below <tt>percentThreshold</tt>, then
	 * its 5' end is checked if it corresponds to a G-capped vector. 
	 * If yes, it is accepted. Otherwise, rejected.
	 * @param uniqueHits input stream with unique hits<br>
	 * The input file should be of the form:
	 * <pre>
	 * EST	querySize	queryStart	queryEnd	chr	strand	start	end	blockSizes
     * 
     * E.g.
     * DB636785	491	0	490	09	-	348520	347900	19,441,30,
	 * </pre>
	 * Here the starts and ends are based on an absolute measurement. Aka, when we are on the Watson (+)
	 * strand, start is less than end but when we are on the Crick (-) strand, start is greater than end.
	 * @param ESTsFasta input stream of ESTs (cDNAs) in fasta format<br>
	 * This file should be of the form:
	 * <pre>
	 * >seq_identifier
	 * sequence
	 * 
	 * E.g.
	 * >DB636784
	 * atgggtaagcctgatgtcccgtaacgtacgt
	 * </pre>
	 * @param TSSs output stream of TSSs that correspond to these ESTs<br>
	 * The output file will be of the form:
	 * <pre>
	 * [EST]	chrom	strand	coord
	 * EST is surrounded with [] because it is optional.
	 * 
	 * E.g.
     * DB638289	chr01	-	5475     or
     * 		chr01	-	5475     
     * 
     * are both valid entries
	 * </pre>
	 * Also, <tt>chrom</tt> will be of the form: {<tt>chrx, chx, cx, x</tt>}, where <tt>x</tt> is a number<br>
	 * and <tt>strand</tt> will take the values: {<tt>+, W, w, -, C, c</tt>}
	 * @param numHeaderLines_hits number of lines corresponding to the header
	 */
	public void printTSSs(InputStream uniqueHits, InputStream ESTsFasta, OutputStream TSSs, int numHeaderLines_hits)
	{
		BufferedReader brHits = null;
		BufferedReader brESTs = null;
		BufferedWriter bw = null;

		try 
		{
			InputStreamReader isrHits = new InputStreamReader(uniqueHits);
			brHits = new BufferedReader(isrHits);
			
			InputStreamReader isrESTs = new InputStreamReader(ESTsFasta);
			brESTs = new BufferedReader(isrESTs);
			
			OutputStreamWriter osw = new OutputStreamWriter(TSSs);
			bw = new BufferedWriter(osw);
			
			bw_rej = new BufferedWriter(new FileWriter(new File(rejectedFile)));

			String EST = "", prevEST = "";
			String str1;
			
			for(int i = 0; i < numHeaderLines_hits; i++)
				str1 = brHits.readLine();
			
			while( (str1 = brHits.readLine() ) != null)
			{
				String[] tokens1 = str1.split("\\s+");
				
				String[] seqID_tokens = tokens1[0].split("\\|");
				seqID = seqID_tokens[0].toUpperCase();
				strain = seqID_tokens[1];
				
				querySize = Integer.parseInt(tokens1[1]);
				queryStart = Integer.parseInt(tokens1[2]);
				queryEnd = Integer.parseInt(tokens1[3]);
				
				chrom = tokens1[4];
				start = Integer.parseInt(tokens1[6]);
				end = Integer.parseInt(tokens1[7]);
				
				strand = tokens1[5];
				
				String blockSizesStr = tokens1[8];
				String[] blockSizesTokens = blockSizesStr.split(",");
				int[] blockSizes = new int[blockSizesTokens.length];
				for(int i = 0; i < blockSizes.length; i++)
					blockSizes[i] = Integer.parseInt(blockSizesTokens[i]);
				
				int alignedPart = 0;  for(Integer blockSize:blockSizes) { alignedPart += blockSize; }
				double percentID = ((double) alignedPart/querySize)*100.0;
				
				if(percentID >= percentThreshold)
				{
					int TSS = start;
					
					Long key = evalKey(TSS); 
					String value = String.format("%s\tchr%s\t%s\t%d", seqID, chrom, strand, TSS);
					
					if(mapTSSs.get(key) == null)
						mapTSSs.put(key, new ArrayList<String>());
					else
						System.out.println("xaxa");
				
					mapTSSs.get(key).add(value);
					numProperCapped++;

				}
				else
				{
					String str2;
				    while( (str2 = brESTs.readLine() ) != null)
				    {
				    	if( str2.startsWith(">"))
				    	{
				    		EST = str2.split("\\|")[0].toUpperCase();
				    		EST = EST.substring(1);
				    		String prevESTSeq = sb.toString();
				    		findTSS(prevEST, prevESTSeq);
				    		if(seqID.equals(prevEST) && isNewSeqEncountered && !isFirstLine)
				    			break;
							 
							isNewSeqEncountered = true;
						}
						else
						{
							sb.append(str2);
						}
						 
				    	prevEST = EST;
				    	isFirstLine = false;
				    }//end of inner while loop
					 
				    String ESTSeq = sb.toString();
				    if(ESTSeq.length() > 0)
				    	findTSS(EST, ESTSeq);
					 
					isNewSeqEncountered = true;
				}
			}//end of outer while loop
			
			bw.write(String.format("EST\tchrom\tstrand\tcoord%n"));
			Long[] keys = mapTSSs.keySet().toArray(new Long[0]);
			for(Long key: keys)
			{
				ArrayList<String> values_of_key = mapTSSs.get(key);
				for(String value : values_of_key)
				{
					String record = String.format("%s%n", value);
					bw.write(record);
				}
			}
			
			System.out.println("There are " + numProperCapped + " properly capped EST sequences.");
			System.out.println("There are " + numImproperCapped + " improperly capped EST sequences.");
		}
		catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally {
			try {
				bw.close();
				bw_rej.close();
				} 
			catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				}
			}
		
	}//end of printTSSs method
	
	/**
	 * It finds the TSS corresponding to this EST. 
	 * If it is less than <tt>percentThreshold</tt> partially aligned 
	 * and its 5' end does not correspond to a capped vector it is rejected. 
	 * @param EST: id of the EST
	 * @param ESTSeq: sequence of the EST
	 */
	private void findTSS(String EST, String ESTSeq)
	{
		if(seqID.equals(EST) && isNewSeqEncountered && !isFirstLine)
		{	
			String cappedSeq = ESTSeq.substring(0, queryStart);
			Matcher capVecMatcher = capVecPattern.matcher(cappedSeq);
			
			int TSS = start;

		    Long key = evalKey(TSS);			
			String value = String.format("%s\tchr%s\t%s\t%d", seqID, chrom, strand, TSS);
			if(capVecMatcher.matches())
			{
				if(mapTSSs.get(key) == null)
					mapTSSs.put(key, new ArrayList<String>());
				else
					System.out.println("xaxa");
			
				mapTSSs.get(key).add(value);
				numProperCapped++;
				
				if(queryStart != 0)
					System.out.println("Capped Sequence");
			}
			else
			{
				String header = String.format(">%s|%s|%s|%d\t%d|%s\t%s\t%d\t%d", seqID, strain, querySize, queryStart, queryEnd, chrom, strand, start, end);
				String seqString = GenbankToFasta.formatSequence(ESTSeq);
				String seq = String.format("%s\n%s%n", header, seqString);
				try {
					bw_rej.write(seq);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				System.out.printf("%s\t%s\t%s\n", value, key, cappedSeq);
				numImproperCapped++;
			}
		}
		
		sb = new StringBuilder();
	}//end of findTSS method
	
	
	private Long evalKey(int TSS)
	{
		// Take only the arithmetic part
		Matcher chromMatcher = chrPattern.matcher(chrom);
		if( chromMatcher.matches() )
			chrom = chromMatcher.group(1);
		
		int chromIndex;
		if( chrom.matches("[Mm][Tt]") )  // check if it is a mitochondrion
			chromIndex = 17;
		else
			chromIndex = Integer.parseInt(chrom);
		
		int strandIndex = (strand.matches("[Cc-]")) ? -1 : 0;
		
		int step  = 2*chromIndex + strandIndex -1;
	    Long key = step*binSize + TSS;
	    
	    return key;
	}//end of evalKey method


}//end of MiuraDataProcessingBLAT class
