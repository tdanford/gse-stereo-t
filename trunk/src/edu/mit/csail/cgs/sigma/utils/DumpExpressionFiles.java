/*
 * Author: tdanford
 * Date: May 8, 2008
 */
package edu.mit.csail.cgs.sigma.utils;

import java.util.*;
import java.io.*;

import edu.mit.csail.cgs.datasets.species.Genome;
import edu.mit.csail.cgs.ewok.verbs.GenomeExpander;
import edu.mit.csail.cgs.sigma.*;
import edu.mit.csail.cgs.sigma.expression.*;
import edu.mit.csail.cgs.sigma.expression.ewok.StandardProbeGenerator;
import edu.mit.csail.cgs.sigma.expression.models.ExpressionProbe;

public class DumpExpressionFiles {
    
    public static void main(String[] args) { 
        dump_data(new StacieExpressionProperties());
        dump_data(new SudeepExpressionProperties());
    }
    
    public static void dump_data(BaseExpressionProperties props) { 
        DumpExpressionFiles dumper = new DumpExpressionFiles(props);
        SigmaProperties sps = props.getSigmaProperties();
        File dir = sps.getBaseDir();
        
        for(String exptKey : props.getExptKeys()) { 
            dumper.loadData(exptKey);
            File f = new File(dir, String.format("%s_rawdata.txt", exptKey)); 
            try {
                dumper.dumpData(f);
            } catch (IOException e) {
                e.printStackTrace();
            }
            dumper.closeData();
            System.out.println("Saved: " + f.getName());
        }
    }
    
    private BaseExpressionProperties props;
    private StandardProbeGenerator generator;
    private String exptKey;

    public DumpExpressionFiles(BaseExpressionProperties bps) { 
        props = bps;
    }
    
    public void loadData(String ek) { 
        exptKey = ek;
        generator = new StandardProbeGenerator(props, exptKey);        
    }
    
    public void closeData() { 
        generator.close();
        generator = null;
        exptKey = null;
    }
    
    public void dumpData(File f) throws IOException {
        String strain = props.parseStrainFromExptKey(exptKey);
        Genome g = props.getGenome(strain);
        PrintStream ps = new PrintStream(new FileOutputStream(f));
        
        GenomeExpander<ExpressionProbe> prober = new GenomeExpander<ExpressionProbe>(generator);
        Iterator<ExpressionProbe> probes = prober.execute(g);
        while(probes.hasNext()) { 
            ExpressionProbe p = probes.next();
            String chrom = p.getChrom();
            int location = p.getLocation();
            char strand = p.getStrand();
            double value = p.mean();
            
            ps.println(String.format("%s:%d:%c\t%f", chrom, location, strand, value));
        }
        
        ps.close();
    }
}
