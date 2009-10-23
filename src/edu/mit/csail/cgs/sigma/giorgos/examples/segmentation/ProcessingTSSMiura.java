package edu.mit.csail.cgs.sigma.giorgos.examples.segmentation;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * This class assumes that the TSSs given by Miura is given in grouped form (see Table 2 of Supplementary Info). That is, cDNA clones 
 * corresponding to the same TSS are written in contiguous form in the file (the one after another). In other words, you cannot have
 * an entry with a TSS having more than one line distance from the previous clone corresponding to the same TSS.
 * @author gio_fou
 *
 */
public class ProcessingTSSMiura {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		try {
			String inPath = "/afs/csail.mit.edu/group/psrg/projects/sigma/LitData/Miura/TSSs_Miura.txt";
			String outPath = "/afs/csail.mit.edu/group/psrg/projects/sigma/LitData/Miura/TSSs_Miura_count.txt";
			
			File f = new File(inPath);
			BufferedReader br = new BufferedReader(new FileReader(f));
			
			FileWriter fw = new FileWriter(outPath);
			fw.write("chr\tstrand\tcoord\tcount\n");
			
			int prevCoord = 0;
			int coord = 0;
			
			String prevChrom = "";
			String chrom = "";
			
			String prevStrand = "";
			String strand = ""; 
			int count = 0;
			int numUniqueTSSs = 0;
			boolean isFirstLine = true;
			
			br.readLine();
			String str;
			while( (str = br.readLine()) != null)
			{
				String[] array = str.split("\\s+");
				
				chrom = array[0];
				strand = array[1];
				coord = Integer.parseInt(array[2]);
				
				chrom = chrom.substring(3);
				strand = strand.equals("W") ? "+" : "-";
				
				if( coord != prevCoord && !isFirstLine)
				{
					fw.write(prevChrom + "\t" + prevStrand + "\t" + prevCoord + "\t" + count + "\n");
					count = 0;
					
					numUniqueTSSs++;
				}
				
				count++;
							
				prevCoord = coord;
				prevChrom = chrom;
				prevStrand = strand;
				isFirstLine = false;
			}
			
			fw.write(chrom + "\t" + strand + "\t" + coord + "\t" + count + "\n");
			
			fw.close();
			
			System.out.println("The number of unique TSSs is: " + (numUniqueTSSs+1));
			
		} 
		
		catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		catch(IOException e) {
			e.printStackTrace();
		}

	}// end of main method

}
