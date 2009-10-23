/*
 * Author: tdanford
 * Date: May 28, 2009
 */
package edu.mit.csail.cgs.sigma.expression.normalization;

import java.util.*;

import edu.mit.csail.cgs.sigma.expression.workflow.models.ProbeLine;

public class AdditiveCorrection<M extends SignalModel> {

	private BackgroundEstimation<M> bg;
	private ArrayList<ProbeLine> probes;
	private int channels;
	
	public AdditiveCorrection(Iterator<ProbeLine> ps, BackgroundEstimation<M> estim) { 
		bg = estim;
		probes = new ArrayList<ProbeLine>();
		channels = -1;
		load(ps);
	}
	
	public AdditiveCorrection(BackgroundEstimation<M> est) { 
		bg = est;
		probes = new ArrayList<ProbeLine>();
		channels = -1;
	}
	
	public void load(Iterator<ProbeLine> ps) { 
		while(ps.hasNext()) {
			ProbeLine probe = ps.next();
			probes.add(probe);
			if(channels == -1) { 
				channels = probe.values.length;
			}
		}
	}
	
	public int getNumChannels() { return channels; }
	public int size() { return probes.size(); }
	
	public void correct() { 
		for(int i = 0; i < channels; i++) { 
			M model = bg.estimateModel(probes.iterator(), i);
			
			for(int j = 0; j < probes.size(); j++) {
				ProbeLine p = probes.get(j);
				if(p.isValidValue(i)) {  
					Double value = p.values[i];
					Double correction = model.expectedX(value);
					Double newValue = value - correction;
					//System.out.println(String.format("%.5f - %.5f = %.5f", value, correction, newValue));
					p.values[i] = newValue;
				}
			}
		}
	}
	
	public Iterator<ProbeLine> iterator() { return probes.iterator(); }

	public boolean isEmpty() {
		return probes.isEmpty();
	}

	public ProbeLine get(int i) {
		return probes.get(i);
	}
}
