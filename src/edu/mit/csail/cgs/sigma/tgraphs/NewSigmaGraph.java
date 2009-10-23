/*
 * Author: tdanford
 * Date: May 20, 2009
 */
package edu.mit.csail.cgs.sigma.tgraphs;

import java.io.IOException;
import java.io.PrintStream;
import java.util.*;

import edu.mit.csail.cgs.datasets.general.Region;
import edu.mit.csail.cgs.datasets.species.Gene;
import edu.mit.csail.cgs.ewok.verbs.Expander;
import edu.mit.csail.cgs.ewok.verbs.GenomeExpander;
import edu.mit.csail.cgs.sigma.OverlappingRegionKeyFinder;
import edu.mit.csail.cgs.sigma.expression.workflow.WholeGenome;
import edu.mit.csail.cgs.sigma.expression.workflow.WorkflowIndexing;
import edu.mit.csail.cgs.sigma.expression.workflow.WorkflowProperties;
import edu.mit.csail.cgs.sigma.expression.workflow.models.DataSegment;
import edu.mit.csail.cgs.sigma.expression.workflow.models.RegionKey;
import edu.mit.csail.cgs.sigma.expression.workflow.models.TranscriptCall;
import edu.mit.csail.cgs.utils.SetTools;
import edu.mit.csail.cgs.utils.graphs.DirectedGraph;
import edu.mit.csail.cgs.utils.iterators.EmptyIterator;
import edu.mit.csail.cgs.utils.iterators.SerialIterator;

public class NewSigmaGraph implements SigmaGraph {

	public static void main(String[] args) { 
		String key = "txns288c";
		String expt = "matalpha";

		NewSigmaGraph graph = loadGraph(key, expt);
		graph.printSummary();
	}
	
	public static NewSigmaGraph loadGraph(String key, String expt) {
		
		WorkflowProperties props = new WorkflowProperties();
		String strain = props.parseStrainFromKey(key);
		
		System.out.println("Building graph...");
		try { 
			WholeGenome genome = WholeGenome.loadWholeGenome(props, key);
			return loadGraph(genome, expt);
			
		} catch(IOException e) { 
			e.printStackTrace(System.err);
			return null;
		}
	}
	
	public static NewSigmaGraph loadGraph(WholeGenome genome, String expt) throws IOException { 
		NewSigmaGraph graph = new NewSigmaGraph();
		System.out.println("Building graph...");
		genome.loadIterators();
		graph.load(genome, expt);
			
		return graph;
	}

	public static SetTools<String> tolls = new SetTools<String>();

	private OverlappingRegionKeyFinder<DataSegment> segmentFinder;
	private OverlappingRegionKeyFinder<GeneKey> geneFinder;
	private OverlappingRegionKeyFinder<TranscriptCall> transcriptFinder;
	
	private Map<String,DataSegment> segments;
	private Map<String,GeneKey> genes;
	private Map<String,TranscriptCall> transcripts;
	private Set<String> attrs, species;
	private Map<String,DirectedGraph> edges;  
	
	public NewSigmaGraph() {
		segmentFinder = new OverlappingRegionKeyFinder<DataSegment>();
		geneFinder = new OverlappingRegionKeyFinder<GeneKey>();
		transcriptFinder = new OverlappingRegionKeyFinder<TranscriptCall>();
		
		segments = new TreeMap<String,DataSegment>();
		transcripts = new TreeMap<String,TranscriptCall>();
		genes = new TreeMap<String,GeneKey>();
		attrs = new TreeSet<String>();
		species = new TreeSet<String>();
		
		edges = new TreeMap<String,DirectedGraph>();
		
		species.add("s288c");
		species.add("sigma");

		edges.put("is", new DirectedGraph());
		edges.put("in", new DirectedGraph());
	}

	public Iterator<GeneKey> findGenes(RegionKey query) {
		return geneFinder.findOverlapping(query).iterator();
	}

	public Iterator<DataSegment> findSegments(RegionKey query) {
		return segmentFinder.findOverlapping(query).iterator();
	}

	public Iterator<TranscriptCall> findTranscripts(RegionKey query) {
		return transcriptFinder.findOverlapping(query).iterator();
	}

