/* 
** Author(s): Miguel Calejo
** Contact:   interprolog@declarativa.com, http://www.declarativa.com
** Copyright (C) Declarativa, Portugal, 2000-2005
** Use and distribution, without any warranties, under the terms of the 
** GNU Library General Public License, readable in http://www.fsf.org/copyleft/lgpl.html
*/
package com.declarativa.interprolog;
import junit.framework.*;
import java.util.*;
import com.declarativa.interprolog.util.*;

public class XSBSubprocessEngineTest extends SubprocessEngineTest {
	public XSBSubprocessEngineTest(String name){
		super(name);
	}
	protected void setUp() throws java.lang.Exception{
		super.setUp();
		engine.deterministicGoal("import append/3,length/2 from basics");		
		engine.waitUntilAvailable(); 
    }
	// JUnit reloads all classes, clobbering variables, 
	// so the path should be obtained elsewhere:
	protected AbstractPrologEngine buildNewEngine(){
		return new XSBSubprocessEngine(/*true*/);
	}
	public void testPrologInstallDir(){
		PrologImplementationPeer peer = engine.getImplementationPeer();
		// The following is too complicated for the assertion presently tested, but ready for deeper stuff:
		engine.teachOneObject(new ConfigurationItem());
		String g = "findall(Obj, ( F=install_dir, xsb_configuration(F,V), ";
		//String g = "findall(Obj, ( "+peer.getFVInstallDirGoal()+", ";
		g += "ipObjectSpec('com.declarativa.interprolog.XSBSubprocessEngineTest$ConfigurationItem',[feature=string(F),value=string(V)],Obj)";
		g += "),L), ipObjectSpec('ArrayOfObject',L,Array)";
		Object[] items = (Object[])engine.deterministicGoal(g,"[Array]")[0];
		ConfigurationItem item = (ConfigurationItem) items[0];
		java.io.File f = new java.io.File(item.value);
		String path = engine.getPrologBaseDirectory();
		assertTrue(path.indexOf(f.getName()/*item.value*/)!=-1);
	}
	public static class ConfigurationItem implements java.io.Serializable{
		String feature,value;
		public String toString(){
			return "FEATURE "+feature+" HAS VALUE "+value;
		}
	}
	// XSB 2.7.1 has float problems on Linux:
	public void testNumbers2(){
		if (AbstractPrologEngine.isWindowsOS()||AbstractPrologEngine.isMacOS())
			super.testNumbers2();
		else System.err.println("Skipping testNumbers2");
	}
	public void testNumbers(){
		if (AbstractPrologEngine.isWindowsOS()||AbstractPrologEngine.isMacOS())
			super.testNumbers();
		else System.err.println("Skipping testNumbers2");
	}
}
