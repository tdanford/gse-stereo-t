/*
 * Author: tdanford
 * Date: Jun 16, 2008
 */
package edu.mit.csail.cgs.sigma;

public class ParserException extends Exception {
    
    private String line;
    private int lineno;

    public ParserException(int lineno, String line, Exception e) { 
        super(String.format("Exception \"%s\" on Line #%d: \"%s\"", e.getMessage(), lineno, line), e);
    }
    
    public String getLine() { return line; }
    public int getLineNumber() { return lineno; }
}
