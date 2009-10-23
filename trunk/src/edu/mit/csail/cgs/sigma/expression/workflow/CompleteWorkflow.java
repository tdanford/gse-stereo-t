/*
 * Author: tdanford
 * Date: Apr 27, 2009
 */
package edu.mit.csail.cgs.sigma.expression.workflow;

import java.io.File;
import java.io.IOException;

public class CompleteWorkflow {
	
	public static void main(String[] args) {
		 if(args.length > 0) { 
			 process(args);
		 } else { 
			 try { 
				 process("test_plus.data");
			 } catch(Exception e) { 
				 e.printStackTrace(System.err);
			 }
			 
			 try { 
				 process("test_negative.data");				 
			 } catch(Exception e) {
				 e.printStackTrace(System.err);
			 }
			                      
		 }
	}

	public static void process(String... args) {
		WorkflowProperties props = new WorkflowProperties();
		String defaultInputName = "test-output.data";
		
		// The idea is that we either (a) take the name we're given on the command 
		// line, or (b) find the "next file" starting from the given default 
		// input file for the workflow.
		File input = props.getMostRecentWorkflowFile(
				new File(props.getDirectory(), args[0]));
		
		Workflow worker = new Workflow(props, args);
		try {
			while((input = worker.processFile(input)) != null) { 
				System.out.println(String.format("Processed to: %s",
						input.getName()));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
