/*
 * Author: tdanford
 * Date: Apr 22, 2009
 */
package edu.mit.csail.cgs.cgstools.tgraphs;

import java.util.Iterator;

public interface QueryEvaluator {
	public Iterator<GraphContext> eval(GraphQuery query);
}