	public GeneKey lookupGene(String name) {
		return genes.get(name);
	}

	public DataSegment lookupSegment(String name) {
		return segments.get(name);
	}

	public TranscriptCall lookupTranscript(String name) {
		return transcripts.get(name);
	}
	
	public void printSummary() { 
		printSummary(System.out);
	}
	
	public void printSummary(PrintStream ps) { 
		Iterator<String> ns = allNodes();
		String[] tags = edgeTags();
		while(ns.hasNext()) { 
			String node = ns.next();
			ps.println(String.format("%s", node));
			for(String tag : tags) { 
				Iterator<String> nbs = forward(node, tag);
				while(nbs.hasNext()) { 
					String nb = nbs.next();
					ps.println(String.format("\t%s -> %s", tag, nb));
				}
			}
		}
	}
	
	public Iterator<String> allNodes() {
		return new SerialIterator<String>(
				genes.keySet().iterator(),
				segments.keySet().iterator(),
				species.iterator(),
				attrs.iterator());
	}
	
	public GeneKey findGene(String nodeID) { 
		return genes.get(nodeID);
	}

	public DataSegment findSegment(String nodeID) { 
		return segments.get(nodeID);
	}
	
	public TranscriptCall findTranscript(String nodeID) { 
		return transcripts.get(nodeID);
	}

	public String[] edgeTags() {
		return edges.keySet().toArray(new String[0]);
	}

	public Iterator<String> forward(String node, String edgeTag) {
		if(edges.containsKey(edgeTag) && edges.get(edgeTag).containsVertex(node)) {  
			DirectedGraph g = edges.get(edgeTag);
			Collection<String> nbs = g.getNeighbors(node);
			return nbs.iterator();
		} else { 
			return new EmptyIterator<String>();
		}
	}

	public boolean hasEdge(String n1, String n2, String tag) {
		return edges.containsKey(tag) && edges.get(tag).containsEdge(n1, n2);
	}

	public boolean hasNode(String n) {
		return genes.containsKey(n) || 
			segments.containsKey(n) || 
			transcripts.containsKey(n) ||
			species.contains(n) || 
			attrs.contains(n);
	}

	public boolean hasNode(String n, String type) {
		if(type.equals("transcript")) { 
			return transcripts.containsKey(n);
		
		} else if (type.equals("segment")) { 
			return segments.containsKey(n);

		} else if (type.equals("gene")) {
			return genes.containsKey(n);
			
		} else if (type.equals("attr")) { 
			return attrs.contains(n);
			
		} else if (type.equals("species")) { 
			return species.contains(n);		
			
		} else { 
			return false;
		}
	}

	public String[] nodeTypes() {
		return new String[] { "transcript", "segment", "gene", "attr", "species" };
	}

	public Iterator<String> reverse(String node, String edgeTag) {
		if(edges.containsKey(edgeTag) && edges.get(edgeTag).containsVertex(node)) {  
			return edges.get(edgeTag).getParents(node).iterator();
		} else { 
			return new EmptyIterator<String>();
		}
	}

	public Iterator<String> typedNodes(String type) {
		if(type.equals("transcript")) { 
			return transcripts.keySet().iterator();
		
		} else if (type.equals("segment")) { 
			return segments.keySet().iterator();

		} else if (type.equals("gene")) {
			return genes.keySet().iterator();
			
		} else if (type.equals("attr")) { 
			return attrs.iterator();
			
		} else if (type.equals("species")) { 
			return species.iterator();
			
		} else { 
			return new EmptyIterator<String>();
		}
	}

	// -- Data Adding Methods -----------------------------------------
	
	public void load(WholeGenome g, String expt) throws IOException {
		
		String key = g.getKey();
		WorkflowProperties props = g.getProperties();
		WorkflowIndexing indexing = props.getIndexing(key);
		
		g.loadIterators();
		String strain = props.parseStrainFromKey(key);
		addGenes(props, strain);
		addSegments(indexing, g.getTranscribedSegments(expt), strain, expt);
	}
	
