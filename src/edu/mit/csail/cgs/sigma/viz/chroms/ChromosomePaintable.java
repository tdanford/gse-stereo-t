/*
 * Author: tdanford
 * Date: May 19, 2008
 */
package edu.mit.csail.cgs.sigma.viz.chroms;

import java.util.*;
import java.awt.*;

import edu.mit.csail.cgs.datasets.general.*;
import edu.mit.csail.cgs.datasets.species.*;
import edu.mit.csail.cgs.ewok.verbs.Expander;
import edu.mit.csail.cgs.ewok.verbs.RefGeneGenerator;
import edu.mit.csail.cgs.sigma.*;
import edu.mit.csail.cgs.sigma.genes.*;
import edu.mit.csail.cgs.sigma.viz.chroms.information.DiffExpressionChromScorer;
import edu.mit.csail.cgs.sigma.viz.chroms.information.ExpressionChromScorer;
import edu.mit.csail.cgs.sigma.viz.chroms.information.GeneChromPainter;
import edu.mit.csail.cgs.sigma.viz.chroms.renderers.DefaultPainterRenderer;
import edu.mit.csail.cgs.sigma.viz.chroms.renderers.OneColorScoreRenderer;
import edu.mit.csail.cgs.sigma.viz.chroms.renderers.PlusMinusScoreRenderer;
import edu.mit.csail.cgs.viz.NonOverlappingIntervalLayout;
import edu.mit.csail.cgs.viz.colors.Coloring;
import edu.mit.csail.cgs.viz.paintable.*;
import edu.mit.csail.cgs.sigma.expression.BaseExpressionProperties;
import edu.mit.csail.cgs.sigma.expression.SudeepExpressionProperties;
import edu.mit.csail.cgs.sigma.expression.ewok.ExpressionProbeGenerator;
import edu.mit.csail.cgs.sigma.expression.ewok.StandardProbeGenerator;
import edu.mit.csail.cgs.sigma.expression.ewok.StandardTwoChannelProbeGenerator;
import edu.mit.csail.cgs.sigma.expression.simulation.*;

/**
 * @author tdanford
 *
 */
public class ChromosomePaintable extends AbstractPaintable {
	
	public static void main(String[] args) { 
		//test(args);
		real(args);
	}
	
	public static void real(String[] args) { 
		
		BaseExpressionProperties props = new SudeepExpressionProperties();
		String expt = "original_sigma_diploid";
		String strain = props.parseStrainFromExptKey(expt);
		Genome g = props.getGenome(strain);
		
		System.out.println("Building Genome viewer...");
		String chromName = "3";
		ChromosomePaintable mcp = new ChromosomePaintable(g, chromName);
		
		System.out.println("Adding gene tracks...");
		GeneGenerator gen = props.getGeneGenerator(strain);
		GeneChromPainter watsonGenes = new GeneChromPainter(mcp.getChrom(), gen, '+'); 
		GeneChromPainter crickGenes = new GeneChromPainter(mcp.getChrom(), gen, '-'); 
		mcp.addPainterTrack(watsonGenes); 
		mcp.addPainterTrack(crickGenes); 
		
		System.out.println("Adding expression tracks...");
		
		StandardTwoChannelProbeGenerator prober = new StandardTwoChannelProbeGenerator(props, expt);
		DiffExpressionChromScorer watson = new DiffExpressionChromScorer(mcp.getChrom(), prober);
		//DiffExpressionChromScorer crick = new DiffExpressionChromScorer(mcp.getChrom(), prober, '-');
		
		//ExpressionProbeGenerator prober = new StandardProbeGenerator(props, expt);
		//ExpressionChromScorer watson = new ExpressionChromScorer(mcp.getChrom(), prober, '+'); 
		//ExpressionChromScorer crick = new ExpressionChromScorer(mcp.getChrom(), prober, '-');
		
		LinkedList<Collection<Region>> regs = new LinkedList<Collection<Region>>();
		regs.add(watsonGenes.features());
		regs.add(crickGenes.features());
		watson.filterAll(regs);
		
		mcp.addScorerTrack(watson);
		//mcp.addScorerTrack(crick);
		
		PaintableFrame pf = new PaintableFrame(chromName, mcp);
	}
	
