/*
 * Author: tdanford
 * Date: Oct 8, 2008
 */
package edu.mit.csail.cgs.sigma.viz.chroms;

public interface ChromScorer extends ChromInformation {
	
	/**
	 * Returns the value in the given bin. <br>
	 * Must be preceded by at least one call to <tt>recalculate</tt>, otherwise
	 * the return value is undefined.
	 * 
	 * @param unit The bin in which to find a value. 
	 * 
	 * @return
	 */
	public Double value(int unit);
	
	/**
	 * No call to <tt>value</tt>, for any value of the <tt>unit</tt> argument, should
	 * ever return a value greater than the valued returned from this method.  (This 
	 * maximum can change after a call to <tt>recalculate</tt>).  
	 * 
	 * @return
	 */
	public Double max();

	/**
	 * 
	 * @return
	 */
	public Double zero();
	
	/**
	 * Returns a character denoting the strand of the given chromosome.
	 * @return Either '+' or '-'
	 */
	public Character strand();
}
