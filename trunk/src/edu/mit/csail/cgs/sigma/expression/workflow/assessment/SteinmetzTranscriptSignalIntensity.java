package edu.mit.csail.cgs.sigma.expression.workflow.assessment;

import java.util.*;
import java.awt.Color;
import java.io.*;

import edu.mit.csail.cgs.datasets.general.StrandedRegion;
import edu.mit.csail.cgs.datasets.species.Genome;
import edu.mit.csail.cgs.datasets.species.Organism;
import edu.mit.csail.cgs.sigma.*;
import edu.mit.csail.cgs.sigma.expression.segmentation.Segment;
import edu.mit.csail.cgs.sigma.expression.workflow.*;
import edu.mit.csail.cgs.sigma.expression.workflow.models.*;
import edu.mit.csail.cgs.sigma.expression.*;
import edu.mit.csail.cgs.sigma.expression.models.*;
import edu.mit.csail.cgs.sigma.litdata.steinmetz.*;
import edu.mit.csail.cgs.utils.ArrayUtils;
import edu.mit.csail.cgs.utils.NotFoundException;
import edu.mit.csail.cgs.utils.iterators.SerialIterator;
import edu.mit.csail.cgs.utils.models.Model;
import edu.mit.csail.cgs.utils.models.ModelInput;
import edu.mit.csail.cgs.utils.models.data.XYPoint;
import edu.mit.csail.cgs.viz.colors.Coloring;
import edu.mit.csail.cgs.viz.eye.ModelHistogram;
import edu.mit.csail.cgs.viz.eye.ModelScatter;
import edu.mit.csail.cgs.viz.paintable.BackgroundPaintable;
import edu.mit.csail.cgs.viz.paintable.Paintable;
import edu.mit.csail.cgs.viz.paintable.PaintableFrame;
import edu.mit.csail.cgs.viz.paintable.PaintableScale;
import edu.mit.csail.cgs.viz.paintable.layout.LayeredPaintable;
import edu.mit.csail.cgs.viz.paintable.layout.StackedPaintable;

public class SteinmetzTranscriptSignalIntensity {
	
