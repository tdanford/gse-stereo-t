/*
 * Created on Apr 14, 2008
 *
 * TODO 
 * 
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package edu.mit.csail.cgs.sigma.blots;

import java.awt.*;
import java.util.*;

import edu.mit.csail.cgs.viz.paintable.*;

public class IntensityHistogram {

    private int[] counts;
    private int total;
    private int max;
    
    public IntensityHistogram(int length) { 
        counts = new int[length];
        total = 0;
        max = 0;
    }
    
    public void count(int intensity, int number) { 
        counts[intensity] += number;
        total += number;
        max = Math.max(counts[intensity], max);
    }
    
    public void finished() { 
    }
    
    public int getCount(int intensity) { 
        return counts[intensity];
    }
    
    public void clear() { 
    	for(int i = 0; i < counts.length; i++) { counts[i] = 0; }
    	total = max = 0;
    }
    
    public int max() { 
    	return max;
    }
    
    public int getLength() { return counts.length; }
}