	public void addGenes(WorkflowProperties props, String strain) { 
		Expander<Region,edu.mit.csail.cgs.datasets.species.Gene> genegen = 
			props.getSigmaProperties().getGeneGenerator(strain);
		GenomeExpander<edu.mit.csail.cgs.datasets.species.Gene> exp = 
			new GenomeExpander<edu.mit.csail.cgs.datasets.species.Gene>(genegen);
		Iterator<edu.mit.csail.cgs.datasets.species.Gene> itr = 
			exp.execute(props.getSigmaProperties().getGenome(strain));
		
		attrs.add("+");
		attrs.add("-");
		
		int c = 0;
		while(itr.hasNext()) { 
			addGene(itr.next(), strain);
			c += 1;
		}
		
		System.out.println(String.format("SigmaGraph -- added %d genes (strain: %s)", c, strain));
	}
	
	public void addGene(edu.mit.csail.cgs.datasets.species.Gene gene, String strain) {
		GeneKey geneKey = new GeneKey(gene);
		String id = geneKey.toString();
		
		genes.put(id, geneKey);
		
		addEdge(id, "in", strain);
		addEdge(id, "strand", geneKey.strand);
		
		geneFinder.addRegion(geneKey);
	}

	public void addSegments(WorkflowIndexing indexing, Iterator<DataSegment> segs, String strain, String expt) {
		
		Map<String,Integer[]> channels = new TreeMap<String,Integer[]>();
		for(String sp : species) { 
			channels.put(sp, indexing.findChannels(sp, expt));
		}
		int lineType = edu.mit.csail.cgs.sigma.expression.segmentation.Segment.LINE;
		Integer[] chs = channels.get(strain);
		
		String diffup = "diff:up";
		String diffdown = "diff:down";
		
		attrs.add(diffup);
		attrs.add(diffdown);
		
		int tandemDistance = 50;
		
		while(segs.hasNext()) { 
			DataSegment s = segs.next();
			if(s.hasConsistentType(lineType, chs)) {
				DataSegment skey = s;
				String sname = skey.toString();
				segments.put(sname, skey);
				
				addEdge(sname, "in", strain);
				addEdge(sname, "strand", s.strand);
				
				if(s.isDifferential(expt)) {
					if(s.getExpectedDifferential(expt) >= 0.0) { 
						addEdge(sname, "is", diffup);
					} else { 
						addEdge(sname, "is", diffdown);
					}
				}
				
				Collection<DataSegment> overs = 
					segmentFinder.findOverlapping(skey);
				
				for(DataSegment over : overs) { 
					addDoubleEdge(over.toString(), "overlaps", sname);
				}
				
				RegionKey left = new RegionKey(s.chrom, s.start-tandemDistance-1, s.start-1, s.strand);
				RegionKey right = new RegionKey(s.chrom, s.end+1, s.end+tandemDistance+1, s.strand);
				
				for(DataSegment leftNear : segmentFinder.findOverlapping(left)) { 
					if(leftNear.strand.equals(s.strand)) { 
						addDoubleEdge(sname, "tandem", leftNear.toString());
					}
				}
				
				for(DataSegment rightNear : segmentFinder.findOverlapping(right)) { 
					if(rightNear.strand.equals(s.strand)) { 
						addDoubleEdge(sname, "tandem", rightNear.toString());
					}
				}
				
				for(GeneKey geneOver : geneFinder.findOverlapping(skey)) {
					String geo = geneOver.toString();
					if(geneOver.strand.equals(skey.strand)) { 
						addEdge(sname, "sense", geo);
					} else { 
						addEdge(sname, "antisense", geo);
					}
				}
				
				segmentFinder.addRegion(skey);
			}
		}
	}
	
	private void addDoubleEdge(String n1, String tag, String n2) { 
		addEdge(n1, tag, n2);
		addEdge(n2, tag, n1);
	}
	
	private void addEdge(String n1, String tag, String n2) { 
		if(!edges.containsKey(tag)) { 
			edges.put(tag, new DirectedGraph());
		}
		DirectedGraph g = edges.get(tag);
		if(!g.containsVertex(n1)) { g.addVertex(n1); }
		if(!g.containsVertex(n2)) { g.addVertex(n2); }
		g.addEdge(n1, n2);
	}

	// ----------------------------------------------------------------

}

