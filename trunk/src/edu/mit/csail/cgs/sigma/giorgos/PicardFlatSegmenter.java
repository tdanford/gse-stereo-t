package edu.mit.csail.cgs.sigma.giorgos;

import java.util.*;

//import sun.awt.image.OffScreenImage;
import edu.mit.csail.cgs.sigma.expression.segmentation.InputData;
import edu.mit.csail.cgs.sigma.expression.segmentation.Segment;
import edu.mit.csail.cgs.sigma.expression.segmentation.Segmenter;
import edu.mit.csail.cgs.sigma.giorgos.segmentationlab.PenOptimization;

import edu.mit.csail.cgs.utils.probability.Gaussian;
import edu.mit.csail.cgs.utils.stats.StatUtil;
import edu.mit.csail.cgs.utils.Pair;

/**
 * This class will implement Franck Picard's segmentation algorithm.<br>
 * The objective function here is the likelihood of the data (<tt>L_K</tt>). <br>
 * That is:<br>
 * For <tt>n</tt> data points: <tt>{y_1, y_2, ..., y_n}</tt>
 * <pre>
 *        K
 * L_K = Sum  l_k 
 *       k=1  
 *       
 * where:
 *          1         t_k                                 y_t - mean_k
 * l_k = - --- *      Sum         { log(2*pi*var_k) + (-----------------)^2 }
 *          2     t = t_(k-1) +1                              var_k
 *          
 * where:
 * {y_(t_(k-1) +1), y_(t_(k-1) +2), ..., y_(t_k)} points belong to <tt>k</tt>-th segment and
 *
 *                1           t_k
 * mean_k = ---------------*  Sum          {y_t}   : the mean of <tt>k</tt>-th segment
 *           t_k - t_(k-1)   t = t_(k-1) +1
 *           
 *                1          t_k
 * var_k = ---------------*  Sum          { (y_t-mean_k)^2 }   : the variance of <tt>k</tt>-th segment
 *           t_k - t_(k-1)   t = t_(k-1) +1
 * </pre>
 * More specifically, the objective is to maximize the likelihood of the data (<tt>L_K</tt>) under a <tt>K</tt>-segment fitting on them:<br>
 * <b>max</b> <tt>{ L_K }</tt> <br>
 * However, we are going to postulate the problem a bit differently by trying to minimize instead:
 * <b>min</b> <tt>{ J_K }</tt> where: <tt>J_K = -2*L_K</tt> <br>
 * <tt>J_K</tt> = (almost) negative likelihood of the data <br>
 * Briefly, the algorithm operates as follows:
 * <pre>
 * 1) For a range of a number of segments: K = {1, 2, ..., k, ..., Kmax}
 * 2) find the likelihood of the data for each of the above # of segments
 * 3) Adapt a penalty model for each k-segment fit of the data
 * 4) Keep the model (optimum k and partitioning) with the minimum penalized negative likelihood
 * </pre> <br>
 * Note: All the notation above will assume that both rows and columns of a matrix start at index <tt>1</tt> for the 
 * sake of better explanation. <br>
 * Since, in Java arrays and matrices start at <tt>0</tt>, subtract from all the analyses below 1 where necessary. 
 * 
 * @author gio_fou
 *
 */
public class PicardFlatSegmenter implements Segmenter
{
	private static final Integer FIT = 0;
	private static final Double defaultVarPenalty = 3.0;
	
	/**
	 * This penalizes the "fit" of a segment, based on the variance of the fit itself.  
	 */
	private Double varPenalty;

	/**
	 * Array of different number of segments
	 */
	private Integer[] k_arr;
	
	/**
	 * flag that determines which pentalty model to use
	 */
	private int modelFlag = 0;
	
	private double beta;
	
	private double s;
	
	/**
	 * Negative likelihood for the corresponding number of clusters
	 */
	private Double[] J;
	
	/**
	 * The maximum of <tt>k_arr</tt>
	 */
	private Integer Kmax;
		
	/**
	 * <tt>n x n x 2</tt> upper triangular matrix <br>
	 * Each row (i) of the matrix represents the start of this segment.<br>
	 * Each column (j) of the matrix represents the end of this segment.<br>
	 * <tt>1 <= i <= j <= n</tt><br>
	 * <tt>oneSeg_params[i][j][]</tt> represents the mean and variance of the one-segment fit which starts at
	 * position <tt>i</tt> and ends at position <tt>j</tt> of the data (not genomic coordinate but probe indexes)<br>
	 * mean = <tt>oneSeg_params[i][j][0]</tt><br>
	 * variance = <tt>oneSeg_params[i][j][1]</tt><br>
	 */
	private Double[][][] oneSeg_params;
	
