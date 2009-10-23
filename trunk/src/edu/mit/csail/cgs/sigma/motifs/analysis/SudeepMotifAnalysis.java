/*
 * Author: tdanford
 * Date: May 15, 2008
 */
package edu.mit.csail.cgs.sigma.motifs.analysis;

import java.util.*;

import edu.mit.csail.cgs.datasets.motifs.*;
import edu.mit.csail.cgs.ewok.verbs.motifs.*;

import edu.mit.csail.cgs.sigma.*;
import edu.mit.csail.cgs.sigma.motifs.*;
import edu.mit.csail.cgs.sigma.genes.*;
import edu.mit.csail.cgs.sigma.expression.*;
import edu.mit.csail.cgs.utils.*;
import edu.mit.csail.cgs.utils.probability.Hypergeometric;

public class SudeepMotifAnalysis {

    public static void main(String[] args) { 
        GeneAnnotationProperties gps = new GeneAnnotationProperties();
        MotifProperties mps = new MotifProperties();
        
        SudeepMotifAnalysis sma = new SudeepMotifAnalysis(gps, mps);
        sma.loadData("sigma");
        
        sma.analyzeDivergentPromoters();
        
        sma.closeData();
    }
    
    private Hypergeometric hypgeom;
    private GeneAnnotationProperties gaProps;
    private MotifProperties mProps;
    private Motifs motifs;
    private String strain;
    private PromoterIdentification promoters;
    private OverlappingRegionFinder<Promoter> promoterFinder;
    private WeightMatrixExpander matrixExpander;
    private float cutoffFraction;
    
    public SudeepMotifAnalysis(GeneAnnotationProperties gps, MotifProperties mps) { 
        gaProps = gps;
        mProps = mps;
        hypgeom = new Hypergeometric();
        cutoffFraction = (float)0.75;
    }
    
    public void loadData(String str) {
        strain = str;
        motifs = new Motifs(mProps, strain);
        motifs.loadData();
        promoters = new PromoterIdentification(gaProps, strain);
        promoterFinder = new OverlappingRegionFinder<Promoter>(promoters.getAllPromoters());
        
        matrixExpander = new WeightMatrixExpander();
        for(WeightMatrix m : motifs.getMatrices()) {
            float cutoff = cutoffFraction * (float)m.getMaxScore();  
            matrixExpander.addWeightMatrix(m, cutoff);
        }
    }
    
    public void closeData() {
        strain = null;
        motifs = null;
        promoters = null;
        promoterFinder = null;
        matrixExpander = null;
    }
    
    private Map<String,Integer> initMatrixHitCounts() { 
        Map<String,Integer> hitCounts= new HashMap<String,Integer>();
        for(WeightMatrix m : motifs.getMatrices()) { 
            hitCounts.put(m.name, 0);
        }
        return hitCounts;
    }
    
    private void updateMatrixHitCounts(WeightMatrixHit h, Map<String,Integer> counts) { 
        String name = h.getMatrix().name;
        counts.put(name, counts.get(name)+1);
    }
    
    public void analyzeDivergentPromoters() { 
        Iterator<Promoter> proms = promoters.getAllPromoters();
        
        Map<String,Integer> totalHits = initMatrixHitCounts();
        Map<String,Integer> divergentHits = initMatrixHitCounts();
        
        int totalCount = 0, divergentCount = 0;
        
        while(proms.hasNext()) { 
            Promoter p = proms.next();
            Iterator<WeightMatrixHit> hits = matrixExpander.execute(p);
            totalCount += 1;
            if(p.isDivergent()) { 
                divergentCount += 1;
            }

            while(hits.hasNext()) { 
                WeightMatrixHit hit = hits.next();
                updateMatrixHitCounts(hit, totalHits);
                
                if(p.isDivergent()) { 
                    updateMatrixHitCounts(hit, divergentHits);
                }
            }
            
            System.out.println(p);
        }
        System.out.println();
        
        TreeSet<Enrichment> enrichments = new TreeSet<Enrichment>();
        int N = totalCount;
        int theta = divergentCount;
        
        for(String motifName : totalHits.keySet()) {
            int n = totalHits.get(motifName);
            if(n > 0) { 
                int x = divergentHits.get(motifName);
                double logPValue = hypgeom.log_hypgeomPValue(N, theta, n, x);
                Enrichment e = new Enrichment(motifName, N, theta, n, x, logPValue);
                enrichments.add(e);
            }
        }
        
        System.out.println();
        System.out.println(String.format("# Promoters: %d", totalCount));
        System.out.println(String.format("# Divergent Promoters: %d", divergentCount));
        
        int i = 0;
        for(Enrichment e : enrichments) { 
            System.out.println(String.format("%d  \t%s", ++i, e.toString()));
        }
    }
}
