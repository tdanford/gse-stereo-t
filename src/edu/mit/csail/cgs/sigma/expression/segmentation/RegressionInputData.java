/*
 * Author: tdanford
 * Date: Nov 21, 2008
 */
package edu.mit.csail.cgs.sigma.expression.segmentation;

import java.util.*;
import java.io.PrintStream;

import Jama.Matrix;

import edu.mit.csail.cgs.utils.*;
import edu.mit.csail.cgs.utils.models.Model;
import edu.mit.csail.cgs.utils.models.data.DataFrame;
import edu.mit.csail.cgs.utils.models.data.DataRegression;

public class RegressionInputData implements InputData {

	private String chrom, strand;
	private Integer[] locations;
	private Double[][] values;
	private Set<String> flags;
	
	public DataFrame<RPModel> frame;
	public DataRegression<RPModel> regression;
	public Matrix X, y;
	public Vector<PackedBitVector> channelBits;
	
	public RegressionInputData(String c, String str, Integer[] locs, Double[][] vals) { 
		chrom = c;
		strand = str;
		locations = locs;
		values = vals;
		channelBits = new Vector<PackedBitVector>();
		flags = new TreeSet<String>();
		
		ArrayList<RPModel> models = new ArrayList<RPModel>();
		for(int i = 0; i < locations.length; i++) { 
			for(int k = 0; k < values.length; k++) { 
				models.add(new RPModel(i, locations[i], k, values[k][i]));
			}
		}
		frame = new DataFrame<RPModel>(RPModel.class, models);
		
		for(int k = 0; k < channels(); k++) { 
			channelBits.add(frame.getMask(new RPModelChannelPredicate(k)));
		}
		
		regression = new DataRegression<RPModel>(frame, "value ~ offset");
		X = regression.getPredictorMatrix();
		y = regression.getPredictedVector();
	}
	
	public PackedBitVector createRegionSelector(int j1, int j2) { 
		PackedBitVector v = frame.getMask(new RPModelIndexPredicate(j1, j2));
		return v;
	}
	
	public String chrom() { return chrom; }
	public String strand() { return strand; }
	
	public void print(PrintStream ps) { 
		ps.println(String.format("%s\t%s\t%d", chrom, strand, values.length));
		for(int i = 0; i < locations.length; i++) { 
			ps.print(i > 0 ? " " : "");
			ps.print(String.format("%d", locations[i]));
		}
		ps.println();
		for(Double[] varray : values) { 
			for(int i = 0; i < locations.length; i++) { 
				ps.print(i > 0 ? " " : "");
				ps.print(String.format("%.2f", varray[i]));
			}
			ps.println();			
		}
	}
	
	public int length() { 
		return locations.length;
	}
	
	public int channels() { 
		return values.length;
	}
	
	/**
	 * 
	 * @return The genomic coordinates of the data
	 */
	public Integer[] locations() {
		return locations;
	}

	/**
	 * 
	 * @return 	The probe values of these data<br>
	 * Each row may represent a different channel
	
	 */
	public Double[][] values() {
		return values;
	} 
	
	public Set<String> flags() { return flags; }
}