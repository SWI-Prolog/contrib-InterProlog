/* 
** Author(s): Miguel Calejo, Vera Pereira
** Contact:   interprolog@declarativa.com, http://www.declarativa.com, http://www.xsb.com
** Copyright (C) XSB Inc., USA, 2001-2005
** Use and distribution, without any warranties, under the terms of the 
** GNU Library General Public License, readable in http://www.fsf.org/copyleft/lgpl.html
*/
package com.xsb.interprolog;

import com.declarativa.interprolog.*;
import com.declarativa.interprolog.util.*;
import java.io.*;

/** A XSB PrologEngine implemented using the Java Native Interface. This class depends on interprolog_callback.c and other files, 
that are included in the emu directory of XSB Prolog 2.5 and later */
public class NativeEngine extends AbstractNativeEngine {
    static { System.loadLibrary("xsb"); }

    protected static int numberOfInstances = 0;
  
	/** C-side debug flag */
    private native void xsb_setDebug(boolean debug);
    protected native int xsb_init_internal(String jXSBPath);
    protected native int xsb_init_internal_arg(String jXSBPath, String[] jXSBParameters);
    protected native int xsb_command_string(String jCommandString);
    protected native int xsb_close_query();
    /** Returns the xsb_query result code */
    protected native int put_bytes(byte[] b, int size, int args, String jStr);
    //protected native byte[] get_bytes(); unnecessary
    /** Simulates a ctrl-C */
    protected native void xsb_interrupt();
    
    /** Calls goal Functor(array), returns true if it suceeds */
    protected boolean commandWithArray(String Functor, byte[] array, int nBytes){
    	int rc = put_bytes(array, nBytes, 1, Functor);
    	xsb_close_query();
    	return rc==0;
    }
    
    protected PrologImplementationPeer makeImplementationPeer(){
    	return new XSBPeer(this);
    }
    			
    public NativeEngine(){
        this(null,false);
    }
    
    public NativeEngine(String XSB_BIN_DIR){
        this(XSB_BIN_DIR,false);
    }
    
    public NativeEngine(String XSB_BIN_DIR, boolean debug){
    	this(XSB_BIN_DIR,null,debug,true);
    }
    
    public NativeEngine(String XSB_BIN_DIR, boolean debug, boolean loadFromJar){
    	this(XSB_BIN_DIR,null,debug,loadFromJar);
    }
    
    /**
     * The XSB dll must be in a directory mentioned in the java.library.path property
     * (e.g., Windows PATH) so the Java loader finds it. You can set this up on launching
      java using the command line option -Djava.library.path=...
	@param XSB_BIN_DIR File path to directory containing the Prolog binary
	@param XSB_ARGS Args for finer control initializing XSB
	@param debug if true, print progress messages to aid debugging
	@param loadFromJar if true, load Prolog initialization files from this classe's jar, else from a relative file directory
     */
    public NativeEngine(String XSB_BIN_DIR, String[] XSB_ARGS, boolean debug, boolean loadFromJar) {
        super(XSB_BIN_DIR,debug,loadFromJar);
        xsb_setDebug(debug);
   		numberOfInstances++;
        if (numberOfInstances > 1)
            throw new IPException("Can't have more than one instance of NativeEngine");
        int ret;
        progressMessage("Initializing XSB dll engine with XSB_ARGS=="+XSB_ARGS+" and base directory "+getPrologBaseDirectory());
        if(XSB_ARGS != null){
            ret = xsb_init_internal_arg(getPrologBaseDirectory(), XSB_ARGS);
        } else {
            ret = xsb_init_internal(getPrologBaseDirectory());
        }
        if (ret != 0)
            throw new IPException("XSB Initialization error");
        progressMessage("Moving on...");
        setupPrologSide();
        startTopGoal();
    }
    
    public void setDebug(boolean d){
        super.setDebug(d);
        xsb_setDebug(d);
    }
    
    public void shutdown(){
    	super.shutdown();
        System.err.println("NO REAL SHUTDOWN in NativeEngine YET!!!");
    }
    
    protected void doInterrupt(){
        xsb_interrupt();
    }
    
    public boolean realCommand(String s){
        int result = xsb_command_string(s+".");
        if (result==0)
            return true;
        else if (result==1)
            return false;
        else
            throw new IPException("Problem executing Prolog command");
    }    

}
