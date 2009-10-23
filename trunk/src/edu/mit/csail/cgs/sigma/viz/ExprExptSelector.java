/*
 * Author: tdanford
 * Date: May 23, 2008
 */
package edu.mit.csail.cgs.sigma.viz;

import java.util.*;
import edu.mit.csail.cgs.sigma.expression.*;

public class ExprExptSelector {
	
	public static Collection<ExprExptSelector> createSelectors(BaseExpressionProperties props, String strain) {
		LinkedList<ExprExptSelector> sels = new LinkedList<ExprExptSelector>();
		for(String ek : props.getExptKeys()) { 
			String str = props.parseStrainFromExptKey(ek);
			if(str.equals(strain))  {
				sels.add(new ExprExptSelector(props, ek));
			}
		}
		return sels;
	}

	private BaseExpressionProperties props;
	private String exptKey;
	
	public ExprExptSelector(BaseExpressionProperties ps, String ek) { 
		props = ps;
		exptKey = ek;
	}
	
	public String getExptKey() { return exptKey; }
	
	public BaseExpressionProperties getProps() { return props; }
	
	public int hashCode() { return exptKey.hashCode(); }
	
	public String toString() { return exptKey; }
	
	public boolean equals(Object o) { 
		if(!(o instanceof ExprExptSelector)) { return false; }
		ExprExptSelector ees = (ExprExptSelector)o;
		if(!exptKey.equals(ees.exptKey)) { return false; }
		return true;
	}
}
