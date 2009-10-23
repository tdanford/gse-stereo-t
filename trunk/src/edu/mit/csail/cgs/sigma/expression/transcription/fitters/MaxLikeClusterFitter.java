/*
 * Author: tdanford
 * Date: Jun 26, 2009
 */
package edu.mit.csail.cgs.sigma.expression.transcription.fitters;

import java.util.Collection;

import edu.mit.csail.cgs.sigma.expression.transcription.Cluster;
import edu.mit.csail.cgs.sigma.expression.transcription.TranscriptionParameters;
import edu.mit.csail.cgs.sigma.expression.transcription.recursive.Endpts;
import edu.mit.csail.cgs.sigma.expression.transcription.recursive.PrimitiveIterator;
import edu.mit.csail.cgs.sigma.expression.workflow.WorkflowProperties;
import edu.mit.csail.cgs.ewok.verbs.Filter;

public class MaxLikeClusterFitter implements ClusterFitter {
	
	private TranscriptionParameters params;
	private Integer[][] gammas;
	private Integer[] channels;
	
	private int arrangementsChecked;
	private double[] overlaps; 
	
	public MaxLikeClusterFitter(TranscriptionParameters p, Integer[] chs, Integer[][] gs) {
		params = p;
		gammas = gs;
		arrangementsChecked = 0;
		channels = chs;
		overlaps = null;
	}
	
	public MaxLikeClusterFitter(WorkflowProperties props, TranscriptionParameters params, Integer[] chs, Integer[][] gs) {
		this(params, chs, gs);
	}
	
	public double[] overlaps() { return overlaps; }
	public int arrangementsChecked() { return arrangementsChecked; }

	public EndptsFit fitCluster(Cluster c) {

		arrangementsChecked = 0;
		int segs = c.segments.length;
		overlaps = new double[segs];
		for(int i = 0; i < segs; i++) { overlaps[i] = 0.0; }
		double totalWeight = 0.0;
		
		/*
		Collection<Integer> dips = c.fivePrimeDipSegments(channel);
		int maxTranscripts = Math.min(c.segments.length, 
				Math.max(c.segments.length/2, dips.size()+1));
		*/
		int maxTranscripts = Math.min(c.segments.length, params.maxCallsPerCluster);
		
		String strand = c.segments[0].strand;
		int maxOverlap = params.maxOverlap;
		
		Double bestScore = null;
		EndptsFit bestFit = null;
		
		EndptsFitter fitter = new MaxLikeEndptsFitter(params, c, channels, gammas);
		Filter<Endpts[],Endpts[]> filter = null;
		/*
		if(dips.size() > 0 && params.filterBreaks) { 
			filter = new PrimitiveIterator.BreakpointFilter(dips, strand, c.segments.length);
		}
		*/
		
		for(int t = 1; t <= maxTranscripts; t++) {
			System.out.println(String.format("------ Fitting %d Transcripts ------", t));
			int count = 0;		
			
			PrimitiveIterator itr = new PrimitiveIterator(segs, t, maxOverlap, null, strand);
		
			while(itr.hasNext()) { 
				
				String eptsString = itr.nextString();
				Endpts[] epts = itr.next();

				if(t == 1 || filter == null || filter.execute(epts) != null) { 

					EndptsFit fit = fitter.fitTranscripts(epts, bestScore);
					count += 1;

					if(fit != null) { 
						if(bestFit == null || fit.score >= bestScore) { 
							bestScore = fit.score;
							bestFit = fit;

							String[] array = eptsString.split("\n");
							for(int i = 0; i < array.length; i++) { 
								System.out.print(array[i]);
								int j = array.length-i-1;
								if(j >= 0 && j < fit.epts.length) {
									int s1 = fit.cluster.segments[fit.epts[j].start].start;
									int s2 = fit.cluster.segments[fit.epts[j].end-1].end;
									int len = s2-s1;
									System.out.print(String.format(" g:%d //", len));
									for(int k = 0; k < fit.gammas.length; k++) { 
										System.out.print(String.format("%.1f ", 
												fit.gammas[k][j]));
									}
								}
								System.out.println();
							}
							System.out.println(String.format("Score: %f", bestScore));
						}

						double weight = fit.likelihood;
						//System.out.println(String.format("-> %f", weight));
						int[] over = overlapCounts(segs, epts);
						for(int i = 0; i < over.length; i++) { 
							if(over[i] > 1) { 
								overlaps[i] += weight;
							}
						}
						totalWeight += weight;
					}
					
					arrangementsChecked += 1;
					
					if(count % 100 == 0) { 
						System.out.println(String.format("### Arrangements Fit: %d ###", count));
					}
				}
			}
			System.out.println(String.format("### Total %d-Arrangements Fit: %d ###", maxTranscripts, count));
		}
		
		System.out.println("Overlap Probabilities:");
		for(int i = 0; i < overlaps.length; i++) { 
			double v = overlaps[i] - totalWeight;
			System.out.println(String.format("\t%d: %f - %f = %f", i, overlaps[i], v, Math.exp(v)));
		}
		
		return bestFit;
	} 
	
	private int[] overlapCounts(int segs, Endpts[] epts) { 
		int[] over = new int[segs];
		for(int i = 0; i < epts.length; i++) { 
			for(int j = epts[i].start; j < epts[i].end; j++) { 
				over[j] += 1;
			}
		}
		return over;
	}
}
