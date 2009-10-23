/*
 * Author: tdanford
 * Date: Oct 25, 2008
 */
package edu.mit.csail.cgs.sigma.lethality;

import java.util.*;
import java.io.*;

import edu.mit.csail.cgs.sigma.*;
import edu.mit.csail.cgs.utils.SetTools;

public class LethalityMain {

	public static void main(String[] args) { 
		String propName = args.length > 0 ? args[0] : "default"; 

		SigmaProperties sigmaProps = new SigmaProperties();
		LethalityProperties props = new LethalityProperties(sigmaProps, propName);

		LethalityData data = new LethalityData(props);
		data.loadData();

		Set<String> s288cLethals = data.getS288CLethalORFs();
		Set<String> sigmaLethals = data.getSigmaLethalORFs();

		SetTools<String> setter = new SetTools<String>();
		Set<String> bothLethal = setter.intersection(s288cLethals, sigmaLethals);
		Set<String> sigmaUnique = setter.subtract(sigmaLethals, s288cLethals);
		Set<String> s288cUnique = setter.subtract(s288cLethals, sigmaLethals);

		System.out.println(String.format("Common Lethals: %d", bothLethal.size()));

		System.out.println(String.format("# S288C Lethals: %d", s288cLethals.size()));
		System.out.println(String.format("\tS288C Unique: %d", s288cUnique.size()));
		System.out.println(s288cUnique.toString());

		System.out.println(String.format("# Sigma Lethals: %d", sigmaLethals.size()));
		System.out.println(String.format("\tSigma Unique: %d", sigmaUnique.size()));
		System.out.println(sigmaUnique.toString());

	}
}
