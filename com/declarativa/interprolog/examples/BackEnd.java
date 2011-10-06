/* 
** Author(s): Miguel Calejo
** Contact:   interprolog@declarativa.com, http://www.declarativa.com
** Copyright (C) Declarativa, Portugal, 2000-2005
** Use and distribution, without any warranties, under the terms of the 
** GNU Library General Public License, readable in http://www.fsf.org/copyleft/lgpl.html
*/
package com.declarativa.interprolog.examples;
import com.declarativa.interprolog.*;
import com.xsb.interprolog.*;
import java.util.*;
public class BackEnd{
	public static class Record implements java.io.Serializable{
		String plainText;
		int plainCount;
		String cyberText;
		float cyberCount;
		public String toString(){
			return "plain:"+plainText+" plainCount:"+plainCount+" \ncyberText:"+cyberText+" cyberCount="+cyberCount;
		}
	}
	public static void main(String args[]) {
		//String prologCommand = com.declarativa.interprolog.gui.ListenerWindow.commandArgs(args);
		PrologEngine engine = new NativeEngine(/*prologCommand*/);
		engine.deterministicGoal("import reverse/2,length/2 from basics"); //  list processing predicates
		if (!engine.deterministicGoal("length([1,2],2)")) System.err.println("Bad length/2 predicate!");
		engine.teachOneObject(new Record()); // send an object prototype to Prolog
		Record r = new Record();
		r.plainText="Declarative is good"; r.plainCount=r.plainText.length();
		Object[] objectsToGo = new Object[]{r};
		String goal = "ipObjectSpec('com.declarativa.interprolog.examples.BackEnd$Record',[plainCount=PC,plainText=string(S)],R)";
		goal += ", name(S,Chars), length(Chars,PC), reverse(Chars,Reversed), name(RS,Reversed), CC is PC+0.0";
		goal += ", ipObjectSpec('com.declarativa.interprolog.examples.BackEnd$Record',[cyberText=string(RS),cyberCount=CC],NewR)";
		System.out.println("Calling "+goal);
		Object[] bindings = engine.deterministicGoal(goal, "[R]", objectsToGo, "[NewR]");
		Record result = (Record)bindings[0];
		System.out.println(result);
		engine.shutdown();
	}
}