/* 
** Author(s): Miguel Calejo
** Contact:   interprolog@declarativa.com, http://www.declarativa.com, http://www.xsb.com
** Copyright (C) XSB Inc., USA, 2001-2005
** Use and distribution, without any warranties, under the terms of the 
** GNU Library General Public License, readable in http://www.fsf.org/copyleft/lgpl.html
*/
package com.xsb.interprolog;
import com.declarativa.interprolog.*;
import com.declarativa.interprolog.util.*;

public class XSBNativeEngineTest extends NativeEngineTest{
	public XSBNativeEngineTest(String name){super(name);}
	protected AbstractPrologEngine buildNewEngine(){
		//String dir = new XSBPeer().getBinDirectoryProperty(System.getProperties());
		return new NativeEngine(/*dir*/);
	}
    protected void setUp() throws java.lang.Exception{
		engine.command("import append/3,length/2 from basics");		
    }
    protected void tearDown() throws java.lang.Exception{
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