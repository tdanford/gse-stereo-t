/*
 * Created on Apr 10, 2008
 *
 * TODO 
 * 
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package edu.mit.csail.cgs.sigma.expression;

import edu.mit.csail.cgs.datasets.general.Stranded;
import edu.mit.csail.cgs.ewok.verbs.Filter;

public class StrandFilter<X extends Stranded> implements Filter<X,X> {
    
    private char strand;
    
    public StrandFilter(char c) { 
        strand = c;
    }

    public X execute(X a) {
        return a.getStrand() == strand ? a : null;
    }

}
