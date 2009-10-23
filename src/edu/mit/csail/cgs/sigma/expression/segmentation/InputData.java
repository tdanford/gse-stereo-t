/*
 * Author: tdanford
 * Date: Nov 21, 2008
 */
package edu.mit.csail.cgs.sigma.expression.segmentation;

import java.util.ArrayList;
import java.util.Set;

import edu.mit.csail.cgs.sigma.Printable;
import edu.mit.csail.cgs.utils.models.Model;
import edu.mit.csail.cgs.utils.models.data.DataFrame;
import edu.mit.csail.cgs.utils.models.data.DataRegression;

public interface InputData extends Printable {
	
	/**
	 * The genomic coordinates of these data
	 * @return
	 */
	public Integer[] locations();
	
	/**
	 * The probe values of these data<br>
	 * Each row may represent a different channel
	 * @return
	 */
	public Double[][] values();
	
	/**
	 * The number of data points
	 * @return
	 */
	public int length();
	
	/**
	 * The number of channels
	 * @return
	 */
	public int channels();
	
	public String chrom();
	public String strand();
	
	public Set<String> flags();
}

