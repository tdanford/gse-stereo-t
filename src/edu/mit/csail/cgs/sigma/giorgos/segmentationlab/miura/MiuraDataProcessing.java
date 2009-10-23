package edu.mit.csail.cgs.sigma.giorgos.segmentationlab.miura;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class defines methods for reading hits after aligning the ESTs against some tool (e.g. <tt>BLAST</tt>, <tt>BLAT</tt>)
 * and printing only the hits corresponding to unique loci through the <tt>printUniqueHits</tt> method and
 * finds the TSSs of these unique hit loci through the <tt>printTSSs</tt> method.
 * @author gio_fou
 *
 */
public abstract class MiuraDataProcessing {
	
	//This pattern captures the following cases: c1, c01, ch1, ch01, chr1, chr01, ..., cmt, cmt, chmt, chmt, chrmt, chrmt  
	private final String chrRegEx = ".*(?:c|ch|chr)(\\d{1,2}|[A-Za-z]{1,2})";
	public final Pattern chrPattern = Pattern.compile(chrRegEx);
	
	//Checks if the 5' unaligned version has this pattern
	private final String capVecRegEx = "[tT]*[gG]+";
	public final Pattern capVecPattern = Pattern.compile(capVecRegEx);
	
	// Assume that each chromosome holds maximally 10^10 bases (created for creating keys for a TreeMap)
	public final long binSize = (long) Math.pow(10, 10);

	/**
	 * It prints the unique hits of a file after aligning the ESTs (cDNAs) against some tool (e.g. <tt>BLAST</tt>, <tt>BLAT</tt>)
	 * @param hits input stream of aligned file with alignment statistics
	 * @param uniqueHits output stream with unique hits
	 * @param numHeaderLines number of lines corresponding to the header
	 */
	public abstract void printUniqueHits(InputStream hits, OutputStream uniqueHits, int numHeaderLines);
	
	/**
	 * Prints the TSSs corresponding to the unique hits.
	 * @param uniqueHits input stream with unique hits
	 * @param ESTsFasta input stream of ESTs (cDNAs) in fasta format 
	 * @param TSSs output stream of TSSs that correspond to these ESTs
	 * @param numHeaderLines_hits number of lines corresponding to the header
	 */
	public abstract void printTSSs(InputStream uniqueHits, InputStream ESTsFasta, OutputStream TSSs, int numHeaderLines_hits);
	
	/**
	 * It prints the unique TSSs for this dataset (assigning a count of occurences for each unique TSS) 
	 * @param TSSs input stream of TSSs<br>
	 * The input file has to be of the form:
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
	 * Also, <tt>chrom</tt> should be of the form: {<tt>chrx, chx, cx, x</tt>}, where <tt>x</tt> is a number<br>
	 * and <tt>strand</tt> should take the values: {<tt>+, W, w, -, C, c</tt>}
	 * @param uniqueTSSs output stream of unique TSSs<br>
	 * The output file will be in the following form:
	 * <pre>
	 * chr	strand	coord	count
	 * 
	 * E.g.
	 * 01	-	58471	15
	 * </pre>
	 * @param numHeaderLines number of lines corresponding to the header
	 */
	public void printUniqueTSSs(InputStream TSSs, OutputStream uniqueTSSs, int numHeaderLines)
	{
		BufferedReader br = null;
		BufferedWriter bw = null;
		
		int count = 0;  // The number of times that a unique TSS is present				
		int numUniqueTSSs = 0;  // The number of unique TSSs
		
		try
		{
			InputStreamReader isr = new InputStreamReader(TSSs);
			br = new BufferedReader(isr);
			
			OutputStreamWriter osw = new OutputStreamWriter(uniqueTSSs);
			bw = new BufferedWriter(osw);
			
			bw.write(String.format("chr\tstrand\tcoord\tcount%n"));
			
			int prevCoord = 0;	    int coord = 0;
			String prevChrom = "";	String chrom = "";
			String prevStrand = "";	String strand = ""; 
			boolean isFirstLine = true;
			
			String str;	
			for(int i = 0; i < numHeaderLines; i++)
				str = br.readLine();
			
			int i = 1;   // initially, making the assumption that the 1st column is useless (e.g. corresponds to EST)
			while( (str = br.readLine()) != null)
			{
				Matcher lineMatcher = chrPattern.matcher(str);
				
				if(lineMatcher.lookingAt() && !str.startsWith("EST\t"))
				{
					String[] array = str.split("\\s+");
					
					// check if 1st column is useless (that is, contains EST information)
					String temp_str = array[0];
					if( chrPattern.matcher(temp_str).lookingAt() )
						i = 0;                        // if i = 0 => it is not useless
					
					chrom = array[0+i];
					strand = array[1+i];
					coord = Integer.parseInt(array[2+i]);
					
					// check if strand has a valid value. Otherwise, skip this record.
					if(strand.matches("[Ww+Cc-]"))
					{
						// Isolate strand. If W, w or +, it is Watson strand.
						strand = (strand.matches("[Ww+]")) ? "+" : "-";

						// Isolate chromosome number
						Matcher chrMatcher = chrPattern.matcher(chrom);
						if(chrMatcher.matches())
						{
							String usefulPart = chrMatcher.group(1);
							chrom = usefulPart.length() == 1 ? "0" + usefulPart : usefulPart;
						}
												
						if( coord != prevCoord && !isFirstLine)
						{
							bw.write(String.format("%s\t%s\t%d\t%d%n", prevChrom, prevStrand, prevCoord, count));
							count = 0;
							numUniqueTSSs++;
						}
						
						prevCoord = coord; 	prevChrom = chrom; 	prevStrand = strand;
						isFirstLine = false;
						count++;
					}
				}
			}//end of while loop
		
			//write last unique TSS
			bw.write(String.format("%s\t%s\t%d\t%d%n", chrom, strand, coord, count));
		} 
		catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace(); System.exit(-1);
		}
		catch(IOException e) {
			e.printStackTrace(); System.exit(-1);
		}
		finally {
			try {
				br.close();
				bw.close();
				System.out.println("The number of unique TSSs is: " + (numUniqueTSSs+1));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}// end of printUniqueTSSs method
	
	
}//end of MiuraDataProcessing abstract class
