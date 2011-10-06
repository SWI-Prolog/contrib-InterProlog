/* 
** Author(s): Miguel Calejo
** Contact:   interprolog@declarativa.com, http://www.declarativa.com
** Copyright (C) Declarativa, Portugal, 2000-2002
** Use and distribution, without any warranties, under the terms of the 
** GNU Library General Public License, readable in http://www.fsf.org/copyleft/lgpl.html
*/
package com.declarativa.interprolog.util;

/** An Exception related to Prolog processing in general */
public class IPException extends RuntimeException{
	private Throwable cause;
	
	public IPException(String s){
		super(s);
		//System.err.println("IPException about to be thrown:"+this);
		cause=null;
	}
    
    public IPException(String s, Throwable cause) {
        super(s);
        this.cause=cause;
    }
    
    /** To allow compilation under JDK 1.3; this method is already defined in Throwable in later JDKs */
    public Throwable getCause(){
    	return cause;
    }
}

