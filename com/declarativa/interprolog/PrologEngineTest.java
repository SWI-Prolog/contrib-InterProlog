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
import javax.swing.*;
import java.awt.*;

public abstract class PrologEngineTest extends TestCase{
	public AbstractPrologEngine engine=null;
	protected int thisID;
	public PrologEngineTest(String name){
		super(name);
	}
	
	public void loadTestFile(){
		engine.consultFromPackage("tests.P",AbstractPrologEngine.class);
	}
	
	/** The implementation-dependent way to construct a new PrologEngine. */
	protected abstract AbstractPrologEngine buildNewEngine();
	
	boolean didInterrupt;
	public void testNewInterrupt(){ 
	
		didInterrupt = false;
		Thread t = new Thread(){
			public void run(){
				try{
					System.out.println("Calling Prolog endless loop");
					engine.deterministicGoal("repeat,fail"); 
					fail("should have thrown IPInterruptedException"); 
				}catch (IPException e){
					if (e instanceof IPInterruptedException) didInterrupt = true;
					System.out.println("didInterrupt=="+didInterrupt+" exception class=="+e.getClass()+" Got exception:"+e);
				}
			}
		};
		System.out.println("Starting thread with goal to be interrupted...");
		t.start();
		try {Thread.sleep(2000);} catch(Exception e){}
		System.out.println("About to interrupt...");
		engine.interrupt();
		System.out.println("Starting loop...");
		while (!didInterrupt) Thread.yield();
		System.out.println("Finished loop...");
		//System.out.println("calling another goal...");
		Object s = engine.deterministicGoal("R=string(still_alive)","[R]")[0];
		//System.out.println("Finished extra goal.");
		assertEquals(s,"still_alive");
		
	}

