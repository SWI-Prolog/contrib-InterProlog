/* 
** Author(s): Miguel Calejo
** Contact:   interprolog@declarativa.com, http://www.declarativa.com
** Copyright (C) Declarativa, Portugal, 2000-2002
** Use and distribution, without any warranties, under the terms of the 
** GNU Library General Public License, readable in http://www.fsf.org/copyleft/lgpl.html
*/
package com.declarativa.interprolog.util;
import java.io.*;
import java.util.*;
import com.declarativa.interprolog.*;

/** An object consuming input from a stream, analysing it and sending messages to a list 
of OutputListener objects; if a debugStream is provided it gets a copy of all the input read  */
public class OutputHandler extends Thread {
	InputStream sourceStream;
	PrintStream debugStream;
	Vector listeners;
	private boolean ignoreStreamEnd;
	String name;
	
	public OutputHandler(InputStream s,OutputStream debugStream,String name){
		if (s instanceof BufferedInputStream) sourceStream = s;
		else sourceStream = new BufferedInputStream(s);
		setDebugStream(debugStream);
		listeners = new Vector();
		ignoreStreamEnd=false;
		this.name=name;
	}
	
	public OutputHandler(InputStream s,OutputStream debugStream){
		this(s,debugStream,"An OutputHandler");
	}
	
	public OutputHandler(InputStream s){
		this(s,null);
	}
	
	public synchronized void addOutputListener(OutputListener ol){
		listeners.addElement(ol);
	}
	public synchronized void removeOutputListener(OutputListener ol){
		listeners.removeElement(ol);
	}
	
	public boolean hasListener(OutputListener ol){
		return listeners.contains(ol);
	}
	
	public void run(){
		byte[] buffer = new byte[1024]; 
		while(true) {
			try{
				int nchars = sourceStream.read(buffer,0,buffer.length);
				if (nchars==-1){
					fireStreamEnded();
					break;
				} else fireABs(buffer,nchars);
			} catch (IOException ex){ throw new IPException("Problem fetching output:"+ex);}
		}
	}
	synchronized void fireStreamEnded(){
		if (ignoreStreamEnd) return;
		for (int L=0; L<listeners.size(); L++)
			((OutputListener)(listeners.elementAt(L))).streamEnded();
		if(debugStream!=null) 
		debugStream.println("PROLOG "+name+" ENDED");
	}
	synchronized void fireABs(byte[] buffer,int nbytes){
		for (int L=0; L<listeners.size(); L++)
			((OutputListener)(listeners.elementAt(L))).analyseBytes(buffer,nbytes);
		if(debugStream!=null) 
		debugStream.println("PROLOG "+name+":"+new String(buffer,0,nbytes));
	}
	
	public void setIgnoreStreamEnd(boolean ignore){
		ignoreStreamEnd=ignore;
	}
	public void setDebugStream(OutputStream debugStream){
		if (debugStream==null || debugStream instanceof PrintStream) this.debugStream=(PrintStream)debugStream;
		else this.debugStream=new PrintStream(debugStream);
	}
}

