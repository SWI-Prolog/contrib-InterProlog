/* 
** Author(s): Miguel Calejo
** Contact:   interprolog@declarativa.com, http://www.declarativa.com
** Copyright (C) Declarativa, Portugal, 2000-2002
** Use and distribution, without any warranties, under the terms of the 
** GNU Library General Public License, readable in http://www.fsf.org/copyleft/lgpl.html
*/
package com.declarativa.interprolog.util;
import java.io.Serializable;
import com.declarativa.interprolog.*;

/** Used to serialize results for deterministicGoal */
public class ResultFromProlog implements Serializable{
	/** Same as passed in GoalFromJava*/
	public int timestamp;
	/** Goal has succeeded */
	public boolean succeeded;
	/** Object array corresponding to the result variable list */
	public Object[] rVars;
	/** Error message, null if none; used to be a String, now can be anything to cater for Prolog exceptions */
	public Object error;
	//public String error;
	public ResultFromProlog(int t,boolean s,int size,Object e){
		rVars = new Object[size];
		timestamp=t; succeeded=s; error=e;
	}
	public String toString(){
		return "ResultFromProlog: timestamp=="+timestamp+", error=="+error;
	}
	/** Prolog complaining about being interrupted. 
	The engine parameter is necessary as the interrupt detection may depend on Prolog implementation or version*/
	public boolean wasInterrupted(AbstractPrologEngine engine){
		// return  error!=null && ("_$abort_ball".equals(error.toString()) || "interprolog_interrupt".equals(error.toString()));
		return  error!=null && engine.getImplementationPeer().isInterrupt(error);		
	}
}

