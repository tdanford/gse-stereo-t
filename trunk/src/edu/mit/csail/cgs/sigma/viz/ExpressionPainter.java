package edu.mit.csail.cgs.sigma.viz;

import java.util.*;
import java.util.logging.*;

import java.awt.*;
import java.io.File;
import java.io.IOException;

import edu.mit.csail.cgs.datasets.chipchip.ChipChipData;
import edu.mit.csail.cgs.datasets.chipchip.SQLData;
import edu.mit.csail.cgs.datasets.general.Region;
import edu.mit.csail.cgs.datasets.general.StrandedRegion;
import edu.mit.csail.cgs.datasets.locators.ChipChipLocator;
import edu.mit.csail.cgs.datasets.species.Genome;
import edu.mit.csail.cgs.sigma.OverlappingRegionFinder;
import edu.mit.csail.cgs.sigma.Parser;
import edu.mit.csail.cgs.sigma.SigmaProperties;
import edu.mit.csail.cgs.sigma.expression.*;
import edu.mit.csail.cgs.sigma.expression.models.Transcript;
import edu.mit.csail.cgs.sigma.expression.noise.FileNoiseModel;
import edu.mit.csail.cgs.sigma.expression.noise.NoiseModel;
import edu.mit.csail.cgs.sigma.expression.noise.PValueNoiseModel;
import edu.mit.csail.cgs.sigma.expression.noise.ThresholdNoiseModel;
import edu.mit.csail.cgs.utils.NotFoundException;
import edu.mit.csail.cgs.utils.Pair;
import edu.mit.csail.cgs.utils.probability.ExponentialDistribution;

public class ExpressionPainter extends RegionPainter implements Labeled {
	
	private static Color[] transcriptColorArray = { Color.blue.brighter(), Color.red.brighter(), Color.red, Color.green, Color.blue };
	
	private BaseExpressionProperties props;
	private SQLData originalData;
	private Logger logger;
	private String strain, exptKey, label;
	private Genome genome;
	
	private Vector<Pair<Integer,Double>> watsonProbes, crickProbes;
	private Vector<ExprPoint> points;
	private double maxValue, logMaxValue;
    private boolean logScale;
    private boolean displayOppositeChannel;
    private boolean paintLabel;
    private Color baseColor;
	
    private ExpressionSearch fgSearcher, bgSearcher;
	private Vector<OverlappingRegionFinder<Transcript>> transcripts;
	private Vector<PaintedTranscript> transcriptWedges;
	
	private Map<Point,ExprPoint> selections;
	
	private ExponentialDistribution bgDist;
	private NoiseModel noise;
	
	public ExpressionPainter(BaseExpressionProperties prs, String ek, boolean oppChannel) { 
		props = prs;
		exptKey = ek;
		label = exptKey;
		strain = props.parseStrainFromExptKey(exptKey);
		genome = props.getGenome(strain);
		//noise = new ThresholdNoiseModel(props);
		
		double lambda = 1.0/93.599162;
		bgDist = new ExponentialDistribution(lambda*10.0);
		noise = new PValueNoiseModel(bgDist, 0.1);
		/*
		noise =new FileNoiseModel(genome, 
				new File("C:\\Documents and Settings\\tdanford\\Desktop\\stacie_figure\\flo11_transcripts.txt"));
		*/
		
		fgSearcher = bgSearcher = null;
		/*
		fgSearcher = new ExpressionSearch(prs, bgDist, true);
		fgSearcher.loadData(exptKey);
		bgSearcher = new ExpressionSearch(prs, bgDist, false);
		bgSearcher.loadData(exptKey);
		*/
		
        displayOppositeChannel = oppChannel;
        paintLabel = true;
        baseColor = displayOppositeChannel ? Color.green: Color.red;
		logger = props.getLogger(String.format("ExpressionPainter.%s", exptKey));
		ChipChipLocator loc = props.getLocator(exptKey);
		originalData = (SQLData)loc.createObject(); 
        originalData.setMaxCount(1);
        
        System.out.println(String.format("ExpressionPainter: \"%s\"", loc.toString()));
		
		watsonProbes = new Vector<Pair<Integer,Double>>();
		crickProbes = new Vector<Pair<Integer,Double>>();
		points = new Vector<ExprPoint>();
		maxValue = 100.0;
        logMaxValue = Math.log(maxValue);
        logScale = false;
		
		transcripts = new Vector<OverlappingRegionFinder<Transcript>>();
		transcriptWedges = null;
		
		selections = new HashMap<Point,ExprPoint>();
	}
	
