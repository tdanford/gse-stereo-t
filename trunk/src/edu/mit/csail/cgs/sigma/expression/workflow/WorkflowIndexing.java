/*
 * Author: tdanford
 * Date: Apr 12, 2009
 */
package edu.mit.csail.cgs.sigma.expression.workflow;

import java.util.*;

import edu.mit.csail.cgs.utils.ArrayUtils;

/**
 * Handles the mapping between "index" and "strain,key" values 
 * that are embedded in the workflow.
 * 
 * @author tdanford
 */
public class WorkflowIndexing {

	private int channels;
	private String arrayStrain;
	
	private String[] exptStrains;
	private String[] exptNames;
	
	public WorkflowIndexing(WorkflowProperties ps, String key) { 
		ResourceBundle bundle = ps.getBundle();
		int n = Integer.parseInt(bundle.getString(String.format("%s_input_channels", key)));
		exptStrains = new String[n];
		exptNames = new String[n];
		channels = n;
		arrayStrain = ps.parseStrainFromKey(key);
		for(int i = 0; i < n; i++) { 
			String k = String.format("%s_input_channel_%d", key, i);
			String[] arr = bundle.getString(k).split(";");
			exptStrains[i] = arr[0];
			exptNames[i] = arr[1];
		}
	}

	/**
	 * OMG, this is an awful hack.  But I don't know how to encode this in a more
	 * standard way, so this will have to do for now.
	 * 
	 * @param strain
	 * @return
	 */
	public String getOppositeStrain(String strain) {
		for(int i = 0; i < exptStrains.length; i++) { 
			if(exptStrains[i].equals(strain)) { 
				if(i % 2 == 0) { 
					return exptStrains[i+1];
				} else { 
					return exptStrains[i-1];
				}
			}
		}
		return null;
	}

	public Integer[][] blocks() { 
		ArrayList<Integer[]> blocks = new ArrayList<Integer[]>();
		Set<String> strs = new TreeSet<String>(ArrayUtils.asCollection(exptStrains));
		Set<String> nms = new TreeSet<String>(ArrayUtils.asCollection(exptNames));
		for(String strain : strs) { 
			for(String key : nms) {
				Integer[] chs = findChannels(strain, key);
				if(chs.length > 0) { 
					blocks.add(chs);
				}
			}
		}
		System.out.println(String.format("WorkflowIndexing.blocks() -> %d blocks", blocks.size()));
		return blocks.toArray(new Integer[0][]);
	}
	
	public int getNumExptNames() { return exptNames.length; }
	
	public String exptStrain(int i) { 
		return exptStrains[i];
	}

	public String[] getExpts(String strain) {
		TreeSet<String> es = new TreeSet<String>();
		for(int i = 0; i < exptStrains.length; i++) { 
			if(exptStrains[i].equals(strain)) { 
				es.add(exptNames[i]);
			}
		}
		return es.toArray(new String[0]);
	}

	public String exptName(int i) { 
		return exptNames[i]; 
	}

	public Set<String> exptNames() { 
		TreeSet<String> ks = new TreeSet<String>();
		for(int i = 0; i < exptNames.length; i++) { ks.add(exptNames[i]); }
		return ks; 
	}
	
	public String getArrayStrain() { return arrayStrain; }
	
	public int getNumChannels() { return channels; }
	
	public String descriptor(int i) { 
		return String.format("%s:%s", exptStrains[i], exptNames[i]);
	}
	
	public Integer[] allChannels() { 
		Integer[] chs = new Integer[channels];
		for(int i = 0; i < chs.length; i++) { 
			chs[i] = i;
		}
		return chs;
	}
	
	public Integer[] findChannels(String str, String key) { 
		TreeSet<Integer> chs = new TreeSet<Integer>();
		for(int i = 0; i < channels; i++) { 
			if(str == null || str.equals(exptStrains[i])) { 
				if(key == null || key.equals(exptNames[i])) { 
					chs.add(i);
				}
			}
		}
		System.out.println(String.format("WorkflowIndexing.findChannels(%s,%s) -> %s", String.valueOf(str), String.valueOf(key), chs.toString()));
		return chs.toArray(new Integer[0]); 
	}

}
