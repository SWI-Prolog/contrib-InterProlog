/* 
** Author(s): Miguel Calejo
** Contact:   interprolog@declarativa.com, http://www.declarativa.com
** Copyright (C) Declarativa, Portugal, 2000-2002
** Use and distribution, without any warranties, under the terms of the 
** GNU Library General Public License, readable in http://www.fsf.org/copyleft/lgpl.html
*/
package com.declarativa.interprolog.util;
import com.declarativa.interprolog.*;

/** A goal scheduled to execute in Prolog whenever possible */
public class GoalToExecute{
	private GoalFromJava goal;
	private ResultFromProlog result;
	private boolean executing;
	private boolean ended;
	private boolean firstGoalStatus = false;
	// something to do in the creator thread
	private Runnable todo=null;
	Thread callerThread;
	
	public GoalToExecute(GoalFromJava goal){
		this(goal,Thread.currentThread());
	}
	
	public GoalToExecute(GoalFromJava goal,Thread t){
		this.goal = goal; 
		result=null; ended=false; executing=false;
		callerThread = t;
	}
	
	public Thread getCallerThread(){
		return callerThread;
	}
	
	public synchronized void executeInThread(Runnable r){
		if(todo!=null||!executing/* comented because of SubprocessEngine:||firstGoalStatus*/||result!=null || hasEnded()){
			System.err.println("bad");
			System.err.println("r=="+r);
			System.err.println("todo=="+todo);
			System.err.println("executing=="+executing);
			System.err.println("firstGoalStatus=="+firstGoalStatus);
			System.err.println("result=="+result);
			System.err.println("hasEnded()=="+hasEnded());
			throw new IPException("bad execute");
		}
		todo=r;
		notify();
	}
	
	/** Obtain result for a Prolog goal, blocking until it is available; meanwhile it will execute
	Runnables if so requested */
	public synchronized ResultFromProlog waitForResult(){
		if (ended) return result;
		while(true){
			if(todo!=null) {
				todo.run();
				todo=null;
			}
			if(result!=null) break;
			try { wait();}
			catch(InterruptedException e){throw new IPException("Unexpected:"+e);}
			// System.out.println("waitForResult loop: result=="+result+",todo=="+todo);
			if (result==null && todo==null) throw new IPException("Inconsistency in GoalToExecute");
		}
		return result; 
	}
	
	public synchronized void setResult(ResultFromProlog result){
		if (this.result!=null || hasEnded() || todo!=null) {
			throw new IPException("Inconsistency in GoalToExecute");
		}
		this.result=result;
		ended=true;
		notify();
	}
	
	public boolean wasInterrupted(){
		return hasEnded() && "interrupted".equals(result.error);
	}

	public boolean wasAborted(){
		return hasEnded() && "aborted".equals(result.error);
	}

	/** Used on the InterProlog Java side to "cascade" an interrupt over pending goals to execute */
	public synchronized void interrupt(){
		raiseError("interrupted");
	}
	
	/** Used on the InterProlog Java side to "cascade" an abort over pending goals to execute */
	public synchronized void abort(){
		raiseError("aborted");
	}
	
	private void raiseError(String s){
		if (result==null) result = new ResultFromProlog(-1,false,0,null);
		result.error=s;
		ended=true;
		notify();
	}
	
	public GoalFromJava getGoal(){ return goal;}
	
	public void prologWasCalled(){
		if (executing) throw new IPException("Bad use of prologWasCalled");
		executing=true;
	}
	
	public boolean hasStarted(){
		return executing;
	}
	
	public boolean hasEnded(){
		return ended;
	}
	
	public int getTimestamp(){return goal.timestamp;}
	
	public void setFirstGoalStatus(){
		firstGoalStatus = true;
	}
	
	public boolean isFirstGoal(){
		return firstGoalStatus;
	}
	
	public String toString(){
		return "GTE's ResultFromProlog: timestamp=="+getTimestamp();
	}
}