	/**
	 * <tt>n x n</tt> upper triangular matrix <br>
	 * The one-segment fit to the data<br>
	 * 
	 * Each row (i) of the matrix represents the length of this segment.<br>
	 * Each column (j) of the matrix represents the start of this segment.<br>
	 * <tt>1 <= i <= n, i <= j <= n</tt><br>
	 * For example, <tt>oneSeg_scores[i][j]</tt> represents the likelihood score of the one-segment fit which starts at
	 * position <tt>j</tt> and ends at position <tt>j+i-1</tt> of the data (not genomic coordinate but probe indexes)<br>
	 * For saving space purposes, we are going to store only the part of the matrix that has elements: <br>
	 * <pre>
	 * oneSeg_scores[1][1] = J1(1,1), oneSeg_scores[1][2] = J1(2,2), ..., oneSeg_scores[1][n-1] = J1(n-1,n-1), oneSeg_scores[1][n] = J1(n,n)
	 * oneSeg_scores[2][1] = J1(1,2), oneSeg_scores[2][2] = J1(2,3), ..., oneSeg_scores[2][n-1] = J1(n-1,n)   
	 * ...
	 * oneSeg_scores[n-1][1] = J1(1,n-1), oneSeg_scores[n-1][2] = J1(2,n)
	 * oneSeg_scores[n][1] = J1(1,n)
	 * </pre>
	 * In general:<br>
	 * <pre>
	 * oneSeg_scores[i][j] = J1(j, j+i-1)
	 * 
	 * with: 1 <= i <= n  and 1 <= j <= n-i+1
	 * </pre>
	 * In Java notation where indexing starts from zero:
	 * <pre>
	 * oneSeg_scores[i][j] = J1(j+1, j+i+1)
	 * 
	 * with: 0 <= i <= n-1  and 0 <= j <= n-i-1 
	 * </pre>
	 */
	private Double[][] oneSeg_scores;
	
	/**
	 * <tt>Kmax x n x 2</tt> upper triangular matrix <br>
	 * All <tt>k</tt> segment fits for <tt>k >= 1</tt> <u>start at the starting position of the region (to be segmented)</u><br>  
	 * <tt>k</tt>-th row represents a region of <tt>k</tt> segments<br>
	 * <tt>j</tt>-th column represents the length of this <tt>k</tt>-segment region<br>
	 * <tt> k <= j <= n </tt> with <tt> 1 <= k <= Kmax</tt> <br>
	 * mean = <tt>params[i][j][0]</tt><br>
	 * variance = <tt>params[i][j][1]</tt><br>
	 */
	private Double[][][] params;
	
	/**
	 * <tt>Kmax x n</tt> upper triangular matrix <br>
	 * All <tt>k</tt> segment fits for <tt>k >= 1</tt> <u>start at the starting position of the region (to be segmented)</u><br>
	 * <tt>k</tt>-th row represents a region of <tt>k</tt> segments<br>
	 * <tt>j</tt>-th column represents the length of this <tt>k</tt>-segment region<br>
	 * <tt> k <= j <= n </tt> with <tt> 1 <= k <= Kmax</tt> <br>
 	 * For example, <tt>scores[k][j]</tt> represents the likelihood score of the <tt>k</tt>-segment fit which starts at
	 * position <tt>1</tt> and ends at position <tt>j</tt> of the data (not genomic coordinate but probe indexes). Hence, it has <tt>j</tt> length<br>
	 * For saving space purposes, we are going to store only the part of the matrix that has elements: <br>
	 * <pre>
	 * scores[1][1] = J1(1,1), scores[1][2] = J1(1,2), ..., scores[1][n-2] = J1(1,n-2), scores[1][n-1] = J1(1,n-1), scores[1][n] = J1(1,n)
	 * scores[2][1] = J2(1,2), scores[2][2] = J2(1,3), ..., scores[2][n-2] = J2(1,n-1), scores[2][n-1] = J2(1,n) 
	 * scores[3][1] = J3(1,3), scores[3][2] = J3(1,4), ..., scores[3][n-2] = J3(1,n)   
	 * ...
	 * scores[Kmax][1] = J_{Kmax}(1,Kmax), scores[Kmax][2] = J_{Kmax}(1,Kmax+1), ..., scores[Kmax][n-Kmax+1] = J_{Kmax}(1,n)
	 * </pre>
	 * In general:
	 * <pre>
	 * scores[k][j] = J_k(1, k+j-1);
	 * 
	 * with: 1 <= k <= Kmax  and 1 <= j <= n-k+1
	 * </pre>
	 * In Java notation where indexing starts from zero:
	 * <pre>
	 * scores[k][j] = J_{k+1}(1, k+j+1);
	 * 
	 * with: 0 <= k <= Kmax-1  and 0 <= j <= n-k-1 
	 * </pre>
	 */
	private Double[][] scores;
	