	private Genome genome;
	private String chromName;
	private Region chrom;

	private int length, maxLength;
	private int cenStart, cenEnd;
	
	private ArrayList<ChromLocations> locations;
	private ArrayList<ChromPainter> painters;
	private ArrayList<ChromScorer> scorers;
	
	public ChromosomePaintable(Genome g, String c) { 
		genome = g;
		chromName = c;
		
		chrom = new Region(genome, chromName, 0, genome.getChromLength(chromName)-1);
		
		length = genome.getChrom(chromName).getLength();
		cenStart = cenEnd = -1;
		maxLength = length;
		
		locations = new ArrayList<ChromLocations>();
		painters = new ArrayList<ChromPainter>();
		scorers = new ArrayList<ChromScorer>();
	}
	
	public Genome getGenome() { return genome; }
	public Region getChrom() { return chrom; }
	
	public ChromScorer getScorer(int i) { return scorers.get(i); }
	public ChromPainter getPainter(int i) { return painters.get(i); }
	public ChromLocations getLocations(int i) { return locations.get(i); }
	
	public int getNumScorers() { return scorers.size(); }
	public int getNumPainters() { return painters.size(); }
	public int getNumLocations() { return locations.size(); }
	
	public void addLocationTrack(ChromLocations loc) { 
		locations.add(loc);
		dispatchChangedEvent();
	}
	
	public void addPainterTrack(ChromPainter ptr) { 
		painters.add(ptr);
		dispatchChangedEvent();
	}
	
	public void addScorerTrack(ChromScorer s) { 
		scorers.add(s);
		dispatchChangedEvent();
	}
	
	public void setMaxLength(int ml) { 
		maxLength = ml;
		dispatchChangedEvent();
	}
	
	public void setCentromere(int cs, int ce) { 
		cenStart = cs; cenEnd = ce;
		dispatchChangedEvent();
	}
	
	public int getLength() { return length; }

