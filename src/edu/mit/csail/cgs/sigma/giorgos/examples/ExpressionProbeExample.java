package edu.mit.csail.cgs.sigma.giorgos.examples;

import java.util.Collection;
import java.util.ArrayList;
import java.util.HashSet;

import edu.mit.csail.cgs.datasets.species.Genome;
import edu.mit.csail.cgs.sigma.expression.models.ExpressionProbe;
import edu.mit.csail.cgs.sigma.expression.regression.ExprRegression;
import edu.mit.csail.cgs.datasets.species.Genome;
import edu.mit.csail.cgs.sigma.expression.SudeepExpressionProperties;
import edu.mit.csail.cgs.datasets.general.StrandedRegion;
import edu.mit.csail.cgs.datasets.species.Genome;
import edu.mit.csail.cgs.cgstools.oldregression.Regression;


public class ExpressionProbeExample {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		SudeepExpressionProperties props = new SudeepExpressionProperties();
		ExprRegression expreg = new ExprRegression(props);
		
		Collection<String> expKeysCol = props.getExptKeys();
		String[] expKeys = expKeysCol.toArray(new String[expKeysCol.size()]);
		String expKey = expKeys[0];
		String strain = props.parseStrainFromExptKey(expKey);
		Genome g = props.getGenome(strain);
		StrandedRegion sregion = new StrandedRegion(g, "1", 1, 1200, '+');
		
		expreg.loadData(expKey);
		Regression reg = expreg.createRegression(sregion, expreg.probes(sregion));
		
		
		
		
		
		
		ArrayList<Double> vals = new ArrayList<Double>();
		vals.add(new Double(-3)); vals.add(new Double(7)); vals.add(new Double(-2)); 
		vals.add(new Double(1)); vals.add(new Double(4)); 
		ExpressionProbe exp = new ExpressionProbe(new Genome("sigma"), "xaxa", 1, '+', "xixi", vals);
		
		System.out.println(exp.std_deviation());
	}

}
