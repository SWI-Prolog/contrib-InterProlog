/* 
** Author(s): Miguel Calejo
** Contact:   interprolog@declarativa.com, http://www.declarativa.com
** Copyright (C) Declarativa, Portugal, 2000-2005
** Use and distribution, without any warranties, under the terms of the 
** GNU Library General Public License, readable in http://www.fsf.org/copyleft/lgpl.html
*/
package com.declarativa.interprolog;
import java.util.*;
import java.io.*;
import com.declarativa.interprolog.util.*;

public class XSBPeer extends PrologImplementationPeer{
    
    public XSBPeer(AbstractPrologEngine engine){
		super(engine);
		operators = new PrologOperatorsContext(PrologOperatorsContext.standardXSBOperators);
		systemName = "XSB Prolog";
	}
        
	public String getBinDirectoryProperty(Properties p){
		return p.getProperty("XSB_BIN_DIRECTORY");
	}
    
	public String executablePath(String d){
		return d + File.separator + "xsb";
	}
    
	protected String fetchPrologNumericVersion(){
		Object[] bindings = engine.deterministicGoal("xsb_configuration(version,V)","[string(V)]");
		return (String)bindings[0];
	}
    
    /*
	public String getFVInstallDirGoal(){
		return "(F=install_dir, xsb_configuration(F,V))";
	}*/
    
	public String[] alternativePrologExtensions(String filename){
		if (!(filename.indexOf('.')==-1)) throw new IPException("Bad use of alternativePrologExtensions");
		if (xsbUsesXwamExtension()) return new String[]{filename+".xwam",filename+".P"};
		else return new String[]{filename+".O",filename+".P"};
	}
	/** This XSB compiles Prolog files into .xwam files. This method works by XSB file presence detection, to be available early in the startup process */
	private boolean xsbUsesXwamExtension(){
		String XSBbase = engine.getPrologBaseDirectory();
		String compilePath = XSBbase+File.separator+"cmplib"+File.separator+"compile";
		if ((new File(compilePath+".xwam")).exists()) return true;
		if ((new File(compilePath+".O")).exists()) return false;
		
		throw new IPException("Weird XSB Prolog installation, could find neither compile.O nor compile.xwam in cmplib:"+compilePath);
	}
    
	/** Assumes that Prolog options can not include "/bin/"... */
	public String prologBinToBaseDirectory(String binDirectoryOrStartCommand){
		binDirectoryOrStartCommand = binDirectoryOrStartCommand.trim();
		/* CAN'T DO: directories may have spaces within...
		int firstSpace = binDirectoryOrStartCommand.indexOf(' ');
		if (firstSpace!=-1) 
			binDirectoryOrStartCommand = binDirectoryOrStartCommand.substring(0,firstSpace);
			*/
		/* This would be nice to get rid of relative paths, but would lose the drive under Windows:
		try{
			binDirectoryOrStartCommand = new File(binDirectoryOrStartCommand).getCanonicalPath();
		} catch (IOException e){
			throw new IPException("Bad file path:"+e);
		}*/
		int baseEnd = binDirectoryOrStartCommand.lastIndexOf(File.separator+"config"+File.separator);
		if (baseEnd==-1) 
			throw new IPException("Can not determine base directory, missing config in known path!");
		binDirectoryOrStartCommand = binDirectoryOrStartCommand.substring(0,baseEnd);
		if (binDirectoryOrStartCommand.endsWith(File.separator)) 
			return binDirectoryOrStartCommand.substring(0,binDirectoryOrStartCommand.length()-1);
		else return binDirectoryOrStartCommand;
	}
    
	public Recognizer makePromptRecognizer(){
		return new Recognizer("| ?-");
	}
    
	public Recognizer makeBreakRecognizer(){
		return new Recognizer(": ?-");
	}
    
    public String interprologFilename() {
		return "xsb/interprolog";
	}
    
	public String visualizationFilename(){
		return "visualization";
	}
	
	public boolean isInterrupt(Object error){
		return error.toString().startsWith("Aborting");
	}

}