	public String getLabel() { 
		return label; 
	}
	
	public void setPaintLabel(boolean v) { 
		paintLabel = v;
		dispatchChangedEvent();
	}
	
	public void setLabel(String lbl) { 
		label = lbl;
		dispatchChangedEvent();
	}
    
    public void setLogScale(boolean s) {
        if(s != logScale) { 
            logScale = s;
            transcriptWedges = null;
        }
    }
	
	public void addTranscripts(File f) { 
		Genome genome = props.getSigmaProperties().getGenome(strain);
		try {
			Parser<Transcript> transcriptfile = 
				new Parser<Transcript>(f, new Transcript.ParsingMapper(genome));
			transcriptWedges = null;
			
			LinkedList<Transcript> ts = new LinkedList<Transcript>();
			while(transcriptfile.hasNext()) { 
				ts.add(transcriptfile.next());
			}
			
			//transcripts.add(new OverlappingRegionFinder<Transcript>(ts));
			
		} catch (IOException e) {
			//e.printStackTrace();
			//transcripts = null;
			transcriptWedges = null;
		}
	}
    
	public void setRegion(Region r) { 
		super.setRegion(r);
		maxValue = 100.0;
		boolean ip = props.isIPData(exptKey);
        if(displayOppositeChannel) { ip = !ip; } 
		
		watsonProbes.clear();
		crickProbes.clear();
		points = null;
		transcriptWedges = null;

		try {
			originalData.window(r.getChrom(), r.getStart(), r.getEnd());
			for(int i = 0; i < originalData.getCount(); i++) { 
				int bp = originalData.getPos(i);
				for(int j = 0; j < originalData.getReplicates(i); j++) { 
					char strand = originalData.getStrand(i, j);
					double value = ip ? originalData.getIP(i, j) : originalData.getWCE(i, j);
                    double oppvalue = ip ? originalData.getWCE(i, j) : originalData.getIP(i, j);
                    
                    if(!Double.isNaN(oppvalue) ) { 
                        maxValue = Math.max(maxValue, oppvalue);
                    }

                    if(!Double.isNaN(value)) { 
					    maxValue = Math.max(value, maxValue);
                        
					    if(strand == '+') { 
					        watsonProbes.add(new Pair<Integer,Double>(bp, value));
					    } else { 
					        crickProbes.add(new Pair<Integer,Double>(bp, value));						
					    }
					}
				}
			}
			
		} catch (NotFoundException e) {
			e.printStackTrace();
			logger.log(Level.SEVERE, String.format("setRegion() SQLException: %s", e.getMessage()));
		}
        
        logMaxValue = Math.log(maxValue);
	}
    
    public void rectifyMaxima(ExpressionPainter ep) { 
        maxValue = Math.max(maxValue, ep.maxValue);
        ep.maxValue = maxValue;
        logMaxValue = Math.max(logMaxValue, ep.logMaxValue);
        ep.logMaxValue = logMaxValue;
    }

	public void doLayout() {
        selections.clear();
		if(points == null) { points = new Vector<ExprPoint>(); }
		points.clear();
		
		String chrom = super.region != null ? super.region.getChrom() : "1";
		
		for(Pair<Integer,Double> p : watsonProbes) { 
			ExprPoint ep = new ExprPoint(chrom, p.getFirst(), p.getLast(), '+');
			points.add(ep);
		}
		
		for(Pair<Integer,Double> p : crickProbes) { 
			ExprPoint ep = new ExprPoint(chrom, p.getFirst(), p.getLast(), '-');
			points.add(ep);
		}
		
		if(region != null) { 
			if(transcriptWedges == null) { 
				transcriptWedges = new Vector<PaintedTranscript>(); 
			}
			transcriptWedges.clear();

			int ti = 0;
			
			for(OverlappingRegionFinder<Transcript> trans : transcripts) {
				Color color = transcriptColorArray[ti].brighter();
				color = new Color(color.getRed(), color.getGreen(), color.getBlue(), 50);
				
				Collection<Transcript> ts = trans.findOverlapping(region);
				for(Transcript t : ts) { 
					transcriptWedges.add(new PaintedTranscript(t, color));
				}
				
				ti++;
			}
			
			if(fgSearcher != null) { 
                StrandedRegion pregion = new StrandedRegion(region, '+');
                
                Color color = transcriptColorArray[ti].brighter();
                color = new Color(color.getRed(), color.getGreen(), color.getBlue(), 25);

                Collection<Transcript> ts = fgSearcher.analyzeStrandedRegion(pregion, bgDist);
                for(Transcript t : ts) { 
                    if(t != null) { 
                        transcriptWedges.add(new PaintedTranscript(t, color));
                    }
                }
                
                StrandedRegion nregion = new StrandedRegion(region, '-');
                color = transcriptColorArray[ti].brighter();
                color = new Color(color.getRed(), color.getGreen(), color.getBlue(), 25);

                ts = fgSearcher.analyzeStrandedRegion(nregion, bgDist);
                for(Transcript t : ts) { 
                    if(t != null) { 
                        transcriptWedges.add(new PaintedTranscript(t, color));
                    }
                }
                ti++;
			}
			if(bgSearcher != null) { 
                StrandedRegion pregion = new StrandedRegion(region, '+');
                
                Color color = transcriptColorArray[ti].brighter();
                color = new Color(color.getRed(), color.getGreen(), color.getBlue(), 25);

                Collection<Transcript> ts = bgSearcher.analyzeStrandedRegion(pregion, bgDist);
                for(Transcript t : ts) { 
                    if(t != null) { 
                        transcriptWedges.add(new PaintedTranscript(t, color));
                    }
                }
                
                StrandedRegion nregion = new StrandedRegion(region, '-');
                color = transcriptColorArray[ti].brighter();
                color = new Color(color.getRed(), color.getGreen(), color.getBlue(), 25);

                ts = bgSearcher.analyzeStrandedRegion(nregion, bgDist);
                for(Transcript t : ts) { 
                    if(t != null) { 
                        transcriptWedges.add(new PaintedTranscript(t, color));
                    }
                }
                ti++;
			}
		}
	}

