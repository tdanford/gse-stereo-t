package edu.mit.csail.cgs.sigma.tgraphs;

import java.util.*;
import java.io.*;

import edu.mit.csail.cgs.utils.SetTools;
import edu.mit.csail.cgs.utils.models.*;
import edu.mit.csail.cgs.utils.graphs.*;
import edu.mit.csail.cgs.utils.iterators.SerialIterator;

import edu.mit.csail.cgs.ewok.verbs.*;
import edu.mit.csail.cgs.datasets.general.Region;
import edu.mit.csail.cgs.datasets.general.StrandedRegion;
import edu.mit.csail.cgs.datasets.species.*;
import edu.mit.csail.cgs.sigma.OverlappingRegionFinder;
import edu.mit.csail.cgs.sigma.expression.segmentation.Segment;
import edu.mit.csail.cgs.sigma.expression.transcription.viz.AbstractRegionPaintable;
import edu.mit.csail.cgs.sigma.expression.workflow.*;
import edu.mit.csail.cgs.sigma.expression.workflow.models.*;

import java.awt.*;

import edu.mit.csail.cgs.viz.paintable.*;
import javax.swing.*;

public class SigmaGraphViz extends AbstractPaintable {
	
	private static SetTools<String> sets = new SetTools<String>(); 
	
	private SigmaGraph graph;
	private RegionKey region;

	public SigmaGraphViz(SigmaGraph g, RegionKey r) {
		graph = g;
		region = r;
	}
	
	
	public void paintItem(Graphics g, int x1, int y1, int x2, int y2) {
		/*
		int radius = 8;
		int diam = radius * 2;
		
		RegionKey query = region;
		
		int w = x2-x1, h = y2-y1;
		Map<String,Point> geneLayout = new TreeMap<String,Point>();
		Map<String,Point> segLayout = new TreeMap<String,Point>();
		
		int h5 = h/4;
		int wsegY = y1 + h5;
		int csegY = y1 + h5*2; 
		int wgeneY = y1 + h5*3;
		int cgeneY = wgeneY;
		
		int min = query == null ? -1 : query.start; 
		int max = query == null ? -1 : query.end;
		
		Iterator<GeneKey> genes = graph.findGenes(query);
		while(genes.hasNext()) { 
			GeneKey r = genes.next();
			int middle = (r.start + r.end) / 2;
			if(min == -1) { 
				min = middle - 1; max = middle + 1; 
			} else if(query == null) { 
				min = Math.min(middle-1, min);
				max = Math.max(middle+1, max);
			}
			geneLayout.put(geneID, new Point(middle, r.getStrand() == '+' ? wgeneY : cgeneY));
		}

		for(String segID : graph.segmentNodes(query)) {  
			StrandedRegion r = graph.region(segID);
			int middle = (r.getStart() + r.getEnd()) / 2;
			if(min == -1) { 
				min = middle - 1; max = middle + 1; 
			} else if(query == null) { 
				min = Math.min(middle-1, min);
				max = Math.max(middle+1, max);
			}
			segLayout.put(segID, new Point(middle, r.getStrand() == '+' ? wsegY : csegY));
		}
		
		for(String id : geneLayout.keySet()) { 
			Point p = geneLayout.get(id);
			int x = p.x;
			double f = (double)(x - min) / (double)(max-min);
			int nx = x1 + (int)Math.round(f * (double)w);
			p.x = nx;
		}
		for(String id : segLayout.keySet()) { 
			Point p = segLayout.get(id);
			int x = p.x;
			double f = (double)(x - min) / (double)(max-min);
			int nx = x1 + (int)Math.round(f * (double)w);
			p.x = nx;
		}
		
		Graphics2D g2 = (Graphics2D)g;
		Stroke oldStroke = g2.getStroke();
		g2.setStroke(new BasicStroke((float)2.0));
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		FontMetrics fm = g2.getFontMetrics();
		int textHeight = fm.getAscent() + fm.getDescent();
		
		for(String id : segLayout.keySet()) {
			Point p1 = segLayout.get(id);

			g.setColor(Color.red);
			Set<String> overlapping = graph.overlapping(id);
			for(String id2 : sets.intersection(overlapping, segLayout.keySet())) {  
				Point p2 = segLayout.get(id2);
				g.drawLine(p1.x, p1.y, p2.x, p2.y);
			}

			g.setColor(Color.blue);
			Set<String> convergent = graph.convergent(id);
			for(String id2 : sets.intersection(convergent, segLayout.keySet())) {  
				Point p2 = segLayout.get(id2);
				g.drawLine(p1.x, p1.y, p2.x, p2.y);
			}
			
			g.setColor(Color.green);
			Set<String> divergent = graph.divergent(id);
			for(String id2 : sets.intersection(divergent, segLayout.keySet())) { 
				Point p2 = segLayout.get(id2);
				g.drawLine(p1.x, p1.y, p2.x, p2.y);
			}

			g.setColor(Color.gray);
			Set<String> tandem = graph.tandem(id);
			for(String id2 : sets.intersection(tandem, segLayout.keySet())) { 
				Point p2 = segLayout.get(id2);
				g.drawLine(p1.x, p1.y, p2.x, p2.y);
			}

			g.setColor(Color.lightGray);
			Set<String> sense = graph.sense(id);
			for(String id2 : sets.intersection(sense, geneLayout.keySet())) { 
				Point p2 = geneLayout.get(id2);
				g.drawLine(p1.x, p1.y, p2.x, p2.y);
			}
			
			g.setColor(Color.orange);
			Set<String> antisense = graph.antisense(id);
			for(String id2 : sets.intersection(antisense, geneLayout.keySet())) { 
				Point p2 = geneLayout.get(id2);
				g.drawLine(p1.x, p1.y, p2.x, p2.y);
			}
		}
		
		g.setColor(Color.black);
		
		for(String id : segLayout.keySet()) { 
			Point p = segLayout.get(id);
			
			int diff = graph.differential(id);
			if(diff != 0) { 
				g.setColor(diff == 1 ? Color.red : Color.green);
				g.fillRect(p.x- radius, p.y-radius, diam, diam);
			}
			
			g.setColor(Color.black);
			g.drawRect(p.x- radius, p.y-radius, diam, diam);
			
			if(graph.isUnique(id)) { 
				g.drawLine(p.x-radius, p.y-radius, p.x+radius, p.y+radius);
				g.drawLine(p.x-radius, p.y+radius, p.x+radius, p.y-radius);
			}
		}
		for(String id : geneLayout.keySet()) { 
			Point p = geneLayout.get(id);
			g.drawOval(p.x- radius, p.y-radius, diam, diam);
			int textWidth = fm.charsWidth(id.toCharArray(), 0, id.length());
			g.drawString(id, p.x - textWidth/2, p.y + radius + fm.getAscent() + 2);
		}
		
		g2.setStroke(oldStroke);
		*/
	}

	protected void update() {
	}
}
