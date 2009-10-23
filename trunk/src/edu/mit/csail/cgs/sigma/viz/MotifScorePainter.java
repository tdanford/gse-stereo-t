/*
 * Author: tdanford
 * Date: May 22, 2008
 */
package edu.mit.csail.cgs.sigma.viz;

import java.util.*;
import java.awt.*;

import edu.mit.csail.cgs.viz.paintable.*;
import edu.mit.csail.cgs.datasets.general.Region;
import edu.mit.csail.cgs.datasets.motifs.*;
import edu.mit.csail.cgs.ewok.verbs.motifs.*;

public class MotifScorePainter extends AbstractPaintable {
	
	private Color color;
	private WeightMatrix matrix;
	private double minScore, maxScore, range;
	private Region region;
	private WeightMatrixScorer scorer;
	
	public MotifScorePainter(WeightMatrix m, Color c) { 
		matrix = m;
		color = c;

		if(matrix != null) { 
			minScore = matrix.getMinScore();
			maxScore = matrix.getMaxScore();
			range = maxScore - minScore;
			scorer = new WeightMatrixScorer(matrix);
		}
		region = null;
	}
	
	public void setMatrix(WeightMatrix m) { 
		matrix = m;
		minScore = matrix.getMinScore();
		maxScore = matrix.getMaxScore();
		range = maxScore - minScore;
		scorer = new WeightMatrixScorer(matrix);
		dispatchChangedEvent();
	}
	
	public void setRegion(Region r) { 
		region = r;
		dispatchChangedEvent();
	}
	
	public void setColor(Color c) { 
		color = c;
		dispatchChangedEvent();
	}
	
	public void doLayout() { 
		
	}
	
	public boolean isReadyToPaint() { 
		return true;
	}

	public void paintItem(Graphics g, int x1, int y1, int x2, int y2) {
		int w = x2 - x1, h = y2 - y1;
		
		Graphics2D g2 = (Graphics2D)g;
		if(region==null || matrix==null) { return; }
		
		g2.setColor(Color.white);
		g2.fillRect(x1, y1, w, h);
		
		g2.setColor(Color.black);
		g2.drawLine(x1, y2, x2, y2);
		g2.drawLine(x1, y1, x2, y1);
		
		double f = (-minScore) / range;
		if(minScore < 0.0 && maxScore > 0.0) { 
			g2.setColor(Color.lightGray);
			int y = y2 - (int)Math.round(f * (double)h);
			g2.drawLine(x1, y, x2, y);
		}
		
		g2.setColor(color);
		WeightMatrixScoreProfile profile = scorer.execute(region);
		
		if(w >= region.getWidth()) {
            System.out.println("MotifScorePainter: Score painting!");
			for(int i = 0; i < profile.length(); i++) { 
				f = (double)i / (double)region.getWidth();
				int x = x1 + (int)Math.floor(f * (double)w);
				f = (profile.getMaxScore(i) - minScore) / range; 
				int y = y2 - (int)Math.round(f * (double)h);
				
				g2.fillOval(x-1,y-1,2,2);
				g2.drawLine(x, y, x, y2-1);
			}
		} else {
            System.out.println("MotifScorePainter: Max painting!");
			int px = -1;
			double max = minScore;
			for(int i = 0; i < profile.length(); i++) { 
				int x = x1 + (int)Math.floor(f * (double)w);
				if(x != px && px != -1) {
					f = (max - minScore) / range;
					int y = y2 - (int)Math.round(f * (double)h);
					g2.drawLine(px, y, px, Math.min(y+2, y2));
					
					px = x;
					max = minScore;
				}
				
				max = Math.max(max, profile.getMaxScore(i));
			}
			
			if(px != -1) { 
				f = (max - minScore) / range;
				int y = y2 - (int)Math.round(f * (double)h);
				g2.drawLine(px, y, px, y+2);				
			}
		}
	}

	
}
