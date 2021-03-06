/* 
** Author(s): Miguel Calejo
** Contact:   interprolog@declarativa.com, http://www.declarativa.com
** Copyright (C) Declarativa, Portugal, 2000-2005
** Use and distribution, without any warranties, under the terms of the 
** GNU Library General Public License, readable in http://www.fsf.org/copyleft/lgpl.html
*/
package com.declarativa.interprolog;
import com.declarativa.interprolog.util.*;
import java.io.*;
import java.net.*;

/** A PrologEngine encapsulating a <a href='http://xsb.sourceforge.net'>XSB Prolog</a> engine, accessed over TCP/IP sockets. 
*/
public class XSBSubprocessEngine extends SubprocessEngine{
    protected PrologImplementationPeer makeImplementationPeer(){
    	return new XSBPeer(this);
    }
    public XSBSubprocessEngine(String prologCommand, boolean debug, boolean loadFromJar){
    	super(prologCommand, debug, loadFromJar);
    }
    public XSBSubprocessEngine(String prologCommand, boolean debug){
    	super(prologCommand, debug);
    }
    public XSBSubprocessEngine(String prologCommand){
    	super(prologCommand);
    }
    public XSBSubprocessEngine(boolean debug){
    	super(debug);
    }
    /** @see com.declarativa.interprolog.AbstractPrologEngine#AbstractPrologEngine(String prologBinDirectoryOrCommand, boolean debug, boolean loadFromJar)*/
    public XSBSubprocessEngine(){
    	super();
    }

	protected synchronized void doInterrupt(){
	    setDetectPromptAndBreak(true);
	    try {
		if(isWindowsOS()){
				// Windows
		    byte[] ctrlc = {3};
		    progressMessage("Attempting to interrupt Prolog...");
		    OutputStream IS = intSocket.getOutputStream();
		    IS.write(ctrlc); IS.flush();
		} else{
				// we'll just use a standard UNIX signal
		    progressMessage("Interrupting Prolog with "+interruptCommand);
		    Runtime.getRuntime().exec(interruptCommand);
		}
			
	    } 
	    catch(IOException e) {throw new IPException("Exception in interrupt():"+e);}
	    waitUntilAvailable();
	    //System.out.println("aqui");
	    sendAndFlushLn("abort."); // leave break mode
	    //sendAndFlushLn("end_of_file."); // leave break mode
	    waitUntilAvailable();
	    //System.out.println("aqui2");
	}
	
}


