/*
 * Author: tdanford
 * Date: Mar 3, 2009
 */
package edu.mit.csail.cgs.sigma;

import java.io.*;

import edu.mit.csail.cgs.datasets.species.Genome;
import edu.mit.csail.cgs.utils.Pair;
import edu.mit.csail.cgs.utils.models.Model;
import edu.mit.csail.cgs.utils.models.ModelInput;

public class GenomeModel extends Model {
	
	public static void main(String[] args) { 
		SigmaProperties props = new SigmaProperties();
		Genome g1 = props.getGenome("s288c");
		Genome g2 = props.getGenome("sigma");
		File dir = props.getBaseDir();
		File f1 = new File(dir, "s288c.genome");
		File f2 = new File(dir, "sigma.genome");
		
		GenomeModel gm1 = new GenomeModel(g1);
		GenomeModel gm2 = new GenomeModel(g2);
		
		try {
			PrintStream ps = new PrintStream(new FileOutputStream(f1));
			ps.println(gm1.asJSON().toString());
			ps.close();

			ps = new PrintStream(new FileOutputStream(f2));
			ps.println(gm2.asJSON().toString());
			ps.close();
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}


	public static Genome loadGenome(File genomeFile) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(genomeFile));
		GenomeModel model = null;
		ModelInput<GenomeModel> input = 
			new ModelInput.LineReader<GenomeModel>(GenomeModel.class, br);
		model = input.readModel();
		br.close();
		return model.genome();
	}

	public String species, version;
	public ChromModel[] chroms;
	
	public GenomeModel() {}
	
	public GenomeModel(String s, String v, Integer[] lens) { 
		species = s;
		version = v;
		chroms = new ChromModel[lens.length];
		for(int i = 0; i < lens.length; i++) { 
			chroms[i] = new ChromModel(String.format("%d", i+1), lens[i]);
		}
	}
	
	public GenomeModel(Genome g) { 
		species = g.getSpecies();
		version = g.getVersion();
		chroms = new ChromModel[g.getChromList().size()];
		int i = 0;
		for(String chrom : g.getChromList()) { 
			int len = g.getChromLength(chrom);
			chroms[i++] = new ChromModel(chrom, len);
		}
	}
	
	public Genome genome() { 
		Pair<String,Integer>[] ps = new Pair[chroms.length];
		for(int i = 0; i < chroms.length; i++) { 
			ps[i] = new Pair<String,Integer>(chroms[i].name, chroms[i].length);
		}
		return new Genome(species, version, ps);
	}
	
	public String[] names() { 
		String[] ns = new String[chroms.length];
		for(int i = 0; i < chroms.length; i++) { ns[i] = chroms[i].name; }
		return ns;
	}
	
	public Integer[] lengths() { 
		Integer[] ns = new Integer[chroms.length];
		for(int i = 0; i < chroms.length; i++) { ns[i] = chroms[i].length; }
		return ns;
	}
	
	public static class ChromModel extends Model { 
		public String name;
		public Integer length;
		
		public ChromModel() {}
		public ChromModel(String n, Integer l) { name = n; length = l; }
	}
}
