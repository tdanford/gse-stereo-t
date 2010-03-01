package edu.mit.csail.cgs.cgstools.oldregression;

public class CategoricalArrayValuation 
	extends ArrayValuation<Integer> 
	implements CategoricalValuation {
	
	private int[] values;
	
	public CategoricalArrayValuation(String n, Integer[] array, int max) { 
		super(n, array);
		values = new int[max+1];
		for(int i = 0; i <= max; i++) { values[i] = i; }
	}

	public CategoricalArrayValuation(String n, Integer[] array) { 
		super(n, array);
		int max = 0; 
		for(int i = 0; i < array.length; i++) { max = Math.max(array[i], max); }
		values = new int[max+1];
		for(int i = 0; i <= max; i++) { values[i] = i; }
	}

	public int[] getValues() {
		return values;
	}

}
