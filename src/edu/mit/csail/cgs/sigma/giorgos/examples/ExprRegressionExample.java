package edu.mit.csail.cgs.sigma.giorgos.examples;

import java.util.Collection;

import edu.mit.csail.cgs.cgstools.oldregression.Regression;
import edu.mit.csail.cgs.datasets.general.StrandedRegion;
import edu.mit.csail.cgs.datasets.species.Genome;
import edu.mit.csail.cgs.sigma.expression.SudeepExpressionProperties;
import edu.mit.csail.cgs.sigma.expression.models.Transcript;
import edu.mit.csail.cgs.sigma.expression.regression.ExprRegression;

import Jama.Matrix;

public class ExprRegressionExample {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		SudeepExpressionProperties props = new SudeepExpressionProperties();
		Collection<String> expKeysCol = props.getExptKeys();
		String[] expKeys = expKeysCol.toArray(new String[expKeysCol.size()]);
		String expKey = expKeys[0];
		
		ExprRegression expreg = new ExprRegression(props);
		expreg.loadData(expKey);
		
		String strain = props.parseStrainFromExptKey(expKey); Genome g = props.getGenome(strain);
		StrandedRegion sregion = new StrandedRegion(g, "1", 1, 12000, '+');
		
		Regression reg = expreg.createRegression(sregion, expreg.probes(sregion));
		
		System.out.println(reg.getSize());
		Matrix x = reg.getX();
		System.out.println("\n --- \nIndependent var X is:\n");
		reg.printMatrix(x, System.out);
		Matrix y = reg.getY();
		System.out.println("\n --- \nDependent var Y is:\n");
		reg.printMatrix(y, System.out);
		
		
		Transcript t = expreg.createTranscript(sregion);
		
		
		int foo = 3;
	}

}
