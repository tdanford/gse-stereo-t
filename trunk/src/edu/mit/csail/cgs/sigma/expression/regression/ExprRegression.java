package edu.mit.csail.cgs.sigma.expression.regression;

import java.io.*;
import java.util.*;

import Jama.Matrix;

import edu.mit.csail.cgs.sigma.*;
import edu.mit.csail.cgs.sigma.expression.BaseExpressionProperties;
import edu.mit.csail.cgs.sigma.expression.StrandFilter;
import edu.mit.csail.cgs.sigma.expression.ewok.StandardProbeGenerator;
import edu.mit.csail.cgs.sigma.expression.models.ExpressionProbe;
import edu.mit.csail.cgs.sigma.expression.models.Transcript;
import edu.mit.csail.cgs.sigma.expression.noise.*;
import edu.mit.csail.cgs.sigma.expression.regression.*;

import edu.mit.csail.cgs.cgstools.oldregression.Datapoints;
import edu.mit.csail.cgs.cgstools.oldregression.MappedValuation;
import edu.mit.csail.cgs.cgstools.oldregression.PredictorSet;
import edu.mit.csail.cgs.cgstools.oldregression.Regression;
import edu.mit.csail.cgs.datasets.general.*;
import edu.mit.csail.cgs.datasets.species.*;
import edu.mit.csail.cgs.ewok.verbs.ChromRegionIterator;
import edu.mit.csail.cgs.ewok.verbs.FilterIterator;

/**
 * A generic interface for performing spot-wise regressions on the expression
 * data.
 * 
 * @author tdanford
 */
public class ExprRegression {

	protected BaseExpressionProperties props;
	protected String exptKey, strain;
	protected int segmentNumProbes;
	
	protected StandardProbeGenerator prober;
	protected StrandFilter<ExpressionProbe> watson, crick;
	
	public ExprRegression(BaseExpressionProperties ps) { 
		props = ps;
		watson = new StrandFilter<ExpressionProbe>('+');
		crick = new StrandFilter<ExpressionProbe>('-');
		segmentNumProbes = props.getMinSegmentProbes();
	}
	
	public void loadData(String ek) { 
		exptKey = ek;
		strain = props.parseStrainFromExptKey(ek);
		prober = new StandardProbeGenerator(props, exptKey);
	}
	
	public void closeData() { 
		exptKey = strain = null;
		prober.close();
		prober = null;
	}
	
	public Iterator<ExpressionProbe> probes(StrandedRegion r) { 
		Iterator<ExpressionProbe> probed = 
			new FilterIterator<ExpressionProbe,ExpressionProbe>(
					r.getStrand()=='+' ? watson : crick, prober.execute(r));
		return probed;
	}
	
	public Transcript createTranscript(StrandedRegion r) {
		return createTranscript(r, probes(r));
	}
	
	public Transcript createTranscript(StrandedRegion r, Iterator<ExpressionProbe> probes) { 
        Regression reg = createRegression(r, probes);
        if(reg==null) { return null; }
        
        double[] params = paramsFromRegression(reg);
        return new Transcript(r, params);
	}
	
	public static double[] paramsFromRegression(Regression reg) { 
		Matrix betaHat = null;
		double s2 = 0.0;
		try { 
			betaHat = reg.calculateBetaHat();
			s2 = reg.calculateS2(betaHat);
		} catch(RuntimeException re) { 
			return null;
		}
        double rms = s2 / (double)reg.getSize();
        
        double intercept = betaHat.get(0,0);
        double slope = betaHat.get(1, 0);
        
        double[] params = new double[3];
        params[0] = intercept; params[1] = slope; params[2] = rms;
		
        return params;
	}
	
	public Regression createRegression(StrandedRegion r, Iterator<ExpressionProbe> ps) {
		
    	Datapoints dps = new Datapoints();
    	PredictorSet preds = new PredictorSet();
    	MappedValuation<Double> values = new MappedValuation<Double>("expression");
    	MappedValuation<Double> offsets = new MappedValuation<Double>("offsets");

    	int count = 0;
    	while(ps.hasNext()) { 
    		ExpressionProbe p = ps.next();
    		double offset = r.getStrand() == '+' ? 
    				(double)(p.getLocation()-r.getStart()) :  
        			(double)(r.getEnd()-p.getLocation());

    		double value = Math.log(p.mean());
			
			if(!Double.isNaN(value) && !Double.isInfinite(value)) {
				String name = String.format("%s:%d:%c", 
						p.getChrom(), p.getLocation(), p.getStrand());
				
				dps.addDatapoint(name);
				offsets.addValue(name, offset);
				values.addValue(name, value);

				count += 1;
			}
    	}

    	if(count < segmentNumProbes) { 
    		return null;
    	}

		preds.addConstantPredictor();
		preds.addQuantitativePredictor(offsets);
    	
		return new Regression(values, preds, dps);
	}

}
