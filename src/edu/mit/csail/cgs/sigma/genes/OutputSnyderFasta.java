/*
 * Created on Mar 13, 2008
 *
 * TODO 
 * 
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package edu.mit.csail.cgs.sigma.genes;

import java.util.*;
import java.io.*;

import edu.mit.csail.cgs.datasets.general.*;
import edu.mit.csail.cgs.datasets.species.*;
import edu.mit.csail.cgs.ewok.verbs.*;
import edu.mit.csail.cgs.sigma.SigmaProperties;

public class OutputSnyderFasta {
    
    public static void main(String[] args) { 
        SigmaProperties sp = new SigmaProperties();
        double threshold = 4.0;
        OutputSnyderFasta fasta = new OutputSnyderFasta(sp.getGenome("sigma"), threshold);
        fasta.outputFasta(args[0]);
    }
    
    private double sdThreshold;
    private Genome genome;

    public OutputSnyderFasta(Genome g, double thresh) { 
        sdThreshold = thresh;
        genome = g;
    }
    
    public void outputFasta(String factor) { 
        SequenceGenerator gen = new SequenceGenerator();
        Iterator<SpottedProbe> probes = findBoundProbes(factor);
        
        PrintStream ps = System.out;
        
        while(probes.hasNext()) { 
            SpottedProbe p = probes.next();
            String seq = gen.execute(p);
            ps.println(String.format(">%s", p.getName()));
            ps.println(seq);
        }
    }
    
    private Iterator<SpottedProbe> findBoundProbes(String factor) { 
        ChromRegionIterator chroms = new ChromRegionIterator(genome);
        Iterator<Region> rchroms = new MapperIterator<NamedRegion,Region>(new CastingMapper<NamedRegion,Region>(), chroms);
        
        SpottedProbeGenerator spgen = new SpottedProbeGenerator(genome, "spottedArray");
        Iterator<SpottedProbe> probes = new ExpanderIterator<Region,SpottedProbe>(spgen, rchroms);

        probes = new FilterIterator<SpottedProbe,SpottedProbe>(new BoundFilter(factor, sdThreshold), probes);
        return probes;
    }
}

class BoundFilter implements Filter<SpottedProbe,SpottedProbe> {
    
    private String factor;
    private double thresh;
    
    public BoundFilter(String f, double t) { 
        factor = f;
        thresh = t;
    }

    public SpottedProbe execute(SpottedProbe a) {
        return a.getBoundFactors(thresh).contains(factor) ? a : null;
    } 
}
