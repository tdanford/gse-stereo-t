/*
 * Created on Dec 17, 2007
 */
package edu.mit.csail.cgs.sigma.motifs;

import java.sql.SQLException;
import java.util.*;
import java.util.logging.*;

import edu.mit.csail.cgs.ewok.verbs.*;
import edu.mit.csail.cgs.ewok.verbs.motifs.WeightMatrixExpander;
import edu.mit.csail.cgs.ewok.verbs.motifs.WeightMatrixFilter;
import edu.mit.csail.cgs.ewok.verbs.motifs.WeightMatrixHit;
import edu.mit.csail.cgs.datasets.general.*;
import edu.mit.csail.cgs.datasets.motifs.*;
import edu.mit.csail.cgs.datasets.species.*;

/**
 * @author tdanford
 * 
 */
public class Motifs {
	
    private MotifProperties props;
    private Logger logger;
    private String strain;
    private Genome genome;
    
    private TotalWeightMatrices matrices;
    private Map<WeightMatrix,Float> cutoffs;
    
    public Motifs(MotifProperties ps, String str) {
    	strain = str;
        props = ps;
        logger = props.getLogger(String.format("Motifs.%s", strain));
        genome = props.getSigmaProperties().getGenome(strain);
        matrices = new TotalWeightMatrices(props);
        cutoffs = new HashMap<WeightMatrix,Float>();
    }
    
    public void loadData() { 
        matrices.loadMatrices();
        double fraction = props.getStandardCutoffFraction();
        
        for(WeightMatrix matrix : matrices.getMatrices()) { 
            double maxScore = matrix.getMaxScore();
            float cutoff = (float)(maxScore * fraction);
            cutoffs.put(matrix, cutoff);
        }
    }
    
    public Collection<WeightMatrix> getMatrices() { 
        return matrices.getMatrices();
    }
    
    public Collection<WeightMatrix> findMatrices() { 
    	WeightMatrixLoader loader = props.createLoader();
    	Collection<WeightMatrix> mats = new LinkedList<WeightMatrix>();

    	try {
            mats.addAll(loader.loadMatrices(props.getSigmaProperties().getOrganism()));
        } catch (SQLException e) {
            e.printStackTrace();
        }

    	loader.close();
    	return mats;
    }

    public Collection<WeightMatrixHit> findAllMotifs(Collection<WeightMatrix> ms, Region r) { 
        WeightMatrixExpander exp = new WeightMatrixExpander();
        
        for(WeightMatrix m : ms) { 
            float c = cutoffs.get(m);
            exp.addWeightMatrix(m, c);
        }
        
        Iterator<WeightMatrixHit> hits = exp.execute(r);
        LinkedList<WeightMatrixHit> hitlist = new LinkedList<WeightMatrixHit>();
        while(hits.hasNext()) { hitlist.add(hits.next()); }
        return hitlist;        
    }

    public Collection<WeightMatrixHit> findMotifs(WeightMatrix m, Region r) { 
        return findMotifs(m, r, cutoffs.get(m));
    }
    
    public Collection<WeightMatrixHit> findMotifs(WeightMatrix m, Region r, double fraction) { 
        float cutoff = (float)(fraction * m.getMaxScore());
        return findMotifs(m, r, cutoff);
    }
    
    private Collection<WeightMatrixHit> findMotifs(WeightMatrix m, Region r, float cutoff) {
        WeightMatrixExpander exp = new WeightMatrixExpander(m, cutoff);
        Iterator<WeightMatrixHit> hits = exp.execute(r);
        LinkedList<WeightMatrixHit> hitlist = new LinkedList<WeightMatrixHit>();
        while(hits.hasNext()) { hitlist.add(hits.next()); }
        return hitlist;
    }

    public boolean hasMotif(WeightMatrix m, Region r) { 
        return hasMotif(m, r, cutoffs.get(m));
    }
    
    public boolean hasMotif(WeightMatrix m, Region r, double fraction) { 
        float cutoff = (float)(fraction * m.getMaxScore());
        return hasMotif(m, r, cutoff);
    }
    
    private boolean hasMotif(WeightMatrix m, Region r, float cutoff) {
        WeightMatrixFilter exp = new WeightMatrixFilter(m, cutoff);
        Region r2 = exp.execute(r);
        return r2 != null;
    }
}
