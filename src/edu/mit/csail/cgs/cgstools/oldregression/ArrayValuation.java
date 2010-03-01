package edu.mit.csail.cgs.cgstools.oldregression;

public class ArrayValuation<ValueType> implements Valuation<ValueType> {
	
	private String name;
	private ValueType[] values;
	
	public ArrayValuation(String n, ValueType[] ar) {
		name = n;
		values = ar.clone();
	}
	
	public String getName() { return name; }

	public ValueType getValue(Datapoints dp, int index) {
		return values[index];
	}
	
	public int size() { return values.length; }
}
