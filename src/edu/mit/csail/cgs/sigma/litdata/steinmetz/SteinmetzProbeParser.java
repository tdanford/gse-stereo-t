package edu.mit.csail.cgs.sigma.litdata.steinmetz;

import java.util.*;
import java.io.*;

import edu.mit.csail.cgs.sigma.expression.*;
import edu.mit.csail.cgs.sigma.expression.models.*;
import edu.mit.csail.cgs.utils.NotFoundException;

import edu.mit.csail.cgs.ewok.verbs.*;
import edu.mit.csail.cgs.datasets.general.*;
import edu.mit.csail.cgs.datasets.species.*;

public class SteinmetzProbeParser implements Iterator<ExpressionProbe> { 
	
	public static void main(String[] args) { 
		SteinmetzProperties sprops = new SteinmetzProperties();
		File f = sprops.getDataFile();
		try {
			Genome g = sprops.getGenome();
			SteinmetzProbeParser parser = new SteinmetzProbeParser(g, f);
			
			while(parser.hasNext()) { 
				ExpressionProbe p = parser.next();
				System.out.println(p.toString());
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private Genome genome;
	private File file;
	private String exptName;
	
	private BufferedReader br;
	private ExpressionProbe nextProbe;
	
	public SteinmetzProbeParser(Genome g, File f) throws IOException { 
		genome = g;
		file = f;
		exptName = file.getName();
		br = new BufferedReader(new FileReader(f));
		br.readLine();  // handles the header line.
		
		findNextProbe();
	}
	
	private void findNextProbe() throws IOException { 
		String line = br.readLine();
		if(line != null) { 
			String[] array = line.split("\\s+");
			String chrom = array[0];
			char strand = array[1].charAt(0);
			int position = Integer.parseInt(array[2]);
			double value = Double.parseDouble(array[3]);
			
			nextProbe = new ExpressionProbe(genome, chrom, position, strand, exptName, value);
			
		} else { 
			br.close();
			nextProbe = null;
		}
	}

	public boolean hasNext() {
		return nextProbe != null;
	}

	public ExpressionProbe next() {
		ExpressionProbe p = nextProbe;
		try {
			findNextProbe();
		} catch (IOException e) {
			e.printStackTrace();
			nextProbe = null;
		}
		return p;
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}
}
