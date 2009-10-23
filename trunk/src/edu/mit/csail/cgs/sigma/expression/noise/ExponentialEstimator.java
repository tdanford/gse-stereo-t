/*
 * Created on Apr 10, 2008
 *
 * TODO 
 * 
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package edu.mit.csail.cgs.sigma.expression.noise;

import edu.mit.csail.cgs.sigma.*;
import edu.mit.csail.cgs.utils.probability.ExponentialDistribution;

public class ExponentialEstimator {
    
    private double defaultLambda;
    private double sum;
    private int count;

    public void startEstimating(double def) { 
        sum = 0.0;
        defaultLambda = def; 
        count = 0;
    }
    
    public void addDatapoint(double value) {
        sum += value;
        count += 1;
    }
    
    public ExponentialDistribution estimate() { 
        double mean = count > 0 ? sum / (double)count : 1.0/defaultLambda;
        System.out.println(String.format("Count: %d", count));
        System.out.println(String.format("Estimated mean: %f", mean));
        double lambda = 1.0 / mean;
        return new ExponentialDistribution(lambda);
    }
}

