/*
 * Author: tdanford
 */
package edu.mit.csail.cgs.sigma.expression.segmentation.dpalgos;

import java.util.*;
import java.lang.reflect.*;
import java.io.*;

import edu.mit.csail.cgs.sigma.expression.segmentation.InputData;
import edu.mit.csail.cgs.sigma.expression.segmentation.Segment;
import edu.mit.csail.cgs.sigma.expression.segmentation.SegmentationParameters;
import edu.mit.csail.cgs.sigma.expression.segmentation.Segmenter;
import edu.mit.csail.cgs.sigma.expression.segmentation.fitters.SegmentFitter;
import edu.mit.csail.cgs.sigma.expression.segmentation.fitters.SteinmetzFitter;
import edu.mit.csail.cgs.sigma.expression.segmentation.sharing.ParameterSharing;
import edu.mit.csail.cgs.sigma.expression.workflow.*;
import edu.mit.csail.cgs.sigma.expression.workflow.models.DataSegment;
import edu.mit.csail.cgs.sigma.expression.workflow.models.FileInputData;
import edu.mit.csail.cgs.sigma.litdata.steinmetz.*;
import edu.mit.csail.cgs.utils.probability.NormalDistribution;

/**
 * Re-implementation of the Steinmetz segmentation based on 
 * the published Picard recursive algorithms. 
 * 
 * @author tdanford
 */
public class RightFirstSteinmetzSegmenter implements Segmenter {
	
	public static void main(String[] args) { 
		SteinmetzProperties props = new SteinmetzProperties();
		WorkflowProperties wprops = new WorkflowProperties();
		
		SteinmetzPackager packager = new SteinmetzPackager(props, 500);
		
		SegmentationParameters segProps = wprops.getDefaultSegmentationParameters();
		RightFirstSteinmetzSegmenter segmenter = new RightFirstSteinmetzSegmenter(segProps);
		
		WorkflowSegmentation wseg = new WorkflowSegmentation(
				3, null, segmenter, packager.data()
				);
		
		WorkflowDataSegmenter wdseg = new WorkflowDataSegmenter(
				wprops, "s288c", wseg
				);
		
		File output = new File(wprops.getDirectory(), "steinmetz.datasegs");
		try { 
			PrintStream ps = new PrintStream(new FileOutputStream(output));
			int count = 0;
			while(wdseg.hasNext()) { 
				DataSegment dseg = wdseg.next();
				ps.println(dseg.asJSON().toString());
				System.out.println(String.format("Segmented: %s:%d-%d:%s", dseg.chrom, dseg.start, dseg.end, dseg.strand));
				count += 1;
			}
			ps.close();
			System.out.println(String.format("Output: %d segments", count));
		} catch(IOException e) { 
			e.printStackTrace();
		}
	}

	private int kMax;
	private double s;
	
	public RightFirstSteinmetzSegmenter(SegmentationParameters p) {
		kMax = 100;
		s = -0.5;
	}

