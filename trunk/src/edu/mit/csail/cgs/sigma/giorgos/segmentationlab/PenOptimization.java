package edu.mit.csail.cgs.sigma.giorgos.segmentationlab;

import java.util.ArrayList;
import java.util.TreeSet;

import edu.mit.csail.cgs.utils.Pair;
import edu.mit.csail.cgs.utils.stats.StatUtil;


/**
 * This class will hold penalty functions as well as algorithms for evaluating
 * an optimized value for the variable of interest under several penalized constrained optimization
 * models.<br>
 * Here, our objective function is the likelihood of the data (<tt>L_k</tt>) which is given by the formula:<br>
 * <pre>
 *          1    K     t_k                                     y_t - mu_k
 * L_k = - ---  Sum    Sum          {log(2*pi*(sigma_k)^2) + (------------)^2 }
 *          2   k=1   t=t_(k-1)+1                                sigma_k
 * </pre> 
 * We seek for the maximum of the above function under some constraint represented by the penalty function.<br>
 * In simple words, we optimize the function below: <br>
 * <tt><b>max</b> {L_k - pen(k)}</tt>, where <tt>k</tt> the # of segments partitioning the data set.<br>
 * Instead, we are going to minimize the following expression (by multiplying the above objective function by <tt>-2</tt>):<br>
 * <tt><b>min</b> {J_k + 2*pen(k)}</tt>, where <tt>J_k = -2*L_k</tt>.<br>
 * The above problem is equivalent to our original maximization one.
 * @author gio_fou
 *
 */
public class PenOptimization {
	
	/** 
	 * @param J negative likelihood of the data
	 * @param k_arr array of # of segments (corresponding to the different values of the negative likelihood (<tt>J</tt>)
	 * @param beta penalty constant
	 * @return the optimized <tt>k</tt> under AIC criterion
	 */
	public static int aic(Double[] J, Integer[] k_arr, double beta)
	{
		if( J.length != k_arr.length) 
			throw new IllegalArgumentException("J and k_arr arguments must be the same size.");
		
		double[] pen = new double[k_arr.length];
		for(int i = 0; i < pen.length; i++)
		{
			int k = k_arr[i];
			pen[i] = aicPen(beta, k);
		}
		
		Double[] penOptim = new Double[J.length];
		for(int i = 0; i < penOptim.length; i++)
			penOptim[i] = J[i] + 2*pen[i];
		
		Pair<Double, TreeSet<Integer>> min_and_min_index = StatUtil.findMin(penOptim);
		Integer[] min_index_arr = min_and_min_index.getLast().toArray(new Integer[min_and_min_index.getLast().size()]);
		
		Integer min_index = min_index_arr[0];
		return k_arr[min_index];
	}//end of aic method
	
	/** 
	 * Default value for <tt>beta=0</tt>
	 * @param J negative likelihood of the data
	 * @param k_arr array of # of segments (corresponding to the different values of the negative likelihood (<tt>J</tt>)
	 * @return the optimized <tt>k</tt> under AIC criterion
	 */
	public static int aic(Double[] J, Integer[] k_arr)
	{
		return aic(J, k_arr, 1);
	}//end of aic default method
	
	/** 
	 * @param J negative likelihood of the data
	 * @param k_arr array of # of segments (corresponding to the different values of the negative likelihood (<tt>J</tt>)
	 * @param n # of data points
	 * @param overlapPenalty penalty constant
	 * @return the optimized <tt>k</tt> under BIC criterion
	 */
	public static int bic(Double[] J, Integer[] k_arr, int n)
	{
		if( J.length != k_arr.length) 
			throw new IllegalArgumentException("J and k_arr arguments must be the same size.");
		
		double[] pen = new double[k_arr.length];
		for(int i = 0; i < pen.length; i++)
		{
			int k = k_arr[i];
			pen[i] = bicPen(n, k);
		}
		
		Double[] penOptim = new Double[J.length];
		for(int i = 0; i < penOptim.length; i++)
			penOptim[i] = J[i] + 2*pen[i];
		
		Pair<Double, TreeSet<Integer>> min_and_min_index = StatUtil.findMin(penOptim);
		Integer[] min_index_arr = min_and_min_index.getLast().toArray(new Integer[min_and_min_index.getLast().size()]);
		
		Integer min_index = min_index_arr[0];
		return k_arr[min_index];
	}//end of bic method
	
