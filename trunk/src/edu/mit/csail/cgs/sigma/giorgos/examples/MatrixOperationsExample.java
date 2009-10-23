package edu.mit.csail.cgs.sigma.giorgos.examples;

import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.linalg.Algebra;

import java.util.Vector;

public class MatrixOperationsExample {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		Vector<Integer> v = new Vector<Integer>();
		v.add(8); v.add(1); v.add(20);
		
		for(Integer i : v)
			System.out.println(i);
		
		
		double[][] a = {{1, 4, 2}, {2, 4, 1}, {9, 2, 1}};
		
		
		Algebra al = new Algebra();
		DoubleMatrix2D A =  new DenseDoubleMatrix2D(a);
		
		// Inverse
		DoubleMatrix2D invA = al.inverse(A);
		System.out.println(invA.toString());
		
		// Product
		DoubleMatrix2D prod = al.mult(A, invA);
		System.out.println(prod.toString());
		
		double[][] x1 = {{2,4}, {5,7}};
		double[][] x2 = {{3,8}, {2,9}};
		
		DoubleMatrix2D X1 = new DenseDoubleMatrix2D(x1);
		DoubleMatrix2D X2 = new DenseDoubleMatrix2D(x2);
		DoubleMatrix2D prod1 = al.mult(X1, X2);
		System.out.println(prod1.toString());
		
		
		
		

	}

}
