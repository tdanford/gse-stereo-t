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

public class OverlapClassifier extends Model {
	
	public static int minSize = 400;
	
	public static void main(String[] args) { 
		String key = args.length > 0 ? args[0] : "s288c";
		WorkflowProperties props = new WorkflowProperties();
		File base = new File("C:\\Documents and Settings\\tdanford\\Desktop");
		File output = new File(base, "transcript-classifications.txt");
		
		try {
			OverlapClassifier classifier = null;
			if(output.exists()) { 
				classifier = new OverlapClassifier(output);
			} else { 
				classifier = new OverlapClassifier(props, key);
				classifier.save(output);
			}
			
			int[] tandem = new int[] { 0, 0 };
			int[] convergent = new int[] { 0, 0 };
			int[] divergent = new int[] { 0, 0 };
			
			int count = 0;
			for(int i = 0; i < classifier.classifications.length; i++) { 
				Classification c = classifier.classifications[i];
				if(c.overlaps) { 
					if(c.isContainedCandidate()) { 
						System.out.println(String.format("CNTD %s:%d-%d:%s\tA: %.2f\tB: %.2f\tC: %.2f", 
								c.chrom, c.start, c.end, c.strand, 
								c.aValue, c.bValueSecond, c.cValue));
						count += 1;

					} else if (c.isAdditiveCandidate() && c.additiveScore() < 2.0) {
						System.out.println(String.format("ADD  %s:%d-%d:%s\tscore: %.3f", 
								c.chrom, c.start, c.end, c.strand, 
								c.additiveScore()));

					}
					if(c.isMeasured() && c.sameStrand && c.contained) { 
						if(c.bValueSecond > c.aValue && c.bValueSecond > c.cValue) { 
							count += 1;
						}
					}
				} 
				
				int overIdx = c.overlaps ? 1 : 0;
				
				if(c.isDivergent()) { 
					divergent[overIdx] += 1;
				}
					
				if(c.isConvergent()) { 
					convergent[overIdx] += 1;
				}
					
				if(c.isTandem()) { 
					tandem[overIdx] += 1;
				}
			}
			
			System.out.println(String.format("# Classified: %d", count));
			System.out.println(String.format("Classifications: %d", classifier.classifications.length));
			
			System.out.println(String.format("           \tA:\tO:"));
			System.out.println(String.format("Divergent :\t%d\t%d", divergent[0], divergent[1]));
			System.out.println(String.format("Convergent:\t%d\t%d", convergent[0], convergent[1]));
			System.out.println(String.format("Tandem    :\t%d\t%d", tandem[0], tandem[1]));
			
		} catch(IOException e) { 
			e.printStackTrace(System.err);
		}
	}
		
	public TranscriptCall[] calls;
	public Classification[] classifications;
	
	public OverlapClassifier() {}
	
	public OverlapClassifier(File f) throws IOException { 
		load(f);
	}
	
	public OverlapClassifier(WorkflowProperties props, String key) throws IOException {
		String expt = "matalpha";
		SteinmetzAverager averager = new SteinmetzAverager();
		LinkedList<Classification> clssList = new LinkedList<Classification>();
		Genome g = null;
		try {
			g = Organism.findGenome("SGDv1");
		} catch (NotFoundException e) {
			e.printStackTrace();
			throw new IllegalArgumentException("SGDv1");
		}
		
		Iterator<TranscriptCall> callItr = new AllTranscripts(props, key);
		calls = ArrayUtils.asArray(new TranscriptCall[0], callItr);
		Arrays.sort(calls);

		System.out.println(String.format("Loaded: %d calls", calls.length));

		int complexCount = 0, pairCount = 0, singleCount = 0;

		for(int i = 0; i < calls.length-1; i++) {
			int start = calls[i].start; 
			int end = calls[i].end;

			//System.out.println(String.format("%s: %d - %d (%s)", calls[i].chrom, calls[i].start, calls[i].end, calls[i].strand));

			int j = i + 1;
			while(j < calls.length && 
					calls[i].chrom.equals(calls[j].chrom) &&
					calls[i].strand.equals(calls[j].strand) && 
					end >= calls[j].start) { 

				end = Math.max(end, calls[j].end); 
				//System.out.println(String.format("\t%s: %d - %d (%s)", calls[j].chrom, calls[j].start, calls[j].end, calls[j].strand));
				j++; 
			}

			TranscriptCall c1 = calls[i], c2 = calls[i+1];
			if(c1.start.equals(c2.start) && c1.end < c2.end) { 
				c2 = calls[i]; c1 = calls[i+1];
			}
			if(c1.chrom.equals(c2.chrom)) { 
				Classification clss = new Classification(c1, c2);

				if(j-i == 1) { 
					singleCount += 1;
					clssList.add(clss);
				} else if(j - i == 2) { 
					pairCount += 1;
					clss.evaluate(g, averager);
					clssList.add(clss);
				} else { 
					complexCount += 1;
				}
			}

			if(i % 1000 == 0 || i / 1000 != (j-1)/1000) { System.out.print(String.format("(%d) ", i)); System.out.flush(); }

			i = j - 1;
		}
		System.out.println();
		
		classifications = clssList.toArray(new Classification[0]);
		
		System.out.println(String.format("Complexes: %d singles, %d doubles, %d complex", 
				singleCount, pairCount, complexCount));
	}
	