	/**
	 * <pre>
	 * Negative normalized likelihood:
	 * 
	 *            J_kmax - J_k
	 * Jtilde_k = -------------*(kmax-1) +1    , k = {1, ..., kmax}
	 *            J_kmax - J_1
	 * 
	 *            
	 * Second derivative (finite difference) of the negative normalized likelihood:
	 *            
	 * D_k = Jtilde_{k-1} -2*Jtilde_k +Jtilde_{k+1}  , k = {1, ..., kmax-1} with D_1 = Infinity
	 * </pre>
	 * @param J negative likelihood of the data
	 * @param k_arr array of # of segments (corresponding to the different values of the negative likelihood (<tt>J</tt>)
	 * @param s threshold
	 * @return the optimized <tt>k</tt> under lavielle criterion
	 * @see <a href="http://www.sciencedirect.com/science?_ob=ArticleURL&_udi=B6V18-4G1MB4Y-1&_user=501045&_rdoc=1&_fmt=&_orig=search&_sort=d&view=c&_acct=C000022659&_version=1&_urlVersion=0&_userid=501045&md5=0c3ccbe855f4cb13d209e8507e3a09f3">
	   Lavielle M. Using penalized contrasts for the change-point problem. <i>Signal Processing 85 (2005) pp. 1501-1510</i></A>
	 */
	public static int lavielle(Double[] J, Integer[] k_arr, double s)
	{
		if( J.length != k_arr.length) 
			throw new IllegalArgumentException("J and k_arr arguments must be the same size.");

		// The index when k = kmax
		int last = J.length-1;
		int kmax = k_arr[last];
		
		double[] Jtilde = new double[J.length];
		for(int i = 0; i < Jtilde.length; i++)
			Jtilde[i] = ((J[last]-J[i])/(J[last]-J[0]))*(kmax-1) + 1;
		
		// second derivative of negative likelihood
		Double[] D = new Double[Jtilde.length-1];
		D[0] = Double.POSITIVE_INFINITY;
		for(int i = 1; i < D.length; i++)
			D[i] = (Jtilde[i-1] -2*Jtilde[i] + Jtilde[i+1])/2;
		
		ArrayList<Integer> pos = StatUtil.find(D, ">", s);
		
		//Take the greatest k s.t. D > s
		int k_opt = 1;
		if( pos.size() != 0)
			k_opt = k_arr[pos.get(pos.size()-1)];
		return k_opt;
	}//end of lavielle method
	
	/**
	 * 
	 * @param J negative likelihood of the data
	 * @param k_arr array of # of segments (corresponding to the different values of the negative likelihood (<tt>J</tt>)
	 * @param n # of data points
	 * @param c1 weight constant
	 * @param c2 weight constant
	 * @return the optimized <tt>k</tt> under lebarbier criterion
	 * @see <a href="http://www.sciencedirect.com/science?_ob=ArticleURL&_udi=B6V18-4F31YRJ-1&_user=501045&_rdoc=1&_fmt=&_orig=search&_sort=d&view=c&_acct=C000022659&_version=1&_urlVersion=0&_userid=501045&md5=07b88eb038438ed2a223f1f92046f8c3">
	   Lebarbier E. Detecting multiple change-points in the mean of Gaussian process by model selection. <i>Signal Processing 85 (2005) pp. 717-736</i></A>
	 */
	public static int lebarbier(Double[] J, Integer[] k_arr, int n, double c1, double c2)
	{
		if( J.length != k_arr.length) 
			throw new IllegalArgumentException("J and k_arr arguments must be the same size.");
		
		double[] pen = new double[J.length];
		for(int i = 0; i < pen.length; i++)
			pen[i] = lebarbierPen(n, k_arr[i], c1, c2);
		
		return penteexplo(J, pen);
	}//end of lebarbier method
	
