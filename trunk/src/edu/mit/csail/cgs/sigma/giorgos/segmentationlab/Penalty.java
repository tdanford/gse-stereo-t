package edu.mit.csail.cgs.sigma.giorgos.segmentationlab;

/**
 * This class will contain penalty functions for penalizing objective functions (i.e.
 * likelihood)
 * @author gio_fou
 *
 */
public class Penalty {
	
	public static double aic(double beta, int K)
	{
		return beta*2*K;
	}// end of aic method
	
	public static double aic(int K)
	{
		return aic(1, K);
	}// end of aic default method
	
	public static double bic(int n, int K)
	{
		return 0.5*Math.log(n)*2*K;
	}// end of bic method
	
	public static double jong(int K)
	{
		return (10/3)*(3*K-1);
	}// end of jong method
	
	//TODO Finish up lebarbier method
	public static double lebarbier(int n, int K, double c1, double c2)
	{
		return -1;
	}// end of lebarbier default method
	
	public static double lebarbier(int n, int K)
	{
		return lebarbier(n, K, 2.6, 2);
	}// end of lebarbier method
	
	//TODO finish up lavielle method
	public static double lavielle(int K)
	{
		return -1;
	}// end of lavielle method

	

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}//end of Penalty class
