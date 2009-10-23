package edu.mit.csail.cgs.sigma.expression.noise;

import edu.mit.csail.cgs.datasets.general.Point;
import edu.mit.csail.cgs.sigma.expression.BaseExpressionProperties;
import edu.mit.csail.cgs.utils.probability.ExponentialDistribution;

public class PValueNoiseModel implements NoiseModel {
    
    private double pvalueThreshold;
    private ExponentialDistribution dist;
	
	public PValueNoiseModel(ExponentialDistribution ed, double pv) { 
        dist = ed;
        pvalueThreshold = pv;
	}

	public boolean isNoise(Point p, double value) {
        double pvalue = 1.0 - dist.calculateCumulProbability(value);
        return pvalue > pvalueThreshold;
	}

	public double noiseScore(Point p, double value) {
        double pvalue = 1.0 - dist.calculateCumulProbability(value);
        return pvalue;
	} 
    
    public String toString() { 
        return String.format("Dist: %s, P-Value: %.3f", dist.toString(), pvalueThreshold);
    }
}
