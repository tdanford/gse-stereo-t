/*
 * Author: tdanford
 * Date: Jul 10, 2009
 */
package edu.mit.csail.cgs.sigma.expression.transcription.recursive;

import edu.mit.csail.cgs.cgstools.singlevarcalculus.FunctionModel;

public class FunctionOptimization {

	public static boolean debugPrintZeroFinding = false;

	public static double findZero(FunctionModel f, double lowerBound) {
		return findZero(f, lowerBound, 1.0e-6);
	}
	
	private static void evalf(FunctionModel f, double x) { 
		System.out.println(String.format("f(%.3f) = %f", x, f.eval(x)));
	}

	/**
	 * findZeroBrackets is a method for taking a FunctionModel and finding 
	 * two points [lower, upper] such that a zero of the function lies between 
	 * the two points.
	 * 
	 * Of course, this isn't a general method -- we're relying on the fact that 
	 * the function is continuous and that there *is* a zero (but this happens 
	 * to be the case for all the functions we consider in this class).  
	 * 
	 * findZeroBrackets() works by starting at an initial guess, supplied by the 
	 * user, and walking outwards in increasingly large steps until either we 
	 * exceed the bounds of the double-number format (at which point we signal 
	 * an error) or we find an appropriate lower/upper sign shift (indicating 
	 * that the number changed signs, and hence crossed zero, in between).  
	 * 
	 * @param f
	 * @param start
	 * @return
	 */
	public static double[] findZeroBrackets(FunctionModel f, double start) { 
		double fstart = f.eval(start);
		
		if(Double.isInfinite(fstart) || Double.isNaN(fstart)) {
			/*
			FunctionModelPaintable fmp = new FunctionModelPaintable(f);
			fmp.setProperty(FunctionModelPaintable.xScaleKey, new PaintableScale(-1.0, 100.0));
			fmp.setProperty(FunctionModelPaintable.yScaleKey, new PaintableScale(-10.0, 10.0));
			new PaintableFrame("error", fmp);

			evalf(f, -1.0);
			evalf(f, 0.0);
			evalf(f, 1.0);
			*/

			throw new IllegalArgumentException(String.format(
					"Illegal start: %f (%f)", start, f.eval(start)));
		}
		
		boolean sign = fstart >= 0.0;
		double lowerSpace = 1.0, upperSpace = 1.0;
		double upper = start+upperSpace, lower = start-lowerSpace;
		double pupper = start, plower = start;
		
		double fupper = f.eval(upper), flower = f.eval(lower);
		boolean usign = fupper >= 0.0, lsign = flower >= 0.0;
		int iters = 0;
		int maxIters = 10000;

		loop: 
		while((isBad(fupper) || usign==sign) && 
			  (isBad(flower) || lsign == sign)) {
			
			iters += 1;
			if(iters > maxIters) { break; }
			if(debugPrintZeroFinding) { 
				System.out.println(String.format("bounds (%d) : %f - %f (%f - %f)", 
						iters, lower, upper, flower, fupper));
			}
			
			if(Double.isInfinite(lower) && Double.isInfinite(upper)) { 
				break loop;
			}
			
			if(isBad(fupper)) {
				upperSpace /= 2.0;
				upper -= upperSpace;
			} else { 
				upperSpace *= 2.0;
				upper += upperSpace;
			}

			fupper = f.eval(upper);
			usign = fupper >= 0.0;
			
			if(isBad(flower)) { 
				lowerSpace /= 2.0;
				lower += lowerSpace;
			} else { 
				lowerSpace *= 2.0;
				lower -= lowerSpace;
			}
			
			flower = f.eval(lower);
			lsign = flower >= 0.0;
		}
		
		if(iters > maxIters || (isBad(fupper) && isBad(flower))) { 
			throw new IllegalArgumentException(
					String.format("Couldn't find zero brackets : start=%f, " +
							"lower=%f, f(lower)=%f, " +
							"upper=%f, f(upper)=%f", start, lower, flower, upper, fupper));
		}
		
		if(usign != sign) {
			//System.out.println(String.format("\n%f -> (%f, %f]", start, start, upper));
			return new double[] { start, upper }; 
		}
		
		if(lsign != sign) { 
			//System.out.println(String.format("\n%f -> [%f, %f)", start, lower, start));
			return new double[] { lower, start }; 
		}
		
		throw new IllegalArgumentException("Couldn't find zero brackets!");
	}
	
	public static boolean isBad(double d) { return Double.isNaN(d) || Double.isInfinite(d); }

	/**
	 * Given the bounds returned by findZeroBrackets(), above, this method 
	 * finds an approximation to the zero of the function itself by simple
	 * bisection and binary search.  
	 * 
	 * @param f
	 * @param start
	 * @param eps
	 * @return
	 */
	public static double findZero(FunctionModel f, double start, double eps) { 
		if(debugPrintZeroFinding) { 
			System.out.println(String.format(" findZero(f,start=%f,eps=%f)", start, eps));
		}
		double[] bounds = findZeroBrackets(f, start);
		if(debugPrintZeroFinding) { 
			System.out.println(String.format("\tbounds: %f, %f", bounds[0], bounds[0]));
		}
		
		double lower = bounds[0], upper = bounds[1];
		return findZero(f, lower, upper, eps);
	}
	
	public static double findZero(FunctionModel f, double lower, double upper, double eps) {
		if(debugPrintZeroFinding) { 
			System.out.println(String.format(" findZero(f,lower=%f,upper=%f,eps=%f)", lower, upper, eps));
		}
		
		double ly = f.eval(lower), uy = f.eval(upper);
		
		//System.out.println(String.format("Zero: [%f, %f] (%f, %f)", lower, upper, ly, uy));
		
		boolean lsign = ly < 0.0;
		boolean usign = uy <= 0.0;

		int iters= 0, maxIters = 10000;
		
		//while(Math.abs(upper-lower) > eps) {
		while(lower < upper && lower + eps < upper) { 
			if(++iters > maxIters) { 
				throw new IllegalArgumentException(String.format(
					"Exceeded max-iters in findZero() : %d", maxIters));
			}
			double diff = (upper-lower);
			double middle = lower + diff/2.0;
			double my = f.eval(middle);
			boolean msign = my < 0.0;


			if(my == 0.0) { 
				return middle; 
			} else if (msign == lsign) {
				lower = middle;
				ly = my;
				lsign = msign;
				if(debugPrintZeroFinding) { 
					System.out.println(String.format("\tU: [%f, %f] (%f, %f)", 
							lower, upper, ly, uy));
				}
			} else { 
				upper = middle;
				uy = my;
				usign = msign;
				if(debugPrintZeroFinding) { 
					System.out.println(String.format("\tL: [%f, %f] (%f, %f)", 
							lower, upper, ly, uy));
				}
			}
		}
		
		double v = (lower+upper)/2.0;
		if(debugPrintZeroFinding) { 
			System.out.println(String.format("\t-> %f", v));
		}
		return v; 
	}
}
