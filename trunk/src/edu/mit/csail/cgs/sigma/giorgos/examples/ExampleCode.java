package edu.mit.csail.cgs.sigma.giorgos.examples;

import java.util.*;
import edu.mit.csail.cgs.datasets.general.*;
import edu.mit.csail.cgs.datasets.species.*;
import edu.mit.csail.cgs.ewok.verbs.*;
import edu.mit.csail.cgs.utils.NotFoundException;

public class ExampleCode {

    public static void main(String[] args) { 
        try {
            Organism org = Organism.getOrganism("Saccharomyces cerevisiae");
            Genome sigmaGenome = org.getGenome("Sigmav6");
            Genome s288cGenome = org.getGenome("SGDv1");
            
            Iterator<NamedRegion> itr1 = new ChromRegionIterator(sigmaGenome);
            
            Mapper<NamedRegion,Region> caster = new CastingMapper<NamedRegion,Region>();
            Iterator<Region> itr2 = new MapperIterator<NamedRegion,Region>(caster, itr1); 
            
            Expander<Region,Gene> gen = new RefGeneGenerator(sigmaGenome, "sgdGene");
            Iterator<Gene> itr3 = new ExpanderIterator<Region,Gene>(gen, itr2);
            
            while(itr3.hasNext()) { 
                Gene g = itr3.next();
                System.out.println(g.getID() + " " + g.getName());
            }
            
        } catch (NotFoundException e) {
            e.printStackTrace();
        }
        
    }
}