	/**
	 * <tt>Kmax x n x k</tt> upper triangular matrix <br>
	 * <tt> 1 <= k <= Kmax,  k <= j <= n,  1 <= m <= k</tt> <br>
	 * The <tt>breakpoints[k][j][]</tt> array would be an array of length <tt>k</tt> <br>
	 * Each element of this array, <tt>breakpoints[k][j][m]</tt>, will hold the <b>end</b> of the corresponding segment. <br>
	 * So, <tt>breakpoints[k][j][m]</tt> will hold the end of the <tt>m</tt>-th segment of this <tt>k</tt>-segment fit
	 */
	private ArrayList<Integer>[][] breakpoints;
	
	/**
	 * The mean of a previous set of data points<br>
	 * For <tt>n</tt> data points: <tt>{y_1, y_2, ..., y_n }</t>:<br>
	 * <pre>
	 *            n
	 * mean_n = (Sum y_i)/n
	 *           i=1
	 * </pre>
	 */
	private Double[] previousMean;
	
	/**
	 * The second moment of a previous set of data points<br>
	 * For <tt>n</tt> data points: <tt>{y_1, y_2, ..., y_n }</t>:<br>
	 * <pre>
	 *                    n
	 * secondMoment_n = (Sum (y_i)^2)/n
	 *                   i=1
	 * </pre>
	 */
	private Double[] previousSecondMoment;
	
	public PicardFlatSegmenter(Integer[] k)
	{
		this(k, defaultVarPenalty);
	}// end of PicardFlatSegmenter constructor

	public PicardFlatSegmenter(Integer[] k, Double vPenalty)
	{
		k_arr = k; 	Arrays.sort(k_arr);  Kmax = k_arr[k_arr.length-1];
		J = new Double[k_arr.length];
		varPenalty = vPenalty;
	}// end of PicardFlatSegmenter constructor
	
	public void setPenaltyModel(String modelName, double s_arg)
	{
		setPenaltyModel(modelName, s_arg, 1.0);
	}
	
	public void setPenaltyModel(String modelName, double s_arg, double beta_arg)
	{
		if( modelName.equals("aic") )
		{
			modelFlag = 1;
			beta = beta_arg;
		}
		else if( modelName.equals("bic") )
		{
			modelFlag = 2;
		}
		else if( modelName.equals("lavielle") )
		{
			modelFlag = 3;
			s = s_arg;
		}
		else
			throw new IllegalArgumentException("No such penalty model exists. Set modelName to a proper value");
	}//end of setPenaltyModel method

