package edu.mit.csail.cgs.cgstools.oldregression;

public interface Valuation<ValueType> {
	public String getName();
	public ValueType getValue(Datapoints dp, int index); 
}
