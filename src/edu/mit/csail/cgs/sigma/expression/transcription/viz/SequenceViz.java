/*
 * Author: tdanford
 * Date: Jan 24, 2009
 */
package edu.mit.csail.cgs.sigma.expression.transcription.viz;

import java.util.*;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JPanel;

import edu.mit.csail.cgs.datasets.chipchip.ChipChipData;
import edu.mit.csail.cgs.datasets.general.Region;
import edu.mit.csail.cgs.datasets.general.StrandedRegion;
import edu.mit.csail.cgs.datasets.locators.ChipChipLocator;
import edu.mit.csail.cgs.datasets.species.Genome;
import edu.mit.csail.cgs.sigma.expression.BaseExpressionProperties;
import edu.mit.csail.cgs.sigma.expression.SudeepExpressionProperties;
import edu.mit.csail.cgs.sigma.expression.models.Transcript;
import edu.mit.csail.cgs.sigma.expression.segmentation.InputData;
import edu.mit.csail.cgs.sigma.expression.segmentation.Segment;
import edu.mit.csail.cgs.sigma.expression.segmentation.viz.ModelSegmentValues;
import edu.mit.csail.cgs.sigma.expression.transcription.Call;
import edu.mit.csail.cgs.sigma.expression.transcription.identifiers.TranscriptIdentifier;
import edu.mit.csail.cgs.sigma.expression.transcription.viz.TranscriptionViz.Datapoint;
import edu.mit.csail.cgs.sigma.expression.transcription.viz.TranscriptionViz.FittedRange;
import edu.mit.csail.cgs.sigma.expression.workflow.models.RegionKey;
import edu.mit.csail.cgs.sigma.genes.GeneAnnotationProperties;
import edu.mit.csail.cgs.sigma.viz.GenePainter;
import edu.mit.csail.cgs.utils.NotFoundException;
import edu.mit.csail.cgs.utils.Pair;
import edu.mit.csail.cgs.utils.database.DatabaseException;
import edu.mit.csail.cgs.utils.models.Model;
import edu.mit.csail.cgs.viz.eye.ModelLocatedValues;
import edu.mit.csail.cgs.viz.eye.ModelRangeValues;
import edu.mit.csail.cgs.viz.eye.PropertyValueWrapper;
import edu.mit.csail.cgs.viz.paintable.DoubleBufferedPaintable;
import edu.mit.csail.cgs.viz.paintable.PaintablePanel;
import edu.mit.csail.cgs.viz.paintable.PaintableScale;
import edu.mit.csail.cgs.viz.paintable.VerticalScalePainter;

public class SequenceViz extends JPanel {

	private GenePainter genePaintable;  // genes.
	private RegionKey key;
	private Genome genome;
	
	private Vector<ChipChipData> chipData;
	private Vector<ModelLocatedValues> chipPaintable;  // chip-chip values.
	
	//////////////////////////////////////////////////////////////////
	// For dynamic painting of segment and probe values
	private Set<Model> nearModel; 
	private Point modelPoint, mousePoint;
	//////////////////////////////////////////////////////////////////
	
	public void addChipChipValues(ChipChipLocator loc, Color c) { 
		ChipChipData data = loc.createObject();
		chipData.add(data);
		
		ModelLocatedValues v = new ModelLocatedValues();
		v.setProperty(ModelLocatedValues.colorKey, c);
		v.setProperty(ModelLocatedValues.stemKey, false);
		v.setProperty(ModelLocatedValues.connectedKey, true);
		
		chipPaintable.add(v);
	}
	
	private void updateChipData(String chrom, Integer start, Integer end) { 
		for(int i = 0; i < chipPaintable.size(); i++) { 
			ModelLocatedValues paintable = chipPaintable.get(i);
			ChipChipData data = chipData.get(i);
			
			paintable.clearModels();
			PaintableScale scale = paintable.getPropertyValue(ModelLocatedValues.scaleKey, null);
			//scale.setScale(0.0, 2.0);
			
			PropertyValueWrapper<Integer[]> boundsWrapper = 
				(PropertyValueWrapper<Integer[]>)paintable.getProperty(
						ModelLocatedValues.boundsKey);
			boundsWrapper.setValue(new Integer[] { start, end });
			
			paintable.setProperty(ModelRangeValues.boundsKey, boundsWrapper);

			try {
				data.window(chrom, start, end);
				
				for(int j = 0; j < data.getCount(); j++) { 
					int loc = data.getPos(j);
					for(int k = 0; k < data.getReplicates(j); k++) {
						double value = data.getRatio(j, k);
						paintable.addModel(new Datapoint(loc, value));
					}
				}
				System.out.println();

			} catch (NotFoundException e) {
				e.printStackTrace();
			}
		}
	}
	
	public SequenceViz(String strain) { 
		super();
		this.setPreferredSize(new Dimension(400, 50));
		
		key = null;
		
		chipData = new Vector<ChipChipData>();
		chipPaintable = new Vector<ModelLocatedValues>();
		
		BaseExpressionProperties bps = new SudeepExpressionProperties();
		GeneAnnotationProperties gaps = new GeneAnnotationProperties();
		
		try { 
			genome = bps.getGenome(strain);
			genePaintable = new GenePainter(gaps, strain);
		} catch(DatabaseException e) { 
			genome = null;
			genePaintable = null;
		}
		
		addComponentListener(new ComponentListener() {

			public void componentHidden(ComponentEvent arg0) {
				nearModel = null;
				modelPoint = null;
				repaint();
			}

			public void componentMoved(ComponentEvent arg0) {
				nearModel = null;
				modelPoint = null;
				repaint();
			}

			public void componentResized(ComponentEvent arg0) {
				nearModel = null;
				modelPoint = null;
				repaint();
			}

			public void componentShown(ComponentEvent arg0) {
				nearModel = null;
				modelPoint = null;
				repaint();
			} 
			
		});
	}
	
	public Region lookupRegion(String name) { 
		return genePaintable != null ? genePaintable.lookupRegion(name) : null;
	}
	
	public void setGenePaintable(Genome g, GenePainter gp) { 
		genome = g;
		genePaintable = gp;
		repaint();
	}

	public void setRegion(RegionKey k) {
		key = k;
		if(genome != null) { 
			StrandedRegion r = 
				new StrandedRegion(genome, key.chrom, key.start, 
						key.end, key.strand.charAt(0));
			genePaintable.setRegion(r);
		}
		
		updateChipData(k.chrom, k.start, k.end);
		
		repaint();
	}
	
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		
		int w = getWidth(), h = getHeight();
		g.setColor(Color.white);
		g.fillRect(0, 0, w, h);
		
		int h5 = h/5;
		int h2 = h/2;
		
		Graphics2D g2 = (Graphics2D)g;
		FontMetrics fm = g2.getFontMetrics();
		
		for(int i = 0; i < chipPaintable.size(); i++) { 
			chipPaintable.get(i).paintItem(g, 0, 0, w, h2);
		}
		
		g.setColor(Color.black);
		g.drawLine(0, h2, w, h2);
		
		if(key != null && genePaintable != null) { 
			genePaintable.paintItem(g, 0, h5, w, h-h5);
		}
	}
}
