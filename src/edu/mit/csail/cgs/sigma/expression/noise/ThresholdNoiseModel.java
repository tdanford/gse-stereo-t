package edu.mit.csail.cgs.sigma.expression.noise;

import edu.mit.csail.cgs.datasets.general.Point;
import edu.mit.csail.cgs.sigma.expression.BaseExpressionProperties;

public class ThresholdNoiseModel implements NoiseModel {
    
	private BaseExpressionProperties props;
	private double zThresh;
	private double[] params;
	private double thresh;
	
	public ThresholdNoiseModel(BaseExpressionProperties ps) { 
		props = ps;
		params = props.getBackgroundParameters();
		zThresh = props.getZScoreThreshold();
		thresh = params[0] + params[1]*zThresh;
	}

	public boolean isNoise(Point p, double value) {
		return value < thresh;
	}

	public double noiseScore(Point p, double value) {
		if(isNoise(p, value)) { 
			return 1.0; 
		} else { 
			return 0.0;
		}
	} 
	
}
