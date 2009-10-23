package edu.mit.csail.cgs.sigma.giorgos.segmentationlab.miura;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.io.FileNotFoundException;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;

import edu.mit.csail.cgs.tools.sequence.GenbankToFasta;

import edu.mit.csail.cgs.utils.Pair;
import edu.mit.csail.cgs.utils.stats.StatUtil;


public class MiuraDataProcessingBLAST extends MiuraDataProcessing {
	
	private StringBuilder sb = new StringBuilder();
	private boolean isNewSeqEncountered = false;
	private boolean isFirstLine = true;

	private String seqID, chrom, strand, strain, querySize;
	private int queryStart, queryEnd, start, end;
	
	private int numUniqueESTs = 0, numUniqueHits = 0, numMultiHits = 0;
	
	private String multiHitsFile;
	private BufferedWriter bw_multi;
	
	private Map<Long, ArrayList<String>> mapTSSs = new TreeMap<Long, ArrayList<String>>();
	private int numProperCapped = 0, numImproperCapped = 0;
	
	private BufferedWriter bw_rej;
	private String rejectedFile;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String path = "/afs/csail.mit.edu/group/psrg/projects/sigma/LitData/Miura/Data/DDBJ/";
		String input = path + "s288c_sk1_31847_against_BLAST.blasttab";
		String output = path + "unique_s288c_sk1_31847_against_BLAST";

