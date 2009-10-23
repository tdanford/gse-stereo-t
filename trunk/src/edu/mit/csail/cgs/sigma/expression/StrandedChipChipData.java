package edu.mit.csail.cgs.sigma.expression;

import java.util.*;
import edu.mit.csail.cgs.datasets.chipchip.ChipChipData;
import edu.mit.csail.cgs.datasets.chipchip.SQLData;
import edu.mit.csail.cgs.utils.NotFoundException;
import edu.mit.csail.cgs.utils.Closeable;

public class StrandedChipChipData implements ChipChipData, Closeable {
	
	private SQLData data;
	private char strand;
	
	private Vector<Integer> pos;
	private Vector<Vector<Double>> ip, wce, ratio;
	private Vector<Vector<Integer>> id;
	
	public StrandedChipChipData(SQLData d, char s) { 
		data = d;
		strand = s;
		pos = new Vector<Integer>();
		ratio = new Vector<Vector<Double>>();
		ip = new Vector<Vector<Double>>();
		wce = new Vector<Vector<Double>>();
		id = new Vector<Vector<Integer>>();
	}
	
	public boolean isClosed() { return data == null; }
	
	public void close() {
		if(data instanceof Closeable) { 
			((Closeable)data).close();
		}
		data=null;
	}
	
	private void rebuildData() { 
		pos.clear(); id.clear();
		ratio.clear();
		ip.clear(); wce.clear();
		
		for(int i = 0; i < data.getCount(); i++) {
			boolean foundStrand = false;

			Vector<Double> wces = null;
			Vector<Double> ips = null;
			Vector<Double> ratios = null;
			Vector<Integer> ids = null;

			for(int j = 0; j < data.getReplicates(i); j++) { 
				char str = data.getStrand(i, j);
				double value = data.getRatio(i, j);

				if(str == strand && !Double.isNaN(value)) {
					if(!foundStrand) { 
						wces = new Vector<Double>();
						ips = new Vector<Double>();
						ratios = new Vector<Double>();
						ids = new Vector<Integer>();
					}
					foundStrand = true;
					
					ratios.add(data.getRatio(i, j));
					ips.add(data.getIP(i, j));
					wces.add(data.getWCE(i, j));
					ids.add(data.getExptID(i, j));
				}
			}
			
			if(foundStrand) { 
				pos.add(data.getPos(i));
				ip.add(ips);
				wce.add(wces);
				ratio.add(ratios);
				id.add(ids);
			}
		}
	}

	public int getExptID(int i, int j) {
		return id.get(i).get(j);
	}

	public double getIP(int i, int j) {
		return ip.get(i).get(j);
	}

	public double getMax() {
		Double max = null;
		for(Vector<Double> a : ratio) { 
			for(int j = 0; j < a.size(); j++) { 
				if(max == null) { 
					max = a.get(j);
				} else { 
					max = Math.max(max, a.get(j));
				}
			}
		}
		return max;
	}

	public double getMin() {
		Double max = null;
		for(Vector<Double> a : ratio) { 
			for(int j = 0; j < a.size(); j++) { 
				if(max == null) { 
					max = a.get(j);
				} else { 
					max = Math.min(max, a.get(j));
				}
			}
		}
		return max;
	}

	public double getMax(String chrom, int start, int stop)
			throws NotFoundException {
		window(chrom, start, stop);
		return getMax();
	}

	public double getMin(String chrom, int start, int stop)
			throws NotFoundException {
		window(chrom, start, stop);
		return getMin();
	}

	public double getRatio(int i, int j) {
		return ratio.get(i).get(j);
	}

	public char getStrand(int i, int j) {
		return strand;
	}

	public double getVar(int i, int j) {
        return 0.0;
	}

	public double getWCE(int i, int j) {
		return wce.get(i).get(j);
	}

	public void window(String chrom, int start, int stop, double minratio)
			throws NotFoundException {
		data.window(chrom, start, stop, minratio);
		rebuildData();
	}

	public void window(String chrom, int start, int stop)
	throws NotFoundException {
		data.window(chrom, start, stop);
		rebuildData();
	}

	public int baseToIndex(int b) {
		int upper = pos.size()-1;
		int lower = 0;
		
		int plower = pos.get(lower);
		int pupper = pos.get(upper);
		if(plower >= b) { return lower; }
		if(pupper <= b) { return upper; }

		while(upper-lower > 1) {
			int middle = (upper+lower)/2;
			int pmiddle = pos.get(middle);
			if(pmiddle == b) { return middle; }
			
			if(pmiddle > b) { 
				upper = middle;
				pupper = pmiddle;
			} else { 
				lower = middle;
				plower = pmiddle;
			}
		}

		int lowerDist = b-plower;
		int upperDist = pupper-b;
		if(lowerDist <= upperDist) { 
			return lower;
		} else { 
			return upper;
		}
	}

	public int getCount() {
		return ratio.size();
	}

	public String getName() {
		return data.getName();
	}

	public int getPos(int i) {
		return pos.get(i);
	}

	public int getReplicates(int i) {
		return ratio.get(i).size();
	}

	public double getValue(int i, int j) {
		return ratio.get(i).get(j);
	}

	public String getVersion() {
		return data.getVersion();
	}

	public String getWindowChrom() {
		return data.getWindowChrom();
	}

	public int getWindowStart() {
		return data.getWindowStart();
	}

	public int getWindowStop() {
		return data.getWindowStop();
	}

}