	public boolean isReadyToPaint() {
		return points != null;
	}

	public void paintRegion(Graphics2D g, int x1, int y1, int w, int h) {
		int h2 = h / 2;
		int rad = 3;
		int diam = rad * 2;

		Color lg = baseColor;
		Color dg = lg.darker();
		lg = dg;  // added later, to make sure that the different strand-values have the same color.
		
		Color noiseLg = new Color(lg.getRed(), lg.getGreen(), lg.getBlue(), 50);
		Color noiseDg = new Color(dg.getRed(), dg.getGreen(), dg.getBlue(), 50);
		
		/*
		 * Draw the Baseline
		 */
		g.setColor(Color.black);
		g.drawLine(x1, y1+h2, x1+w, y1+h2);

        Stroke oldStroke = g.getStroke();
        g.setStroke(new BasicStroke((float)2.0));

		/*
		 * Draw the transcripts
		 */
		if(transcriptWedges != null) {
			for(PaintedTranscript pt : transcriptWedges) { 
                g.setColor(pt.color);
                pt.drawPolygon(g, x1, y1);
                g.setColor(Color.lightGray);
                g.drawLine(x1+pt.xs[1], y1+pt.ys[1], x1+pt.xs[2], y1+pt.ys[2]);
			}
		}
		
		/*
		 * Draw the datapoints
		 */
		for(ExprPoint ep : points) {
			if(ep.strand == '+') { 
				if(noise.isNoise(ep.point, ep.value)) { 
					g.setColor(noiseDg);
				} else { 
					g.setColor(dg);
				}
			} else { 
				if(noise.isNoise(ep.point, ep.value)) { 
					g.setColor(noiseLg);
				} else { 
					g.setColor(lg);
				}
			}
			
			g.drawOval(x1+ep.x-rad, y1+ep.y-rad, diam, diam);
		}
		
		g.setStroke(oldStroke);
		
		/*
		 * Paint the hash marks...
		 */
		if(!displayOppositeChannel) { 
		    g.setColor(Color.black);
		    boolean flipper = true;
		    for(int value = 100; value <= maxValue; value *= (flipper ? 5 : 2), flipper = !flipper) { 
		        int yoff = getYOffset((double)value);
		        String line = String.format("%d", value);
		        int uy = y1+h2 - yoff, ly = y1+h2 + yoff;

		        g.drawLine(x1, uy, x1+10, uy);
		        g.drawString(line, x1+12, uy+5);

		        g.drawLine(x1, ly, x1+10, ly);
		        g.drawString(line, x1+12, ly+5);
		    }
		}
		
		/*
		 * Draw any selections.
		 */
		
		g.setColor(Color.black);
		for(Point p : selections.keySet()) {
			ExprPoint ep = selections.get(p);
			g.drawLine(p.x, p.y, ep.x, ep.y);
			g.drawString(ep.getLabel(), p.x, p.y);
		}
		
		/*
		 * Draw the label in the upper-right hand corner.
		 */
		if(paintLabel) { 
			g.setColor(Color.black);
			Font oldFont = g.getFont();
			Font newFont = new Font("Arial", Font.BOLD, 24);
			g.setFont(newFont);
			FontMetrics fm = g.getFontMetrics();
			int lblHeight = fm.getAscent() + fm.getDescent();
			int lblWidth = fm.charsWidth(label.toCharArray(), 0, label.length());

			int padding = 5;
			int lblx = x1 + w - lblWidth - padding;
			int lbly = y1 + lblHeight + padding;

			g.drawString(label, lblx, lbly);

			g.setFont(oldFont);
		}
		
	}
	
