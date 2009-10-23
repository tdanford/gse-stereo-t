/*
 * Author: tdanford
 * Date: May 21, 2008
 */
package edu.mit.csail.cgs.sigma.expression;

import java.util.Collection;

import edu.mit.csail.cgs.sigma.expression.models.Transcript;

public interface ExpressionFinder {

    public void loadData(String exptKey);
    public void closeData();
    public Collection<Transcript> findExpression();
}
