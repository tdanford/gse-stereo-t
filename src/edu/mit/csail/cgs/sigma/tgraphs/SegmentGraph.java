package edu.mit.csail.cgs.sigma.tgraphs;

import java.util.*;
import java.util.regex.*;
import java.io.*;

import edu.mit.csail.cgs.utils.models.*;
import edu.mit.csail.cgs.utils.graphs.*;
import edu.mit.csail.cgs.utils.iterators.EmptyIterator;
import edu.mit.csail.cgs.utils.iterators.SerialIterator;
import edu.mit.csail.cgs.utils.iterators.SingleIterator;

import edu.mit.csail.cgs.ewok.verbs.*;
import edu.mit.csail.cgs.cgstools.tgraphs.TaggedGraph;
import edu.mit.csail.cgs.datasets.general.*;
import edu.mit.csail.cgs.datasets.species.*;
import edu.mit.csail.cgs.sigma.OverlappingRegionFinder;
import edu.mit.csail.cgs.sigma.expression.segmentation.Segment;
import edu.mit.csail.cgs.sigma.expression.workflow.*;
import edu.mit.csail.cgs.sigma.expression.workflow.models.*;

public class SegmentGraph implements TaggedGraph {
	
	public static void main(String[] args) {
		// s288c, sigma
		// mata, matalpha, diploid
		try {
			WorkflowProperties props = new WorkflowProperties();
			String key = "s288c";
			WholeGenome genome = WholeGenome.loadWholeGenome(props, key);
			SegmentGraph graph = new SegmentGraph(props, key, "matalpha");
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static int nearDistance = 300;
	
	private static String[] tags = new String[] { 
		"sense", "antisense", "divergent", "convergent", 
		"partOf", "tandem", "overlapping", "info"
	};
	
	private WorkflowProperties props;
	private String key, strain, oppositeStrain, expt;
	private Genome genome;
	private WorkflowIndexing indexing;

	private ArrayList<String> genes, segments, transcripts;
	private Map<String,Integer> diffSegments;
	
	private Map<String,StrandedRegion> nodes;
	private Map<String,UndirectedGraph> graphs;
	
	public SegmentGraph(WorkflowProperties ps, SegmentGraphModel m) { 
		props = ps;
		setFromModel(m);
	}
	
	public void setFromModel(SegmentGraphModel m) {
		key = m.key;
		strain = m.strain;
		expt = m.expt;
		oppositeStrain = strain.equals("s288c") ? "sigma" : "s288c";
		indexing = props.getIndexing(key);
		
		genome = props.getSigmaProperties().getGenome(strain);
		
		graphs = new TreeMap<String,UndirectedGraph>();
		for(int i = 0; i < m.graphs.length; i++) { 
			graphs.put(m.graphNames[i], new UndirectedGraph(m.graphs[i]));
		}
		
		nodes = new TreeMap<String,StrandedRegion>();

		genes = new ArrayList<String>();
		for(int i = 0; i < m.genes.length; i++) { 
			genes.add(m.genes[i]);
			nodes.put(m.genes[i], parseStrandedRegion(genome, m.geneRegions[i]));
		}
		
		segments = new ArrayList<String>();
		for(int i = 0; i < m.segments.length; i++) { 
			segments.add(m.segments[i]);
			nodes.put(m.segments[i], parseStrandedRegion(genome, m.segmentRegions[i]));
		}		

		transcripts = new ArrayList<String>();
		for(int i = 0; i < m.transcripts.length; i++) { 
			transcripts.add(m.transcripts[i]);
			nodes.put(m.transcripts[i], parseStrandedRegion(genome, m.transcriptRegions[i]));
		}		

		diffSegments = new TreeMap<String,Integer>();
		for(int i = 0; i < m.diffUp.length; i++) { 
			diffSegments.put(m.diffUp[i], 1);
		}
		for(int i = 0; i < m.diffDown.length; i++) { 
			diffSegments.put(m.diffDown[i], -1);
		}		
	}
	
	public SegmentGraph(WorkflowProperties ps, String key, String expt) throws IOException { 
		props = ps;
		this.expt = expt;
		this.key = key;
		strain = props.parseStrainFromKey(key); 
		genome = props.getSigmaProperties().getGenome(strain);
		
		oppositeStrain = strain.equals("s288c") ? "sigma" : "s288c";
		indexing = props.getIndexing(key);
		
		nodes = new TreeMap<String,StrandedRegion>();
		graphs = new TreeMap<String,UndirectedGraph>();
		for(String tag : tags) { 
			graphs.put(tag, new UndirectedGraph());
		}
		
		genes = new ArrayList<String>();
		segments = new ArrayList<String>();
		transcripts = new ArrayList<String>();
		diffSegments = new TreeMap<String,Integer>();
		
		addData();
	}
	
	public SegmentGraph(WorkflowProperties ps, File f) throws IOException {
		this(ps, loadModel(f));
	}
	
	public static SegmentGraph loadGraph(String key, String expt) throws IOException {
		WorkflowProperties props = new WorkflowProperties();
		String fname = String.format("%s-%s-segment-graph.txt", key, expt);
		File f = new File(props.getDirectory(), fname);
		if(f.exists()) { 
			return new SegmentGraph(props, f);			
		} else {
			SegmentGraph g = new SegmentGraph(props, key, expt);
			g.save(f);
			return g;
		}
	}
	
	private static SegmentGraphModel loadModel(File f) throws IOException { 
		FileReader fr = new FileReader(f);
		ModelInput<SegmentGraphModel> modelsin = 
			new ModelInput.LineReader<SegmentGraphModel>(SegmentGraphModel.class, fr);
		SegmentGraphModel model = modelsin.readModel();
		fr.close();
		return model;
	}
	
	public void save() throws IOException { 
		String fname = String.format("%s-%s-segment-graph.txt", strain, expt);
		File f = new File(props.getDirectory(), fname);
		save(f);
	}
	
	public void save(File f) throws IOException { 
		PrintStream ps = new PrintStream(new FileOutputStream(f));
		ps.println(asModel().asJSON().toString());
		ps.close();
	}
	
	public SegmentGraphModel asModel() { return new SegmentGraphModel(this); }
	
	public String[] nodeTypes() { 
		return new String[] { "gene", "segment", "transcript", "info" }; 
	}
	
	public String[] edgeTags() { return tags; }

	public Iterator<String> allNodes() {
		return new SerialIterator<String>(geneNodes().iterator(), segmentNodes().iterator(), infoNodes().iterator());
	}
	
	public Set<String> infoNodes() { 
		TreeSet<String> info = new TreeSet<String>();
		info.add("diff:up");
		info.add("diff:down");
		info.add("unique");
		return info;
	}

	public Iterator<String> forward(String node, String edgeTag) {
		if(graphs.containsKey(edgeTag)) { 
			return graphs.get(edgeTag).getNeighbors(node).iterator();
		} else { 
			return new EmptyIterator<String>();
		}
	}

	public boolean hasEdge(String n1, String n2, String edgeTag) {
		if(graphs.containsKey(edgeTag)) { 
			return graphs.get(edgeTag).containsEdge(n1, n2);
		} else { 
			return false;
		}
	}

	public boolean hasNode(String n) {
		return genes.contains(n) || segments.contains(n);
	}

	public boolean hasNode(String n, String type) {
		if(type.equals("gene")) { 
			return genes.contains(n);
		} else if (type.equals("segment")) { 
			return segments.contains(n);
		} else if (type.equals("transcript")) { 
			return transcripts.contains(n);
		} else if (type.equals("info")) { 
			return infoNodes().contains(n);
		} else { 
			return false;
		}
	}

	public Iterator<String> reverse(String node, String edgeTag) {
		return forward(node, edgeTag);
	}

	public Iterator<String> typedNodes(String type) {
		if(type.equals("gene")) { 
			return genes.iterator();
		} else if (type.equals("segment")) { 
			return segments.iterator();
		} else if (type.equals("transcript")) { 
			return transcripts.iterator();
		} else if (type.equals("info")) { 
			return infoNodes().iterator();
		} else { 
			return new EmptyIterator<String>();
		}		
	}

	public String getStrain() { return strain; }
	public String getExpt() { return expt; }
	
	public StrandedRegion region(String id) { return nodes.get(id); }
	
	public Set<String> geneNodes() { return new TreeSet<String>(genes); }
	public Set<String> segmentNodes() { return new TreeSet<String>(segments); }
	public Set<String> transcriptNodes() { return new TreeSet<String>(transcripts); }
	
	public int differential(String id) { 
		return diffSegments.containsKey(id) ? diffSegments.get(id) : 0; 
	}
	
	public Set<String> geneNodes(Region r) { 
		TreeSet<String> ns = new TreeSet<String>();
		for(String id : geneNodes()) { 
			if(r == null || nodes.get(id).overlaps(r)) { 
				ns.add(id);
			}
		}
		return ns;
	}
	
	public Set<String> segmentNodes(Region r) { 
		TreeSet<String> ns = new TreeSet<String>();
		for(String id : segmentNodes()) { 
			if(r == null || nodes.get(id).overlaps(r)) { 
				ns.add(id);
			}
		}
		return ns;
	}
	
	public Set<String> transcriptNodes(Region r) { 
		TreeSet<String> ns = new TreeSet<String>();
		for(String id : transcriptNodes()) { 
			if(r == null || nodes.get(id).overlaps(r)) { 
				ns.add(id);
			}
		}
		return ns;
	}
	
	public boolean isUnique(String id) { 
		return graphs.get("info").getNeighbors(id).contains("unique");
	}
	
	public Set<String> partOf(String id) { 
		return graphs.get("partOf").getNeighbors(id);
	}
	
	public Set<String> sense(String id) { 
		return graphs.get("sense").getNeighbors(id);
	}
	
	public Set<String> tandem(String id) { 
		return graphs.get("tandem").getNeighbors(id);
	}
	
	public Set<String> antisense(String id) { 
		return graphs.get("antisense").getNeighbors(id);
	}
	
	public Set<String> overlapping(String id) { 
		return graphs.get("overlapping").getNeighbors(id);
	}
	
	public Set<String> convergent(String id) { 
		return graphs.get("convergent").getNeighbors(id);
	}
	
	public Set<String> divergent(String id) { 
		return graphs.get("divergent").getNeighbors(id);
	}
	
	private static <X extends Comparable> Set<X> either(Collection<X> c1, Collection<X> c2) { 
		TreeSet<X> s = new TreeSet<X>();
		s.addAll(c1); s.addAll(c2);
		return s;
	}
	
	private void addGeneNode(
			OverlappingRegionFinder<edu.mit.csail.cgs.datasets.species.Gene> geneFinder, 
			String id, edu.mit.csail.cgs.datasets.species.Gene gene) {
		
		addNode(id, gene);
		geneFinder.addRegion(gene);
		genes.add(id);
	}

	private void addTranscribedNode(
			OverlappingRegionFinder<StrandedRegion> segmentFinder,
			OverlappingRegionFinder<edu.mit.csail.cgs.datasets.species.Gene> geneFinder,
			OverlappingRegionFinder<NamedStrandedRegion> transcriptFinder,
			String id, StrandedRegion region) { 
		addNode(id, region);
		
		/** Find and mark sense and antisense overlaps **/
		Collection<edu.mit.csail.cgs.datasets.species.Gene> genes = 
			geneFinder.findOverlapping(region);
		for(edu.mit.csail.cgs.datasets.species.Gene g : genes) { 
			if(g.getStrand() == region.getStrand()) { 
				graphs.get("sense").addEdge(id, g.getID());
			} else { 
				graphs.get("antisense").addEdge(id, g.getID());
			}
		}
		
		Collection<StrandedRegion> segs = segmentFinder.findOverlapping(region);
		
		double fractionCutoff = 0.5;
		
		for(StrandedRegion seg : segs) {
			String segID = regionID(seg);
			int overSize = seg.getOverlapSize(region);
			double regionFrac = (double)overSize / (double)region.getWidth();
			double segFrac = (double)overSize / (double)seg.getWidth();
			
			if(seg.getStrand() != region.getStrand() && regionFrac >= fractionCutoff && segFrac >= fractionCutoff) {
				graphs.get("overlapping").addEdge(id, segID);
			}
		}
		
		StrandedRegion div = null, conv = null, tand = null;
		
		if(region.getStrand() == '+') { 
			conv = findNearest(segmentFinder, region, nearDistance, true, '-'); 
			div = findNearest(segmentFinder, region, nearDistance, false, '-');
			tand = findNearest(segmentFinder, region, nearDistance, true, '+');
		} else { 
			conv = findNearest(segmentFinder, region, nearDistance, false, '+');
			div = findNearest(segmentFinder, region, nearDistance, true, '+'); 
			tand = findNearest(segmentFinder, region, nearDistance, false, '-');
		}
		
		if(conv != null) { 
			graphs.get("convergent").addEdge(id, regionID(conv));
		}
		
		if(div != null) { 
			graphs.get("divergent").addEdge(id, regionID(div));
		}
		
		if(tand != null) {
			graphs.get("tandem").addEdge(id, regionID(tand));
		}
		
		segmentFinder.addRegion(region);
		segments.add(id);
	}

	/**
	 * This it a utility class for finding the "nearest neighbor" from a set of 
	 * regions, in either direction from a starting point, on either either strand, 
	 * within a certain maximum distance. 
	 * 
	 * @param segmentFinder  The set of regions to search
	 * @param base  The start of the search
	 * @param maxDist  The farthest out we should search
	 * @param direction  true = search "to the right" (higher coordinates), false = search "to the left"
	 * @param strand only search for regions on this strand.  
	 * @return returns the nearest neighbor, if found, or null otherwise.  
	 */
	private StrandedRegion findNearest(
			OverlappingRegionFinder<StrandedRegion> segmentFinder, 
			StrandedRegion base, int maxDist, boolean direction, char strand) {
		
		int qstart = direction ? base.getEnd() + 1 : base.getStart() - maxDist;
		int qend = qstart + maxDist - 1;
		Region query = new Region(genome, base.getChrom(), qstart, qend);
		TreeSet<StrandedRegion> nearest = new TreeSet<StrandedRegion>(new SortingComparator(direction));
		nearest.addAll(segmentFinder.findOverlapping(query));
		
		for(StrandedRegion near : nearest) { 
			if(!near.overlaps(base) && near.getStrand() == strand) { 
				return near;
			}
		}
		
		return null;
	}
	
	private void addNode(String id, StrandedRegion region) { 
		nodes.put(id, region);
		for(String tag : graphs.keySet()) { 
			graphs.get(tag).addVertex(id);
		}
	}
	
	/**
	 * Central method that loads data from files, and creates gene/segment/other nodes
	 * from it, populating all the internal graphs.  
	 * 
	 * @throws IOException
	 */
	public void addData() throws IOException {
		
		OverlappingRegionFinder<edu.mit.csail.cgs.datasets.species.Gene> geneFinder = 
			new OverlappingRegionFinder<edu.mit.csail.cgs.datasets.species.Gene>();
		OverlappingRegionFinder<StrandedRegion> segmentFinder = 
			new OverlappingRegionFinder<StrandedRegion>();
		OverlappingRegionFinder<NamedStrandedRegion> transcriptFinder = 
			new OverlappingRegionFinder<NamedStrandedRegion>();
		
		for(String n : infoNodes()) { 
			graphs.get("info").addVertex(n);
		}
		
		addGenes(geneFinder);
		addSegments(geneFinder, segmentFinder, transcriptFinder);
	}
	
	public void addGenes(
			OverlappingRegionFinder<edu.mit.csail.cgs.datasets.species.Gene> geneFinder) {
		
		GenomeExpander<edu.mit.csail.cgs.datasets.species.Gene> exp = 
			new GenomeExpander<edu.mit.csail.cgs.datasets.species.Gene>(
					props.getSigmaProperties().getGeneGenerator(strain));
		
		Iterator<edu.mit.csail.cgs.datasets.species.Gene> genes = exp.execute(genome);
		while(genes.hasNext()) { 
			edu.mit.csail.cgs.datasets.species.Gene g = genes.next();
			String id = g.getID();
			addGeneNode(geneFinder, id, g);
		}
	}
	
	public void addTranscripts(
			String exptName,
			OverlappingRegionFinder<edu.mit.csail.cgs.datasets.species.Gene> geneFinder,
			OverlappingRegionFinder<StrandedRegion> segmentFinder,
			OverlappingRegionFinder<NamedStrandedRegion> transcriptFinder) throws IOException {

		WholeGenome wholeGenome = WholeGenome.loadWholeGenome(props, key); 
		wholeGenome.loadIterators();
		
		Integer[] fg = indexing.findChannels(strain, expt);
		Integer[] bg = indexing.findChannels(oppositeStrain, expt);
		
		Iterator<TranscriptCall> calls = wholeGenome.getTranscripts(exptName);

		while(calls.hasNext()) { 
			TranscriptCall call = calls.next();

			StrandedRegion segRegion = new StrandedRegion(
					genome, call.chrom, call.start, call.end, call.strand.charAt(0));
			String id = regionID(segRegion);
			addTranscribedNode(segmentFinder, geneFinder, transcriptFinder, id, segRegion);
		}
	}
	
	public void addSegments(
			OverlappingRegionFinder<edu.mit.csail.cgs.datasets.species.Gene> geneFinder,
			OverlappingRegionFinder<StrandedRegion> segmentFinder,
			OverlappingRegionFinder<NamedStrandedRegion> transcriptFinder) throws IOException {
		
		WholeGenome wholeGenome = WholeGenome.loadWholeGenome(props, key); 
		wholeGenome.loadIterators();
		
		Integer[] fg = indexing.findChannels(strain, expt);
		Integer[] bg = indexing.findChannels(oppositeStrain, expt);
		
		Iterator<DataSegment> segs = new SerialIterator<DataSegment>(
				wholeGenome.getWatsonSegments(), wholeGenome.getCrickSegments());

		while(segs.hasNext()) { 
			DataSegment seg = segs.next();
			
			//if(seg.hasConsistentType(Segment.LINE, fg)) { 
			if(seg.hasConsistentType(Segment.LINE, fg)) { 

				StrandedRegion segRegion = new StrandedRegion(genome, seg.chrom, seg.firstLocation(), seg.lastLocation(), seg.strand.charAt(0));
				String id = regionID(segRegion);
				addTranscribedNode(segmentFinder, geneFinder, transcriptFinder, id, segRegion);

				if(seg.isDifferential(expt)) { 
					
					int threeprime = seg.strand.equals("+") ? seg.lastLocation() : seg.firstLocation();
					double pred1 = seg.predicted(fg[0], threeprime);
					double pred2 = seg.predicted(bg[0], threeprime);
					
					if(pred1 >= pred2) { 
						diffSegments.put(id, 1);
						graphs.get("info").addEdge(id, "diff:up");
					} else { 
						diffSegments.put(id, -1);
						graphs.get("info").addEdge(id, "diff:down");
					}
					
				} else if (!seg.hasType(Segment.LINE, bg)) { 
					diffSegments.put(id, 1);
					graphs.get("info").addEdge(id, "diff:up");
					graphs.get("info").addEdge(id, "unique");
				}
			}
		}
	}
	
	public static String regionID(StrandedRegion reg) { 
		return String.format("%s:%c", reg.getLocationString(), reg.getStrand());
	}

	public static class SegmentGraphModel extends Model {
		
		public String species, genome;
		public String key, strain, expt;
		public String[] genes, segments, transcripts;
		public String[] diffUp, diffDown; 
		public String[] geneRegions, segmentRegions, transcriptRegions;
		
		public String[] graphNames; 
		public GraphModel[] graphs;
		
		public SegmentGraphModel() { 
		}
		
		public SegmentGraphModel(SegmentGraph g) {
			key = g.key;
			species = g.genome.getSpecies();
			genome = g.genome.getVersion();
			strain = g.strain;
			expt = g.expt;

			genes = g.genes.toArray(new String[0]);
			segments = g.segments.toArray(new String[0]);
			transcripts = g.transcripts.toArray(new String[0]);
			
			geneRegions = new String[genes.length];
			segmentRegions = new String[segments.length];
			transcriptRegions = new String[transcripts.length];
			
			for(int i = 0; i < geneRegions.length; i++) { 
				StrandedRegion r = g.nodes.get(genes[i]);
				geneRegions[i] = String.format(
						"%s:%d-%d:%c", 
						r.getChrom(), r.getStart(), r.getEnd(), r.getStrand());
			}
			for(int i = 0; i < segmentRegions.length; i++) { 
				StrandedRegion r = g.nodes.get(segments[i]);
				segmentRegions[i] = String.format(
						"%s:%d-%d:%c", 
						r.getChrom(), r.getStart(), r.getEnd(), r.getStrand());
			}
			for(int i = 0; i < transcriptRegions.length; i++) { 
				StrandedRegion r = g.nodes.get(transcripts[i]);
				transcriptRegions[i] = String.format(
						"%s:%d-%d:%c", 
						r.getChrom(), r.getStart(), r.getEnd(), r.getStrand());
			}
			
			graphNames = g.graphs.keySet().toArray(new String[0]); 
			graphs = new GraphModel[graphNames.length];
			for(int i = 0; i < graphNames.length; i++) { 
				graphs[i] = g.graphs.get(graphNames[i]).asModel();
			}
			
			int numUp = 0, numDown = 0;
			for(String id : g.diffSegments.keySet()) { 
				if(g.diffSegments.get(id) == 1) { 
					numUp += 1;
				} else if (g.diffSegments.get(id) == -1) { 
					numDown += 1;
				}
			}
			
			diffUp = new String[numUp];
			diffDown = new String[numDown];
			
			int kup = 0, kdown = 0;
			for(String id : g.diffSegments.keySet()) { 
				if(g.diffSegments.get(id) == 1) { 
					diffUp[kup++] = id;
				} else if (g.diffSegments.get(id) == -1) { 
					diffDown[kdown++] = id;
				}
			}
		}
	}

	private static Pattern strandedRegionPattern = 
		Pattern.compile("([^:]+):(\\d+)-(\\d+):([+-])");
	
	public static StrandedRegion parseStrandedRegion(Genome g, String rstr) { 
		Matcher m = strandedRegionPattern.matcher(rstr);
		if(!m.matches()) { throw new IllegalArgumentException(rstr); }
		String chrom = m.group(1);
		int start = Integer.parseInt(m.group(2));
		int end = Integer.parseInt(m.group(3));
		char strand = m.group(4).charAt(0);
		return new StrandedRegion(g, chrom, start, end, strand);
	}
	
}

class SortingComparator implements Comparator<StrandedRegion> { 
	private boolean direction;
	private int b; 
	
	public SortingComparator(boolean d) { direction = d; b = direction ? -1 : 1; }

	public int compare(StrandedRegion r1, StrandedRegion r2) {
		if(!r1.getChrom().equals(r2.getChrom())) { 
			return r1.getChrom().compareTo(r2.getChrom());
		}

		if(direction) { 
			if(r1.getStart() < r2.getStart()) { return b; }
			if(r1.getStart() > r2.getStart()) { return -b; }
			if(r1.getEnd() < r2.getEnd()) { return b; }
			if(r1.getEnd() > r2.getEnd()) { return -b; }
		} else { 
			if(r1.getEnd() < r2.getEnd()) { return b; }
			if(r1.getEnd() > r2.getEnd()) { return -b; }
			if(r1.getStart() < r2.getStart()) { return b; }
			if(r1.getStart() > r2.getStart()) { return -b; }
		}

		return 0;
	}
}