	public void clearSelections() { 
		selections.clear();
	}
	
	public void addSelection(Point p) { 
		ExprPoint ep = null;
		int minDist = -1;
		
		for(ExprPoint ex : points) {
			int sd = ex.screenDist(p);
			if(ep == null || sd < minDist) { 
				ep = ex;
				minDist = sd;
			}
		}
		
		if(ep != null) { 
			Set<Point> remove = new HashSet<Point>();
			for(Point rp : selections.keySet()) { 
				if(selections.get(rp).equals(ep)) { 
					remove.add(rp);
				}
			}
			
			selections.put(p, ep);
			
			for(Point rp : remove) { 
				selections.remove(rp);
			}
		}
	}
	
	public int getYOffset(double value) {
        double frac = 0.0;
        if(logScale) { 
            frac = Math.log(value) / logMaxValue;
        } else { 
            frac = value / maxValue;
        }
        int h = height/2;
		int pix = (int)Math.round(frac * (double)h);
        
        /*
        System.out.println(String.format("%.2f value, %.2f max, %dpx height --> %.3f frac, %d pix",
                value, maxValue, h, frac, pix));
        */
        
        return pix;
	}
	
	private class PaintedTranscript { 
		public Transcript transcript;
		public int[] xs, ys;
		private int[] xs_offset, ys_offset;
		
		public Color color;
		
		public PaintedTranscript(Transcript t, Color c) { 
			color = c;
			transcript = t;
			xs = new int[4];
			ys = new int[4];
			xs_offset = new int[4];
			ys_offset = new int[4];
			
			int x1 = getXOffset(transcript.getStart());
			int x2 = getXOffset(transcript.getEnd());

			double valueStart = transcript.estimateValue(transcript.getStart());
			double valueEnd = transcript.estimateValue(transcript.getEnd());
			
			boolean watson = transcript.getStrand() == '+';
			int h2 = height/2;
			int y1 = watson ? h2 - getYOffset(valueStart) : h2 + getYOffset(valueStart);
			int y2 = watson ? h2 - getYOffset(valueEnd) : h2 + getYOffset(valueEnd);
			
			xs[0] = x1; ys[0] = h2;
			xs[1] = x1; ys[1] = y1;
			xs[2] = x2; ys[2] = y2;
			xs[3] = x2; ys[3] = h2;
		}

		public void drawPolygon(Graphics2D g, int x1, int y1) {
			for(int i = 0; i < 4; i++) { 
				xs_offset[i] = xs[i] + x1;
				ys_offset[i] = ys[i] + y1;
			}
			g.fillPolygon(xs_offset, ys_offset, 4);
		}
	}

	private class ExprPoint {
		
		public edu.mit.csail.cgs.datasets.general.Point point;
		public double value;
		public int bp;
		public char strand;
		public int x, y;
		
		public ExprPoint(String chrom, int loc, double val, char str) { 
			bp = loc;
			value = val;
			strand = str;
			x = getXOffset(bp);
			y = getYOffset(value);

            if(strand == '+') { 
				y = height/2 - y;
			} else { 
				y = height/2 + y;
			}
            
            point = new edu.mit.csail.cgs.datasets.general.Point(genome, chrom, loc);
		}
		
		public int screenDist(Point ep) { 
			int dx = (x-ep.x);
			int dy = (y-ep.y);
			return (int)Math.sqrt((double)(dx*dx + dy*dy));
		}
		
		public String getLabel() { 
			return String.format("%.1f", value);
		}
		
		public int hashCode() { 
			int code = 17;
			code += bp; code *= 37;
			code += strand; code *= 37;
			code += y; code *= 37;
			return code;
		}
		
		public boolean equals(Object o) { 
			if(!(o instanceof ExprPoint)) { return false; }
			ExprPoint ep = (ExprPoint)o;
			return ep.bp == bp && ep.strand==strand && ep.y == y;
		}
	}
}
