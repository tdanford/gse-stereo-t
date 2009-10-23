/*
 * Created on Mar 12, 2008
 *
 * TODO 
 * 
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package edu.mit.csail.cgs.sigma.genes;

import java.util.*;
import java.util.regex.*;
import java.io.*;

import edu.mit.csail.cgs.sigma.*;
import edu.mit.csail.cgs.utils.NotFoundException;
import edu.mit.csail.cgs.utils.Pair;
import edu.mit.csail.cgs.datasets.general.*;
import edu.mit.csail.cgs.datasets.species.*;
import edu.mit.csail.cgs.ewok.verbs.FilterIterator;

public class ProbesFromPrimers {
    
    public static void main(String[] args) { 
        try {
            Genome g = Organism.findGenome(args[0]);
            File blasttab = new File(args[1]);
            ProbesFromPrimers pfp = new ProbesFromPrimers(g, blasttab);
            Collection<NamedRegion> probes = pfp.parseProbes();
            
            for(NamedRegion probe : probes) {
                String probeName = probe.getName();
                System.out.println(String.format("%s\t%s\t%d\t%d", probeName, 
                        probe.getChrom(), probe.getStart(), probe.getEnd()));
            }
            
        } catch (NotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }
    
    private File blasttab;
    private Genome genome;
    
    public ProbesFromPrimers(Genome g, File bt) {
        genome = g;
        blasttab = bt;
    }
    
    public Collection<NamedRegion> parseProbes() throws IOException {
        double ethreshold = 1.0e-3;
        Parser<BlastTabEntry> parser = 
            new Parser<BlastTabEntry>(blasttab, new BlastTabEntry.ParsingMapper());
        Iterator<BlastTabEntry> itr = 
            new FilterIterator<BlastTabEntry,BlastTabEntry>(
                    new BlastTabEntry.ExpectedScoreFilter(ethreshold), parser);
        
        Map<String,Set<Region>> primerHits = new TreeMap<String,Set<Region>>();
        Set<String> primerNames = new TreeSet<String>();
        
        while(itr.hasNext()) { 
            BlastTabEntry entry = itr.next();
            NamedRegion r = createNamedRegion(entry); 
            if(!primerHits.containsKey(entry.getQuery())) { 
                primerHits.put(entry.getQuery(), new HashSet<Region>());
            }
            primerHits.get(entry.getQuery()).add(r);
            primerNames.add(removePrimerLR(entry.getQuery()));
        }
        
        LinkedList<NamedRegion> probes = new LinkedList<NamedRegion>();
        
        for(String primerName : primerNames) { 
            Pair<Region,Region> hits = findProbePair(primerName, primerHits);
            if(hits != null) { 
                Region left = hits.getFirst(), right = hits.getLast(); 
				int start = Math.min(left.getStart(), right.getStart());
				int end = Math.max(left.getEnd(), right.getEnd());

                NamedRegion probe = 
                    new NamedRegion(genome, left.getChrom(), start, end, primerName);
                probes.addLast(probe);
            }
        }
        
        return probes;
    }
    
    public NamedRegion createNamedRegion(BlastTabEntry e) { 
        int start = Math.min(e.getSubjectStart(), e.getSubjectEnd());
        int end = Math.max(e.getSubjectStart(), e.getSubjectEnd());
        String chrom = getChrom(e.getSubject());
        return new NamedRegion(genome, chrom, start, end, e.getQuery());
    }
    
    private Pair<Region,Region> findProbePair(String primerName, Map<String,Set<Region>> primerHits) { 
        String leftName = String.format("%s_left", primerName);
        String rightName = String.format("%s_right", primerName);
        
        if(!primerHits.containsKey(leftName) || !primerHits.containsKey(rightName)) { return null; }
        if(primerHits.get(leftName).size() != 1 || primerHits.get(rightName).size() != 1) { return null; }

        Iterator<Region> leftitr = primerHits.get(leftName).iterator();
        Iterator<Region> rightitr = primerHits.get(rightName).iterator();
        Region left = leftitr.next();
        Region right = rightitr.next();
        
        if(!left.getChrom().equals(right.getChrom())) { return null; }

        return new Pair<Region,Region>(left, right);
    }

    public static Pattern primerLRPattern = Pattern.compile("(\\d+_\\d+_\\d+)_(?:(?:left)|(?:right))");
    public static Pattern primerNamePattern = Pattern.compile("([\\d]+)_([\\d]+)_([\\d]+)_(?:(?:left)|(?:right))");
    public static Pattern chromPattern = Pattern.compile("(?:chr)?(.*)");
    
    public static String removePrimerLR(String pname) { 
        Matcher m = primerLRPattern.matcher(pname);
        if(!m.matches()) { throw new IllegalArgumentException(pname); }
        return m.group(1);
    }

    public static int[] decodePrimerName(String pname) { 
        Matcher m = primerNamePattern.matcher(pname);
        if(!m.matches()) { throw new IllegalArgumentException(pname); }
        int[] a = new int[3];
        a[0] = Integer.parseInt(m.group(1));
        a[1] = Integer.parseInt(m.group(2));
        a[2] = Integer.parseInt(m.group(3));
        return a;
    }
    
    public static String getChrom(String chromName) { 
        Matcher m = chromPattern.matcher(chromName);
        if(!m.matches()) { return chromName; }
        return m.group(1);
    }
}

