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
import java.util.*;
import java.lang.reflect.*;

/** A PrologEngine implemented over TCP/IP sockets. A SubprocessEngine object represents and gives access to a running Prolog process in background.
Multiple instances correspond to multiple Prolog processes, outside the Java Virtual Machine. 
*/
public abstract class SubprocessEngine extends AbstractPrologEngine{

    Process prolog;
    PrintWriter prologStdin;
    OutputHandler stdoutHandler, stderrHandler;
    ServerSocket serverSocket;
    protected Socket socket;
    ServerSocket intServerSocket=null; Socket intSocket=null; // Used only for a particular way of interrupting Prolog
    String interruptCommand=null; // Used only for UNIX
    Vector listeners = new Vector();
    protected boolean available;
    
    static class ClientRecognizer extends Recognizer implements RecognizerListener{
        PrologOutputListener client;
        ClientRecognizer(PrologOutputListener client){
            this.client=client;
            addRecognizerListener(this);
        }
        public void recognized(Recognizer source,Object extra){
            client.print((String)extra);
        }
    }
    
    /**
     * Add a PrologOutputListener to this engine.  All stdout and stderr output will be routed to the client.
     * @param client An object interested in receiving messages depicting Prolog's progress
     * @see com.declarativa.interprolog.PrologOutputListener
     */
    public synchronized void addPrologOutputListener(PrologOutputListener client) {
        ClientRecognizer RE = new ClientRecognizer(client);
        listeners.addElement(RE);
        addPrologStdoutListener(RE);
        addPrologStderrListener(RE);
    }
	
	public synchronized void removePrologOutputListener(PrologOutputListener client){
		for (int i=0;i<listeners.size();i++) {
			ClientRecognizer cr = (ClientRecognizer)(listeners.elementAt(i));
			if (cr.client==client) {
				listeners.removeElementAt(i);
				removePrologStdoutListener(cr);
				removePrologStderrListener(cr);
			}
		}
	}
	
	/** 
         * Add a OutputListener to get output from Prolog's standard output.
         * This is a lower level interface than addPrologOutputListener(PrologOutputListener).
         * @param client An object interested in Prolog's standard output
         * @see com.declarativa.interprolog.util.OutputListener
         */
	public void addPrologStdoutListener(OutputListener client){
		stdoutHandler.addOutputListener(client);
	}
	
	public void addPrologStderrListener(OutputListener l){
		stderrHandler.addOutputListener(l);
	}
	
	public void removePrologStdoutListener(OutputListener l){
		stdoutHandler.removeOutputListener(l);
	}
	
	public void removePrologStderrListener(OutputListener l){
		stderrHandler.removeOutputListener(l);
	}
	
	Recognizer promptTrigger = peer.makePromptRecognizer();
	Recognizer breakTrigger = peer.makeBreakRecognizer();
            
