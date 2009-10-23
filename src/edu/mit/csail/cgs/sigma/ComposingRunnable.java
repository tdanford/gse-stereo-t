/*
 * Author: tdanford
 * Date: May 27, 2008
 */
package edu.mit.csail.cgs.sigma;

public class ComposingRunnable implements Runnable {

	private Runnable r1, r2;
	
	public ComposingRunnable(Runnable rf, Runnable rs) { 
		r1 = rf; r2 = rs;
	}
	
	public void run() { 
		r1.run();
		r2.run();
	}
}