	public static class NumberTypes implements java.io.Serializable{
		byte b;
		short s;
		int i;
		float f;
		public NumberTypes(byte b,short s,int i, float f){
			this.b=b; this.s=s; this.i=i; this.f=f;
		}
		public static ObjectExamplePair example(){
			return new ObjectExamplePair(
				new NumberTypes(Byte.MIN_VALUE,Short.MIN_VALUE,PrologEngine.MIN_INT_VALUE,SMALL_FLOAT_VALUE),
				new NumberTypes(Byte.MAX_VALUE,Short.MAX_VALUE,PrologEngine.MAX_INT_VALUE,LARGE_FLOAT_VALUE)
			);
		}
		public String toString(){
			return "b=="+b+",s=="+s+",i=="+i+",f=="+f;
		}
		public boolean equals(Object o){
			if (!(o instanceof NumberTypes)) return false;
			NumberTypes io = (NumberTypes)o;
			return io.b==b && io.s==s && io.i==i && io.f==f;
		}
	}
	// floats not very precise on some Prologs, let's use reasonable values:
	static final float SMALL_FLOAT_VALUE = (float)-3.14159;
	//static final float LARGE_FLOAT_VALUE = (float)817.3E4;
	static final float LARGE_FLOAT_VALUE = (float)181.25;
	public void testNumbers(){
		ObjectExamplePair[] examples = {NumberTypes.example()};
		//engine.setDebug(true);
		assertTrue("Sent NumberTypes examples",engine.teachMoreObjects(examples));
		engine.waitUntilIdle(); 
		//engine.setDebug(false);
		
		/*
		String g = "ipObjectSpec('com.declarativa.interprolog.PrologEngineTest$NumberTypes',Min,[";
		g += Byte.MIN_VALUE+","+SMALL_FLOAT_VALUE+","+PrologEngine.MIN_INT_VALUE+","+Short.MIN_VALUE+"],_), ";
		g += "ipObjectSpec('com.declarativa.interprolog.PrologEngineTest$NumberTypes',Max,[";
		g += Byte.MAX_VALUE+","+LARGE_FLOAT_VALUE+","+PrologEngine.MAX_INT_VALUE+","+Short.MAX_VALUE+"],_)";
		g += ",tell('treta.txt'),writeln(Min),writeln(Max),writeln(Min2),writeln(Max2),told,Min=Min2";
		*/
		String g = 
		     // "ipProgressMessage(tarahhh), ipObjectSpec('com.declarativa.interprolog.PrologEngineTest$NumberTypes',Min,["+Byte.MIN_VALUE+",SmallFloat,"+PrologEngine.MIN_INT_VALUE+","+Short.MIN_VALUE+"],_), ";
		     "ipObjectSpec('com.declarativa.interprolog.PrologEngineTest$NumberTypes',Min,[SmallByte,SmallFloat,SmallInt,SmallShort],_), ipProgressMessage(gOT-[SmallByte,SmallFloat,SmallInt,SmallShort]), ";
		g += "ipProgressMessage(got_here), ";
		g += "ipObjectSpec('com.declarativa.interprolog.PrologEngineTest$NumberTypes',Max,["+Byte.MAX_VALUE+",LargeFloat,"+PrologEngine.MAX_INT_VALUE+","+Short.MAX_VALUE+"],_),";
		g += "ipProgressMessage(got_there), ";
		g += "DeltaSmall is SmallFloat - ("+SMALL_FLOAT_VALUE+"),DeltaLarge is LargeFloat - "+LARGE_FLOAT_VALUE+", DeltaSmall*DeltaSmall<0.001, DeltaLarge*DeltaLarge<0.001";
		// Allow for some rounding error
		
		NumberTypes MIN = new NumberTypes(Byte.MIN_VALUE,Short.MIN_VALUE,PrologEngine.MIN_INT_VALUE,SMALL_FLOAT_VALUE);
		NumberTypes MAX = new NumberTypes(Byte.MAX_VALUE,Short.MAX_VALUE,PrologEngine.MAX_INT_VALUE,LARGE_FLOAT_VALUE);
		Object [] args = {MIN,MAX};
		//engine.setDebug(true);
		assertTrue("Numbers well sent and understood",engine.deterministicGoal(g,"[Min,Max]",args));
		g = "ipObjectSpec('com.declarativa.interprolog.PrologEngineTest$NumberTypes',Min,[";
		g += Byte.MIN_VALUE+","+SMALL_FLOAT_VALUE+","+PrologEngine.MIN_INT_VALUE+","+Short.MIN_VALUE+"],_), ";
		g += "ipObjectSpec('com.declarativa.interprolog.PrologEngineTest$NumberTypes',Max,[";
		g += Byte.MAX_VALUE+","+LARGE_FLOAT_VALUE+","+PrologEngine.MAX_INT_VALUE+","+Short.MAX_VALUE+"],_)";

		Object [] bindings = engine.deterministicGoal(g,"[Min,Max]");
		assertEquals("MIN arrived well",MIN,bindings[0]);
		assertEquals("MAX arrived well",MAX,bindings[1]);
	}
	public static class MyClass implements java.io.Serializable{
		int one,two;
		public MyClass(){one=0;two=0;}
	}
	public void testPrototypeStuff(){
		engine.teachOneObject(new MyClass());
		String g = "ipObjectSpec('com.declarativa.interprolog.PrologEngineTest$MyClass',[one=1,two=2],Obj)";
		g += ", ipObjectSpec('com.declarativa.interprolog.PrologEngineTest$MyClass',[one=One,two=Two],Obj), Three is One+Two, ";
		g += "ipObjectSpec('java.lang.Integer',[value=Three],Integer)";
		Object[] bindings = engine.deterministicGoal(g,"[Obj,Integer]");
		MyClass result0 = (MyClass)bindings[0];
		assertTrue( 1==result0.one );assertTrue( 2==result0.two );
		assertEquals(new Integer(3),bindings[1]);
	}
	public void testAutoTermModel(){
		Object[] bindings = engine.deterministicGoal("X=1,Y=hello(complex(term))",null);
		assertEquals(bindings.length,1);
		assertTrue(bindings[0] instanceof TermModel);
	}
    public void testBuildTermModel(){
    	TermModel t1 = (TermModel)engine.deterministicGoal("buildTermModel(a(b,c(1)),Model)","[Model]")[0];
    	assertEquals("a(b,c(1))",t1.toString());
    	assertEquals("a(_,_)",t1.getTemplate());
    	assertEquals(2,t1.getChildCount());
    	
    	TermModel t2 = (TermModel)engine.deterministicGoal("buildTermModel(a(X,c(X)),Model)","[Model]")[0];
    	TermModel t2child = (TermModel)t2.getChild(0);
    	assertTrue("Child is var",t2child.isVar());
    	Object t3 = t2.clone();
    	assertEquals(t2.toString(),t3.toString());
    	t2.assignToVar((VariableNode) t2child.node, "someX");
    	assertEquals("a(someX,c(someX))",t2.toString());
    	
    	Object[] passed = {t2,t3};
    	String g = "recoverTermModel(Model3,T3), arg(1,T3,X), arg(2,T3,c(XX)), XX==X, recoverTermModel(Model2,T2), T2=T3, ";
    	g += "arg(1,T2,someX), functor(T2,F,N), ipObjectSpec('java.lang.Integer',Integer,[N],_)";
    	Object[] bindings = engine.deterministicGoal(g, "[Model2,Model3]", passed,"[string(F),Integer]");
    	assertTrue(bindings!=null);
    	assertEquals("a",bindings[0]);
    	assertEquals(new Integer(2),bindings[1]);
    }
	public void testNumbers2(){
		Object[] objects = {new Float(16.25),new Float(0.0), new Float(15.5)};
		Object[] bindings = engine.deterministicGoal(
			"append([97,98],[99,100],L), length(L,N), ipObjectSpec('java.lang.Integer',Integer,[N],_), name(A,L),"+
			"assert(foofoo(Objects))", 
			"Objects",
			objects,
			"[Integer,string(A)]"
			);
		assertTrue("Got a result",bindings!=null);
		assertEquals("First result",bindings[0],new Integer(4));
		assertEquals("Second result",bindings[1],"abcd");
		//engine.setDebug(true);
		Object[] floats = engine.deterministicGoal("foofoo([F1,F2,F3])","[F1,F2,F3]");
		engine.setDebug(false);
		assertTrue("succeeded",floats!=null);
		assertEquals(floats[0],new Float(16.25));
		assertEquals(floats[1],new Float(0.0));
		assertEquals("Third float OK",floats[2],new Float(15.5));
	}
	public void testDeterministicGoal(){
		//engine.setDebug(true);
		assertTrue(engine.deterministicGoal("true"));
		engine.waitUntilAvailable();
		try{
			engine.deterministicGoal("true","[foo]");
			fail("should raise an IPException due to grammar failure");
		} catch (IPException e){
			assertTrue("IPException should complain about bad object specification:"+e,e.toString().indexOf(" specification")!=-1);
		}
		try{
			engine.deterministicGoal("true","[[_]]");
			fail("should raise an IPException due to grammar failure");
		} catch (IPException e){
			assertTrue("IPException should complain about spec. of result bindings:"+e,e.toString().indexOf("bindings")!=-1);
		}
		try{
			Object[] bindings = engine.deterministicGoal("true","[_]");
			System.out.println("R:"+bindings[0]);
			fail("should raise an IPException due to bad object specification");
		} catch (IPException e){
			assertTrue("IPException should complain about bad object specification:"+e,e.toString().indexOf(" specification")!=-1);
		}
		
		try{
			engine.deterministicGoal("bad goal");
			fail("should raise an IPException with syntax error");
		} catch (IPException e){
			assertTrue("IPException should denote syntax error:"+e,e.toString().indexOf("Syntax")!=-1);
		}
		assertTrue("Engine ready2",engine.isIdle());
		assertTrue("Engine working",engine.deterministicGoal("true"));
		try{
			engine.deterministicGoal("true","BadResultsList");
			fail("should raise an IPException complaining about lack of a Prolog list");
		} catch (IPException e){}
		assertTrue("Engine ready1",engine.isIdle());
		java.util.Vector v = new java.util.Vector();
		Object[] objects = {new Integer(16),new Short((short)0), new Byte((byte)15)};		
		v.addElement(objects); v.addElement("Hello there");
		for(int i=0;i<30;i++){
			Vector vv=new Vector();
			for (int j=0;j<100;j++)
				vv.addElement(new Integer(j));
			v.addElement(vv);
		}
        engine.waitUntilAvailable();
		long tortureStart= System.currentTimeMillis();
        
		Object [] toSerialize = {v};
		String g = "streamContents([Object],handles(NH,_),Bytes,[]), length(Bytes,NB), ";
		g += "ipObjectSpec('java.lang.Integer',IntegerNH,[NH],_), ipObjectSpec('java.lang.Integer',IntegerNB,[NB],_)";
		
		Object [] yetanother = engine.deterministicGoal(g,"[Object]",toSerialize,"[Object,IntegerNH,IntegerNB]");
		long duration = System.currentTimeMillis()-tortureStart;
		int nbytes = ((Integer)yetanother[2]).intValue();
		System.out.println("Bulk torture took "+(duration)+" mS ("+(1000*nbytes/duration)+" bytes gone and returned / second)");
		
		// Win98, Celeron 400 MHz: 4670 ms
		// Win NT4 Workstation, Pentium 400 MHz: 4607 mS (7128 bytes gone and returned / second)
		// Win 2k, Pentium 400 MHz: 7221 mS (4548 bytes gone and returned / second)
		// If Vector's internals change in subsequent Java releases review these values by printing them above:
		assertEquals(new Integer(3073),yetanother[1]); 
		// Vector serialization change on JDK 1.4.2:
		if (System.getProperty("java.version").compareTo("1.4.2")>=0)
			assertEquals(new Integer(32906),yetanother[2]);
		else assertEquals(new Integer(32843),yetanother[2]);
		assertTrue(yetanother[0] instanceof java.util.Vector);
		
		tortureStart= System.currentTimeMillis();
		int ngoals=3;
		for (int i=0;i<ngoals;i++)
			assertTrue(engine.deterministicGoal("true"));
		System.out.println("Busy torture took "+(System.currentTimeMillis()-tortureStart)/ngoals+" mS/goal");
		
		// Win98, Celeron 400 MHz: 203 ms/goal
		// Win NT4 Wokstation, Pentium 400 MHz: 402 mS/goal
		// Win 2k, Pentium 400 MHz: 36 mS/goal
	}
	/* Java programmers (should) never need this... use instead findall or findNsolutions on the Prolog side!
	might be useful for Prolog engines with threads...
	public void testNonDeterministicGoal(){
		engine.deterministicGoal("import member/2 from basics");
		engine.goal("member(X,[1,2,3,4,5]), ipObjectSpec('java.lang.Integer',I,[X],_)","[I]");
		Object[] bindings; int i=1;
		while ( (bindings=engine.nextSolution())!=null) {
			assertEquals(new Integer(i++),bindings[0]);
		}
		assertTrue(i==6);
		try{ 
			engine.nextSolution();
			fail("Should throw IPException");
		} catch (IPException e){assertTrue(!(e.toString.indexOf("...")!=-1));}
	}*/
	