	public static class Classification extends RegionKey { 
		
		public Boolean sameStrand;
		public Boolean contained, overlaps;
		public TranscriptCall first, second;
		
		public RegionKey a, b, c;
		public Double aValue, bValueFirst, bValueSecond, cValue;
		
		public Classification() {}
		
		public Classification(TranscriptCall c1, TranscriptCall c2) {
			super(c1.chrom, Math.min(c1.start, c2.start), Math.max(c1.end, c2.end), c1.strand);
			
			if(c1.start > c2.start || (c1.start.equals(c2.start) && c1.end < c2.end)) { 
				throw new IllegalArgumentException(String.format("%s and %s", c1.toString(), c2.toString()));
			}
			first = c1; second = c2;
			
			sameStrand = first.strand.equals(second.strand);
			contained = first.start <= second.start && first.end >= second.end;
			overlaps = first.strandInvariantOverlaps(second);
			
			if(overlaps) { 
				a = new RegionKey(first.chrom, first.start, second.start, first.strand);
				b = new RegionKey(first.chrom, second.start, Math.min(first.end, second.end), first.strand);
				if(first.end >= second.end) { 
					c = new RegionKey(first.chrom, second.end, first.end, first.strand);
				} else { 
					c = new RegionKey(second.chrom, first.end, second.end, second.strand);
				}

				aValue = bValueFirst = bValueSecond = cValue = null;
			}
		}
		
		public boolean isConvergent() {
			if(!overlaps || !contained) { 
				return first.strand.equals("+") && second.strand.equals("-");
			} else if(!first.strand.equals(second.strand)) { 
				int firstFive = first.fivePrime();
				int firstThree = first.threePrime();
				int secondFive = second.fivePrime();
				
				int fiveDist = Math.abs(secondFive - firstFive), threeDist = Math.abs(secondFive - firstThree);
				return threeDist <= fiveDist;
			} else { 
				return false;
			}
		}
		
		public boolean isDivergent() {
			if(!overlaps || !contained) { 
				return first.strand.equals("-") && second.strand.equals("+");
			} else if(!first.strand.equals(second.strand)) { 
				int firstFive = first.fivePrime();
				int firstThree = first.threePrime();
				int secondFive = second.fivePrime();
				
				int fiveDist = Math.abs(secondFive - firstFive), threeDist = Math.abs(secondFive - firstThree);
				return threeDist > fiveDist;
			} else { 
				return false;
			}
		}
		
		public boolean isTandem() { 
			return first.strand.equals(second.strand); 
		}
		
		public void evaluate(Genome g, SteinmetzAverager avg) { 
			if(overlaps) { 
				StrandedRegion ra = new StrandedRegion(g, a.chrom, a.start, a.end, a.strand.charAt(0));
				StrandedRegion rbw = new StrandedRegion(g, b.chrom, b.start, b.end, '+');
				StrandedRegion rbc = new StrandedRegion(g, b.chrom, b.start, b.end, '-');
				StrandedRegion rc = new StrandedRegion(g, c.chrom, c.start, c.end, c.strand.charAt(0));

				aValue = avg.mean(ra); 
				bValueFirst = avg.mean(first.strand.equals("+") ? rbw : rbc); 
				bValueSecond = avg.mean(second.strand.equals("+") ? rbw : rbc); 
				cValue = avg.mean(rc);
			}
		}
		
		public boolean isMeasured() { 
			return aValue != null && bValueFirst != null && bValueSecond != null && cValue != null;
		}
		
		public boolean isContainedCandidate() { 
			if(!isMeasured() || !contained || b.width() < minSize) { return false; }
			double av = aValue, bv = bValueFirst, cv = cValue;
			if(bv < av || bv < cv) { return false; }
			double acMean = (av + cv)/2.0;
			double bDiff = bv - acMean;
			double acDiff = Math.abs(av - cv);
			return acDiff <= 0.5 && bDiff > acDiff * 2.0; 
		}
		
		public double additiveScore() { 
			double av = Math.exp(aValue);
			double bv = Math.exp(bValueFirst);
			double cv = Math.exp(cValue);
			
			return Math.abs(bv - (av + cv)); 
		}

		public boolean isAdditiveCandidate() { 
			if(!isMeasured() || contained || b.width() < minSize) { return false; }
			return true;
		}
	}

	public static class AllTranscripts implements Iterator<TranscriptCall> { 
		private Iterator<TranscriptCall> itr; 
		
		public AllTranscripts(WorkflowProperties ps, String key) throws IOException { 
			File f1 = new File(ps.getDirectory(), String.format("%s_plus.transcripts", key));
			File f2 = new File(ps.getDirectory(), String.format("%s_negative.transcripts", key));
			itr = new SerialIterator<TranscriptCall>(
					new WorkflowTranscriptReader(f1), 
					new WorkflowTranscriptReader(f2));
		}

		public boolean hasNext() { return itr.hasNext(); }
		public TranscriptCall next() { return itr.next(); }
		public void remove() { itr.remove(); }
	}
}


