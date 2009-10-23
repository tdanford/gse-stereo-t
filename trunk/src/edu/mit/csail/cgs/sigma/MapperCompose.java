/*
 * Author: tdanford
 * Date: May 21, 2008
 */
/**
 * 
 */
package edu.mit.csail.cgs.sigma;

import edu.mit.csail.cgs.ewok.verbs.Mapper;

/**
 * @author tdanford
 *
 */
public class MapperCompose<X,Y,Z> implements Mapper<X,Z> {
	
	private Mapper<X,Y> mapper1; 
	private Mapper<Y,Z> mapper2;

	public MapperCompose(Mapper<X,Y> m1, Mapper<Y,Z> m2) { 
		mapper1 = m1; mapper2 = m2;
	}
	
	public Z execute(X v) { 
		return mapper2.execute(mapper1.execute(v));
	}
}
