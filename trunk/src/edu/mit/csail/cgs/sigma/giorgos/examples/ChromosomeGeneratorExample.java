package edu.mit.csail.cgs.sigma.giorgos.examples;

import edu.mit.csail.cgs.ewok.verbs.ChromosomeGenerator;
import edu.mit.csail.cgs.sigma.expression.SudeepExpressionProperties;
import edu.mit.csail.cgs.sigma.expression.BaseExpressionProperties;
import edu.mit.csail.cgs.datasets.general.Region;
import edu.mit.csail.cgs.datasets.species.Genome;
import edu.mit.csail.cgs.ewok.verbs.Expander;
import edu.mit.csail.cgs.sigma.SigmaProperties;

import java.util.Collection;
import java.util.Iterator;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ChromosomeGeneratorExample {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		//Get the expression keys that correspond to Sudeep's data
		SudeepExpressionProperties props = new SudeepExpressionProperties();
		Collection<String> exptKeys = props.getExptKeys();
		String[] expKeysString = exptKeys.toArray(new String[exptKeys.size()]);
		
		// Focus on the first key (original s288c)
		/*
		String ek0 = expKeysString[0];
		Genome g = props.getGenome(props.parseStrainFromExptKey(ek0));
		
		
		
		// take the regions corresponding to genome g, either by constructing an ChromosomeGenerator object or
		ChromosomeGenerator<Genome> chg = new ChromosomeGenerator<Genome>();
		Iterator<Region> chrs = chg.execute(g);
		
		// an Expander that is created by a ChromosomeGenerator
		Expander<Genome, Region> regionExpander = new ChromosomeGenerator<Genome>();
		Iterator<Region> rr = regionExpander.execute(g);
		*/
		
		// chrs and rr are the SAME
		
		
		SigmaProperties sip = props.getSigmaProperties();
		Collection<String> strainsCollection = sip.getStrains();
		String[] strains = strainsCollection.toArray(new String[strainsCollection.size()]);
		String currStrain = strains[1];
		Collection<String> cellsKeysCollection = props.getStrainCellKeys(currStrain);
		Collection<String> replCellsKeysColection = props.getStrainReplicateCellKeys(currStrain);
		
		
		Collection<String> cellsCollection = props.getStrainCells(currStrain);
		Collection<String> replCellsCollection = props.getStrainReplicateCells(currStrain);
		
		
		
		
		
		
		
		
		//String[] cells = cellsKeysCollection.toArray(new String[cellsKeysCollection.size()]);
		String[] cells = cellsCollection.toArray(new String[cellsCollection.size()]);
		String currCell = cells[0];
		
		File expSegmFile = props.getExpressionSegmentFile(currStrain, currCell);
		File expTransFile = props.getExpressionTranscriptFile(currStrain, currCell);
		
		try 
		{
			FileWriter fw1 = new FileWriter("Y:\\u\\g\\geopapa\\Desktop\\Geopapa\\Research\\Data\\test\\expressionSegmentFile.txt");
			FileWriter fw2 = new FileWriter("Y:\\u\\g\\geopapa\\Desktop\\Geopapa\\Research\\Data\\test\\expressionTranscriptionFile.txt");
			BufferedReader br1 = new BufferedReader(new FileReader(expSegmFile));
			String line; 
			while((line = br1.readLine()) != null)
			{
				int foo1 = 2;
				fw1.write(line + "\n");
			}
			br1.close();
			fw1.close();
			
			BufferedReader br2 = new BufferedReader(new FileReader(expTransFile));
			while((line = br2.readLine()) != null)
			{
				int foo1 = 2;
				fw2.write(line + "\n");
			}
			br2.close();
			fw2.close();
			
			
			
			
		
			
			
			
		} 
		catch (FileNotFoundException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
		
		int foo = 3;
	}

}
