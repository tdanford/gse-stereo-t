/*
 * Author: tdanford
 * Date: Mar 2, 2009
 */
package edu.mit.csail.cgs.sigma.litdata;

import java.util.*;
import java.io.*;

import edu.mit.csail.cgs.utils.parsing.expression.*;

public class SOFTToTableParser {

	public static void main(String[] args) { 
		File desktop = new File("C:\\Documents and Settings\\tdanford\\Desktop");
		File gpl90_dir = new File(desktop, "Microarray_Platforms\\GEO\\GPL90");
		File gpl2529_dir = new File(desktop, "Microarray_Platforms\\GEO\\GPL2529");
		File iyer_dir = new File(desktop, "Microarray_Platforms\\GEO\\Iyer");
		File dir = iyer_dir;
		
		//File f = new File(dir, "GPL3588_family.soft");
		//File f = new File(dir, "GPL90_family.soft");
		//File f = new File(dir, "GPL2529_family.soft");
		File f = new File(dir, "GPL3588_family.soft");

		try {
			//File tableFile = new File(dir, "table_output.txt");
			File tableFile = new File(dir, "pvalue_table_output.txt");
			SOFTToTableParser parser = new SOFTToTableParser(f, tableFile);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private SOFTParser parser;
	
	public SOFTToTableParser(File f, File tf) throws IOException { 
		parser = new SOFTParser(f, tf);
	}
	
	public void outputTable(File f) throws IOException { 
		PrintStream ps = new PrintStream(new FileOutputStream(f));
		SOFTPlatform platform = parser.getPlatform();
		ps.print("ID\tDescription");
		for(String id : platform.allIDs()) { 
			ps.print("\t" + id);
		}
		ps.println();
		
		for(int i = 0; i < parser.getNumSamples(); i++) { 
			SOFTSample sample = parser.getSample(i);
			ps.print(String.format("%s\t%s",
					sample.getAttributes().getCompleteValue("SAMPLE"),
					sample.getAttributes().getCompleteValue("Sample_title")));
			for(String id : platform.allIDs()) { 
				Double value = sample.getValue(id);
				ps.print(String.format("\t%.2f", value));
			}
			ps.println();
		}
		ps.close();
	}
}
