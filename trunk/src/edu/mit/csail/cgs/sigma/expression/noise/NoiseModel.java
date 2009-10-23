package edu.mit.csail.cgs.sigma.expression.noise;

import java.util.*;

import edu.mit.csail.cgs.datasets.general.Point;

public interface NoiseModel {
	public boolean isNoise(Point p, double value);
	public double noiseScore(Point p, double value);
}