        /** Construct a SubprocessEngine, launching a Prolog process in background.
         * @param prologCommand The command to launch Prolog, as if given from a console shell.
         * Must not be null.
         * @param debug If true this engine will send debugging messages to System.out
         * @see SubprocessEngine#shutdown
         * @see SubprocessEngine#teachMoreObjects(ObjectExamplePair[])
         * @see SubprocessEngine#setDebug(boolean)
         */
	public SubprocessEngine(String prologCommand, boolean debug, boolean loadFromJar) {
            super(prologCommand,debug,loadFromJar);
            // Let's make sure PrologEngines get their finalize() message when we exit
            if (System.getProperty("java.version").compareTo("1.3")>=0) {
                Runtime.getRuntime().addShutdownHook(new Thread(){
                    public void run(){
                        if (prolog!=null) prolog.destroy();
                    }
                });
            } else {
                // For JDK 1.2 - considered unsafe
                // To avoid seeing warnings about deprecated methods
                // call the following instead of
                // System.runFinalizersOnExit(true);
                try{
                    Method finalizeOnExit = System.class.getMethod("runFinalizersOnExit",
                                            new Class[]{boolean.class});
                    finalizeOnExit.invoke(null,new Object[]{new Boolean(true)}); // for static methods first arg of invoke is ignored
                } catch (Exception e){
                }
                
            }
            
            try {
            	RecognizerListener availableSetter = new RecognizerListener(){
                    public void recognized(Recognizer source,Object extra){
                        available=true;
                        progressMessage("I'm available! source:"+source+" extra:"+extra);
                    }
                };
                promptTrigger.addRecognizerListener(availableSetter);
                breakTrigger.addRecognizerListener(availableSetter);
                if (prologCommand==null) 
                	prologCommand = prologBinDirectoryOrCommand;
                prolog = createProcess(prologCommand);
                
                // No explicit buffering, because it's already being done by our Process's streams
                // If not, OutputHandler will handle the issue
                stdoutHandler = new OutputHandler(prolog.getInputStream(),(debug?System.err:null),"stdout");
                stderrHandler = new OutputHandler(prolog.getErrorStream(),(debug?System.err:null),"stderr");
                setDetectPromptAndBreak(true);
                stdoutHandler.start();
                stderrHandler.start();
                Thread.yield(); // let's try to catch Prolog output ASAP
                prologStdin = new PrintWriter(prolog.getOutputStream());
                
                loadInitialFile();
                
                String myHost="127.0.0.1"; // to avoid annoying Windows dialup attempt
            	progressMessage("Allocating the ServerSocket...");
            	serverSocket = new ServerSocket(0); // let the system pick a port
            	progressMessage("server port:"+serverSocket.getLocalPort());
				//waitUntilAvailable(); Hangs Yap
                command("ipinitialize('"+myHost+"',"+
                	serverSocket.getLocalPort()+","+
                	registerJavaObject(this)+","+
                	debug +
                ")");
                progressMessage("Waiting for the socket to accept...");
                socket = serverSocket.accept();
                
                progressMessage("Teaching examples to Prolog...");
                PrologOutputObjectStream bootobjects = buildPrologOutputObjectStream(socket.getOutputStream());
                ObjectOutputStream oos = bootobjects.getObjectStream();
                teachIPobjects(oos);
                teachBasicObjects(oos);
                bootobjects.flush();
                progressMessage("Sent all examples...");
	            waitUntilAvailable();
	            setupCallbackServer();
                prepareInterrupt(myHost); // OS-dependent Prolog interrupt generation, must be after the previous step
	            waitUntilAvailable();
                progressMessage("Ended SubprocessEngine constructor");
            } catch (IOException e){
                    throw new IPException("Could not launch Prolog executable:"+e);
            }
        }
	
	public SubprocessEngine(String prologCommand, boolean debug){
		this(prologCommand,debug,true);
	}
	
	public SubprocessEngine(String startPrologCommand){
		this(startPrologCommand,false);
	}
	
	public SubprocessEngine(boolean debug){
		this(null,debug);
	}
	
	public SubprocessEngine(){
		this(false);
	}
	
	protected PrologOutputObjectStream buildPrologOutputObjectStream(OutputStream os) throws IOException{
		return new PrologOutputObjectStream(os);
	}
	
	protected Process createProcess(String prologCommand) throws IOException{
        progressMessage("Launching subprocess "+prologCommand);
        return Runtime.getRuntime().exec(prologCommand);
    }
	public void setDebug(boolean debug){
		stdoutHandler.setDebugStream(debug?System.err:null);
		stderrHandler.setDebugStream(debug?System.err:null);
		super.setDebug(debug);
	}
	
	/** Prolog is thought to be idle */
	public boolean isAvailable(){
		return available;
	}
	
	protected void setupCallbackServer(){
		prologHandler = new Thread(){
			public void run(){
				try{
					while(!shutingDown) {
						progressMessage("Waiting to receive object");
						Object x = receiveObject();
						progressMessage("Received object:"+x);
						Object y = handleCallback(x);
						progressMessage("Handled object and computed:"+y);
						if (y!=null) sendObject(y);
					}
				} catch (IOException e){
                    // If this happens, it means there was a communications error
                    // with prolog.  We have to abort all current goals so that
                    // calls are not wait()-ing forever.
                    if (!shutingDown) {
                        IPException toThrow = new IPException("Bad exception in setupCallbackServer", e);
                        SubprocessEngine.this.endAllTasks(toThrow);
                        throw toThrow;
                    }
                }
            }
        };
        progressMessage("Starting up callback service...");
		prologHandler.setName("Prolog handler");
		prologHandler.start();
	}
	