	/**
	 * 
	 * @param data The input data which are going to be segmented
	 * @return
	 */
	public Collection<Segment> segment(InputData data) 
	{
		if(modelFlag == 0)
			throw new IllegalStateException("You have first to set the penalty model via the setPenaltyModel method");
		
		Integer[] locations = data.locations();
		Double[][] values = data.values();
		int n = locations.length;   // # of data points
		int numChannels = values.length;
		
		oneSeg_scores = new Double[n][];  oneSeg_params = new Double[n][][];
		for(int i = 0; i < n; i++) { oneSeg_scores[i] = new Double[n-i]; oneSeg_params[i] = new Double[n-i][]; }
		
		scores = new Double[Kmax][];  params = new Double[Kmax][][]; breakpoints = new ArrayList[Kmax][];  
		for(int k = 0; k < Kmax; k++) { scores[k] = new Double[n-k]; params[k] = new Double[n-k][]; breakpoints[k] = new ArrayList[n-k];}
		
		// Create the breakpoints matrix of type ArrayList starting at row 0
		for(int j = 0; j < n; j++)
			breakpoints[0][j] = new ArrayList<Integer>();
		
		previousMean = new Double[n];
		previousSecondMoment = new Double[n];
		
		BaseFitter baseFitter = baseFit(values);
		for(int i = 0; i < n; i++)
		{
			oneSeg_scores[0][i] = -2.0*baseFitter.scores[i];
			oneSeg_params[0][i] =      baseFitter.params[i];
		}
		
		Double[] currPointValues = new Double[numChannels];
		// set the base segment matrix (one_Seg)
		for(int i = 1; i < n; i++) { 
			// This is the length of the segments that we'll be considering at this 
			// iteration of the DP.
			int segmentLength = i + 1;
			int numOffsets = n-segmentLength+1;
			
			for(int j = 0; j < numOffsets; j++) {

				for(int channel = 0; channel < numChannels; channel++)
					currPointValues[channel] = values[channel][j+segmentLength-1];
				
				Double[] fitParams = dynFit(j, j+segmentLength, currPointValues);
				Double fitScore = scoreDynFit(j, j+segmentLength, fitParams, oneSeg_params, currPointValues);
								
				oneSeg_scores[i][j] = -2.0*fitScore;
				oneSeg_params[i][j] = fitParams;	 
			}
		}
		
		// Start building the scores and params matrices
		for(int i = 0; i < n; i++) { scores[0][i] = oneSeg_scores[i][0];  params[0][i] = oneSeg_params[i][0];}
		for(int j = 0; j < n; j++) { breakpoints[0][j].add(j); }
		
		// Set all the remaining for k = 1...Kmax-1
		for(int k = 1; k <= Kmax-1; k++)           // # of segments
		{
			for(int j = 1; j <= n-k; j++)        // # of offsets for each segment
			{
				Double[] currScores = new Double[j];
				
				for(int h = k; h <= k+j-1; h++) {
					currScores[h-k] = scores[k-1][h-k] + oneSeg_scores[k+j-1-h][h];
				}
				
				Pair<Double, TreeSet<Integer>> min_minIndex  = StatUtil.findMin(currScores);
				scores[k][j-1] = min_minIndex.getFirst();
				Integer min_index = min_minIndex.getLast().first();
				Integer currBreakpoint = k + j -1;   
				breakpoints[k][j-1] = new ArrayList<Integer>(breakpoints[k-1][min_index]);
				breakpoints[k][j-1].add(currBreakpoint);
			}
		}
		
		for(int i = 0; i < k_arr.length; i++) {
			int k_index = k_arr[i]-1;
			J[i] = scores[k_index][scores[k_index].length-1];
		}
		
		Integer k_opt;
		switch( modelFlag )
		{
			case 1: { k_opt = PenOptimization.aic(J, k_arr, beta); break; }
			case 2: { k_opt = PenOptimization.bic(J, k_arr, n); break; }
			default: { k_opt = PenOptimization.lavielle(J, k_arr, s); break; }
		}
		
		Integer k_opt_index = k_opt-1;
		Integer[] opt_breakpoints =	breakpoints[k_opt_index][breakpoints[k_opt_index].length-1].toArray(new Integer[0]);
		
		Collection<Segment> segs = new TreeSet<Segment>();
		
		int start = 0, end, length;
		for(int i = 0; i < opt_breakpoints.length; i++)
		{
			end = opt_breakpoints[i];
			length = end-start+1;
			Segment seg = new Segment(start, end, oneSeg_params[length-1][start]);
			segs.add(seg);
			
			start = end+1;
		}
		
		return segs;
	}// end of segment method
	
	/**
	 * <tt>fit</tt> method here is used just once to find all fits of length <tt>1</tt> and initialize the 
	 * <tt>previousMean</tt> and the <tt>previousSecondMoment</tt> fields.<br> 
	 * Calculates an array of parameters, to fit the data in the index range [j1, j2) 
	 * 
	 * @param j1 The starting index of the fit (inclusive).
	 * @param j2 The ending index of the fit (exclusive).
	 * @param values  The array of measured (experimental) values.
	 * @return  An array of parameters for a fit segment in this location.  
	 */
	private Double[] fit(int j1, int j2, Double[][] values) 
	{	
		// The parameters of the fit are going to be the mean and variance 
		// of the values in this segment.  
		
		Double sum = 0.0;
		for(int i = j1; i < j2; i++) { 
			for(int channel = 0; channel < values.length; channel++ ) {
				sum += values[channel][i];	
			}
		}
		Double mean = sum / (double)Math.max(1, j2-j1);
		sum = 0.0;
		
		for(int i = j1; i < j2; i++) { 
			for(int channel = 0; channel < values.length; channel++ ) {
				double currValue = values[channel][i];
				sum += currValue*currValue;
			}
		}
		Double secondMoment = sum / (double)Math.max(1, j2-j1);
		Double var = secondMoment - mean*mean;
		
		previousMean[j1] = mean;
		previousSecondMoment[j1] = secondMoment;
		
		return new Double[] { mean, var };
	}//end of fit method
	
