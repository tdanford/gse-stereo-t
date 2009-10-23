package edu.mit.csail.cgs.sigma.giorgos.examples.segmentation;

import java.util.*;

public interface Tiler {
	public Integer[] tile(int start, int end, int num); 
}

class RegularTiler implements Tiler { 

	public Integer[] tile(int start, int end, int num) {
		Integer[] array = new Integer[num];
		int length = end-start+1;
		
		for(int i = 0; i < num; i++) { 
			array[i] = start + (int)Math.floor((double)(i * length) / (double)num);
		}
		
		return array;
	}
}

class RandomTiler implements Tiler {
	
	Random rand = new Random();

	public Integer[] tile(int start, int end, int num) {
		Integer[] array = new Integer[num];
		int length = end-start+1;
		
		if(num > length) { throw new IllegalArgumentException(); }
		if(num == length) { 
			for(int i = 0; i < array.length; i++) { 
				array[i] = start+i;
			}
		} else { 
			Set<Integer> values = new TreeSet<Integer>();
			while(values.size() < num) { 
				values.add(rand.nextInt(start+length));
			}
			int i = 0;
			for(Integer value : values) { 
				array[i++] = value;
			}
		}
		
		return array;
	}
	
	
}