	/**
	 * Default values for <tt>c1</tt>, <tt>c2</tt>: <tt>c1 = 2.6, c2 = 2</tt>
	 * @param J negative likelihood of the data
	 * @param k_arr array of # of segments (corresponding to the different values of the negative likelihood (<tt>J</tt>)
	 * @param n # of data points
	 * @return the optimized <tt>k</tt> under lebarbier criterion
	 * @see <a href="http://www.sciencedirect.com/science?_ob=ArticleURL&_udi=B6V18-4F31YRJ-1&_user=501045&_rdoc=1&_fmt=&_orig=search&_sort=d&view=c&_acct=C000022659&_version=1&_urlVersion=0&_userid=501045&md5=07b88eb038438ed2a223f1f92046f8c3">
	   Lebarbier E. Detecting multiple change-points in the mean of Gaussian process by model selection. <i>Signal Processing 85 (2005) pp. 717-736</i></A>
	 */
	public static int lebarbier(Double[] J,  Integer[] k_arr, int n)
	{
		if( J.length != k_arr.length) 
			throw new IllegalArgumentException("J and k_arr arguments must be the same size.");
		
		double[] pen = new double[J.length];
		for(int i = 0; i < pen.length; i++)
			pen[i] = lebarbierPen(n, k_arr[i]);
		
		return penteexplo(J, pen);
	}//end of lebarbier default method
	
	
	//TODO The description of this method in the paper is incomplete.
    //     It seems that it uses the same principle with Lavielle and 
	//     the implementation in Matlab is very complicated
	/**
	 * 
	 * @param J negative likelihood of the data
	 * @param pen penalty of the data
	 * @return the optimized <tt>k</tt> under lebarbier criterion
	 */
	private static int penteexplo(Double[] J, double[] pen)
	{
		double[] beta = new double[J.length];
		for(int i = 0; i < J.length-1; i++)
		{
			beta[i] = (J[i+1]-J[i])/(pen[i]-pen[i+1]);
		}
		return -1;
	}//end of penteexplo method
	
	/**
	 * Akaike Information Criterion (AIC) penalty function
	 * @param beta penalty constant
	 * @param k # of parameters
	 * @return the value of the aic function
	 */
	public static double aicPen(double beta, int k)
	{
		return beta*2*k;
	}// end of aicPen method
	
	/**
	 * Akaike Information Criterion (AIC)  penalty function<br>
	 * Default value for <tt>beta</tt> is <tt>1</tt>.
	 * @param k # of parameters
	 * @return the value of the aic function
	 */
	public static double aicPen(int k)
	{
		return aicPen(1, k);
	}// end of aicPen default method
	
	/**
	 * Bayesian Information Criterion (BIC) penalty function
	 * @param n # of data points
	 * @param k # of parameters
	 * @return the value of the bic function
	 */
	public static double bicPen(int n, int k)
	{
		return 0.5*Math.log(n)*2*k;
	}// end of bicPen method
	
	/**
	 * Jong penalty function 
	 * @param k # of parameters
	 * @return the value of the jong function
	 */
	public static double jongPen(int k)
	{
		return ((double)10/3)*(3*k-1);
	}// end of jongPen method
	
	/**
	 * Lebarbier penalty function
	 * @param n # of data points
	 * @param k # of parameters
	 * @param c1 weight constant
	 * @param c2 weight constant
	 * @return the value of the lebarbier function
	 */
	public static double lebarbierPen(int n, int k, double c1, double c2)
	{
		return 2*k*(c1 + c2*Math.log((double)n/k));
	}// end of lebarbierPen method
	
	/**
	 * Lebarbier penalty function<br>
	 * Default values: <tt>beta=1, c1=2.6, c2=2</tt>
	 * @param n # of data points
	 * @param k # of parameters
	 * @return the value of the lebarbier function
	 */
	public static double lebarbierPen(int n, int k)
	{
		return lebarbierPen(n, k, 2.6, 2);
	}// end of lebarbierPen default method
	

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}// end of PenOptimization class