	public void testDG2(){
		String G = "(X=a;X=b)";
		String T = "X";
		String GG = "findall(TM, ("+G+",buildTermModel("+T+",TM)), L), ipObjectSpec('ArrayOfObject',L,LM)";
		Object[] solutions = (Object[])engine.deterministicGoal(GG,"[LM]")[0];
		assertEquals(2,solutions.length);
		assertEquals("a",solutions[0].toString());
	}
	public int luckyNumber(){return 13;}
	public void testJavaMessage(){
		assertEquals(engine.registerJavaObject(this),thisID);
		String callback = "javaMessage("+thisID+",R,luckyNumber), ipObjectSpec('java.lang.Integer',R,[13],_)";
		assertTrue("Succeeded 1st",engine.deterministicGoal(callback));
		String clauseAsserts = "assert((tortureJM(0) :- !)), ";
		clauseAsserts += "assert((tortureJM(N) :- NN is N-1, "+callback+", tortureJM(NN)))";
		assertTrue("Succeeded 2nd",engine.deterministicGoal(clauseAsserts));
		long tortureStart= System.currentTimeMillis();
		int ngoals=10;
		assertTrue("Succeeded torture",engine.deterministicGoal("tortureJM("+ngoals+")"));		
		System.out.println("Callback torture took "+(System.currentTimeMillis()-tortureStart)/ngoals+" mS/message");
		// Win98, Celeron 400 MHz: 220 ms/message
		// Win NT4 Workstation, Pentium 400 MHz: 441 mS/message
		// Win 2k, Pentium 400 MHz: 57 mS/message
	}
	public void testIPobjects(){
		//assertTrue(engine.deterministicGoal("import length/2 from basics"));
		assertTrue(engine.deterministicGoal("assert(myListLength([],0))"));
		assertTrue(engine.deterministicGoal("assert((myListLength([_|L],N):-myListLength(L,NN),N is NN+1))"));
		String goal = "findall(foo,ipObjectSpec(_,_,_,_), L), myListLength(L,_N), ";
		goal += "findall(foo,ipObjectTemplate(_,_,_,_,_), LL), myListLength(LL,_N), ";
		goal += "ipObjectSpec('java.lang.Integer',Integer,[_N],_) ";
		Integer result = (Integer)engine.deterministicGoal(goal,"[Integer]")[0];
		assertTrue(result.intValue()>20);
	}
	public static class Loop implements java.io.Serializable{
		Loop next;
	}
	public void testLoops(){
		engine.teachOneObject(new Loop());
		Loop L = new Loop();
		L.next=L;
		assertTrue(engine.deterministicGoal("true","[L]",new Object[]{L})); // but if you replace true by write(L)..
	}
     public int somaN(int n) {
        Object[] bindings = engine.deterministicGoal (
            "somaN("+n+",X), ipObjectSpec('java.lang.Integer',Spec,[X],_)", "[Spec]"
        );
        return ((Integer)bindings[0]).intValue(); 
    }
    
