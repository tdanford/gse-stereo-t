/*
 * Author: tdanford
 * Date: Jan 22, 2009
 */
package edu.mit.csail.cgs.sigma.expression.segmentation.input;

import edu.mit.csail.cgs.sigma.expression.segmentation.InputData;

public interface InputGenerator {
	public void generate(String chrom, int start, int end, String strand);
	public InputData inputData();
}
