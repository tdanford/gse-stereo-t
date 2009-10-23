/*
 * Author: tdanford
 * Date: Jan 28, 2009
 */
package edu.mit.csail.cgs.sigma.expression.workflow.assessment;

import java.util.*;

import edu.mit.csail.cgs.datasets.binding.BindingEvent;
import edu.mit.csail.cgs.datasets.binding.BindingExtent;
import edu.mit.csail.cgs.datasets.chipchip.ChipChipBayes;
import edu.mit.csail.cgs.datasets.general.NamedRegion;
import edu.mit.csail.cgs.datasets.general.Region;
import edu.mit.csail.cgs.datasets.locators.*;
import edu.mit.csail.cgs.datasets.species.*;
import edu.mit.csail.cgs.ewok.verbs.*;
import edu.mit.csail.cgs.utils.NotFoundException;

public class Reb1BindingAssessment {

	public static void main(String[] args) { 
		Genome genome = null;
		try {
			genome = Organism.findGenome("SGDv1");
		} catch (NotFoundException e) {
			e.printStackTrace();
		}
		String exptName = "Sc S288C mat-a Reb1 vs WCE";
		String exptVersion = "median linefit";
		String jbdVersion = "12/18/08 standard params: 12/18/08";
		ChipChipLocator locator = new ChipChipLocator(genome, exptName, exptVersion);
		BayesLocator jbdloc = new BayesLocator(genome, exptName, jbdVersion);
		ChipChipBayes bayes = jbdloc.createObject();
		
		double prob = 0.25;
		double size = 1.0;
		BayesBindingGenerator generator = new BayesBindingGenerator(bayes, prob, size, true);
		
		Iterator<Region> itr = new MapperIterator<NamedRegion,Region>(
				new CastingMapper<NamedRegion,Region>(), 
				new ChromRegionIterator(genome));
		Iterator<BindingExtent> bitr = new ExpanderIterator<Region,BindingExtent>(
				generator, itr);

		int c = 0;
		while(bitr.hasNext()) { 
			BindingExtent ext = bitr.next();
			if(!ext.getChrom().equals("mt")) { 
				c += 1;
				System.out.println(ext.toString());
			}
		}
		
		System.out.println(String.format("# Events: %d", c));
	}
}
