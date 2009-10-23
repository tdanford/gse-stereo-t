package edu.mit.csail.cgs.sigma.giorgos.examples;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Vector;

/**
 * This class is to divide an expression file containing all the chromosomes and all the strands in 
 * separate files, each one having only the strand of a chromosome.
 * 
 * The lines of the file should have the form:
 * <pre>
 * chromosome	strand	anything_else
 * 
 * E.g.
 * 1	-	1	0.01
 * </pre>
 * @author gio_fou
 *
 */
public class SeperateChromsStrands {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Vector<FileWriter> fWriters = new Vector<FileWriter>();
		
		try {
			long start = System.currentTimeMillis();
			
			String commonPath = "/afs/csail.mit.edu/group/psrg/projects/sigma/LitData/Steinmetz/Expression/";
			String outPath = commonPath + "picard/chrs/";
			String inPath = commonPath + "polyA_norm_Wats_Picar_ourV_window=25_sameStart.txt";
			File f = new File(inPath);
			
			String prevChrom="";
			String chrom;
			String prevStrand = "";
			String strand;
			
			BufferedReader br = new BufferedReader(new FileReader(f));
			br.readLine();
			String str;
			FileWriter fw = null;
			while( (str = br.readLine()) != null )
			{
				String[] array = str.split("\\s+");
				
				chrom = array[0];
				strand = array[1];
				
				if( (!chrom.equals(prevChrom)) || (!strand.equals(prevStrand)) )
				{
					String chrom_msg = chrom;
					if( chrom_msg.length() == 1 )
						chrom_msg = "0" + chrom_msg;
					
					System.out.println("chr" + chrom_msg + ", strand:" + strand + " is parsed...");	
						
					fw = new FileWriter(outPath + "chr" + chrom_msg + "_" + strand + "_Stein_sameStart.txt");
					fWriters.add(fw);
				}
				
				fw.write(str + "\n");	
				
				prevChrom = chrom;
				prevStrand = strand;
			}
			
			for(int i = 0; i < fWriters.size(); i++)
			{
				fWriters.get(i).close();
			}
			
			long end = System.currentTimeMillis();
			
			long duration = end-start;
			double durationD = (double) duration; durationD /= 1000.0;
			
			System.out.println("\nThe run took " + durationD + " seconds...");
			
			
		} 
		
		catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}// end of main method

}
