package edu.mit.csail.cgs.sigma.expression.workflow;

import java.util.*;
import java.util.regex.*;
import java.io.*;

import edu.mit.csail.cgs.ewok.verbs.*;
import edu.mit.csail.cgs.ewok.verbs.sequence.RegionCoverage;
import edu.mit.csail.cgs.datasets.general.*;
import edu.mit.csail.cgs.datasets.species.*;
import edu.mit.csail.cgs.sigma.IteratorCacher;
import edu.mit.csail.cgs.sigma.SigmaProperties;
import edu.mit.csail.cgs.sigma.expression.StrandFilter;
import edu.mit.csail.cgs.sigma.expression.segmentation.Segment;
import edu.mit.csail.cgs.sigma.expression.workflow.models.*;
import edu.mit.csail.cgs.utils.Interval;
import edu.mit.csail.cgs.utils.NotFoundException;
import edu.mit.csail.cgs.utils.OverlapSum;
import edu.mit.csail.cgs.utils.Pair;
import edu.mit.csail.cgs.utils.UnitCoverage;
import edu.mit.csail.cgs.utils.iterators.EmptyIterator;
import edu.mit.csail.cgs.utils.iterators.SerialIterator;
import edu.mit.csail.cgs.utils.models.Model;

public class WholeGenome {
	