	public static void main(String[] args) { 
		String key = args.length > 0 ? args[0] : "s288c";
		WorkflowProperties props = new WorkflowProperties();
		File f = new File("C:\\Documents and Settings\\tdanford\\Desktop\\steinmetz-intensity-comparison.txt");

		try {
			SteinmetzTranscriptSignalIntensity inten = null;
			
			if(f.exists()) { 
				inten = new SteinmetzTranscriptSignalIntensity(props, key, f);				
			} else { 
				inten = new SteinmetzTranscriptSignalIntensity(props, key);
				inten.save(f);
			}
			
			Color rc = Coloring.clearer(Coloring.clearer(Color.red));
			Color bc = Coloring.clearer(Coloring.clearer(Color.blue));
			
			ModelScatter transcriptScatter = new ModelScatter("sudeep", "steinmetz");
			ModelScatter noiseScatter = new ModelScatter("sudeep", "steinmetz");

			noiseScatter.addModels(inten.noise.iterator());
			transcriptScatter.addModels(inten.transcript.iterator());
			
			transcriptScatter.setProperty(ModelScatter.radiusKey, 2);
			noiseScatter.setProperty(ModelScatter.radiusKey, 2);
			
			transcriptScatter.setProperty(ModelScatter.showScaleKey, false);
			noiseScatter.setProperty(ModelScatter.showScaleKey, false);
			
			transcriptScatter.setProperty(ModelScatter.colorKey, rc);
			noiseScatter.setProperty(ModelScatter.colorKey, bc);
			
			PaintableScale s = new PaintableScale(-1.0, 10.0);

			transcriptScatter.setProperty(ModelScatter.xScaleKey, s);
			transcriptScatter.setProperty(ModelScatter.yScaleKey, s);
			
			transcriptScatter.synchronizeProperty(ModelScatter.xScaleKey, noiseScatter);
			transcriptScatter.synchronizeProperty(ModelScatter.yScaleKey, noiseScatter);
			
			Paintable scatter = new LayeredPaintable(
					new BackgroundPaintable(Color.white), 
					noiseScatter, transcriptScatter);
			
			Paintable steinhist = createHistogram(inten, "steinmetz", s);
			Paintable sudhist = createHistogram(inten, "sudeep", s);
			
			PaintableFrame pft = new PaintableFrame("Steinmetz/Sudeep Comparison", scatter);
			PaintableFrame pfh1 = new PaintableFrame("Steinmetz Histogram", steinhist);
			PaintableFrame pfh2 = new PaintableFrame("Sudeep Histogram", sudhist);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static Paintable createHistogram(SteinmetzTranscriptSignalIntensity inten, String value, PaintableScale scale) {
		Color rc = Coloring.clearer(Color.red);
		Color bc = Coloring.clearer(Color.blue);
		
		ModelHistogram tHist = new ModelHistogram(value);
		ModelHistogram nHist = new ModelHistogram(value);
		
		tHist.addModels(inten.transcript.iterator());
		nHist.addModels(inten.noise.iterator());
		
		tHist.setProperty(ModelHistogram.colorKey, rc);
		nHist.setProperty(ModelHistogram.colorKey, bc);			
		
		tHist.setProperty(ModelHistogram.xScaleKey, scale);
		tHist.synchronizeProperty(ModelHistogram.xScaleKey, nHist);
		tHist.synchronizeProperty(ModelHistogram.yScaleKey, nHist);
		
		tHist.setProperty(ModelHistogram.binsKey, 100);
		nHist.setProperty(ModelHistogram.binsKey, 100);
		
		tHist.rebin();
		nHist.rebin();
		
		return new StackedPaintable(tHist, nHist);
	}
	
	private WorkflowProperties props;
	private String key;
	public LinkedList<PairedValues> transcript, noise;
	
	public SteinmetzTranscriptSignalIntensity(WorkflowProperties ps, String k, File f) throws IOException { 
		key = k;
		props = ps;
		load(f);
	}

	public SteinmetzTranscriptSignalIntensity(WorkflowProperties props, String key) throws IOException {
		this.props = props;
		this.key = key;
		String expt = "matalpha";
		SteinmetzAverager averager = new SteinmetzAverager();
		transcript = new LinkedList<PairedValues>();
		noise = new LinkedList<PairedValues>();
		Genome g = null;
		try {
			g = Organism.findGenome("SGDv1");
		} catch (NotFoundException e) {
			e.printStackTrace();
			throw new IllegalArgumentException();
		}
		WorkflowIndexing index = props.getIndexing(key);
		Integer[] channels = index.findChannels(key, expt);
		
		Iterator<DataSegment> segs = loadSegments();
		int si = 0;
		
		while(segs.hasNext()) { 
			DataSegment seg  = segs.next();
			//System.out.println(String.format("%s:%d-%d:%s", seg.chrom, seg.start, seg.end, seg.strand));
			StrandedRegion query = new StrandedRegion(g, seg.chrom, seg.start, seg.end, seg.strand.charAt(0));
			int threePrime = seg.strand.equals("+") ? seg.end : seg.start; 
			
			Double mean = averager.mean(query);
			if(mean != null) { 
				double segEstimate = seg.predicted(channels, threePrime);
				PairedValues pv = new PairedValues(seg, mean, segEstimate);

				if(seg.hasConsistentType(Segment.LINE)) {
					transcript.add(pv);
					//System.out.println(String.format("\tTranscribed: %.2f -> %.2f", pv.sudeep, pv.steinmetz));
				} else { 
					noise.add(pv);
					//System.out.println(String.format("\tNoise: %.2f -> %.2f", pv.sudeep, pv.steinmetz));
				}
			}
			
			si += 1;
			if(si % 100 == 0) { System.out.print("."); System.out.flush(); }
			if(si % 1000 == 0) { System.out.println(String.format("(%d)", si)); System.out.flush(); } 
		}
		
		System.out.println(String.format("\nCollected %d transcript values, %d noise values.", 
				transcript.size(), noise.size()));
	}
	
	public void save(File f) throws IOException { 
		PrintStream ps = new PrintStream(new FileOutputStream(f));
		ps.println(new Values(transcript, noise).asJSON().toString());
		ps.close();
	}
	
	public void load(File f) throws IOException { 
		ModelInput.LineReader<Values> r = new ModelInput.LineReader<Values>(Values.class, new FileReader(f));
		Values v = r.readModel();
		transcript = new LinkedList<PairedValues>(ArrayUtils.asCollection(v.transcripts));
		noise = new LinkedList<PairedValues>(ArrayUtils.asCollection(v.noise));
		r.close();
	}
	
	public Iterator<DataSegment> loadSegments() throws IOException { 
		File watsonSegs = new File(props.getDirectory(), String.format("%s_plus.datasegs", key));
		File crickSegs = new File(props.getDirectory(), String.format("%s_negative.datasegs", key));
		return new SerialIterator<DataSegment>(
				new WorkflowDataSegmentReader(watsonSegs), 
				new WorkflowDataSegmentReader(crickSegs));
	}
	
	public static class Values extends Model { 
		public PairedValues[] transcripts; 
		public PairedValues[] noise;
		
		public Values() {}

		public Values(Collection<PairedValues> t, Collection<PairedValues> n) { 
			transcripts = t.toArray(new PairedValues[0]);
			noise = n.toArray(new PairedValues[0]);
		}
	}
	
	public static class PairedValues extends Model { 
		
		public String chrom, strand;
		public Integer start, end;
		public Double steinmetz;
		public Double sudeep;
		
		public PairedValues() {}
		
		public PairedValues(DataSegment ss, double s, double t) {
			chrom = ss.chrom;
			start = ss.start; 
			end = ss.end; 
			strand = ss.strand;
			steinmetz = s; 
			sudeep = t;
		}
	}
}