    public void testSomaN(){
		engine.waitUntilIdle();
       	engine.command("ipObjectSpec('InvisibleObject',_T,["+thisID+"],_),"+
                        "assert(ipSomaN(_T))"); 
 		engine.waitUntilIdle();
        Object[] bindings = engine.deterministicGoal (
            "somaN(10,X), ipObjectSpec('java.lang.Integer',Spec,[X],_)", "[Spec]"
            );
        Integer result = (Integer)bindings[0];
        assertTrue ("Got a result",bindings!=null);
        engine.progressMessage("result: "+bindings[0]);
        assertEquals ("First result",result, new Integer (55));
    }
    
    public int fibonaci(int n) {
        Object[] bindings = engine.deterministicGoal (
            "fib("+n+",X), ipObjectSpec('java.lang.Integer',Spec,[X],_)", "[Spec]"
        );
        return ((Integer)bindings[0]).intValue(); 
    }
    public void testFibonaci(){
        engine.command("ipObjectSpec('InvisibleObject',_T,["+thisID+"],_),"+
                        "assert(ipFibonaci(_T))"); 
        engine.waitUntilIdle();
        Object[] bindings = engine.deterministicGoal (
            "fib(10,X), ipObjectSpec('java.lang.Integer',Spec,[X],_)", "[Spec]"
            );
        Integer result = (Integer)bindings[0];
        assertTrue ("Got a result",bindings!=null);
        engine.progressMessage("result: "+bindings[0]);
        assertEquals ("First result",result, new Integer (89));
    }
    