	protected Object receiveObject() throws IOException{
     	progressMessage("entering receiveObject()");
   		Object x=null;
    	try{
			ObjectInputStream ios = new ObjectInputStream(socket.getInputStream());
			x = ios.readObject();
		} catch (ClassNotFoundException e){
			x = e;
		}
     	progressMessage("exiting receiveObject():"+x);
		return x;
	}
	
    protected void sendObject(Object y) throws IOException{
    	progressMessage("entering sendObject("+y+")");
		PrologOutputObjectStream poos = 
		    buildPrologOutputObjectStream(socket.getOutputStream());
		poos.writeObject(y);
		poos.flush(); // this actually writes to the socket stream
    	progressMessage("exiting sendObject("+y+")");
	}
	
	/** Shuts down the background Prolog process as well as the dependent Java threads.
	*/
	public synchronized void shutdown(){
		super.shutdown();
		available=false;
		stdoutHandler.setIgnoreStreamEnd(true);
		stderrHandler.setIgnoreStreamEnd(true);
        try{
            socket.close();
            serverSocket.close();
        }catch(IOException e) {throw new IPException("Problems closing sockets:"+e);}
        
        if(intServerSocket!=null){
			try {
				// closing sockets will stop them, no need to deprecate:
				// stdoutHandler.stop(); stderrHandler.stop(); cbhandler.stop();
				intSocket.close(); intServerSocket.close();
			}
			catch (IOException e) {throw new IPException("Problems closing sockets:"+e);}
			finally{prolog.destroy();}
		}
		prologHandler.interrupt(); // kills javaMessage/deterministicGoal thread
	}
		
	/** Kill the Prolog background process. If you wish to make sure this message is sent on exiting, 
	use System.runFinalizersOnExit(true) on initialization
	*/
	protected void finalize() throws Throwable{
		if (prolog!=null) prolog.destroy();
	}
	
	protected void setDetectPromptAndBreak(boolean yes){
		if (yes==isDetectingPromptAndBreak()) return;
		if(yes){
			stdoutHandler.addOutputListener(promptTrigger);
			stdoutHandler.addOutputListener(breakTrigger);
			stderrHandler.addOutputListener(promptTrigger);
			stderrHandler.addOutputListener(breakTrigger);
		} else{
			stdoutHandler.removeOutputListener(promptTrigger);
			stdoutHandler.removeOutputListener(breakTrigger);
			stderrHandler.removeOutputListener(promptTrigger);
			stderrHandler.removeOutputListener(breakTrigger);
		}
	}
	protected boolean isDetectingPromptAndBreak(){
		return stdoutHandler.hasListener(promptTrigger) /*&& stderrHandler.hasListener(promptTrigger)*/ &&
			stdoutHandler.hasListener(breakTrigger) /*&& stderrHandler.hasListener(breakTrigger)*/;
	}
	
	/** Sends a String to Prolog's input. Its meaning will naturally depend on the current state of Prolog: it can be
        a top goal, or input to an ongoing computation */
	public synchronized void sendAndFlush(String s){
		available=false;
		prologStdin.print(s); prologStdin.flush();
	}
	
	public void sendAndFlushLn(String s){
		sendAndFlush(s+nl);
	}
	
	protected void prepareInterrupt(String myHost) throws IOException{ // requires successful startup steps
		if (isWindowsOS()){ 
			intServerSocket = new ServerSocket(0);
			command("setupWindowsInterrupt('"+myHost+"',"+intServerSocket.getLocalPort()+")");
			intSocket = intServerSocket.accept();
		} else {
			//available=true; // sort of a hack... but when will the state of 'available' become valid ?
			waitUntilAvailable();
			Object bindings[] = deterministicGoal("getPrologPID(N), ipObjectSpec('java.lang.Integer',Integer,[N],_)",
				"[Integer]");
			progressMessage("Found Prolog process ID");
			if (bindings!=null) interruptCommand = "/bin/kill -s INT "+bindings[0];
			else throw new IPException("Could not find Prolog's PID");
		}
	}

	protected abstract void doInterrupt();
	
	/** This implementation may get stuck if the command includes variables, because the Prolog
	top level interpreter may offer to compute more solutions; use variables prefixed with '_' */
	public boolean realCommand(String s){
		progressMessage("COMMAND:"+s+".");
		sendAndFlushLn(s+".");
		return true; // we do not really know
	}
	
