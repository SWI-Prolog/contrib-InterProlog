/* 
** Author(s): Miguel Calejo
** Contact:   interprolog@declarativa.com, http://www.declarativa.com
** Copyright (C) Declarativa, Portugal, 2000-2004
** Use and distribution, without any warranties, under the terms of the 
** GNU Library General Public License, readable in http://www.fsf.org/copyleft/lgpl.html
*/
package com.declarativa.interprolog.util;
/** An Exception originated by a Prolog error */
public class IPPrologError extends IPException{
	Object t;
	public IPPrologError(Object t){
		super(t.toString());
		this.t=t;
	}
	public String toString(){return t.toString();}
	
	public Object getError(){return t;}
}

