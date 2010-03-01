/*
 * Author: tdanford
 * Date: Apr 21, 2009
 */
package edu.mit.csail.cgs.cgstools.tgraphs.patterns;

public interface EdgePattern extends GraphPattern { 
	public String fromNode(); 
	public String toNode();
}
