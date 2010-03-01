package edu.mit.csail.cgs.cgstools.oldregression;

public class TransformedValuation implements Valuation<Double> {
	
	private Valuation<Double> original;
	private Transformation trans;
	
	public TransformedValuation(Valuation<Double> v, Transformation t) { 
		original = v;
		trans = t;
	}

	public String getName() {
		return original.getName();
	}

	public Double getValue(Datapoints dp, int index) {
		return trans.transform(original.getValue(dp, index));
	}

}