    public int factorial(int n) {
        Object[] bindings = engine.deterministicGoal (
            "fac("+n+",X), ipObjectSpec('java.lang.Integer',Spec,[X],_)", "[Spec]"
        );
        return ((Integer)bindings[0]).intValue(); 
    }
    
    public void testFactorial(){
        engine.command("ipObjectSpec('InvisibleObject',_T,["+thisID+"],_),"+
                        "assert(ipFactorial(_T))"); 
        engine.waitUntilIdle();
        Object[] bindings = engine.deterministicGoal (
            "fac(7,X), ipObjectSpec('java.lang.Integer',Spec,[X],_)", "[Spec]"
            );
        Integer result = (Integer)bindings[0];
        assertTrue ("Got a result",bindings!=null);
        engine.progressMessage("result: "+bindings[0]);
        assertEquals ("First result",result, new Integer (5040));
    } 
    
    public TermModel[] someTerms(){
    	return new TermModel[]{new TermModel("a"),new TermModel("b")};
    }
    public void testGetRealJavaObject(){
		String g = "javaMessage("+thisID+",Tref,someTerms), ";
		g = g+ "ipPrologEngine(E), javaMessage(E,Obj,getRealJavaObject(Tref)), recoverTermModelArray(Obj,[a,b])";
		assertTrue(engine.deterministicGoal(g));    	
    }
    public void testStrangeChar(){
    	//assertEquals("R",engine.deterministicGoal("name(CircleR,[82])","[string(CircleR)]")[0]);
   		assertEquals(new String(new char[]{'\u0080'}),engine.deterministicGoal("name(CircleR,[128])","[string(CircleR)]")[0]);
    	assertTrue(new String(new char[]{'\u00e7'}),engine.deterministicGoal("name(CircleR,[231])","[string(CircleR)]")!=null);
    }
    public void testBlockdataSerialization1(){
    	Object [] toSerialize = {new Container()};
   		Object [] yetanother = engine.deterministicGoal("true","[W]",toSerialize,"[W]");
    	Container w = (Container)yetanother[0];
    }
    public void testBlockdataSerialization2(){
    	Object [] toSerialize = {new JFrame("My window")};
   		// to get the JFrame back to Java replace "[]" by "[W]":
   		Object [] yetanother = engine.deterministicGoal("true","[W]",toSerialize,"[]"); 
   		assertTrue(yetanother!=null);
     	/* Although they get serialized ok to the Prolog side, JFrame objects contain
     	circular references, so the InterProlog grammar can not serialize it back unless 
     	repeatedObjectsDetectedGenerating is asserted, which is currently not recommended,
     	see comments in streamcontents/4 in interprolog.P Without repeatedObjectsDetectedGenerating,
     	the serialization back to Java never terminates. */
    	//Frame w = (Frame)yetanother[0];
    	//assertEquals(w.getTitle(),"My window");
    }
    class JavaThread1 extends Thread{
    	public void run(){
    		boolean rc = engine.deterministicGoal("javaMessage("+engine.registerJavaObject(JavaThread1.this)+",hang)");
    		firstThreadWoke = rc;
    	}
    	public void hang() throws InterruptedException{
    		while(!secondThreadArrived) Thread.sleep(1);
    	}
    }
    boolean secondThreadArrived=false;
    boolean firstThreadWoke=false;
    public void kickThread1(){
    	secondThreadArrived=true;
    }
    
