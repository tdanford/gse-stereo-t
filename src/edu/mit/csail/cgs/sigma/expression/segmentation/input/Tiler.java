/*
 * Author: tdanford
 * Date: Nov 19, 2008
 */
package edu.mit.csail.cgs.sigma.expression.segmentation.input;

import java.util.*;

public interface Tiler {
	public Integer[] tile(int start, int end, int num); 
}

class RegularTiler implements Tiler { 

	/**
	 * 
	 * @param start start position
	 * @param end end position 
	 * @param num # of locations
	 * @return regularly tiled locations
	 */
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

	/**
	 * 
	 * @param start start position
	 * @param end end position 
	 * @param num # of locations
	 * @return randomly tiled locations
	 */
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