		try {
			InputStream hits = new FileInputStream(new File(input));
			OutputStream uniqueHits = new FileOutputStream(new File(output));
			
			MiuraDataProcessingBLAST mdp = new MiuraDataProcessingBLAST();
			mdp.printUniqueHits(hits, uniqueHits, 0);
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public MiuraDataProcessingBLAST()
	{
		multiHitsFile = "/afs/csail.mit.edu/group/psrg/projects/sigma/LitData/Miura/Data/DDBJ/" + "multiHitsBLAST";
		rejectedFile = "/afs/csail.mit.edu/group/psrg/projects/sigma/LitData/Miura/Data/DDBJ/" + "rejectedBLAST.txt";
	}//default constructor
	
	public MiuraDataProcessingBLAST(String multiHitsF, String rejF, double percThres)
	{
		multiHitsFile = multiHitsF;
		rejectedFile = rejF;
	}//customizable constructor

	
	/**
	 * It prints the unique hits of the input stream <tt>hits</tt> (after BLASTing the ESTs) w.r.t. the <tt>bitScore</tt>. Hits of the same
	 * EST (cDNA) with equal maximum <tt>bitScore</tt> corresponding to multiple loci are discarded.
	 * @param hits input stream containing the hits of ESTs against a genome with <tt>BLAST</tt> tool<br>
	 * The input file <tt>hits</tt> should be of the form:
	 * <pre>
	 * EST(cDNA)	chrom	percentID numMismatches	numGaps	queryStart queryEnd	subjectStart	subjectEnd	eValue	bitScore
	 * 
	 * E.g. 
	 * DB636784     chr4    99.44   354     2       0       1       354     721530  721177  0.0      686
	 * </pre>
	 * @param uniqueHits output stream with unique hits<br>
	 * The output file will be of the form:
	 * <pre>
	 * key	chrom	queryStart	queryEnd	start	end	strand
	 * 
	 * E.g.
	 * DB636784     chr4    1       354     721530  721177  -
	 * </pre>
	 * @param numHeaderLines number of lines corresponding to the header
	 */
	public void printUniqueHits(InputStream hits, OutputStream uniqueHits, int numHeaderLines)
	{
		BufferedReader br = null;
		BufferedWriter bw = null;
		try 
		{
			InputStreamReader isr = new InputStreamReader(hits); 
			br = new BufferedReader(isr);
			
			OutputStreamWriter osw = new OutputStreamWriter(uniqueHits);
			bw = new BufferedWriter(osw);
			bw.write(String.format("EST\tchr\tqueryStart\tqueryEnd\tstart\tend\tstrand%n"));
			
			bw_multi = new BufferedWriter(new FileWriter(new File(multiHitsFile)));
			
			ArrayList<Double> bitScores = new ArrayList<Double>();
			ArrayList<String> records = new ArrayList<String>();
			
			int count = 0;
			String str;
			String prevKey = "";
			boolean isFirst = true;
			
			for(int i = 0; i < numHeaderLines; i++)
				str = br.readLine();
				
			while( (str = br.readLine()) != null )
			{	
				String[] tokens = str.split("\\s+");
				String key = tokens[0];
				String chrom = tokens[1];
				// Isolate chromosome number
				Matcher chrMatcher = chrPattern.matcher(chrom);
				if(chrMatcher.matches())
				{
					String usefulPart = chrMatcher.group(1);
					chrom = usefulPart.length() == 1 ? "0" + usefulPart : usefulPart;
				}

				//coordinate of EST where matching starts
				int queryStart = Integer.parseInt(tokens[6])-1;   // indexing starts at 0
				
				//coordinate of EST where matching ends
				int queryEnd = Integer.parseInt(tokens[7])-1;   // indexing starts at 0
				
				//coordinate on the genome where matching starts
				int start = Integer.parseInt(tokens[8])-1;   // indexing starts at 0
				
				//coordinate on the genome where matching starts
				int end = Integer.parseInt(tokens[9])-1;   // indexing starts at 0
				
				// Determine the strand
				char strand = (start <= end) ? '+' : '-';
				
				String record = String.format("%s\t%s\t%d\t%d\t%d\t%d\t%c", key, chrom, queryStart, queryEnd, start, end, strand);
				
				Double bitScore = Double.parseDouble(tokens[tokens.length-1]);
								
				if( !( key.equals(prevKey) || isFirst ) )
				{
					printUniqueRecord(bitScores, records, bw);
					bitScores.clear(); records.clear();
					numUniqueESTs++;
				}
				
				bitScores.add(bitScore);  records.add(record);
				
				prevKey = key;  isFirst = false;
				
				count++;
				if( count%10000 == 0 )
					System.out.println("entry " + count + " has been parsed...");
			}// end of while condition
			
			printUniqueRecord(bitScores, records, bw);
			
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
	 * @param bitScores bit scores of all hits for this EST (cDNA)
	 * @param records corresponding records
	 * @param bw output stream where the best hits will be writen
	 */
	private void printUniqueRecord(ArrayList<Double> bitScores, ArrayList<String> records, BufferedWriter bw)
	{
		try 
		{
			Double[] bitScoresD = bitScores.toArray(new Double[0]);
			Pair<Double, TreeSet<Integer>> max_maxIndexes = StatUtil.findMax(bitScoresD);
			TreeSet<Integer> maxIndexes = max_maxIndexes.getLast();

			// Discard more than one best hits: EST refers to multiple loci
			if(maxIndexes.size() == 1)
			{
				Integer index = maxIndexes.toArray(new Integer[0])[0];
				String bestRecord = records.get(index);
				bw.write(String.format("%s%n", bestRecord));
				
				numUniqueHits++;
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
	 * Prints the TSSs corresponding to the unique hits. If an EST is not fully aligned, 
	 * its 5' end is checked if it corresponds to a G-capped vector. 
	 * If yes, it is accepted. Otherwise, rejected.
	 * @param uniqueHits input stream with unique hits<br>
	 * The input file should be of the form:
	 * <pre>
	 * key	chrom	queryStart	queryEnd	start	end	strand
	 * 
	 * E.g.
	 * DB636784     chr4    1       354     721530  721177  -
	 * </pre>
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
				querySize = seqID_tokens[3];
				
				queryStart = Integer.parseInt(tokens1[2]);
				queryEnd = Integer.parseInt(tokens1[3]);
				
				chrom = tokens1[1];
				start = Integer.parseInt(tokens1[4]);
				end = Integer.parseInt(tokens1[5]);
				
				strand = tokens1[6];
			   
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
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}//end of printTSSs method
	
	/**
	 * It finds the TSS corresponding to this EST. If it is not fully aligned and its 5' end does not 
	 * correspond to a capped vector it is rejected. 
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
			
			String value = String.format("%s\tchr%s\t%s\t%d", seqID, chrom, strand, TSS);
			if(capVecMatcher.matches() || queryStart == 0)
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

}//end of MiuraDataProcessingBLAST class
