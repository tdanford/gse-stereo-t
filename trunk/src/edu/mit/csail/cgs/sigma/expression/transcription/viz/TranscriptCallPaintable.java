/*
 * Author: tdanford
 * Date: Aug 19, 2009
 */
package edu.mit.csail.cgs.sigma.expression.transcription.viz;

import java.awt.*;
import java.util.*;

import edu.mit.csail.cgs.datasets.general.Region;
import edu.mit.csail.cgs.sigma.NonOverlappingRegionKeyLayout;
import edu.mit.csail.cgs.sigma.expression.workflow.models.RegionKey;
import edu.mit.csail.cgs.sigma.expression.workflow.models.TranscriptCall;
import edu.mit.csail.cgs.utils.iterators.EmptyIterator;
import edu.mit.csail.cgs.viz.paintable.*;

public class TranscriptCallPaintable extends AbstractRegionPaintable {
	
	private PaintableScale scale;
	private Collection<TranscriptCall> callSource;
	private ArrayList<TranscriptCall> calls;
	
	public TranscriptCallPaintable() { 
		this(new EmptyIterator<TranscriptCall>());
	}
	
	public TranscriptCallPaintable(Iterator<TranscriptCall> cs) { 
		callSource = new LinkedList<TranscriptCall>();
		calls = new ArrayList<TranscriptCall>();
		scale = new PaintableScale(0.0, 1.0);
		addTranscriptCalls(cs);
	}
	
	public TranscriptCallPaintable(Collection<TranscriptCall> cs) { 
		this(cs.iterator());
	}
	
	public void addTranscriptCalls(Iterator<TranscriptCall> cs) {
		while(cs.hasNext()) { 
			callSource.add(cs.next());
		}
		update();
	}

	public void setTranscripts(Collection<TranscriptCall> ts) {
		callSource = new ArrayList<TranscriptCall>(ts);
		update();
		dispatchChangedEvent();
	}
	
	public void clearTranscriptCalls(Region region) { 
		Iterator<TranscriptCall> itr = callSource.iterator();
		while(itr.hasNext()) { 
			TranscriptCall c = itr.next();
			if(c.overlapsStrandInvariant(region)) { 
				itr.remove();
			}
		}
	}
	
	public PaintableScale getScale() { return scale; }
	
	public void setScale(PaintableScale s) { 
		scale = s;
	}

	protected void update() {
		System.out.println("** Updating transcript call painter...");
		calls.clear();
		if(region != null) { 
			RegionKey key = new RegionKey(region.getChrom(), region.getStart(), region.getEnd(), "+");
			for(TranscriptCall c : callSource) { 
				if(c.strandInvariantOverlaps(key)) { 
					calls.add(c);
				}
			}
		}
	}

	private int getPixX(int bp, int x1, int x2) {
		if(region == null) { throw new IllegalArgumentException(); }
		int rstart = region.getStart(), rend = region.getEnd();
		double f = (bp - rstart) / (double)(rend - rstart);
		return x1 + (int)Math.round(f * (double)(x2-x1));
	}
	
	private int getPixY(double val, int y1, int y2) { 
		double f = scale.fractionalOffset(val);
		return y2 - (int)Math.round(f * (double)(y2-y1));
	}

	public void paintItem(Graphics g, int x1, int y1, int x2, int y2) {
		int w = x2-x1, h = y2-y1;
		NonOverlappingRegionKeyLayout<TranscriptCall> layout = 
			new NonOverlappingRegionKeyLayout<TranscriptCall>();
		layout.setRegions(calls);
		
		ArrowPainter tagPainter = new ArrowPainter();
		tagPainter.setFillColor(Color.orange);
		
		Graphics2D g2 = (Graphics2D)g;
		Stroke oldStroke = g2.getStroke();
		
		Stroke baseStroke = new BasicStroke(2.0f);
		
		Stroke dotted = new BasicStroke(
				  1f,
			      BasicStroke.CAP_ROUND, 
			      BasicStroke.JOIN_ROUND, 
			      2f, 
			      new float[] {2f}, 
			      0f);
		
		int trackHeight = 10;
		FontMetrics fm = g2.getFontMetrics();
		
		for(TranscriptCall c : calls) { 
			int clength = c.start-c.end;
			int track = layout.getTrack(c);
			double falloff = c.falloff;
			System.out.println(String.format("Call: %s", c.toString())); 

			if(c.strand.equals("+")) { 
				falloff = -falloff;
			}

			// the extents (left right) of the arrow
			int cx1 = getPixX(c.start, x1, x2); 
			int cx2 = getPixX(c.end, x1, x2);

			// the height coordinates of the tag itself.
			int ty1 = y2-trackHeight * (track+1);
			int ty2 = ty1 + trackHeight;

			for(int i = 0; i < c.intensities.length; i++) { 

				double intensity = Math.log(c.intensities[i]);
				if(!c.strand.equals("+")) { 
					intensity += ((double)clength * falloff);
				}

				// the height of the *actual* expression
				int cy1 = getPixY(intensity, y1, y2);
				int cy2 = getPixY(intensity - falloff * (double)clength, y1, y2);
				
				g2.setStroke(baseStroke);
				g.setColor(Color.black);
				
				String lbl = String.format("%.1f", intensity);

				if(c.strand.equals("+")) { 
					tagPainter.setDirection(true);
					g2.drawLine(cx1, cy1, cx2, cy1);
					g2.setStroke(dotted);
					g2.drawLine(cx1,cy2, cx2,cy1);

					g2.setStroke(oldStroke);
					g2.drawString(lbl, cx2+1, cy1);
				} else { 
					tagPainter.setDirection(false);
					g2.drawLine(cx1, cy2, cx2, cy2);
					g2.setStroke(dotted);
					g2.drawLine(cx2,cy1,cx1,cy2);

					g2.setStroke(oldStroke);
					g2.drawString(lbl, 
							cx1-fm.charsWidth(lbl.toCharArray(), 0, lbl.length())-1, 
							cy2);

				}
				
			}
			
			g2.setStroke(baseStroke);
			tagPainter.paintItem(g, cx1, ty1, cx2, ty2);
		}
		
		g2.setStroke(oldStroke);
	}

	public Collection<TranscriptCall> getTranscripts(RegionKey key) {
		LinkedList<TranscriptCall> cs = new LinkedList<TranscriptCall>();
		for(TranscriptCall c : callSource) { 
			if(c.overlaps(key)) { 
				cs.add(c);
			}
		}
		return cs;
	}

}
