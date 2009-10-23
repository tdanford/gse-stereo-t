/*
 * Author: tdanford
 * Date: May 6, 2009
 */
package edu.mit.csail.cgs.sigma.expression.simulation;

import java.util.*;
import java.util.regex.*;
import java.io.*;

import edu.mit.csail.cgs.sigma.expression.workflow.models.TranscriptCall;
import edu.mit.csail.cgs.utils.models.Model;

public class SimParameters extends Model {
	
	public String chrom;
	public Integer start, end;
	public Integer probes;
	public Double noise;
	public Double falloff;
	
	public SimExpt[] expts;
	public SimTranscript[] plus, minus;
	
	public SimParameters() {}
	
    public SimParameters(String name) { 
    	String path = String.format(
    			"edu.mit.csail.cgs.sigma.expression.simulation.%s", name);
		ResourceBundle bundle = ResourceBundle.getBundle(path);
		
		chrom = bundle.getString("chrom");
		start = Integer.parseInt(bundle.getString("start"));
		end = Integer.parseInt(bundle.getString("end"));
		probes = Integer.parseInt(bundle.getString("probes"));
		noise = Double.parseDouble(bundle.getString("noise"));
		falloff = Double.parseDouble(bundle.getString("falloff"));
		
		String tstr = bundle.getString("transcripts");
		String[] array = tstr.split(",");
		
		ArrayList<SimTranscript> wtrans = new ArrayList<SimTranscript>();
		ArrayList<SimTranscript> ctrans = new ArrayList<SimTranscript>();
		
		for(int i = 0; i < array.length; i++) { 
			String key = array[i].trim();
			String value = bundle.getString(key);
			SimTranscript st = new SimTranscript(falloff, value);
			
			if(st.strand.equals("+")) { 
				wtrans.add(st);
			} else { 
				ctrans.add(st);
			}
		}
		
		plus = wtrans.toArray(new SimTranscript[0]);
		minus = ctrans.toArray(new SimTranscript[0]);
		
		String exptstring = bundle.getString("expts");
		String[] earray = exptstring.split(",");
		ArrayList<SimExpt> exptList = new ArrayList<SimExpt>();
		
		for(int i = 0; i < earray.length; i++) { 
			String key = earray[i].trim();
			String value = bundle.getString(key);
			SimExpt expt = new SimExpt(value);
			exptList.add(expt);
		}
		
		expts = exptList.toArray(new SimExpt[0]);
    }
    
    public static class SimExpt extends Model {
    	
    	public Double additive, scaling;
    	
    	public SimExpt() {}
    	public SimExpt(String e) { 
    		String[] array = e.split(",");
    		if(array.length != 2) { throw new IllegalArgumentException(e); }
    		additive = Double.parseDouble(array[0]);
    		scaling = Double.parseDouble(array[1]);
    	}
    }
    
    private static Pattern transpatt = Pattern.compile("(\\d+)-(\\d+):(.):(.*)");
    
    public static class SimTranscript extends Model {
    	
    	public Integer start, end;
    	public String strand; 
    	public Double intensity, slope;
    	
    	public SimTranscript() {}
    	
    	public SimTranscript(double slope, String t) { 
    		Matcher m = transpatt.matcher(t);
    		if(!m.matches()) { throw new IllegalArgumentException(t); }
    		start = Integer.parseInt(m.group(1));
    		end = Integer.parseInt(m.group(2));
    		strand = m.group(3);
    		intensity = Double.parseDouble(m.group(4));
    		this.slope = slope; 
    	}
    }
    
	public Collection<Integer> breakpoints(boolean dir) { 
		Set<Integer> breaks = new TreeSet<Integer>();
		SimTranscript[] ts = dir ? plus : minus;
		for(SimTranscript trans : ts) { 
			breaks.add(trans.start);
			breaks.add(trans.end);
		}
		return breaks;
	}
	
	public Collection<TranscriptCall> transcripts(boolean dir) { 
		ArrayList<TranscriptCall> breaks = new ArrayList<TranscriptCall>();
		String strand = dir ? "+" : "-";
		for(SimTranscript trans : (dir ? plus : minus)) { 
			breaks.add(new TranscriptCall(chrom, strand, 
					trans.start, trans.end, trans.intensity, trans.slope));
		}
		return breaks;
	}
}
