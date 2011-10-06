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

/** A PrologEngine encapsulating a <a href='http://www.ncc.up.pt/~vsc/Yap/'>YAP Prolog</a> engine, accessed over TCP/IP sockets. 
*/
public class YAPSubprocessEngine extends SubprocessEngine{
    protected PrologImplementationPeer makeImplementationPeer(){
    	return new YAPPeer(this);
    }
    public YAPSubprocessEngine(String prologCommand, boolean debug, boolean loadFromJar){
    	super(prologCommand, debug, loadFromJar);
    }
    public YAPSubprocessEngine(String prologCommand, boolean debug){
    	super(prologCommand, debug);
    }
    public YAPSubprocessEngine(String prologCommand){
    	super(prologCommand);
    }
    public YAPSubprocessEngine(boolean debug){
    	super(debug);
    }
    public YAPSubprocessEngine(){
    	super();
    }
	protected PrologOutputObjectStream buildPrologOutputObjectStream(OutputStream os) throws IOException{
		return new PrologOutputObjectStream(os,true /* use escape byte mechanism */);
	}	
	public boolean realCommand(String s){
		progressMessage("COMMAND:"+s+".");
		sendAndFlushLn("("+s+"), write('"+YAPPeer.REGULAR_PROMPT+"'), flush_output, !, fail."); // to make sure YAP doesn't hang showing variables
		return true; // we do not really know
	}
	protected void prepareInterrupt(String myHost) throws IOException{ // requires successful startup steps
	    if (isWindowsOS()) 
	    	progressMessage("InterProlog does not support interrupts to YAP on Windows, only on Unix");
	    else {
			intServerSocket = new ServerSocket(0);
			command("setupWindowsInterrupt('"+myHost+"',"+intServerSocket.getLocalPort()+")");
			intSocket = intServerSocket.accept();
			progressMessage("interrupt prepared");
		}
	}
	protected synchronized void doInterrupt(){
	    if (isWindowsOS()) throw new IPException("InterProlog does not support interrupts to YAP on Windows");
	    try {
			// Always use the "Windows" strategy, relying on a Prolog thread:
		    byte[] ctrlc = {3};
		    progressMessage("Attempting to interrupt Prolog...");
		    OutputStream IS = intSocket.getOutputStream();
		    IS.write(ctrlc); IS.flush();
	    } 
	    catch(IOException e) {throw new IPException("Exception in interrupt():"+e);}
	    waitUntilAvailable();
	}
}


