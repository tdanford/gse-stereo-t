/*
 * Author: tdanford
 * Date: May 7, 2009
 */
package edu.mit.csail.cgs.sigma.expression.rma;

import edu.mit.csail.cgs.sigma.expression.segmentation.InputData;
import edu.mit.csail.cgs.sigma.expression.workflow.models.DataSegment;
import edu.mit.csail.cgs.utils.models.Model;

public class HData extends Model {

	public String chrom, strand;
	public Double[][] y;
	public Integer[] x;
	
	private DataSegment dataSegment;
	
	public HData() {}
	
	public HData(String c, String s, Integer[] x, Double[][] y) {
		chrom = c; 
		strand = s;
		this.x = x.clone();
		this.y = y.clone();
		dataSegment = null;
	}
	
	public HData(InputData d) { 
		chrom = d.chrom();
		strand = d.strand();
		x = d.locations();
		y = d.values();
		dataSegment = null;
	}
	
	public HData(DataSegment ds) { 
		chrom = ds.chrom;
		strand = ds.strand;
		x = ds.dataLocations;
		y = ds.dataValues;
		dataSegment = ds;
	}
	
	public DataSegment getDataSegment() { return dataSegment; }

	public void saveDifferentials(Double[] meanLevels, Double meanSlope, Double[] meanAlphas, Double[] diffs) {
		if(dataSegment != null) {
			
			if(meanLevels.length != dataSegment.dataValues.length) { 
				throw new IllegalArgumentException(String.format("%d != %d", 
						meanLevels.length, dataSegment.dataValues.length)); 
			}
			
			/*
			for(int i = 0; i < dataSegment.segmentParameters.length; i++) { 
				dataSegment.segmentParameters[i][0] = meanLevels[i];
				dataSegment.segmentParameters[i][1] = meanSlope;				
			}
			*/
			
			if(meanAlphas.length != dataSegment.dataLocations.length) { 
				throw new IllegalArgumentException(String.format("%d != %d", 
						meanAlphas.length, dataSegment.dataLocations.length)); 
			}			
			
			dataSegment.probeAlphas = new Double[dataSegment.dataLocations.length];
			
			for(int i = 0; i < meanAlphas.length; i++) { 
				dataSegment.probeAlphas[i] = meanAlphas[i];
			}
			
			dataSegment.differential = diffs;
		}
	}
}