    class JavaThreadN extends Thread{
    	public void run(){
    		assertTrue("...",engine.deterministicGoal(
    			"ipObjectSpec(long,X,[long(0,0,0,"+Math.round(Math.random()*10)+")],_), javaMessage('java.lang.Thread',sleep(X))" // notice hack for specifying (small) long values
    			));
    	}
    }
  
    public void testJavaThreads() throws InterruptedException{
    	engine.setThreadedCallbacks(false); // reuse the calling threads
    	Thread t1 = new JavaThread1();
    	t1.start();
    	Thread.sleep(50);
    	assertTrue("Got result for thread 2 (reusing)",engine.deterministicGoal("S=hello, javaMessage("+ engine.registerJavaObject(this)+",kickThread1)","[string(S)]")[0].equals("hello"));
    	t1.join();
    	assertTrue("firstThreadWoke(reusing)",firstThreadWoke);
    	
    	firstThreadWoke = false; secondThreadArrived = false;
    	engine.setThreadedCallbacks(true); // callbacks run in new threads
    	Thread t2 = new JavaThread1();
    	t2.start();
    	Thread.sleep(50);
    	assertTrue("Got result for thread 2",engine.deterministicGoal("S=hello, javaMessage("+ engine.registerJavaObject(this)+",kickThread1)","[string(S)]")[0].equals("hello"));
    	t2.join();
    	assertTrue("firstThreadWoke",firstThreadWoke);
    	
    	Thread[] threads = new Thread[3];
    	for(int t=0;t<threads.length;t++){
    		threads[t] = new JavaThreadN();
    		threads[t].start();
    	}
    	for(int t=0;t<threads.length;t++){
    		System.out.println("Waiting for end of thread "+t);
    		threads[t].join();
    	}
    		
    }
}
