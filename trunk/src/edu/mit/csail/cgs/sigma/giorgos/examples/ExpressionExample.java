package edu.mit.csail.cgs.sigma.giorgos.examples;


import java.util.*;

import edu.mit.csail.cgs.sigma.expression.*;
import edu.mit.csail.cgs.sigma.expression.ewok.StandardProbeGenerator;
import edu.mit.csail.cgs.sigma.expression.models.ExpressionProbe;
import edu.mit.csail.cgs.datasets.species.*;
import edu.mit.csail.cgs.datasets.general.*;
import edu.mit.csail.cgs.ewok.verbs.*;

public class ExpressionExample {
    
    public static void main(String[] args) { 
        BaseExpressionProperties props = new SudeepExpressionProperties();
        
        for(String exptKey : props.getExptKeys()) { 
            System.out.println(exptKey);
            String strain = props.parseStrainFromExptKey(exptKey);
            Genome g = props.getGenome(strain);
            System.out.println(g.toString());
        }

        Vector<String> exptKeys = new Vector<String>(props.getExptKeys());
        
        String ek0 = exptKeys.get(0);
        Genome g = props.getGenome(props.parseStrainFromExptKey(ek0));

        String ek1 = exptKeys.get(1);
        Genome g1 = props.getGenome(props.parseStrainFromExptKey(ek1));

        
        Expander<Region,ExpressionProbe> prober =
            new StandardProbeGenerator(props, ek0);
        
        Expander<Genome,ExpressionProbe> gexp = 
            new GenomeExpander<ExpressionProbe>(prober);
        
        Iterator<ExpressionProbe> probes = gexp.execute(g);
        
        
        Expander<Region,ExpressionProbe> prober1 =
            new StandardProbeGenerator(props, ek1);
        
        Expander<Genome,ExpressionProbe> gexp1 = 
            new GenomeExpander<ExpressionProbe>(prober1);
        
        Iterator<ExpressionProbe> probes1 = gexp1.execute(g1);
        
        // what are the values between 10,000bp and 20,000bp on 
        // chromosome 3?
        Region r = new Region(g, "3", 10000, 20000);
        Iterator<ExpressionProbe> rprobes = prober.execute(r);
        
        while(probes.hasNext()) { 
            ExpressionProbe p = probes.next();
            
            Genome probeGenome = p.getGenome();
            String chrom = p.getChrom();
            int bpLocation = p.getLocation();
            char strand = p.getStrand();  // '+' or '-'
            String probeKey = p.getExptKey();
            Collection<Double> values = p.getAllValues();
            Iterator<Double> v = values.iterator();
            Double f = v.next();
            
            System.out.println(String.format("%s:%d:%s\t%.3f", 
                    chrom, bpLocation, String.valueOf(strand), f));
        }
        
        int foo = 3;
        
        
    }
}
