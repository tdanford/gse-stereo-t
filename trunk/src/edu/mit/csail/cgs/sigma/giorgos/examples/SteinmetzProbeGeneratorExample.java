package edu.mit.csail.cgs.sigma.giorgos.examples;

import java.io.File;

import edu.mit.csail.cgs.datasets.species.Genome;
import edu.mit.csail.cgs.datasets.species.Organism;
import edu.mit.csail.cgs.sigma.litdata.steinmetz.SteinmetzProbeGenerator;
import edu.mit.csail.cgs.sigma.litdata.steinmetz.SteinmetzProbeParser;
import edu.mit.csail.cgs.utils.NotFoundException;

public class SteinmetzProbeGeneratorExample {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		File f =  new File("Y:\\group\\psrg\\projects\\sigma\\LitData\\Steinmetz\\Expression\\polyA_norm_ourV_window=25.txt");
		
		try
		{
			Genome g = Organism.findGenome("SGDv1");
			SteinmetzProbeGenerator spg = new SteinmetzProbeGenerator(g, f);
		}
		
		catch(NotFoundException e)
		{
			e.printStackTrace();
		}
		
		

	}

}