	public void paintItem(Graphics g, int x1, int y1, int x2, int y2) {
		Graphics2D g2 = (Graphics2D)g;
		int w = x2 - x1, h = y2 - y1;
		
		Stroke oldStroke = g2.getStroke();
		
		int tracks = 1 + scorers.size() + (locations.size() > 0 ? 1 : 0);
		int trackHeight = (int)Math.floor((double)h / (double)(tracks+1));
		int trackSpacing = (int)Math.floor((double)trackHeight / (double)(tracks+1));
		
		double lengthf = (double)length / (double)maxLength;
		int chromPixWidth = (int)Math.floor(lengthf * (double)w);

		/*
		 * Fill in a background. 
		 */
		g2.setColor(Color.white);
		g2.fillRect(x1, y1, w, h);

		/*
		 * Draw the chromosome body itself.
		 */
		int[] rgba = Coloring.rgba(Coloring.brighten(Color.gray));

		Color chromColor = Coloring.asColor(rgba);
		
		g2.setColor(chromColor);
		
		//int scorersAbove = scorers.size()/2 + 1;
		int scorersAbove = (int)Math.ceil((double)scorers.size()/2.0);
		
		int cx1 = x1, 
			cy1 = y1+
				trackHeight*(scorersAbove) + 
				trackSpacing*(scorersAbove+1);
		
		int chromRounding = Math.max(2, trackHeight/2);
		
		if(cenStart != -1) { 
			double leftf = (double)cenStart / (double)maxLength;
			double rightf = (double)cenEnd / (double)maxLength;
			
			int cenx1 = x1 + (int)Math.round(leftf * (double)w);
			int cenx2 = x1 + (int)Math.round(rightf * (double)w);

			g2.fillRoundRect(cx1, cy1, cenx1-cx1, trackHeight, chromRounding, chromRounding);
			g2.fillOval(cenx1, cy1, cenx2-cenx1, trackHeight);
			g2.fillRoundRect(cenx2, cy1, chromPixWidth-cenx2, trackHeight, chromRounding, chromRounding);

		} else { 
			g2.fillRoundRect(cx1, cy1, chromPixWidth, trackHeight, chromRounding, chromRounding);
		}

		/*
		 * Draw the painted regions on teh chromosome itself.  
		 */
		rgba = Coloring.rgba(Color.blue);
		rgba[3] = 75;
		Color geneColor = Coloring.asColor(rgba);
		g2.setColor(geneColor);
		
		ChromInformationRenderer<ChromPainter> ptrRender = new DefaultPainterRenderer(geneColor);
		
		int chromSpacing = trackHeight/5;
		int totalPaintedHeight = trackHeight-(chromSpacing*2);  
		
		int py = cy1 + chromSpacing;
		
		for(ChromPainter painter : painters) {
			// TODO: this should paint in tracks, and it doesn't.
			ptrRender.paintInformation(painter, g2, 
					x1, py, x1+chromPixWidth, py+totalPaintedHeight);
		}

		/*
		int ph = totalPaintedHeight/Math.max(1, painters.size());
		for(int k = 0; k < painters.size(); k++) { 
			ChromPainter ptr = painters.get(k);

			ptr.recalculate(chromPixWidth);

			for(int i = 0; i < chromPixWidth; i++) {
				if(ptr.value(i)) { 
					int x = x1 + i;
					g2.drawLine(x, py, x, py+ph);
				}
			}
			
			py += ph;
		}
		*/

		/*
		 * Draw the score tracks as separate levels above the chromosome.  
		 */
		
		Color[] colorArray = new Color[] { Color.red, Color.green, Color.blue };
		
		ChromInformationRenderer<ChromScorer> redRender = 
			new OneColorScoreRenderer(Color.red, false);
		ChromInformationRenderer<ChromScorer> greenRender = 
			new OneColorScoreRenderer(Color.green, false);
		ChromInformationRenderer<ChromScorer> diffRender = 
			new PlusMinusScoreRenderer(Color.red, Color.green, false);
		
		g2.setStroke(new BasicStroke((float)2.0));
		
		int upperY1 = y1;
		int lowerY1 = cy1 + trackHeight;
		
		for(int i = 0; i < scorers.size(); i++) { 
			boolean flipped = (i%2 == 1);
			ChromScorer s = scorers.get(i);

			int idx = i/2;
			int ty1 = (flipped ? lowerY1 : upperY1) + 
				idx*trackHeight + 
				(idx+1)*trackSpacing;
			
			// TODO: There should be flipping logic here, but I took it out.

			if(s instanceof DiffExpressionChromScorer) { 
				diffRender.paintInformation(s, g2,
						x1, ty1, x1+chromPixWidth, ty1 + trackHeight);
				
			} else { 

				if(flipped) { 
					greenRender.paintInformation(s, g2, 
							x1, ty1, x1+chromPixWidth, ty1+trackHeight);
				} else { 
					redRender.paintInformation(s, g2, 
							x1, ty1, x1+chromPixWidth, ty1+trackHeight);				
				}
			}

		}
		
		g2.setStroke(oldStroke);

		/*
		 * Draw the locations entries, as a single track along the bottom. 
		 */
		g2.setColor(Coloring.darken(Color.green));
		
		for(ChromLocations loc : locations) { 
			loc.recalculate(chromPixWidth);
			int ly = y1 + 
				trackSpacing * (2 + scorers.size()) + trackHeight * (1 + scorers.size());
			
			Iterator<Integer> points = loc.locations();
			while(points.hasNext()) {
				int lx = x1 + points.next();
				g2.drawLine(lx, ly, lx, ly+trackHeight);
			}
		}
		
	}
}
