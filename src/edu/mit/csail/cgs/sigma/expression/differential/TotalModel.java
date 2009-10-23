/*
 * Author: tdanford
 * Date: Mar 19, 2009
 */
package edu.mit.csail.cgs.sigma.expression.differential;

import java.util.ArrayList;

import edu.mit.csail.cgs.sigma.expression.workflow.models.DataSegment;
import edu.mit.csail.cgs.utils.models.Model;
import edu.mit.csail.cgs.utils.models.ModelFieldAnalysis;

public class TotalModel extends Model { 
	
	public ArrayList<ChannelModel> channels; 
	public ArrayList<SliceModel> slices;
	
	public Double g, s;
	public Double msigma, nsigma, esigma;
	
	public Double sigmag, sigmas;  // are these estimated?  Maybe not.

	public static ModelFieldAnalysis<TotalModel> analysis = 
		new ModelFieldAnalysis<TotalModel>(TotalModel.class);

	public TotalModel(DataSegment[] segs, int numchan) {
		channels = new ArrayList<ChannelModel>();
		slices = new ArrayList<SliceModel>();
		
		for(int i = 0; i < numchan; i++) { 
			new ChannelModel(this, i);
		}
		
		for(int i = 0; i < segs.length; i++) { 
			new SliceModel(this);
		}
		
		g = s = 1.0;
		msigma = nsigma = esigma = 1.0;
		
		for(int i = 0; i < segs.length; i++) { 
			DataSegment seg = segs[i];
			
		}
	}
	
	public Double estimateSigmag() { 
		return 0.0;
	}
}