	public Object[] deterministicGoal(String G, String OVar, Object[] objectsP, String RVars){
		boolean first=false;
		synchronized(this){
		if (!topGoalHasStarted){
			topGoalHasStarted = true;
				first=true;
			}
		}
		if (first){
			if (!isIdle()) throw new IPException("Inconsistency in deterministicGoal:");
			Object[] result = firstGoal(G, OVar, objectsP, RVars);
			return result;
		} else return super.deterministicGoal(G, OVar, objectsP, RVars);
	}

	/** Very alike deterministicGoal except that it sends the initial GoalFromJava object over the socket */
    protected Object[] firstGoal(String G, String OVar, Object[] objectsP, String RVars) {

        Object[] resultToReturn=null;
 		int mytimestamp = incGoalTimestamp();
        
        try{
       	GoalFromJava GO = makeDGoalObject(G, OVar, objectsP, RVars, mytimestamp);
       	progressMessage("Prepared GoalFromJava:"+GO);
            
            progressMessage("Schedulling (first) goal "+G+", timestamp "+mytimestamp+" in thread "+Thread.currentThread().getName());
            GoalToExecute goalToDo = new GoalToExecute(GO);
            goalToDo.setFirstGoalStatus();
            scheduleGoal(goalToDo);
 			goalToDo.prologWasCalled();
                        //if(this.isWindowsOS()){
 			pushDGthread(goalToDo.getCallerThread());
			    //}
            //setupErrorHandling();
            sendObject(GO);
 			realCommand("deterministicGoal"); // assynchronous
           	ResultFromProlog result = goalToDo.waitForResult();
            // goalToDo is forgotten by handleCallback
            progressMessage("got dG result for timestamp "+mytimestamp);         
            if (result==null) throw new IPException("Problems in goal result");
            if (goalToDo.wasAborted()) throw new IPAbortedException(G+" was aborted");
            if (goalToDo.wasInterrupted()) throw new IPInterruptedException(G+" was interrupted");
            if (result.wasInterrupted(this)) 
				throw new IPInterruptedException(G+" was interrupted, Prolog detected"); 
            // if (result.error!=null) throw new IPException (result.error.toString());
            if (result.error!=null) throw new IPPrologError(result.error);
            if (result.timestamp!=mytimestamp)
                throw new IPException("bad timestamp in deterministicGoal, got "+result.timestamp+" instead of "+goalTimestamp);
            if (result.succeeded)
                resultToReturn = result.rVars;
        } catch (IPException e) {
            throw e;
        } catch (Exception e) {
            throw new IPException("Problem in deterministicGoal:"+e);
        } finally{
			topGoalHasStarted = false; // is this OK? this assumes no initiative from the Prolog side, which is probably correct
			//removeErrorHandling();
			progressMessage("Leaving firstGoal for "+G+", timestamp "+mytimestamp+" isIdle()=="+isIdle());
        }
        return resultToReturn;
    }

	protected Object doSomething(){
		if (onlyFirstGoalSchedulled()) return null;
		else return super.doSomething();
	}
	
	protected synchronized boolean onlyFirstGoalSchedulled(){
		return isIdle() || (messagesExecuting.size()==0 && goalsToExecute.size()==1 && 
			((GoalToExecute)goalsToExecute.elementAt(0)).isFirstGoal());
	}
	
	// deterministicGoal helpers
	
    protected void setupErrorHandling(){
		setDetectPromptAndBreak(false);
		stderrHandler.addOutputListener(errorTrigger); // no need to listen to stdout
		abortMessage = "";
		final Thread current = Thread.currentThread();
		// We could dispense creating this every time:
		errorHandler = new RecognizerListener(){
			public void recognized(Recognizer source,Object extra){
			    abortMessage = (String)extra;
			    current.interrupt(); 
			}
		    };
		errorTrigger.addRecognizerListener(errorHandler);
    }
    
    protected void removeErrorHandling(){
    	errorTrigger.removeRecognizerListener(errorHandler);
    	stderrHandler.removeOutputListener(errorTrigger);
    	errorHandler=null;
		setDetectPromptAndBreak(true);
    }
    private RecognizerListener errorHandler=null;
    Recognizer errorTrigger = new Recognizer("++Error",true); // was "++Error: " for XSB 2.4
    private String abortMessage;
}