	public Collection<Segment> segment(InputData data) {
		
		System.out.println(String.format("Picard Segmentation: %s:%d-%d:%s", 
				data.chrom(), 
				data.locations()[0], data.locations()[data.length()-1],
				data.strand()));
		
		int n = data.length();

		MeanTable means = new MeanTable(data);
		VarTable vars = new VarTable(data, means);
		
		// k=1 is calculated and kept separately. 
		BaseTable k1 = new BaseTable(data, means, vars);

		// We don't need these anymore... and they are *big* in memory.
		means = null;
		vars = null;
		
		LinkedList<KTable> ktables = new LinkedList<KTable>();
		
		// k=1 and k=2 are added manually, since they use different 
		// constructors than the "default" case.  
		ktables.add(new KTable(k1));
		ktables.add(new KTable(n, k1));    
		
		for(int k = 3; k <= kMax; k++) { // k=3+ are added in a loop. 
			KTable kt = new KTable(k, n, ktables.getLast(), k1);
			ktables.addLast(kt);
		}
		
		KTable[] tables = ktables.toArray(new KTable[0]);
		int bestK = 1;
		double dkthresh = s * (double)n;

		for(int k = 0; k < tables.length; k++) { 
			float ll = tables[k].getJ(n-1);
			System.out.println(String.format("LL %d: %.5f", k, ll));
		}
		
		for(int k = 2; k < tables.length; k++) {
			int i1 = k-2, i2 = k-1, i3 = k;
			double ll1 = tables[i1].getJ(n-1);
			double ll2 = tables[i2].getJ(n-1);
			double ll3 = tables[i3].getJ(n-1);
			double dk = - ( ll1 + ll3 - 2.0 * ll2 );
			if(dk <= dkthresh) {  
				bestK = k; 
				System.out.println(String.format(
						"dk %f < %f, bestK = %d", 
						dk, dkthresh, k));
			} else { 
				System.out.println(String.format(
						"k=%d, dk %f > %f",
						k, dk, dkthresh));
			}
		}
		System.out.println(String.format("bestK = %d", bestK));

		// Finally, we decode the segments by walking backwards
		// from k=KMax down to k=1
		LinkedList<Segment> segs = new LinkedList<Segment>();
		
		for(int k = bestK, h = n-1; k >= 1; k--) { 
			KTable ktable = tables[k-1];
			Integer nextH = ktable.getChoice(h);
			float mean = calculateMean(data, nextH+1, h);
			float var = calculateVar(data, nextH+1, h, mean);
			Double[] params = new Double[] { (double)mean, (double)var };
			Segment seg = new Segment(0, true, Segment.FLAT, nextH+1, h, params);
			segs.addFirst(seg);
			h = nextH;
		}

		// Am I missing a segment here at the end?  
		
		return segs;
	}

	private static float calculateMean(InputData data, int i, int j) { 
		float sum = 0.0f;
		Double[] values = data.values()[0];
		for(int h = i; h <= j; h++) { 
			float value = values[h].floatValue();
			sum += value;
		}
		return sum / (float)(j-i+1);
	}

	private static float calculateVar(InputData data, int i, int j, float mean) { 
		float sum = 0.0f;
		Double[] values = data.values()[0];
		for(int h = i; h <= j; h++) { 
			float value = values[h].floatValue();
			float diff = (value-mean);
			sum += (diff*diff);
		}
		return sum / (float)(j-i+1);
	}
}

/**
 * DPTable<N> represents an upper-triangular matrix of values 
 * that are required for the dynamic programming, where elements of 
 * the matrix have type N.  
 * 
 * @author tdanford
 *
 * @param <N>
 */
class DPTable { 

	private int n, k; 
	private float[][] table;
	
	public DPTable(int k, int n, float dft) { 
		this.n = n;
		this.k = k;
		
		System.out.println(String.format("Building DPTable (n=%d)", n));
		
		table = new float[n][];
		for(int i = 0; i < n; i++) {
			table[i] = new float[n-i];
			for(int j = i; j < n; j++) { 
				table[i][j-i] = dft;
			}
		}
	}
	
	public int k() { return k; }
	public int n() { return n; }
	
	/**
	 * Returns the value in the table for region [i, j]
	 * (That is, our indexing is inclusive.)  
	 * 
	 * @param i 
	 * @param j
	 * @return
	 */
	public float get(int i, int j) { 
		if(j < i) { throw new IllegalArgumentException(String.format("i,j = %d,%d", i, j)); }
		return table[i][j-i]; 
	}
	
	public void set(int i, int j, float v) {  
		if(j < i) { throw new IllegalArgumentException(String.format("i,j = %d,%d", i, j)); }
		table[i][j-i] = v;
	}
}

class KTable { 

	private int k, n;
	private int[] choices;
	private float[] values;
	
	/**
	 * k = 1;
	 * @param k1
	 */
	public KTable(DPTable k1) { 
		this.k = 1; 
		this.n = k1.n();

		System.out.println(String.format("KTable, k=%d", k));

		choices = new int[n];
		values = new float[n];

		for(int i = 0; i < n; i++) { 
			choices[i] = -1;
			values[i] = k1.get(0, i);
		}
	}
	
	/**
	 * This constructor is for k=2
	 * @param n
	 * @param k1
	 */
	public KTable(int n, DPTable k1) { 
		this.k = 2; 
		this.n = n;
		
		choices = new int[n];
		values = new float[n];

		System.out.println(String.format("KTable, k=%d", k));

		// There's an implicit "i = 0" in here.  
		for(int j = 0; j < n; j++) {

			float minValue = Float.MAX_VALUE;
			int minH = -1;

			for(int h = 0; h < j; h++) {
				// The h < j requirement ensures that [0, h] and [h+1, j]
				// are disjoint subsets...
				float value = k1.get(0, h) + k1.get(h+1, j);
				if(value < minValue) { 
					minH = h; 
					minValue = value;
				}
			}
			
			choices[j] = minH;
			values[j] = minValue;
		}
	}

