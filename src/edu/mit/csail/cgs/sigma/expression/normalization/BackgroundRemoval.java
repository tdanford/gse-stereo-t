/*
 * Author: tdanford
 * Date: May 29, 2009
 */
package edu.mit.csail.cgs.sigma.expression.normalization;

import java.util.*;
import java.io.*;

import edu.mit.csail.cgs.ewok.verbs.Filter;
import edu.mit.csail.cgs.ewok.verbs.FilterIterator;
import edu.mit.csail.cgs.ewok.verbs.Mapper;
import edu.mit.csail.cgs.ewok.verbs.MapperIterator;
import edu.mit.csail.cgs.sigma.expression.workflow.*;
import edu.mit.csail.cgs.sigma.expression.workflow.models.ProbeLine;
import edu.mit.csail.cgs.utils.Closeable;

public class BackgroundRemoval {

	public static void main(String[] args) { 
	
		BackgroundEstimation estimation = new BolstadEMBackgroundEstimation();
		//BackgroundEstimation estimation = new UniformBackgroundEstimation();
		
		String key = args[0];
		
		WorkflowProperties props = new WorkflowProperties();
		File fplus = new File(props.getDirectory(), String.format("%s_plus.raw", key)); 
		File fminus = new File(props.getDirectory(), String.format("%s_negative.raw", key));
		
		//LogTransform transform = LogTransform.NONE;
		LogTransform transform = LogTransform.LOG_TO_EXP;
		
		BackgroundRemoval norm = new BackgroundRemoval(estimation, transform);

		try {
			System.out.println(String.format("Loading: %s", fplus.getName()));
			norm.load(new WorkflowDataLoader(fplus));
			System.out.println(String.format("Loading: %s", fminus.getName()));
			norm.load(new WorkflowDataLoader(fminus));

			WorkflowIndexing indexing = props.getIndexing(key);

			System.out.println(String.format("Removing background...")); 
			norm.removeBackground();

			Filter<ProbeLine,ProbeLine> plus = new Filter<ProbeLine,ProbeLine>() { 
				public ProbeLine execute(ProbeLine p) { 
					return p.strand.equals("+") ? p : null;
				}
			};
			Filter<ProbeLine,ProbeLine> minus = new Filter<ProbeLine,ProbeLine>() { 
				public ProbeLine execute(ProbeLine p) { 
					return p.strand.equals("-") ? p : null;
				}
			};

			System.out.println(String.format("Outputting background-removed results..."));
			File plusOut = new File(props.getDirectory(), String.format("%s_plus.corrected", key)); 
			File minusOut = new File(props.getDirectory(), String.format("%s_negative.corrected", key)); 
			
			outputProbes(new FilterIterator<ProbeLine,ProbeLine>(plus, norm.iterator()), plusOut); 
			System.out.println(String.format("\t%s", plusOut.getAbsolutePath()));

			outputProbes(new FilterIterator<ProbeLine,ProbeLine>(minus, norm.iterator()), minusOut);
			System.out.println(String.format("\t%s", minusOut.getAbsolutePath()));

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void outputProbes(Iterator<ProbeLine> lines, File f) throws IOException { 
		PrintStream ps = new PrintStream(new FileOutputStream(f));
		while(lines.hasNext()) { 
			ProbeLine line = lines.next();
			ps.println(line.toString());
		}
		ps.close();
	}
	
	public static enum LogTransform { LOG_TO_EXP, EXP_TO_LOG, NONE };
	
	private LogTransform transform;
	private BackgroundEstimation bg;
	private AdditiveCorrection correction;
	
	public BackgroundRemoval(BackgroundEstimation est) { 
		this(est, LogTransform.NONE);
	}
	
	public BackgroundRemoval(BackgroundEstimation est, LogTransform tr) { 
		bg = est;
		transform = tr;
		correction = new AdditiveCorrection(bg);
	}
	
	public void load(Iterator<ProbeLine> ps) {
		if(transform.equals(LogTransform.LOG_TO_EXP)) { 
			ps = new MapperIterator<ProbeLine,ProbeLine>(new ExpMapper(), ps);			
		} else if (transform.equals(LogTransform.EXP_TO_LOG)) { 
			ps = new MapperIterator<ProbeLine,ProbeLine>(new LogMapper(), ps);
		}
		correction.load(ps);
	}
	
	public void removeBackground() { 
		correction.correct();
	}
	
	public Iterator<ProbeLine> iterator() {
		Iterator<ProbeLine> itr = correction.iterator();
		if(transform.equals(LogTransform.LOG_TO_EXP)) { 
			itr = new MapperIterator<ProbeLine,ProbeLine>(new LogMapper(), itr);			
		} else if (transform.equals(LogTransform.EXP_TO_LOG)) { 
			itr = new MapperIterator<ProbeLine,ProbeLine>(new ExpMapper(), itr);
		}
		return itr;
	}
	
	public static class ExpMapper implements Mapper<ProbeLine,ProbeLine> { 
		public ProbeLine execute(ProbeLine p) { 
			return p.expLine();
		}
	}

	public static class LogMapper implements Mapper<ProbeLine,ProbeLine> { 
		public ProbeLine execute(ProbeLine p) { 
			return p.logLine();
		}
	}
}
