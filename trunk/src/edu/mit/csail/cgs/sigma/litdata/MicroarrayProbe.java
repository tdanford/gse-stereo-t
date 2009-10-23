/*
 * Author: tdanford
 * Date: Mar 3, 2009
 */
package edu.mit.csail.cgs.sigma.litdata;

import edu.mit.csail.cgs.utils.models.Model;

public class MicroarrayProbe extends Model {

	public String id;
	public Double[] values;
	public String[] samples;
	
	public MicroarrayProbe() {}
	
	public MicroarrayProbe(String i, Double[] v, String[] s) { 
		id = i;
		values = v.clone();
		samples = s.clone();
		
		if(values.length != samples.length) { 
			throw new IllegalArgumentException(
					String.format("%d != %d", values.length, samples.length));
		}
	}
	
	public int length() { return values.length; }
	
	public void append(MicroarrayProbe p) {
		if(id == null) { 
			id = p.id;
			values = p.values.clone();
			samples = p.samples.clone();
			return;
		}
		
		if(!p.id.equals(id)) { throw new IllegalArgumentException(p.id); }
		Double[] v = new Double[values.length + p.values.length];
		String[] s = new String[samples.length + p.samples.length];
		
		for(int i = 0; i < v.length; i++) { 
			v[i] = i < values.length ? values[i] : p.values[i-values.length];
			s[i] = i < samples.length ? samples[i] : p.samples[i-samples.length];
		}
		
		values = v;
		samples = s;
	}
}