	/**
	 * This constructor is for k=3+
	 * 
	 * @param n
	 * @param k
	 * @param lastk
	 * @param k1
	 */
	public KTable(int k, int n, KTable lastk, DPTable k1) { 
		this.k = k; this.n = n;
		
		choices = new int[n];
		values = new float[n];
		
		System.out.println(String.format("KTable, k=%d", k));

		for(int j = 0; j < n; j++) {

			float minValue = Float.MAX_VALUE;
			int minH = -1;

			for(int h = 0; h < j; h++) { 
				float value = lastk.getJ(h) + k1.get(h+1, j);
				if(value < minValue) { 
					minH = h; 
					minValue = value;
				}
			}
			
			choices[j] = minH;
			values[j] = minValue;
		}
	}
	
	public float getJ(int j) { return values[j]; }
	public int n() { return n; }
	public int k() { return k; }
	public int getChoice(int j) { return choices[j]; }

}

/**
 * The k = 1 table, which contains the fits for all [i, j] pairs.  
 * Right now, this only fits the mean -- there also needs to be a variance 
 * table as well.  
 * 
 * @author tdanford
 *
 */
class BaseTable extends DPTable { 
	
	public BaseTable(InputData data, DPTable means, DPTable vars) { 
		super(1, means.n(), 0.0f);
		
		int n = means.n();
		Double[] values = data.values()[0];
		
		System.out.println(String.format("Building base table (n=%d)", n));
		
		for(int i = 0; i < n; i++) { 
			for(int j = i; j < n; j++) {
				float score = 0.0f;
				float mean = means.get(i, j);
				float var = vars.get(i, j);
				
				for(int h = i; h <= j; h++) { 
					float value = values[h].floatValue();
					score += J(value, mean, var);
				}
				
				if(i == j) { score = Float.MAX_VALUE; }
				set(i, j, score);
			}
		}
	}
	
	public static double log2Pi = Math.log(2.0 * Math.PI);
	
	public static double J(double x, double m, double v) { 
		double d = x-m;
		double expt = (d*d) / (v);
		double c = log2Pi + Math.log(v);
		return c + expt;
	}
}

class MeanTable extends DPTable {
	
	public MeanTable(InputData data) { 
		super(1, data.length(), 0.0f);
		
		Double[] values = data.values()[0];
		int n = n();
		
		System.out.println(String.format("Building mean table (%d)", n));
		
		float[] leftrun = new float[n];
		float[] rightrun = new float[n];
		float total = 0.0f;
		
		for(int i = 0; i < n; i++) { 
			float vi = values[i].floatValue();
			float vni = values[n-i-1].floatValue();
			leftrun[i] = i > 0 ? leftrun[i-1] + vi : vi;
			rightrun[i] = i > 0 ? rightrun[n-i] + vni : vni;
			total += vi;
		}
		
		for(int i = 0; i < n; i++) { 
			for(int j = i; j < n; j++) {
				float left = i > 0 ? leftrun[i-1] : 0.0f;
				float right = j < n-1 ? rightrun[n-j-1] : 0.0f;
				float middle = total - left - right;
				float mean = middle / (float)(j-i+1);
				
				set(i, j, mean);
			}
		}
	}
}

class VarTable extends DPTable {
	
	public VarTable(InputData data, MeanTable base) { 
		super(1, base.n(), 0.0f);
	
		Double[] values = data.values()[0];
		int n = data.length();

		System.out.println(String.format("Building var table (%d)", n));		

		for(int i = 0; i < n; i++) { 
			for(int j = i; j < n; j++) {
				float sum = 0.0f;
				float mean = base.get(i, j);
	
				for(int h = i; h <= j; h++) { 
					float value = values[h].floatValue();
					float diff = value - mean;
					sum += (diff*diff);
				}
				
				sum /= (float)(j-i+1);
				set(i, j, sum);
			}
		}
	}
}
