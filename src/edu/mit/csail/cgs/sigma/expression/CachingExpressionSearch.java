/*
 * Author: tdanford
 * Date: May 21, 2008
 */
package edu.mit.csail.cgs.sigma.expression;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.*;

import edu.mit.csail.cgs.datasets.species.Genome;
import edu.mit.csail.cgs.ewok.verbs.Mapper;
import edu.mit.csail.cgs.sigma.*;
import edu.mit.csail.cgs.sigma.expression.models.Transcript;

public class CachingExpressionSearch implements ExpressionFinder {
    
    private BaseExpressionProperties props;
    private boolean fg;
    private String exptKey, strain;
    private Genome genome;
    private ExpressionFinder searcher;
    private Logger logger;

    public CachingExpressionSearch(BaseExpressionProperties ps, ExpressionFinder search) {
        props = ps;
        searcher = search;
        fg = true;
        logger = props.getLogger("CachingExpressionSearch");
    }

    public CachingExpressionSearch(BaseExpressionProperties ps, ExpressionFinder search, boolean channel) {
        props = ps;
        searcher = search;
        fg = channel;
        logger = props.getLogger("CachingExpressionSearch");
    }

    public Collection<Transcript> findExpression() {
        
        File transcriptFile = props.getExpressionTranscriptFile(exptKey, fg);
        if(transcriptFile.exists()) { 
            try {
                SavedFile<Transcript> saved = new SavedFile<Transcript>(new TranscriptDecoder(genome),
                        new TranscriptEncoder(), transcriptFile);
                saved.load();
                return saved.getValues();
                
            } catch (IOException e) {
                logger.log(Level.SEVERE, e.getMessage());
            }
        } else { 
            logger.log(Level.INFO, String.format("Couldn't find transcripts in %s", transcriptFile.getName()));
        }
        
        logger.log(Level.FINE, "Creating Transcripts...");
        
        Collection<Transcript> transcripts = searcher.findExpression();

        try {
            SavedFile<Transcript> saver = new SavedFile<Transcript>(new TranscriptDecoder(genome),
                    new TranscriptEncoder(), transcriptFile, transcripts);
            saver.save();
            logger.log(Level.INFO, String.format("Saved transcripts: %s", transcriptFile.getName()));
            
        } catch (IOException e) {
            logger.log(Level.SEVERE, e.getMessage());
        }
        
        return transcripts;
    }

    public void closeData() {
        searcher.closeData();
    }

    public void loadData(String ek) {
        exptKey = ek;
        strain = props.parseStrainFromExptKey(exptKey);
        genome = props.getGenome(strain);
        searcher.loadData(exptKey);
    }
    
    private class TranscriptDecoder implements Mapper<String,Transcript> {
        private Genome genome;
        public TranscriptDecoder(Genome g) { 
            genome = g;
        }
        public Transcript execute(String a) {
            return Transcript.decode(genome, a);
        } 
    }
    
    private class TranscriptEncoder implements Mapper<Transcript,String> {
        public String execute(Transcript a) {
            return Transcript.encode(a);
        } 
    }
}