	public static void main(String[] args) {
		WorkflowProperties props = new WorkflowProperties();
		try {
			WholeGenome s288c = loadWholeGenome(props, "s288c");
			WholeGenome sigma = loadWholeGenome(props, "sigma");
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static WholeGenome createWholeGenome(WorkflowProperties ps, String key) 
		throws IOException { 
		
		WholeGenome wholeGenome = new WholeGenome(ps, key);

		wholeGenome.loadGenes();
		
		WorkflowIndexing indexing = ps.getIndexing(key);
		Set<String> expts = indexing.exptNames();
		
		for(String expt : expts) {
			wholeGenome.loadTranscription(expt);
			wholeGenome.loadAntisense(expt);
			wholeGenome.loadDivergentSites(expt);
			wholeGenome.loadDifferentialTranscription(expt);
			System.out.println(String.format("\tWholeGenome: added %s", expt));
		}
		
		System.out.println("Created WholeGenome : " + key);

		return wholeGenome;
	}
	
	public static WholeGenome loadWholeGenome(WorkflowProperties ps, String key) 
		throws IOException { 
		
		File f = new File(ps.getDirectory(), 
				String.format("%s-whole-genome.txt", key));
		if(f.exists()) {
			WholeGenome g = new WholeGenome(ps, f);
			System.out.println(String.format("Loaded WholeGenome: %s", key));
			return g;
		} else { 
			WholeGenome g = createWholeGenome(ps, key);
			g.save(f);
			return g;
		}
	}
	
	private IteratorCacher<DataSegment> wsegs, csegs;
	
	private WorkflowProperties props;
	private WorkflowIndexing indexing;
	private String workflowKey, strain;
	private Genome genome;
	private Map<String,RegionCoverage> watson, crick;
	
	public WholeGenome(WorkflowProperties ps, File f) throws IOException { 
		this(ps, Model.loadFromFile(WholeGenomeModel.class, f)); 
	}
	
	public WholeGenome(WorkflowProperties ps, String key) throws IOException { 
		props = ps;
		workflowKey = key;
		System.out.println(String.format("Workflow Key: %s", workflowKey));
		indexing = props.getIndexing(workflowKey);
		strain = props.parseStrainFromKey(workflowKey);
		genome = props.getSigmaProperties().getGenome(strain);
		
		watson = new TreeMap<String,RegionCoverage>();
		crick = new TreeMap<String,RegionCoverage>();

		loadIterators();
	}
	
	public WholeGenome(WorkflowProperties ps, WholeGenomeModel m) { 
		props = ps;
		workflowKey = m.key;
		System.out.println(String.format("Workflow Key: %s", workflowKey));
		indexing = props.getIndexing(workflowKey);
		strain = m.strain;
		genome = m.getGenome();

		watson = new TreeMap<String,RegionCoverage>();
		crick = new TreeMap<String,RegionCoverage>();

		for(int i = 0; i < m.keys.length; i++) {
			RegionCoverage wrc = new RegionCoverage(genome, m.watson[i]);
			RegionCoverage crc = new RegionCoverage(genome, m.crick[i]);
			
			watson.put(m.keys[i], wrc);
			crick.put(m.keys[i], crc);
		}
	}
	
	public WholeGenome(WholeGenome g) {
		workflowKey = g.workflowKey;
		props = g.props;
		indexing = g.indexing;
		strain = g.strain;
		genome = g.genome;
		watson = new TreeMap<String,RegionCoverage>();
		crick = new TreeMap<String,RegionCoverage>();
		wsegs = g.wsegs;
		csegs = g.csegs;
		
		for(String k : g.watson.keySet()) { 
			watson.put(k, new RegionCoverage(genome, g.watson.get(k)));
		}
		for(String k : g.crick.keySet()) { 
			crick.put(k, new RegionCoverage(genome, g.crick.get(k)));
		}
	}
	
	public String getStrain() { return strain; }
	
	public void printTrack(File f, String key) throws IOException { 
		PrintStream ps = new PrintStream(new FileOutputStream(f)); 
		printTrack(ps, key);
		ps.close();
	}
	
	public void printTrack(PrintStream ps, String key) { 
		Iterator<Region> regs = watson.get(key).regions();
		while(regs.hasNext()) { 
			Region reg = regs.next();
			ps.println(String.format("%s:%d-%d:%c", reg.getChrom(), reg.getStart(), reg.getEnd(), '+'));
		}
		regs = crick.get(key).regions();
		while(regs.hasNext()) { 
			Region reg = regs.next();
			ps.println(String.format("%s:%d-%d:%c", reg.getChrom(), reg.getStart(), reg.getEnd(), '-'));
		}
	}
	
	/**
	 * Returns pairs of StrandedRegion objects, which are "nearest" to each other 
	 * in the sense of either: 
	 * 	a) they overlap
	 *  b) they are nearest-neighbors
	 * 
	 * The first element of each pair is always from the Watson strand -- the second 
	 * element is either from Watson or Crick depending on whether the 'sameStrand' 
	 * parameter is either true or false. 
	 * 
	 * No two non-overlapping pairs are more than 'maxDist' apart from their nearest edges.
	 * 
	 * The parameter 'rightPairs' determines the direction of the match -- if rightPairs
	 * is true, then the first region in each pair is matched with the nearest neighbor
	 * of equal or higher coordinate.  If 'rightPairs' is false, then the first region is
	 * matched with the nearest neighbor of lower coordinate. 
	 * 
	 * @param k1
	 * @param k2
	 * @param maxDist
	 * @param rightPairs
	 * @param sameStrand
	 * @return
	 */
	public Collection<Pair<StrandedRegion,StrandedRegion>> 
		findPairs(String k1, String k2, int maxDist, boolean rightPairs, boolean sameStrand) {
		
		Collection<Pair<Region,Region>> regs = null;
		if(sameStrand) { 
			regs = watson.get(k1).findPairs(maxDist, watson.get(k2), rightPairs); 			
		} else { 
			regs = watson.get(k1).findPairs(maxDist, crick.get(k2), rightPairs);
		}
		
		ArrayList<Pair<StrandedRegion,StrandedRegion>> strpairs = 
			new ArrayList<Pair<StrandedRegion,StrandedRegion>>();
		
		for(Pair<Region,Region> p : regs) { 
			Region r1 = p.getFirst(), r2 = p.getLast();
			StrandedRegion s1 = new StrandedRegion(r1, '+');
			StrandedRegion s2 = new StrandedRegion(r2, sameStrand ? '+' : '-');
			strpairs.add(new Pair<StrandedRegion,StrandedRegion>(s1, s2));
		}
		
		return strpairs;
	}
	
	public StrandedRegion findNearestRight(String key, StrandedPoint p) {
		Region r = null;
		if(p.getStrand() == '+') { 
			r = watson.containsKey(key) ? watson.get(key).findNearestRight(p) : null;
		} else { 
			r = crick.containsKey(key) ? crick.get(key).findNearestRight(p) : null;
		}
		if(r != null) { 
			return new StrandedRegion(r, p.getStrand());
		} else { 
			return null;
		}
	}
	
	public StrandedRegion findNearestLeft(String key, StrandedPoint p) {
		Region r = null;
		if(p.getStrand() == '+') { 
			r = watson.containsKey(key) ? watson.get(key).findNearestLeft(p) : null;
		} else { 
			r = crick.containsKey(key) ? crick.get(key).findNearestLeft(p) : null;
		}
		if(r != null) { 
			return new StrandedRegion(r, p.getStrand());
		} else { 
			return null;
		}
	}
	
	public void save() throws IOException { 
		save(new File(props.getDirectory(), String.format("%s-whole-genome.txt", strain)));
	}
	
	public void save(File f) throws IOException { 
		Model.writeToFile(asModel(), f);
	}
	
	public Iterator<DataSegment> getWatsonSegments() { return wsegs.iterator(); }
	public Iterator<DataSegment> getCrickSegments() { return csegs.iterator(); }

    public Iterator<DataSegment> getAllSegments() { 
        return new SerialIterator<DataSegment>(getWatsonSegments(),
                                               getCrickSegments());
    }

    public Iterator<DataSegment> getTranscribedSegments(String expt) { 
        return new FilterIterator<DataSegment,DataSegment>(new TranscribedSegmentFilter(indexing.findChannels(strain, expt)),
                                                           getAllSegments());
    }

    public static class TranscribedSegmentFilter implements Filter<DataSegment,DataSegment> { 
        private Integer[] channels;
        public TranscribedSegmentFilter(Integer[] chs) { 
            channels = chs.clone();
        }

        public DataSegment execute(DataSegment a) { 
            return a.hasConsistentType(Segment.LINE, channels) ? a : null;
        }
    }
	
	public Iterator<TranscriptCall> getTranscripts(String exptName) throws IOException { 
		return new SerialIterator<TranscriptCall>(loadWatsonCalls(exptName), loadCrickCalls(exptName));
	}
	
	public Iterator<TranscriptCall> loadWatsonCalls(String exptName) throws IOException { 
		String fname = String.format("%s_plus.%s.transcripts", workflowKey, exptName);
		File f= new File(props.getDirectory(), fname);
		return new WorkflowTranscriptReader(f);
	}
	
	public Iterator<TranscriptCall> loadCrickCalls(String exptName) throws IOException { 
		String fname = String.format("%s_negative.%s.transcripts", workflowKey, exptName);
		File f= new File(props.getDirectory(), fname);
		return new WorkflowTranscriptReader(f);
	}
	
	public void loadIterators(File w, File c) throws IOException { 
		if(w.exists()) { 
			WorkflowDataSegmentReader wreader = 
				new WorkflowDataSegmentReader(w);
			wsegs = new IteratorCacher<DataSegment>(wreader);
			wreader.close();
		} else { 
			wsegs = new IteratorCacher<DataSegment>(new EmptyIterator<DataSegment>());
		}
		if(c.exists()) { 
			WorkflowDataSegmentReader creader = 
				new WorkflowDataSegmentReader(c);
			csegs = new IteratorCacher<DataSegment>(creader);
			creader.close();
		} else { 
			csegs = new IteratorCacher<DataSegment>(new EmptyIterator<DataSegment>());
		}
	}

	public void loadIterators() throws IOException { 
		if(wsegs == null || csegs == null) { 
			WorkflowDataSegmentReader wreader = transcriptionReader(workflowKey, "plus"); 
			WorkflowDataSegmentReader creader = transcriptionReader(workflowKey, "negative"); 

			wsegs = new IteratorCacher<DataSegment>(wreader);
			System.out.println("\tLoaded watson segments...");

			csegs = new IteratorCacher<DataSegment>(creader);
			System.out.println("\tLoaded crick segments...");

			wreader.close();
			creader.close();
		}
	}

	private WorkflowDataSegmentReader transcriptionReader(String k, String strand) 
	throws IOException { 
		File segs = new File(props.getDirectory(), 
				String.format("%s_%s.datasegs", k, strand));

		WorkflowDataSegmentReader reader = new WorkflowDataSegmentReader(segs);
		return reader;
	}

	public void jointCoverage(String k1, String k2, String newKey) {
		jointCoverage(k1, k2, newKey, true);
	}
	
	public void jointCoverage(String k1, String k2, String newKey, boolean sameStrand) {
		checkKeys(k1, k2, newKey);
		watson.put(newKey, watson.get(k1).intersection(
				sameStrand ? watson.get(k2) : crick.get(k2)));
		crick.put(newKey, crick.get(k1).intersection(
				sameStrand ? crick.get(k2) : watson.get(k2)));
	}
	
	public void subtractCoverage(String k1, String k2, String newKey) {
		subtractCoverage(k1, k2, newKey, true);
	}
	
	public void subtractCoverage(String k1, String k2, String newKey, boolean sameStrand) {
		checkKeys(k1, k2, newKey);
		watson.put(newKey, watson.get(k1).subtract(
				sameStrand ? watson.get(k2) : crick.get(k2)));
		crick.put(newKey, crick.get(k1).subtract(
				sameStrand ? crick.get(k2) : watson.get(k2)));
	}
	
	public void eitherCoverage(String k1, String k2, String newKey) {
		eitherCoverage(k1, k2, newKey, true);
	}
	
	public void eitherCoverage(String k1, String k2, String newKey, boolean sameStrand) {
		checkKeys(k1, k2, newKey);
		watson.put(newKey, watson.get(k1).union(
				sameStrand ? watson.get(k2) : crick.get(k2)));
		crick.put(newKey, crick.get(k1).union(
				sameStrand ? crick.get(k2) : watson.get(k2)));
	}

	public boolean checkKeys(String k1, String k2, String newKey) { 
		if(!watson.containsKey(k1) || !crick.containsKey(k1)) { 
			throw new IllegalArgumentException(k1);
		}
		if(!watson.containsKey(k2) || !crick.containsKey(k2)) { 
			throw new IllegalArgumentException(k2);
		}		
		return true;
	}

	public Genome getGenome() { return genome; }
	
	public Set<String> findOverlapping(StrandedRegion r) { 
		TreeSet<String> keys = new TreeSet<String>();
		if(r.getStrand() == '+') { 
			for(String k : watson.keySet()) { 
				if(watson.get(k).hasCoverage(r)) { 
					keys.add(k);
				}
			}
		} else { 
			for(String k : crick.keySet()) { 
				if(crick.get(k).hasCoverage(r)) { 
					keys.add(k);
				}
			}
		}		
		return keys;
	}
	
	public boolean hasOverlapping(String key, StrandedRegion r) { 
		if(r.getStrand() == '+') { 
			if(!watson.containsKey(key)) { 
				throw new IllegalArgumentException(String.format("%s not in %s", 
						key, watson.keySet().toString()));
			}
			return watson.get(key).hasCoverage(r);
		} else { 
			return crick.get(key).hasCoverage(r);
		}
	}

	public void printSummary() { 
		printSummary(System.out);
	}
	
	public void printSummary(PrintStream ps) { 
		ps.println(String.format("Whole Genome: %s (%s)", workflowKey, genome.getVersion()));
		for(String k : watson.keySet()) { 
			ps.println(String.format(
					"\t%s + : %d regions, %d bp", 
					k, watson.get(k).size(), watson.get(k).area()));
		}
		for(String k : crick.keySet()) { 
			ps.println(String.format(
					"\t%s - : %d regions, %d bp", 
					k, crick.get(k).size(), crick.get(k).area()));
		}
	}
	
	public void loadDifferentialTranscription(String celltype) { 
		loadTranscription(celltype);
		String key = String.format("diff:%s", celltype);
		System.out.println(String.format("%s -> %s", key, workflowKey));
		if(!watson.containsKey(key)) { 
			watson.put(key, 
					parseDiffSegments(wsegs.iterator(), 
							indexing.findChannels(strain, celltype), celltype));
		}
		
		if(!crick.containsKey(key)) { 
			crick.put(key, 
					parseDiffSegments(csegs.iterator(), 
							indexing.findChannels(strain, celltype), celltype));
		}
	}

	public void loadDivergentSites(String celltype) {
		loadTranscription(celltype);

		System.out.println(String.format("%s divergent -> %s", celltype, workflowKey));
		
		String key = String.format("div:%s", celltype);
		
		RegionCoverage wtrans = watson.get(celltype);
		RegionCoverage ctrans = crick.get(celltype);
		
		RegionCoverage div = new RegionCoverage(genome);
		
		Iterator<Region> wregs = wtrans.regions();
		while(wregs.hasNext()) {
			Region wreg = wregs.next();
			Point p = new Point(genome, wreg.getChrom(), wreg.getStart());
			Region creg = ctrans.leftNearest(p);
			
			if(creg != null && creg.getEnd() < wreg.getStart()-1) { 
				Region divreg = 
					new Region(genome, wreg.getChrom(), creg.getEnd()+1, wreg.getStart()-1);
				if(!wtrans.hasCoverage(divreg)) { 
					div.addRegion(divreg);
				}
			}
		}
		
		watson.put(key, div);
		crick.put(key, div);
	}
	
	public void loadAntisense(String celltype) throws IOException { 
		loadTranscription(celltype);
		loadGenes();
		
		System.out.println(String.format("%s antisense -> %s", celltype, workflowKey));
		
		String key = String.format("anti:%s", celltype);
		String geneKey = "genes";
		
		watson.put(key, watson.get(celltype).intersection(crick.get(geneKey)));
		crick.put(key, crick.get(celltype).intersection(watson.get(geneKey)));
	}
	
	private static Pattern antiPattern = Pattern.compile("anti:(.*)");
	private static Pattern divPattern = Pattern.compile("div:(.*)");
	private static Pattern diffPattern = Pattern.compile("diff:(.*)");
	
	public void load(String key) throws IOException {
		Matcher antim = antiPattern.matcher(key);
		Matcher divm = divPattern.matcher(key);
		Matcher diffm = diffPattern.matcher(key);
		
		if(key.equals("genes")) { 
			loadGenes();
			
		} else if (antim.matches()) { 
			loadAntisense(antim.group(1));
			
		} else if (divm.matches()) { 
			loadDivergentSites(divm.group(1));
			
		} else if (diffm.matches()) { 
			loadDifferentialTranscription(diffm.group(1));
			
		} else { 
			loadTranscription(key);
		}
	}

	public void loadGenes() {
		System.out.println(String.format("genes -> %s", workflowKey));
		String key = "genes";
		if(!watson.containsKey(key)) { 
			watson.put(key, parseGenes('+'));
		}
		if(!crick.containsKey(key)) { 
			crick.put(key, parseGenes('-'));
		}
	}
	
	public void loadTranscription(String celltype) {
		
		System.out.println(String.format("%s transcription -> %s", celltype, workflowKey));
		
		if(!watson.containsKey(celltype)) {
			
			if(wsegs == null) { 
				try {
					loadIterators();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			watson.put(celltype, 
					parseSegments(wsegs.iterator(), 
							indexing.findChannels(strain, celltype)));
		}
		
		if(!crick.containsKey(celltype)) {
			
			if(csegs == null) { 
				try {
					loadIterators();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			crick.put(celltype, 
					parseSegments(csegs.iterator(), 
							indexing.findChannels(strain, celltype)));
		}
	}
	
	public void loadRegions(String key, Iterator<StrandedRegion> rs) {
		RegionCoverage w = new RegionCoverage(genome);
		RegionCoverage c = new RegionCoverage(genome);
		while(rs.hasNext()) { 
			StrandedRegion r = rs.next();
			if(r.getStrand() == '+') { 
				w.addRegion(r);
			} else { 
				c.addRegion(r);
			}
		}
		
		watson.put(key, w);
		crick.put(key, c);
	}
	
	private RegionCoverage parseGenes(char strand) { 
		Iterator<Region> itr = new MapperIterator<NamedRegion,Region>(new CastingMapper<NamedRegion,Region>(), 
				new ChromRegionIterator(genome));
		Iterator<Gene> genes = new ExpanderIterator<Region,Gene>(props.getSigmaProperties().getGeneGenerator(strain), itr);
		genes = new FilterIterator<Gene,Gene>(new StrandFilter<Gene>(strand), genes);
		return new RegionCoverage(genome, genes);
	}
	
	private RegionCoverage parseSegments(Iterator<DataSegment> segs, Integer[] channels) {
		RegionCoverage coverage = new RegionCoverage(genome);
		while(segs.hasNext()) { 
			DataSegment seg = segs.next();
			String chrom = seg.chrom;
			if(seg.hasConsistentType(Segment.LINE, channels)) { 
				coverage.addInterval(chrom, seg.start, seg.end);
			}
		}
		return coverage;
	}
	
	private RegionCoverage parseDiffSegments(Iterator<DataSegment> segs, Integer[] channels, String k) { 
		RegionCoverage coverage = new RegionCoverage(genome);
		while(segs.hasNext()) { 
			DataSegment seg = segs.next();
			String chrom = seg.chrom;
			if(seg.hasConsistentType(Segment.LINE, channels) && 
				seg.isDifferential(k)) { 
				coverage.addInterval(chrom, seg.start, seg.end);
			}
		}
		return coverage;		
	}

	/**
	 * This is just a test method, for double-checking that two different ways
	 * of calculating the "same thing" (for transcription) match up.  
	 */
	void compareChromTranscription(String celltype, String... chroms) throws IOException { 

		WorkflowDataSegmentReader reader = transcriptionReader(workflowKey, "plus");
		ArrayList<DataSegment> segs = new ArrayList<DataSegment>();
		while(reader.hasNext()) { segs.add(reader.next()); }
		reader.close();
		reader = null;

		for(String chrom : chroms) { 
			Iterator<DataSegment> itr = new FilterIterator<DataSegment,DataSegment>(
					new DSChromFilter(chrom), segs.iterator());
			UnitCoverage cov = new UnitCoverage();
			while(itr.hasNext()) { 
				DataSegment seg = itr.next();
				if(seg.hasConsistentType(Segment.LINE, 
						indexing.findChannels(strain, celltype))) { 
					cov.addInterval(seg.start, seg.end);
				}
			}

			reader = transcriptionReader(workflowKey, "plus");
			itr = new FilterIterator<DataSegment,DataSegment>(
					new DSChromFilter(chrom), segs.iterator());

			//RunningOverlapSum sum = new RunningOverlapSum(genome, chrom);
			OverlapSum sum = new OverlapSum();
			while(itr.hasNext()) { 
				DataSegment seg = itr.next();
				if(seg.hasConsistentType(Segment.LINE, 
						indexing.findChannels(strain, celltype))) {
					sum.addInterval(seg.start, seg.end);
				}
			}

			System.out.println(String.format("Chrom %s Mismatched Coverage: ", chrom));
			Collection<Interval> failed = cov.compareToOverlapSum(sum, 1);
			for(Interval intv : failed) { 
				System.out.println(String.format("\t%s,%s", intv.start, intv.end));
			}

			System.out.println(String.format("# Failing: %d", failed.size()));
		}
	}

	public WholeGenomeModel asModel() { return new WholeGenomeModel(this); }
	
	public static class WholeGenomeModel extends Model { 
		
		public String key;
		public String[] keys;
		public RegionCoverage.RegionCoverageModel[] watson, crick;
		public String species, version, strain;
		
		public WholeGenomeModel() {}
		
		public WholeGenomeModel(WholeGenome g) {
			this.key = g.workflowKey;
			this.strain = g.strain;
			species = g.genome.getSpecies();
			version = g.genome.getVersion();
			keys = g.watson.keySet().toArray(new String[0]);
			this.watson = new RegionCoverage.RegionCoverageModel[keys.length];
			this.crick = new RegionCoverage.RegionCoverageModel[keys.length];
			
			for(int i = 0; i < keys.length; i++) { 
				this.watson[i] = g.watson.get(keys[i]).asModel(); 
				this.crick[i] = g.crick.get(keys[i]).asModel(); 
			}
		}
		
		public Genome getGenome() { 
			try {
				SigmaProperties props = new SigmaProperties();
				Genome g = props.getGenome(strain);
				if(g != null) { 
					return g; 
				} else { 
					Organism org = Organism.getOrganism(species);
					return org.getGenome(version);
				}
			} catch (NotFoundException e) {
				e.printStackTrace();
				return null;
			}
		}
	}

	public int coverage(String key, StrandedRegion r) {
		if(r.getStrand() == '+') { 
			return watson.containsKey(key) ? watson.get(key).coverage(r) : 0;
		} else { 
			return crick.containsKey(key) ? crick.get(key).coverage(r) : 0;
		}
	}

	public boolean isContained(String key, StrandedRegion r) {
		return r.getStrand() == '+' ? 
				watson.get(key).isContained(r) :
				crick.get(key).isContained(r);
	}

	public WorkflowProperties getProperties() {
		return props;
	}

	public String getKey() {
		return workflowKey;
	}
}

class DSChromFilter implements Filter<DataSegment,DataSegment> {
	private String chrom;
	public DSChromFilter(String c) { 
		chrom = c;
	}
	public DataSegment execute(DataSegment s) { 
		return s.chrom.equals(chrom) ? s : null;
	}
}

class MapExpander<X,Y> implements Expander<X,Y> {
	
	private Map<X, ? extends Collection<Y>> map;
	
	public MapExpander(Map<X,? extends Collection<Y>> m) { 
		map = m;
	}
	
	public Iterator<Y> execute(X key) { 
		return map.containsKey(key) ? 
				map.get(key).iterator() : 
				new EmptyIterator<Y>();
	}
}
