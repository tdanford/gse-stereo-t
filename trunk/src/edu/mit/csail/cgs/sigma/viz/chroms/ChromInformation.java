/*
 * Author: tdanford
 * Date: Feb 19, 2009
 */
package edu.mit.csail.cgs.sigma.viz.chroms;

public interface ChromInformation {
	
	/**
	 * Called whenever the information along a chromosome needs to be re-calculated
	 * for visualization.  The <tt>units</tt> argument is the number of "bins" into which
	 * the data should be summarized. 
	 * 
	 * @param units
	 */
	public void recalculate(int units);
	
	/**
	 * Should return the value of the <tt>unit</tt> argument to the last call to 
	 * recalculate, or an undefined value if recalculate has not been called yet.
	 * 
	 * @return
	 */
	public int size();
	
	/**
	 * Returns the length of the chromosome in question (measured in base-pairs, 
	 * or whatever the general arbitrary smallest unit of measurement is).  
	 * 
	 * @return
	 */
	public int length();  
}