	/**
	 * Scores a potential fit on the region [j1, j2) of the data. This implementation is just 
	 * "fitting averages" to segments, so the params field will be two numbers (mean and variance)
	 * and the "fit" will just be the log-likelihood of the model on the data in this segment.    
	 * 
	 * @param j1
	 * @param j2
	 * @param params
	 * @param values
	 * @return
	 */
	private Double scoreFit(int j1, int j2, Double[] params, Double[][] values)
	{
		Gaussian nd = new Gaussian(params[0], params[1]);	
		Double sum = 0.0;
		for(int j = j1; j < j2; j++) { 
			for(int channel = 0; channel < values.length; channel++){
				double value = values[channel][j];
				double logp = nd.calcLogProbability(value);
				sum += logp;	
			}	
		}
		
		Double score = sum - varPenalty * params[1];
		return score;
	}//end of scoreFit method
	
	private Double[] dynFit(int j1, int j2, Double[] currPointValue)
	{	
		Double meanCurrPointValue = 0.0;
		for(Double e:currPointValue)
			meanCurrPointValue += e;
		meanCurrPointValue /= currPointValue.length;
		
		// # of data points
		int n = j2-j1;
		
		Double mean = ( (n-1)*previousMean[j1] + meanCurrPointValue )/n;
		Double secondMoment =  ((n-1)*previousSecondMoment[j1] + meanCurrPointValue*meanCurrPointValue )/n;  
		Double var = secondMoment - mean*mean;
		
		previousMean[j1] = mean;
		previousSecondMoment[j1] = secondMoment;
		return new Double[] { mean, var };
	}//end of dynFit method
	
	/**
	 * Scores a potential fit on the region [j1, j2) of the data. This implementation is just 
	 * "fitting averages" to segments, so the params field will be two numbers (mean and variance)
	 * and the "fit" will just be the log-likelihood of the model on the data in this segment minus a penalty for the variance.     
	 * @param j1
	 * @param j2
	 * @param fitParams parameters of the current set of <tt>n</tt> data points
	 * @param params
	 * @param values values of the current <tt>n</tt>'th data point across experiments/channels
	 * @return the log-likelihood of the model on the data in this segment minus a penalty for the variance.
	 */
	private Double scoreDynFit(int j1, int j2, Double[] fitParams, Double[][][] params, Double[] values) 
	{
		Double meanCurrPointValue = 0.0;
		for(Double e:values)
			meanCurrPointValue += e;
		meanCurrPointValue /= values.length;

		// # of data points
		int n = j2-j1;
		Double mean = fitParams[0];
		Double var  = fitParams[1];
		
		// The (n-1)th element of params corresponds to length n (starting the indexing at 0)
		// So, the (n-2)th element of params corresponds to length n-1
		Double prevMean = params[(n-1)-1][j1][0];
		Double prevVar = params[(n-1)-1][j1][1];
	
		Double loglik = Gaussian.dynCalcTotalLogProbability(meanCurrPointValue, n, mean, var, prevMean, prevVar);
		Double score = loglik - varPenalty * var;
		return score;
	}//end of scoreDynFit method

	
	/**
	 * Finds the one-segment fits of length 1 to the data
	 * @param values
	 * @return
	 */
	private BaseFitter baseFit(Double[][] values)
	{
		int segmentLength = 1;
		int numOffsets = values[0].length-segmentLength+1;
		
		Double[] scores = new Double[numOffsets];
		Double[][] params = new Double[numOffsets][];
		
		for(int j = 0; j < scores.length; j++) 
		{
			Double[] fitParams = fit(j, j+segmentLength, values);
			Double fitScore = scoreFit(j, j+segmentLength, fitParams, values);
			params[j] = fitParams;
			scores[j] = fitScore;
		}
		
		return new BaseFitter(scores, params);
	}//end of baseFit method

	
	private class BaseFitter
	{
		Double[] scores;
		Double[][] params;
		
		public BaseFitter(Double[] s, Double[][] p)
		{
			scores = s;
			params = p;
		}//end of constructor
	}//end of BaseFitter class	
	
}//end of PicardFlatSegmenter class

