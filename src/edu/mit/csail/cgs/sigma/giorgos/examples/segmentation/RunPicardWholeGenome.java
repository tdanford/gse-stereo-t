package edu.mit.csail.cgs.sigma.giorgos.examples.segmentation;

import java.io.File;
import java.io.IOException;
import java.util.Vector;

import edu.mit.csail.cgs.sigma.FilePrinter;
import edu.mit.csail.cgs.sigma.expression.segmentation.Segmenter;
import edu.mit.csail.cgs.sigma.expression.workflow.WorkflowChunkReader;
import edu.mit.csail.cgs.sigma.expression.workflow.WorkflowChunker;
import edu.mit.csail.cgs.sigma.expression.workflow.WorkflowDataLoader;
import edu.mit.csail.cgs.sigma.expression.workflow.WorkflowPackageReader;
import edu.mit.csail.cgs.sigma.expression.workflow.WorkflowPackager;
import edu.mit.csail.cgs.sigma.expression.workflow.WorkflowSegmentation;
import edu.mit.csail.cgs.sigma.expression.workflow.models.Chunk;
import edu.mit.csail.cgs.sigma.expression.workflow.models.FileInputData;
import edu.mit.csail.cgs.sigma.expression.workflow.models.InputSegmentation;
import edu.mit.csail.cgs.sigma.giorgos.PicardFlatSegmenter;

public class RunPicardWholeGenome {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		int split = (int) Double.POSITIVE_INFINITY;
		int min = 20;
		int channel = 0;
		
		int k_start = 10, k_end = 50;
		int k_step = 10;
		Vector<Integer> k_vec = new Vector<Integer>();
		k_vec.add(5);
		for(int k = k_start; k <= k_end; k += k_step)
			k_vec.add(k);
		
		
		
		Integer[] k_arr = k_vec.toArray(new Integer[0]);
		
		String path = "/afs/csail.mit.edu/u/g/geopapa/Desktop/Geopapa/Research/Expts/test/";
		String chrDir = "chr01/";
		String chunkDir = "chunks/";
		String packDir = "packages/";
		String segDir = "segs/";
		String[] chrFiles = {"chr01_+_0-1000.txt"};
		
		for(int i = 0; i < chrFiles.length; i++)
			chrFiles[i] = path + chrDir + chrFiles[i]; 
		
		try {
			
			for(String s : chrFiles)
			{   
				File f = new File(s);
				////////////////////
				// DATA -> CHUNKS /
				//////////////////
				
				// load data file
				WorkflowDataLoader loader = new WorkflowDataLoader(f);
				
				// data -> chunks
				WorkflowChunker chunker = new WorkflowChunker(split,loader);
				
				// create chunks' file
				File outputChunks = new File(path + chunkDir + f.getName() + "_chunks.txt");
				new FilePrinter<Chunk>(outputChunks).printAndClose(chunker);
				
				
				////////////////////////
				// CHUNKS -> PACKAGES /
				//////////////////////
				
				// Load chunks file 
				WorkflowChunkReader chunkReader = new WorkflowChunkReader(outputChunks);
				
				// chunks -> packages 
				WorkflowPackager packages = new WorkflowPackager(channel, chunkReader);
				
				// create packages' file
				File outputPackages = new File(path + packDir + f.getName() + "_packages.txt");
				new FilePrinter<FileInputData>(outputPackages).printAndClose(packages);
	
				
				//////////////////////////
				// PACKAGES -> SEGMENTS /
				////////////////////////
				
				// Load packages file
				WorkflowPackageReader packageReader = new WorkflowPackageReader(outputPackages);
				
				// packages -> segments
				PicardFlatSegmenter fitter = new PicardFlatSegmenter(k_arr);
				fitter.setPenaltyModel("lavielle", 0.49);
				//fitter.setPenaltyModel("bic", 0.49);
				WorkflowSegmentation workflowSeg = new WorkflowSegmentation(min, null, fitter, packageReader);
				
				// create segments file
				File outputSegm = new File(path + segDir + f.getName() + "_segments.txt");
				new FilePrinter<InputSegmentation>(outputSegm).printAndClose(workflowSeg);
				
			}
		} 
		
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}// end of main method

}
