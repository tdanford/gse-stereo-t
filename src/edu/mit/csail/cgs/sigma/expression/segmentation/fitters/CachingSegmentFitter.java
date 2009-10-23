/*
 * Author: tdanford
 * Date: May 1, 2009
 */
package edu.mit.csail.cgs.sigma.expression.segmentation.fitters;

import java.util.*;
import java.io.*;
import edu.mit.csail.cgs.sigma.expression.segmentation.InputData;

public class CachingSegmentFitter implements SegmentFitter {

	private SegmentFitter fitter;
	
	private int N;
	private Double[] cachedFits, cachedScores;
	
	public CachingSegmentFitter(SegmentFitter f) { 
		
	}
	
	private int cacheOffset(int j1, int j2) { 
		int w = j2-j1;
		int idx = 0;
		
		return idx;
	}
	
	public void loadFits(File f, int minWidth) throws IOException { 
		DataInputStream dis = new DataInputStream(new FileInputStream(f));
		
		N = dis.readInt();
		
		int Nw = N-minWidth;
		int Nw2 = Nw/2;
		int size = numParams() * Nw * Nw2;
		
		cachedFits = new Double[size];
		cachedScores = new Double[size/numParams()];
		
		int i = 0, j = 0;
		
		for(int w = minWidth; w < N; w++) {
			for(int j1 = 0; j1 < N - w; j1++) { 
				int j2 = j1 + w; 
				for(int k = 0; k < numParams(); k++) { 
					cachedFits[i++] = dis.readDouble();
				}
				cachedScores[j++] = dis.readDouble();
			}
		}
		
		dis.close();
	}
	
	public void cacheFits(File f, int minWidth, InputData data, Integer[] channels) throws IOException {
		DataOutputStream dos = new DataOutputStream(new FileOutputStream(f));
		N = data.length();
		dos.writeInt(N);
		for(int w = minWidth; w < N; w++) { 
			for(int j1 = 0; j1 < N-w; j1++) {
				int j2 = j1 + w;
				Double[] params = fitter.fit(j1, j2, data, channels);
				for(int k = 0; k < params.length; k++) { 
					dos.writeDouble(params[k]);
				}
				dos.writeDouble(fitter.score(j1, j2, params, data, channels));
			}
		}
		dos.close();
	}

	public Double[] fit(int j1, int j2, InputData data, Integer[] channels) {
		if(cachedFits == null) { 
			return fitter.fit(j1, j2, data, channels);
		} else { 
			int idx = cacheOffset(j1, j2);
			Double[] p = new Double[numParams()];
			for(int k = 0; k < p.length; k++) { 
				p[k] = cachedFits[idx+k];
			}
			return p;
		}
	}

	public int numParams() {
		return fitter.numParams();
	}

	public Double score(int j1, int j2, Double[] params, InputData data, Integer[] channels) {
		return fitter.score(j1, j2, params, data, channels);
	}

	public int type() {
		return fitter.type();
	}
}
