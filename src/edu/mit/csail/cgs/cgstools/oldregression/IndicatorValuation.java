package edu.mit.csail.cgs.cgstools.oldregression;

public class IndicatorValuation implements Valuation<Boolean> {
	
	private Valuation<Integer> categorical;
	private int value;
	
	public IndicatorValuation(Valuation<Integer> p, int v) { 
		categorical = p;
		value = v;
	}

	public Boolean getValue(Datapoints dp, int index) {
		return categorical.getValue(dp, index) == value;
	}

	public String getName() { return String.format("%s=%d", categorical.getName(), value); }
}
