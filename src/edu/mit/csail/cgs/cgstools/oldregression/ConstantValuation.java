package edu.mit.csail.cgs.cgstools.oldregression;

public class ConstantValuation<X> implements Valuation<X> {
	
	private String name;
	private X constant;
	
	public ConstantValuation(String n, X v) {
		name = n;
		constant = v;
	}

	public String getName() {
		return name;
	}

	public X getValue(Datapoints dp, int index) {
		return constant;
	}

}
