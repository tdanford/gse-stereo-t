/*
 * Author: tdanford
 * Date: Dec 16, 2008
 */
package edu.mit.csail.cgs.sigma.expression.workflow;

import java.io.*;
import java.util.*;

import edu.mit.csail.cgs.sigma.expression.segmentation.InputData;
import edu.mit.csail.cgs.sigma.expression.segmentation.RegressionInputData;
import edu.mit.csail.cgs.sigma.expression.segmentation.Segment;
import edu.mit.csail.cgs.sigma.expression.segmentation.SegmentationParameters;
import edu.mit.csail.cgs.sigma.expression.segmentation.Segmenter;
import edu.mit.csail.cgs.sigma.expression.segmentation.dpalgos.MultiChannelSegmenter;
import edu.mit.csail.cgs.sigma.expression.segmentation.fitters.FlatFitter;
import edu.mit.csail.cgs.sigma.expression.segmentation.fitters.LineFitter;
import edu.mit.csail.cgs.sigma.expression.workflow.models.DataSegment;
import edu.mit.csail.cgs.sigma.expression.workflow.models.InputSegmentation;
import edu.mit.csail.cgs.sigma.expression.workflow.models.RegionKey;
import edu.mit.csail.cgs.utils.Pair;
import edu.mit.csail.cgs.utils.models.ModelInput;
import edu.mit.csail.cgs.utils.models.ModelInputIterator;

public class WorkflowDataSegmentReader extends ModelInputIterator<DataSegment> {
	
	public static void main(String[] args) { 
		String key = args.length > 0 ? args[0] : "s288c";
		WorkflowProperties props = new WorkflowProperties();
		File wf = new File(props.getDirectory(), String.format("%s_plus.datasegs", key));
		File cf = new File(props.getDirectory(), String.format("%s_negative.datasegs", key));
		
		try {
			TreeSet<RegionKey> totalTrans = new TreeSet<RegionKey>();
			TreeSet<RegionKey> totalDiff = new TreeSet<RegionKey>();
			
			int size = 0, count = 0, transcribed = 0, diff = 0;
			WorkflowDataSegmentReader reader = new WorkflowDataSegmentReader(wf);
			while(reader.hasNext()) { 
				DataSegment s = reader.next();
				size += s.width();
				count += 1;
				if(s.hasConsistentType(Segment.LINE)) {  
					transcribed += s.width();
					RegionKey rs = new RegionKey(s);
					totalTrans.add(rs);
					if(s.isDifferential(null)) { 
						diff += s.width();
						totalDiff.add(rs);
					}
				}
			}
			System.out.println(String.format("Watson Strand: %d segs, %d bases, %d transcribed, %d diff", count, size, transcribed, diff));
			
			count = size = transcribed = diff = 0;
			reader = new WorkflowDataSegmentReader(cf);
			while(reader.hasNext()) { 
				DataSegment s = reader.next();
				size += s.width();
				count += 1;
				if(s.hasConsistentType(Segment.LINE)) {  
					transcribed += s.width();
					RegionKey rs = new RegionKey(s);
					totalTrans.add(rs);
					if(s.isDifferential(null)) { 
						diff += s.width();
						totalDiff.add(rs);
					}
				}
			}
			System.out.println(String.format("Crick Strand: %d segs, %d bases, %d transcribed, %d diff", count, size, transcribed, diff));
			
			System.out.println(String.format("Total Transcribed: %d bp", totalSize(collapse(totalTrans))));
			System.out.println(String.format("Total Diff: %d bp", totalSize(collapse(totalDiff))));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static int totalSize(Collection<RegionKey> ks) { 
		int s = 0;
		for(RegionKey k : ks) { 
			s += k.width();
		}
		return s;
	}
	
	public static Collection<RegionKey> collapse(TreeSet<RegionKey> keys) { 
		ArrayList<RegionKey> rs = new ArrayList<RegionKey>();
		Iterator<RegionKey> ks = keys.iterator();
		RegionKey c = null;
		while(ks.hasNext()) { 
			RegionKey k = ks.next();
			if(c == null) { 
				c = k; 
			} else { 
				if(c.strandInvariantOverlaps(k)) { 
					c = new RegionKey(c.chrom, Math.min(c.start, k.start), Math.max(c.end, k.end), c.strand);
				} else {
					rs.add(c);
					c = k;
				}
			}
		}
		if(c != null) { rs.add(c); }
		return rs;
	}
	
	public WorkflowDataSegmentReader() { 
		this(System.in);
	}
	
	public WorkflowDataSegmentReader(File f) throws IOException { 
		this(new FileInputStream(f));
	}

	public WorkflowDataSegmentReader(InputStream is) {
		super(new ModelInput.LineReader<DataSegment>(DataSegment.class, is));
	}
}
