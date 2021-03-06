/*** SWI module header; for copyright information read top of com/declarativa/interprolog/xsb/interprolog.P ***/

:- module(interprolog, [
	% Initialization:
	ipinitialize/4, setupWindowsInterrupt/2, getPrologPID/1, ipLearnExamples/0, ipProcessExamples/1,
	% Glorified remote procedure calling:
	deterministicGoal/0,javaMessage/7, javaMessage/6, javaMessage/3, javaMessage/2,
	% Runtime object structures and references, grammar:
	ipObjectSpec/3,ipObjectSpec/4, ipObjectTemplate/5, ipPrologEngine/1,ipIsObjectReference/1,streamContents/4,
	% Term<->TermModel:
	buildTermModel/2, buildTermModels/2, buildTermModel_/2, buildTermModelList_/2, buildTermModelList_2/2, buildTermModelArray/2,
	ip_inc_var_counter/1,recoverTermModel/2, recoverTermModels/2, recoverTermModelArray/2,
	% Used by Java side in the JNI version:
	ipLearnExamples/1,
	% Utility:
	stringArraytoList/2, ipIsDebugging/0, ipProgressMessage/1
	]).


/*** SWI-dependent stuff ***/

socket_get0(Sockfd,C) :- get_byte(Sockfd,C).

socket_put(Sockfd,C)  :- put_byte(Sockfd,C).

writeAllSocketChars(More,S) :- writeAllSocketChars_(More,S), flush_output(S).

ipinitsockets(Host,Sport) :-
	ipProgressMessage('Creating socket'),
	tcp_socket(S),
	(tcp_connect(S, Host:Sport)->true;write('Weird socket connect failure 1'),nl,fail),
	tcp_open_socket(S, InStream, OutStream),
	ipProgressMessage('Created socket'-S),
	asserta(ipsocketstreams(InStream, OutStream)),
	ipLearnExamples.

% Actually will be used for Unix too:
setupWindowsInterrupt(Host,INTport) :-
	tcp_socket(IntS),
	(tcp_connect(IntS, Host:INTport)->true;write('Weird socket connect failure 3'),nl,fail),
	tcp_open_socket(IntS, InStream, _OutStream),
	setupJavaInterrupt(InStream),
	ipProgressMessage('interrupt was setup').


% This is specific for UNIX, does not apply to Windows 95/NT
getPrologPID(N) :- current_prolog_flag(pid,N).

atomRead(Atom,Term) :- catch(atom_to_term(Atom,Term,_),_E,Term=end_of_file).

reset_dcg_mode. % do nothing, only necessary in SWI Prolog

setupJavaInterrupt(InStream) :-
	thread_self(Main), thread_create(ipInterrupter(InStream,Main),_Id,[alias(interProlog_interrupter)]).

ipInterrupter(InStream,Main) :-
	socket_get0(InStream,C), C=3,
	!,
	thread_signal(Main,throw(interprolog_interrupt)),
	ipInterrupter(InStream,Main).
ipInterrupter(_InStream,_Main) :- ipProgressMessage('Failed to read proper char in interrupt socket').

% For JNI only: calls the C function that calls the Java method byte[] callback(byte[])
interprolog_callback(_Length,_Bytes,_NewBytes) :- true. % needs to invoke the "INTERPROLOG_CALLBACK" built-in

supportsExceptions.

get_ip_exception_description(E, E).


float_to_intrep(Sign,Power,Mantissa,Float) :-
	(Float < 0.0
         ->     FloatA is - Float,
                Sign = 1
	 ;      FloatA = Float,
	        Sign = 0
	),
	extractPower(FloatA,Rem,0,Power),
	Rem1 is Rem - 1.0,
	extractMantissa(23,Rem1,_,0,Mantissa1),
	(Mantissa1 > 0 -> Mantissa is Mantissa1+1 ; Mantissa = Mantissa1).
        % This last seems needed for rounding.

extractMantissa(N,Float,Rem,MantI,Mant) :-
	(N =< 0
         ->     Mant = MantI,
	        Rem = Float
	 ;      (Float > 0.5
                 ->    MantI1 is MantI+MantI+1,
	               Float1 is Float - 1.0 + Float
                 ;     MantI1 is MantI+MantI,
	               Float1 is Float + Float
		),
		N1 is N-1,
		extractMantissa(N1,Float1,Rem,MantI1,Mant)
	).


/*** GENERIC STUFF - SAME FOR ALL SUPPORTED PROLOGS ***/

handle_ip_exception(Exception, ExceptionModel):-
	% ipProgressMessage(handlingException-Exception),
	get_ip_exception_description(Exception, ExceptionDescription),
	(buildTermModel(ExceptionDescription, ExceptionModel) -> true ; ExceptionModel = string('Weird exception')).

% Some facilities for Prolog-side debugging, namely of this file...
:- dynamic(ipIsDebugging/0).

ipProgressMessage(M,L,L) :- ipProgressMessage(M). % for simpler use within grammar rules

ipProgressMessageIf(M,C,L,L) :- ipProgressMessageIf(M,C). % for simpler use within grammar rules

ipProgressMessage(M) :- ipIsDebugging, !, write(M),nl.
ipProgressMessage(_).

ipProgressMessageIf(M,C) :- ipIsDebugging, C, !, write(M),nl.
ipProgressMessageIf(_,_).

ipShowTerminalsIf(C,L,L) :- ipIsDebugging, C, !, write('Current grammar terminals:'), write(L), nl.
ipShowTerminalsIf(_,L,L).


% For use in grammar rules meta logical conditions
ipPeekTerminal(T,[T|L],[T|L]).



/****** Socket Utilities ******/

readNSocketChars(0,_,[]) :- !.
readNSocketChars(N,S,[C|Cn]) :- socket_get0(S,C), NN is N-1, readNSocketChars(NN,S,Cn).

writeAllSocketChars_([],_).
writeAllSocketChars_([C|More],S) :- socket_put(S,C), writeAllSocketChars_(More,S).


/****** Initialization ******/

ipinitialize(Host,Sport,EngineID,Debug) :-
	(Debug=true->asserta(ipIsDebugging);true),
	ipinitsockets(Host,Sport),
	ipObjectSpec('InvisibleObject',E,[EngineID],_), asserta(ipPrologEngine(E)),
	ipProgressMessage('ipinitialize concluded').


:- dynamic(ipPrologEngine/1).
:- dynamic(ipsocketstreams/2).
:- dynamic(ipinterruptsocketstreams/2).
:- dynamic(ipObjectSpec/4).
:- dynamic(ipObjectTemplate/5).

% Fetches all examples available from the callback socket
ipLearnExamples :-
	ipsocketstreams(Input,_Output),
	ipProgressMessage('Reading four bytes'),
	readNSocketChars(4,Input,FourBytes),
	int(Size,FourBytes,[]), % stick to our grammar...
	ipProgressMessage('Reading more bytes'-Size),
	readNSocketChars(Size,Input,Bytes),
	streamContents(Examples,_,Bytes,[]), !,
	ipProgressMessage('Grammar finished analysing examples'),
	ipProcessExamples(Examples),
	ipProgressMessage('Asserted ipObjectXXX facts').
ipLearnExamples :- ipProgressMessage('ipLearnExamples failed'), fail.

% Fetches all examples given by the argument passed through JNI
ipLearnExamples(Bytes) :-
	streamContents(Examples,_,Bytes,[]), !,
	ipProcessExamples(Examples).

ipProcessExamples([]) :- !.
ipProcessExamples([
	object( % This clause must be strictly in sync with this Java class:
		class('com.declarativa.interprolog.ObjectExamplePair',_ ,_),
		[] + [A,B,string(Name)] ) | MoreExamples]
	) :-
	ipcompareTerms(A,B,G,Vars,SubstA,SubstB),
	ipAnalyseTerm(A,Template,ANames,TVars,TSubs),
	% Currently we keep 2 (sort of redundant) template flavours, until more programming
	% experience with this is collected:
	assertz(ipObjectSpec(Name,G,Vars,examples-[SubstA,SubstB]/ANames)),
	assertz(ipObjectTemplate(Name,Template,ANames,TVars,TSubs)),
	ipProcessExamples(MoreExamples).





% ipcompareTerms(A,B,G,Vars,SubstA,SubstB).
% Given two *ground* object specs, obtains a generic object, the substitutions
% that would allow the production of both from the generic object,
% and list of its free variables
ipcompareTerms(A,A,A,[],[],[]) :- ! .
ipcompareTerms(A,B,G,Vars,VA,VB) :- functor(A,F,N), functor(B,F,N), N>0, \+ ipWholeTerm(A), !,
	A=..[_|ArgsA], B=..[_|ArgsB],
	ipcompareArgs(ArgsA,ArgsB,ArgsG,Vars,VA,VB),
	G=..[F|ArgsG].
ipcompareTerms(A,B,G,[G],[A],[B]). % different atomic term or different functor

ipcompareArgs([],[],[],[],[],[]) :- !.
ipcompareArgs([A|An],[B|Bn],[G1|Gn],Vars,VA,VB) :-
	ipcompareTerms(A,B,G1,Vars1,VA1,VA2),
	ipcompareArgs(An,Bn,Gn,Varsn,VAn,VBn),
	append(Vars1,Varsn,Vars), append(VA1,VAn,VA), append(VA2,VBn,VB).

% For convenience, some grammar semantic representations are terms,
% although later they may be converted to atomic terms...
% ipWholeTerm(long(_,_,_,_,_,_,_,_)).
ipWholeTerm(long(_,_,_,_)).
ipWholeTerm(double(_,_,_,_,_,_,_,_)).
% not any more: ipWholeTerm(float(_,_,_)).


ipIsObjectReference(object(class('com.declarativa.interprolog.util.InvisibleObject',_,_),_)).

/****** Support for Prolog -> Java goal calls (callbacks) ******/

% javaMessage, non-sugared version

javaMessage(Target,Result,Exception,MessageName,ArgList,ReturnArgs,NewArgList) :-
	(atom(MessageName) -> true ; write('*** MessageName must be an atom'), fail),
	ip_inc_jm_counter(Timestamp),
	specifyJavaCallback(ReturnArgs,Timestamp,Target,MessageName,ArgList,MessageFromProlog),
	( streamContents([MessageFromProlog],handles(_,_),Bytes,[]) -> true
	; write('*** streamContents failed'),nl, fail),
	javaMessage2(Bytes,Timestamp,Result,Exception,NewArgList).

javaMessage2(Bytes,Timestamp,Result,Exception,NewArgList):-
	interprologSendReceive(Bytes,ResultBytes),
	streamContents([Contents],_,ResultBytes,[]), !,
	handleCallbackResult(Contents,Timestamp,Result,Exception,NewArgList).

interprologSendReceive(Bytes, NewBytes) :-
	ipsocketstreams(Input,Output), !, % socket implementation
	ipProgressMessage(writeAllSocketChars-Output),
	writeAllSocketChars(Bytes,Output),
	ipProgressMessage(readNSocketChars-Input),
	readNSocketChars(4,Input,FourBytes),
	int(Size,FourBytes,[]), % stick to our grammar...
	readNSocketChars(Size,Input,NewBytes).
interprologSendReceive(Bytes, NewBytes) :- % JNI implementation
	length(Bytes, L), interprolog_callback(L,Bytes,NewBytes).


handleCallbackResult(RFJ,Timestamp,Result,Exception,NewArgList):-
	extractResultParts(RFJ,NewTimestamp,Result,Exception,NewArgList),
	!, % this is a result to the last javaMessage
	(NewTimestamp==Timestamp ->true ; write('Error: different timestamps in handleCallbackResult'),nl,fail).
handleCallbackResult(Contents,Timestamp,Result,Exception,NewArgList):-
	extractGoalVars(Contents,NewTimestamp,Goal,RVars,Error),
	!, % this is a new deterministicGoal call
	handleDeterministicGoal(Goal,RVars,Error,NewTimestamp,NewBytes), !, % nec for gc
	javaMessage2(NewBytes,Timestamp,Result,Exception,NewArgList).

% The next clauses must be kept in sync with the corresponding Java Classes:
specifyJavaCallback(ReturnArgs,Timestamp,Target,MessageName,ArgList,MessageFromProlog) :-
       ipObjectSpec('MessageFromProlog',MessageFromProlog,[ReturnArgs,Timestamp,ArgList,MessageName,Target],_).

extractResultParts(ResultFromJava,Timestamp,Result,Exception,Arguments) :-
       ipObjectSpec('ResultFromJava',ResultFromJava,[Timestamp,Arguments,Exception,Result],_).


% Counter/timestamp for javaMessage requests
:- dynamic(ip_jm_counter/1).
% :- asserta(ip_jm_counter(1)).
:- initialization(asserta(ip_jm_counter(1))).

% To make sure we number Prolog variables uniquely in a XJ session
ip_inc_jm_counter(N) :- retract(ip_jm_counter(N)), !,  N1 is N+1, asserta(ip_jm_counter(N1)).



%%% javaMessage, sugared versions

/*
Examples:
javaMessage(string(miguel),R,E,length,[],0,NewArgs), ipObjectSpec(Class,R,Value,_).
ipObjectSpec(int,BeginIndex,[2],_),ipObjectSpec(int,EndIndex,[4],_),
       javaMessage(string('miguel'),R,E,substring,[BeginIndex,EndIndex],1,NewArgs).
ipObjectSpec('IPClassVariable',Out,['java.lang.System','out'],_),
       javaMessage(Out,R,E,println,[string('Hello world!')],NewArgs).
ipObjectSpec('IPClassObject',Class,['java.lang.System'],_),
       javaMessage(Class,R,E,'getProperty',[string('java.home')],NArgs).
*/

% For compatibility:
javaMessage(Target,Result,Exception,MessageName,ArgList,NewArgList) :-
	javaMessage(Target,Result,Exception,MessageName,ArgList,1,NewArgList).

javaMessage(Target,Message) :- javaMessage(Target,_,Message).
javaMessage(Target,Result,Message) :-
	javaTarget(Target,RealTarget),
	Message =..[MName|Args],
	javaTargets(Args,RealArgs),
	javaMessage(RealTarget,Result,Exception,MName,RealArgs,0,_),
	(Exception\=null ->nl, write('Exception in javaMessage'), /* write(Exception),*/ nl, fail;true).

javaTarget(V,_) :- var(V), !, ipProgressMessage('javaMessage target can not be a variable'), fail.
javaTarget(null,null) :- !.
javaTarget(Target,RealTarget) :- integer(Target), !,
	ipObjectSpec('InvisibleObject',RealTarget,[Target],_).
javaTarget(Target,RealTarget) :- atom(Target), !,
	ipObjectSpec('IPClassObject',RealTarget,[Target],_).
javaTarget(Class-Variable,RealTarget) :- atom(Class), atom(Variable), !,
	ipObjectSpec('IPClassVariable',RealTarget,[Class,Variable],_).
javaTarget(Target,Target).
javaTargets([],[]) :- !.
javaTargets([A|Args],[RA|RealArgs]) :- javaTarget(A,RA), javaTargets(Args,RealArgs).


extractGoalVars(Object,Timestamp,Goal,RVars,Error) :-
  var(Error),
  ipObjectSpec('GoalFromJava',Object,[Timestamp,GoalAtom,OVar],_),
  atomRead(GoalAtom,X),
  (X==end_of_file -> Error=string('Syntax error in goal')
   ; X=gfj(Goal,OVar,RVars)).


mayPrepareGoalBindings(RVars,G,[Model],(G,buildTermModel(G,Model))) :- RVars==null, !.
mayPrepareGoalBindings(RVars,G,RVars,G).



handleDeterministicGoal(_Goal,_RVars,Error,Timestamp,NewBytes) :- nonvar(Error), !,
	specifyPrologResult(Timestamp,0,[],Result,Error),
	streamContents([Result],_,NewBytes,[]).
handleDeterministicGoal(Goal,RVars,Error,Timestamp,NewBytes)  :-
	mayPrepareGoalBindings(RVars,Goal,RVars2,Goal2),
	ipProgressMessage('Calling dg '-Goal2),
	%( call(Goal2) -> Succeeded=1, NewRVars=RVars2 ; Succeeded=0, NewRVars=[]),
	(supportsExceptions ->
		( catch(Goal2,PE, handle_ip_exception(PE,Error2)) ->
			(var(Error2) -> Succeeded=1, NewRVars=RVars2 ; Succeeded=0, NewRVars=[])
			;
			Succeeded=0, NewRVars=[])
		;
		( call(Goal2) -> Succeeded=1, NewRVars=RVars2 ; Succeeded=0, NewRVars=[]), Error2=_
	),
	ipProgressMessage(dg_result-Goal2-Succeeded/PE),
	(	specifyPrologResult(Timestamp,Succeeded,NewRVars,Result,Error2), streamContents([Result],_,NewBytes,[]) ->
		Error2=Error
		;
		Error = string('IP grammar failure, probably bad object specification'),
		specifyPrologResult(Timestamp,0,[],Result,Error), streamContents([Result],_,NewBytes,[])) .


% The next clauses must be kept in sync with the corresponding Java Classes:
% specifyPrologResult(Timestamp,Succeeded,RVars,Result,Error)

specifyPrologResult(Timestamp,Succeeded,RVars,Result,Error) :- nonvar(Error), !,
	ipObjectSpec('ResultFromProlog',Result,[Succeeded,Timestamp,Error,RVars],_).

specifyPrologResult(Timestamp,Succeeded,RVars,Result,Error) :- var(Error),
	is_list(RVars),
	\+ ((RVars=[X|_], (var(X);is_list(X)))), % guard against common programmer error
	!,
	ipObjectSpec('ResultFromProlog',Result,[Succeeded,Timestamp,null,RVars],_).

specifyPrologResult(Timestamp,_Succeeded,_RVars,Result,Error) :- var(Error),
	ipObjectSpec('ResultFromProlog',Result,[0,Timestamp,string('Bad specification of result bindings'),[]],_).


/****** Support for Java -> Prolog TOP deterministicGoal over sockets ******/

deterministicGoal :-
   ipsocketstreams(Input,Output),
   % ipProgressMessage(readNSocketChars_from-Input),
   readNSocketChars(4,Input,FourBytes),
   int(Size,FourBytes,[]), % stick to our grammar...
   readNSocketChars(Size,Input,Bytes),
   % tell('lastBytes.txt'),write(Bytes),nl,told,
   (streamContents([Contents],_,Bytes,[]) -> extractGoalVars(Contents,NewTimestamp,Goal,RVars,Error) ; Error=string('Could not understand objects sent from Java')),
   handleDeterministicGoal(Goal,RVars,Error,NewTimestamp,NewBytes),
   writeAllSocketChars(NewBytes,Output),
   ipProgressMessage('Exiting deterministicGoal '-Goal).



/****** Prolog term <-> TermModel specification ******/

% buildTermModel(Term,TermModelSpec)
buildTermModel(X,Model) :- copy_term(X,XX), buildTermModel_(XX,Model).

% Binds variables with iP_Variable_ terms
buildTermModel_(X,Model) :- integer(X), !,
	ipObjectSpec('java.lang.Integer',Integer,[X],_),
	ipObjectSpec('TermModel',Model,[0,null,Integer],_).
buildTermModel_(X,Model) :- float(X), !,
	ipObjectSpec('java.lang.Float',Float,[X],_),
	ipObjectSpec('TermModel',Model,[0,null,Float],_).
buildTermModel_(X,Model) :- var(X), !,
	ip_inc_var_counter(N), X= iP_Variable_(N), buildTermModel_(X,Model).
buildTermModel_(X,Model) :- atom(X), !,
	(is_list(X) -> IsList=1;IsList=0),
	ipObjectSpec('TermModel',Model,[IsList,null,string(X)],_).
buildTermModel_(iP_Variable_(N),Model) :- !,
	ipObjectSpec('VariableNode',VN,[N],_),
	ipObjectSpec('TermModel',Model,[0,null,VN],_).
/* flat lists: this implies revision in list2string etc.:
buildTermModel_(X,Model) :- is_list(X), !,
	ipObjectSpec('TermModel',Model,[ModelList,string('.')],_),
	buildTermModelList_(X,ModelList). % flat the list
*/
buildTermModel_(X,Model) :- X=..[Functor|Args],
	(is_list(X)->HasListFunctor=1;HasListFunctor=0),
	ipObjectSpec('TermModel',Model,[HasListFunctor,ModelList,string(Functor)],_),
	buildTermModelList_(Args,ModelList).

buildTermModelList_([],null) :- !.
buildTermModelList_(Args,ModelList) :-
	buildTermModelList_2(Args,ModelList_),
	ipObjectSpec('ArrayOfTermModel',ModelList,[ModelList_],_).

buildTermModelList_2([],[]).
buildTermModelList_2([A1|An],[Model1|ModelN]) :-
	buildTermModel_(A1,Model1), buildTermModelList_2(An,ModelN).

% Renumbers each term''s variables separately
buildTermModels([],[]).
buildTermModels([T1|Tn],[M1|Mn]) :- buildTermModel(T1,M1), buildTermModels(Tn,Mn).


% buildTermModelArray(+Terms,-Array)
buildTermModelArray(T,ArrayModel) :-
	buildTermModelList_2(T,TMs), ipObjectSpec('ArrayOfTermModel',ArrayModel,[TMs],_).

:- dynamic(ip_var_counter/1).
% :- asserta(ip_var_counter(0)).
:- initialization(asserta(ip_var_counter(0))).

% To make sure we number Prolog variables uniquely in a XJ session
ip_inc_var_counter(N) :- retract(ip_var_counter(N)), !,  N1 is N+1, asserta(ip_var_counter(N1)).



% recoverTermModel(Model,Term) ground(Model) holds
recoverTermModel(Model,Term) :-
	recoverTermModel(Model,VarChunks,[],Term),
	bindRepeatedVars(VarChunks).

recoverTermModels(Models,Terms) :-
	recoverTermModelList2(Models,VarChunks,[],Terms),
	bindRepeatedVars(VarChunks).

bindRepeatedVars([]) :- !.
bindRepeatedVars([_]) :- !.
bindRepeatedVars([C1|Chunks]) :- bindRepeatedVars(Chunks,C1), bindRepeatedVars(Chunks).

bindRepeatedVars([],_) :- !.
bindRepeatedVars([N-Var|Chunks],N-Var) :- !, bindRepeatedVars(Chunks,N-Var).
bindRepeatedVars([_|Chunks],NV) :- bindRepeatedVars(Chunks,NV).


recoverTermModel(Model,[N-Var|VarChunks],VarChunks,Var) :-
	ipObjectSpec('TermModel',Model,[0,null,VN],_), % A variable cannot have children, Prolog syntax
	ipObjectSpec('VariableNode',VN,[N],_), !.
recoverTermModel(Model,V,V,[]) :-
	ipObjectSpec('TermModel',Model,[1,null,string('.')],_), !.
recoverTermModel(Model,V,V,[]) :-
	ipObjectSpec('TermModel',Model,[1,null,string('[]')],_), !.
recoverTermModel(Model,V,V,Atom) :-
	ipObjectSpec('TermModel',Model,[0,null,string(Atom)],_), !.
recoverTermModel(Model,V,V,X) :-
	ipObjectSpec('TermModel',Model,[0,null,Integer],_),
	ipObjectSpec('java.lang.Integer',Integer,[X],_), !.
recoverTermModel(Model,V,V,X) :-
	ipObjectSpec('TermModel',Model,[0,null,Float],_),
	ipObjectSpec('java.lang.Float',Float,[X],_), !.
recoverTermModel(Model,V1,Vn,X) :-
	ipObjectSpec('TermModel',Model,[_PrologWillAssume,ModelList,string(Functor)],_),
	recoverTermModelList(ModelList,V1,Vn,Args),
	X=..[Functor|Args].

recoverTermModelList(null,V,V,[]) :- !.
recoverTermModelList(ModelList,V1,Vn,Args) :-
	ipObjectSpec('ArrayOfTermModel',ModelList,[ModelList_],_),
	recoverTermModelList2(ModelList_,V1,Vn,Args).

recoverTermModelList2([],V,V,[]) :- !.
recoverTermModelList2([M1|Mn],V1,Vn,[A1|An]) :-
	recoverTermModel(M1,V1,V2,A1), recoverTermModelList2(Mn,V2,Vn,An).


% recoverTermModelArray(+Model,-List)
recoverTermModelArray(Model,List) :-
	ipObjectSpec('ArrayOfTermModel',Model,[ModelList_],_),
	recoverTermModelList2(ModelList_,VarChunks,[],List),
	bindRepeatedVars(VarChunks).


%stringArraytoList(?StringArrayObject,?List)
stringArraytoList(Array,List) :-
	ipObjectTemplate('ArrayOfString',Array,_,[L],_),
	stringArraytoList2(L,List).

stringArraytoList2([],[]).
stringArraytoList2([string(X)|L],[X|List]) :- stringArraytoList2(L,List).


/****** Object <-> Term Definite Clause Grammar ******/

% A DCG implementing most of Sun Object Serialization Stream Protocol Grammar,
% cf. Serialization Specification JDK 1.1,
% http://java.sun.com/products/jdk/1.1/docs/guide/serialization/spec/protocol.doc.html
% Provides InterProlog's core ability to map Java objects to/from their Prolog term specifications
% Same as the stream grammar, first letters changed to lowercase where needed
% Grammar terminals are stream bytes
% Two extra args are PreviousHandles, NewHandles, to keep previous known objects
% Java references among objects in the stream map into Prolog unification
% For unimplemented aspects check the InterProlog README file, and comments below

% Asserting 'repeatedObjectsDetectedGenerating' will cause streamContents/4
% to detect repeated terms and map them into object handles,
% as one would normally expect. The default however is to detect only repeated
% CLASS, string (note these are kept unique anyway on the Java side)
% and VariableNode objects, for speed... AND for correctness too:
% Consider two objects X and Y in Java, such that !(X==Y) && X.equals(Y(), e.g. they're different objects
% but similar; they'll be serialized to Prolog as two different terms, TX and TY. But since TX and TY
% are ground, on the Prolog side TX==TY. When sending them back to Java, if repeatedObjectsDetectedGenerating,
% then a single object Z will be received by Java. This may be fixable by adding an extra (free) variable
% to object term specifications

% Handles in the Java-> Prolog direction are ALWAYS mapped into term unification,
% possibly creating circular Prolog terms. So to serialize such objects back to Java,
% you need 'repeatedObjectsDetectedGenerating' asserted... Which works, but incurring in the above
% correctness problem :-( In conclusion, this needs fixing in a future version - but not on this one
% because there's been no need yet to serialize objects that contain cyclic references from Prolog to Java ...

% The object <-> term grammar has been extended to support blockdata, so for example Vectors continue
% to be serializable to Prolog (even under JDK 1.4.2, which changed the serialization ways of Vectors)
% and other things such as AWT objects etc. may be serialized to Prolog too, and recovered later by Java
% (but see comments above for limitations). Notice that typically this "huge serialization" business
% is bad practice - if something is huge and mostly opaque from the Prolog side, it's better to
% pass an InvisibleObject (reference to the object) rather than to the object itself, and javaMessage to it as needed


:- dynamic(repeatedObjectsDetectedGenerating/0).
%:- asserta(repeatedObjectsDetectedGenerating).

streamContents(C,Handles,Bytes0,Bytes) :-
	(var(Bytes0) ->
		Job=generated(N), ipProgressMessage(grammar_generating), nonvar(C)
		% tell('generatingFor.txt'),write(C), nl,told
		;
		Job=parsed(N), ipProgressMessage(grammar_analysing)
	),
	reset_dcg_mode, % to be compatible with programs using tphrase_set_string
	streamContents0(C,Handles,Bytes0,Bytes),
        !,
	Handles = handles(N,_).
streamContents(C,Handles,Bytes0,Bytes) :- ipIsDebugging,
	tell('lastStreamContents.txt'), write('This failed unexpectedly:'), nl,
	write(streamContents(C,Handles,Bytes0,Bytes)), nl, told,
	fail.

streamContents0(C,Handles) -->
	magic, streamversion, contents(C,handles(-1,[]),Handles), !.

% Slightly different recursion pattern at the top level:
contents([C|More],H1,Hn) --> content(C,H1,H2), moreContents(More,H2,Hn).

moreContents(More,H1,Hn) --> contents(More,H1,Hn).
moreContents([],H,H) --> [].

content(O,H1,Hn) --> object(O,H1,Hn).
content(Block,H,H) --> blockdata(Block).

object(O,H1,Hn) --> nullReference(O,H1,Hn) /*, !*/.
object(O,H1,Hn) --> prevObject(O,H1,Hn). % this alternative must be before the next ones
object(O,H1,Hn) --> newString(O,H1,Hn).
object(O,H1,Hn) --> newObject(O,H1,Hn).
object(O,H1,Hn) --> newArray(O,H1,Hn).
object(O,H1,Hn) --> newClass(O,H1,Hn).
object(O,H1,Hn) --> newClassDesc(O,H1,Hn).
object(O,H1,Hn) --> exception(O,H1,Hn).
object(O,H1,Hn) --> newEnum(O,H1,Hn).
object(tC_RESET,H,H) --> tC_RESET.

newClass(D,H1,Hn) --> tC_CLASS, classDesc(D,H1,Hn) /* buggy Sun grammar! , newHandle(D)*/.

classDesc(D,H1,Hn) --> prevObject(D,H1,Hn), {D = class(_,_,_) /* probably superfluous */}, !.
classDesc(D,H1,Hn) --> nullReference(D,H1,Hn), !.
classDesc(D,H1,Hn) --> newClassDesc(D,H1,Hn).

superClassDesc(D,H1,Hn) --> classDesc(D,H1,Hn).

newClassDesc(class(Name,VUID,Info),H1,Hn) -->
	tC_CLASSDESC, className(Name,H1,H2),
	% {ipProgressMessage(newClassDesc-Name)},
	% ipShowTerminalsIf(Name='[Ljava.lang.String;'),
	serialVersionUID(VUID,H2,H3),
	newHandle(class(Name,VUID,Info),H3,H4),
	% {ipProgressMessageIf('added handle',Name=='[Ljava.lang.String;')},
	classDescInfo(Info,H4,Hn).
newClassDesc(_Class,H,H) -->
      tC_PROXYCLASSDESC, { /* ipProgressMessage('Not handling proxy class descriptions'),*/ fail}.

classDescInfo(classDescInfo(Fields,Flags,Super),H1,Hn) -->
	classDescFlags(Flags,H1,H2), fields(Fields,H2,H3), classAnnotation(H3,H4),
	superClassDesc(Super,H4,Hn).

className(Name,H,H) --> utf(Name).

serialVersionUID(VUID,H,H) --> long(VUID).

classDescFlags(Flags,H,H) --> byte(Flags).

fields(Fields,H1,Hn) --> ipPeekTerminal(NextByte),
%	{ground(Fields) -> length(Fields,Count) ; Count=Count},
	{var(NextByte) -> length(Fields,Count) ; Count=Count},
	short(Count), nFieldDesc(Count,Fields,H1,Hn).

nFieldDesc(0,[],H,H) --> [].
nFieldDesc(N,[F|More],H1,Hn) -->
	a_fieldDesc(F,H1,H2), {NN is N-1}, nFieldDesc(NN,More,H2,Hn).

a_fieldDesc(F,H1,Hn) -->
	(primitiveDesc(F,H1,Hn) ; objectDesc(F,H1,Hn)).

primitiveDesc(byte(Name),H,H) --> [66], fieldName(Name). % B
primitiveDesc(boolean(Name),H,H) --> [90], fieldName(Name). % Z
primitiveDesc(short(Name),H,H) --> [83], fieldName(Name). % S
primitiveDesc(int(Name),H,H) --> [73], fieldName(Name). % I
primitiveDesc(char(Name),H,H) --> [67], fieldName(Name). % C
primitiveDesc(float(Name),H,H) --> [70], fieldName(Name). % F
primitiveDesc(double(Name),H,H) --> [68], fieldName(Name). % D
primitiveDesc(long(Name),H,H) --> [74], fieldName(Name). % J

objectDesc(arrayField(Name,ClassName),H1,Hn) -->
	[91], fieldName(Name), object(string(ClassName),H1,Hn). % '[' previously used nonterminal className
objectDesc(objectField(Name,ClassName),H1,Hn) -->
	[76], fieldName(Name), object(string(ClassName),H1,Hn). % 'L' previously used nonterminal className

fieldName(Name) --> utf(Name).

% className(Name) --> utf(Name).

classAnnotation(H,H) --> endBlockData. % Not accepted: endBlockData ; contents, endBlockData.

newArray(arrayObject(D,Values),H1,Hn) -->
	tC_ARRAY, classDesc(D,H1,H2), newHandle(arrayObject(D,Values),H2,H3),
	ipPeekTerminal(NextByte),
	{var(NextByte) -> length(Values,Size) ; Size=Size},
%	{ground(Values) -> length(Values,Size) ; Size=Size},
	int(Size), nvalues(Size,D,Values,H3,Hn).


nvalues(0,_,[],H,H) --> [], !.
nvalues(N,D,[V|More],H1,Hn) --> elementValue(D,V,H1,H2), {NN is N-1}, nvalues(NN,D,More,H2,Hn).

% this test should be done before the loop... but would require either redundancy or a meta-call...
elementValue(class('[B',_,_),V,H,H) --> !, byte(V).
elementValue(class('[Z',_,_),V,H,H) --> !, boolean(V).
elementValue(class('[S',_,_),V,H,H) --> !, short(V).
elementValue(class('[I',_,_),V,H,H) --> !, int(V).
elementValue(class('[C',_,_),V,H,H) --> !, char(V).
elementValue(class('[F',_,_),V,H,H) --> !, float(V).
elementValue(class('[D',_,_),V,H,H) --> !, double(V).
elementValue(class('[J',_,_),V,H,H) --> !, long(V).
elementValue(class('[I',_,_),V,H,H) --> !, int(V).
elementValue(_,V,H1,Hn) --> object(V,H1,Hn).
	% Final case: not an array of basic type. Type verifications (D...) superfluous...


newObject(object(D,Data),H1,Hn) -->
	tC_OBJECT, classDesc(D,H1,H2),
	newHandle(object(D,Data),H2,H3), classesData(D,Data,H3,Hn).

% Topmost class (Object) causes topmost value to always be []...
% Beware of ingenuous use of append to flatten + .. + ... we do need invertibility ;-)
classesData(class(Name,VUID,classDescInfo(Fields,Flags,Super)),SData+ThisData,H1,Hn) -->
	superClassData(Super,SData,H1,H2),
	classData(class(Name,VUID,classDescInfo(Fields,Flags,Super)),ThisData,H2,Hn).

superClassData(null,[],H,H) --> !.
superClassData(D,Data,H1,Hn) --> classesData(D,Data,H1,Hn).

/* Several cases to consider depending the serialization flags. First is
serializable without writeObject method:*/
classData(class(Name,VUID,classDescInfo(Fields,Flags,Super)),Data,H1,Hn) -->
	{Flags=2},
	% cf. http://java.sun.com/j2se/1.4.2/docs/api/constant-values.html#java.io.ObjectStreamConstants.SC_SERIALIZABLE
	nowrclass(class(Name,VUID,classDescInfo(Fields,Flags,Super)),Data,H1,Hn).
/* Second is serializable with writeObject method:*/
classData(class(Name,VUID,classDescInfo(Fields,Flags,Super)),DefaultData-BlockData,H1,Hn) -->
	{Flags=3},
	% cf. http://java.sun.com/j2se/1.4.2/docs/api/constant-values.html#java.io.ObjectStreamConstants.SC_WRITE_METHOD
	wrclass(class(Name,VUID,classDescInfo(Fields,Flags,Super)),DefaultData,H1,H2),
	objectAnnotation(BlockData,H2,Hn).
/* Cases involving Externalizable are not handled, as in general parsing requires knowledge coded in Externalizable.readExternal */
classData(class(Name,_VUID,classDescInfo(_Fields,Flags,_Super)),_,_,_) -->
	{nonvar(Flags), Flags\==2, Flags\==3, !, ipProgressMessage(('Bad flags in class:'-Flags-Name)), fail}.


nowrclass(D,Data,H1,Hn) --> values(D,Data,H1,Hn). % fields in order of class descriptor.....

wrclass(D,Data,H1,Hn) --> nowrclass(D,Data,H1,Hn).

objectAnnotation(blockdata(empty),H,H) --> endBlockData.
objectAnnotation(Contents,H1,Hn) --> contents(Contents,H1,Hn) , endBlockData.

endBlockData --> tC_ENDBLOCKDATA.

blockdata(Block) --> blockdatashort(Block).
blockdata(Block) --> blockdatalong(Block).

blockdatashort(blockdata(Block)) -->
	tC_BLOCKDATA, [Size], {var(Size)->length(Block,Size);true},
	{Size<256, Size>=0},
	blockbytes(Size,Block).
blockdatalong(blockdata(Block)) -->
	tC_BLOCKDATALONG, {nonvar(Block)->length(Block,Size);true},
	int(Size),
	{Size>255},
	blockbytes(Size,Block).

% blockbytes(+Size,?Block)
blockbytes(0,[]) --> !.
blockbytes(Size,[Byte|Block]) -->
	[Byte], {NewSize is Size-1}, blockbytes(NewSize,Block).

newString(string(S),H1,Hn) --> tC_STRING, utf(S), newHandle(string(S),H1,Hn).
newString(_S,H,H) --> tC_LONGSTRING,
	{/* ipProgressMessage('Not handling long strings yet'),*/ fail}.

newEnum(_O,H,H) -->
       tC_ENUM,  {/*ipProgressMessage('Not handling enums yet'),*/ fail}.

prevObject(X,H1,Hn) --> tC_REFERENCE, handle(X,H1,Hn).


/* Handles argument: handles(TotalCount,Objects), where Objects is a
2-3 tree (see below) with key-value pairs of the object
and its handle number.  If the use is java->prolog (parsing), then
handle number is the key and the object is the value; if the use is
prolog->java (generation), then the object is the key and the
handle-number is the value.  This is for improved indexing. */

/*
handle(X,Handles,Handles) --> [0,126,H1,H0],
	% baseWireHandle in java.io.ObjectStreamConstants; no more than 64k-1 objects!
	{(nonvar(H1)-> N is H1*256+H0 ; N=N)},
	{handle2Object(N,Handles,X), H0 is N mod 256, H1 is N//256}.
*/
% No longer limited to 64k-1 objects, courtesy XSB, Inc.:
handle(X,Handles,Handles) --> [B1,B2,B3,B4],
	{(nonvar(B1)
	  ->	 N is (B1<<24 + B2<<16 + B3<<8 + B4) - 8257536 /*16'7E0000*/,
		 handle2Object(N,Handles,X)
	  ;	 handle2Object(N1,Handles,X),
		 N is N1 + 8257536 /*16'7E0000*/,
		 B4 is N /\ 255,
		 Na is N>>8,
		 B3 is Na /\ 255,
		 Nb is Na>>8,
		 B2 is Nb /\ 255,
		 B1 is Nb>>8 /\ 255
	 )}.

handle2Object(N,H1,X) :-
	nonvar(N) -> fetchPreviousHandle(H1,N,X) ; findPreviousObject(X,H1,N), nonvar(N).


% var(N) and nonvar(X) hold, we are generating
% findPreviousObject(X,Handles,N)
% notice that objects in handles are ground terms

findPreviousObject(Object,handles(_N,Tree),N) :-
	once(( Object = class(_,_,_) ; Object = string(_) ;
	  Object = object(class('com.declarativa.interprolog.VariableModel',_,_),_);
	  repeatedObjectsDetectedGenerating )),
	find(Tree,Object,N).

% nonvar(N) holds, we are parsing
fetchPreviousHandle(handles(_C,Tree),N,Object) :-
	find(Tree,N,Object).

% we''re generating:
newHandle(Object,handles(N0,Tree0),handles(N1,Tree1),Bytes,Bytes) :-
	N1 is N0+1,
	(nonvar(Bytes)
         ->     addkey(Tree0,N1,Object,Tree1)
	 ;      addkey(Tree0,Object,N1,Tree1)
        ),
     !.
% do not fail if already in tree, in sync with findPreviousObject
newHandle(_,handles(N0,Tree0),handles(N1,Tree0),Bytes,Bytes) :- N1 is N0+1.


nullReference(null,H,H) --> tC_NULL.

exception(unsupportedException,H,H) --> tC_EXCEPTION.
	% not supported: , reset, object(X), {is_throwable(X)}, reset ????

%%%resetContext --> tC_RESET.

magic --> sTREAM_MAGIC.

streamversion --> sTREAM_VERSION.

% values(+ClassDescription,-Data,H1,Hn)
values(class(_Name,_VUID,classDescInfo(Fields,_Flags,_Super)),Data,H1,Hn) -->
	valuesOfFields(Fields,Data,H1,Hn).

valuesOfFields([],[],H,H) --> [].
valuesOfFields([F1|Fn],[D1|Dn],H1,Hn) --> valueOfField(F1,D1,H1,H2), valuesOfFields(Fn,Dn,H2,Hn).

valueOfField(byte(_FieldName),X,H,H) --> byte(X).
valueOfField(short(_FieldName),X,H,H) --> short(X).
valueOfField(int(_FieldName),X,H,H) --> int(X).
valueOfField(long(_FieldName),X,H,H) --> long(X).
valueOfField(boolean(_FieldName),X,H,H) --> boolean(X).
valueOfField(char(_FieldName),X,H,H) --> char(X).
valueOfField(float(_FieldName),X,H,H) --> float(X).
valueOfField(double(_FieldName),X,H,H) --> double(X).
valueOfField(objectField(_FieldName,_ClassName),Value,H1,Hn) -->
	object(Value,H1,Hn). % Avoid binding ClassName downwards, weird chars in it...
	% No type checking here, that's not our job
valueOfField(arrayField(_FieldName,_ClassName),Values,H1,Hn) -->
	object(Values,H1,Hn).



% BASIC TYPES...
byte(X) --> [B0], {var(X) -> analyseByte(B0,X) ;  integer(X), generateByte(B0,X)}.
short(X) --> [B1,B0], {var(X) -> analyseShort(B1,B0,X) ;  integer(X), generateShort(B1,B0,X)}.
int(X) --> [B3,B2,B1,B0],
	{var(X) -> analyseInt(B3,B2,B1,B0,X) ; integer(X), generateInt(B3,B2,B1,B0,X)}.

analyseByte(B0,X) :- B0>=128, !, X is B0-256.
analyseByte(B0,B0).

generateByte(B0,X) :- X<0, !, B0 is X+256.
generateByte(B0,B0).


analyseShort(B1,B0,X) :- B1>=128, !, X is B1*256+B0 - 65536.
analyseShort(B1,B0,X) :- X is B1*256+B0.

generateShort(B1,B0,X) :- X<0, !, XX is X + 65536, B0 is XX mod 256, B1 is XX // 256.
generateShort(B1,B0,X) :- B0 is X mod 256, B1 is X // 256, B1 < 256.

analyseInt(B3,B2,B1,B0,X) :-
	X is (B3<< 24) \/ (B2 << 16) \/ (B1 << 8) \/ B0.

generateInt(B3,B2,B1,B0,X) :-
	B3 is (X >> 24) /\ 255, B2 is (X >> 16) /\  255, B1 is (X >> 8) /\  255, B0 is X /\ 255.

% to big for Prolog to handle:
% long(long(B7,B6,B5,B4,B3,B2,B1,B0)) --> [B7,B6,B5,B4,B3,B2,B1,B0].
long(long(B76,B54,B32,B10)) --> short(B76), short(B54), short(B32), short(B10).

boolean(X) --> [X].

float(X) --> [B3,B2,B1,B0],
	{var(X) -> analyseFloat(B3,B2,B1,B0,S,M,E), cvtFloat(S,E,M,X)
			 ; float(X),
			 (X=0.0 -> B3=0, B2=0, B1=0, B0=0 ;
			  cvtFloat(S,E,M,X), generateFloat(B3,B2,B1,B0,S,M,E)) }.

double(double(B7,B6,B5,B4,B3,B2,B1,B0)) --> [B7,B6,B5,B4,B3,B2,B1,B0]. % cf. IEEE 754

char(X) --> [B1,B0],
	{var(X) -> X is B1*256 + B0 ;
		integer(X), X >= 0, B0 is X mod 256, B1 is X // 256, B1 < 256}.


/*atom_chars is bad according to Michael Kifer, should use atom_codes:*/
utf(Name) --> {var(Name)}, !,
	short(Length), utf2Atomchars(Length,List), {atom_codes(Name,List)}.
utf(Name) --> {atom_codes(Name,List)}, [B1,B0],
	atomchars2utf(List,ByteCount), {generateShort(B1,B0,ByteCount)}.

/* obsolete
utfChars(0,[]) --> [].
utfChars(N,[C|More]) --> [C], {NN is N-1}, utfChars(NN,More).
*/

% cf. DataInput.readUTF() documentation:
% utf2Atomchars(+BytesLeft,-AtomChars)  parsing the stream
utf2Atomchars(0,[]) --> [].
utf2Atomchars(N,[C|More]) --> [C], {C<128, !, NN is N-1}, utf2Atomchars(NN,More).
utf2Atomchars(N,[AC|More]) --> [A,B],
	{NN is N-2, AC is (((A /\ 31) << 6) \/ (B /\ 63)), AC<256}, utf2Atomchars(NN,More).

% cf. DataOutput.writeUTF(String) documentation:
% atomchars2utf(+AtomChars,-ByteCount)  generating the stream
atomchars2utf([],0) --> [].
atomchars2utf([C|More],N) --> [C], {C<128, !},
	atomchars2utf(More,NN), {N is NN+1}.
atomchars2utf([C|More],N) --> [C1,C2],
	{C<256, C1 is (192 \/ (31 /\ (C >> 6))), C2 is (128 \/ (63 /\ C))},
	atomchars2utf(More,NN), {N is NN+2}.


%cf. java.io.ObjectStreamConstants
sTREAM_MAGIC --> [172,237].
sTREAM_VERSION --> [0,5].
tC_NULL --> [112].
tC_REFERENCE --> [113].
tC_CLASSDESC --> [114].
tC_OBJECT --> [115].
tC_STRING --> [116].
tC_ARRAY --> [117].
tC_CLASS --> [118].
tC_BLOCKDATA --> [119].
tC_ENDBLOCKDATA --> [120].
tC_RESET --> [121].
tC_BLOCKDATALONG --> [122].
tC_EXCEPTION --> [123].
tC_LONGSTRING --> [124].
tC_PROXYCLASSDESC --> [125].
tC_ENUM --> [126].


%%%%%%%%% Grammar ends here

% cf. IEEE 754 float(B3,B2,B1,B0)

analyseFloat(B3,B2,B1,B0,Sign,Mantissa,Exponent) :-
	Sign is B3 >> 7, Exponent is ((B3 /\ 127)<<1)+ (B2 >>7) - 127,
	Mantissa is ((B2 /\  127)<< 16) + (B1<<8) + B0. % without the extra 1 to the left of decimal point

generateFloat(B3,B2,B1,B0,Sign,Mantissa,Exponent) :-
	B3 is (Sign << 7) \/ ((Exponent+127) >> 1),
	B2 is (((Exponent+127) /\ 1) << 7)   \/ (Mantissa >> 16),
	B1 is (Mantissa >> 8) /\ 255,
	B0 is Mantissa /\ 255.

/************************************************/
/* convert float (IEEE 754), courtesy of Davis S. Warren
cvtFloat(+Sign,+Exponent,+Mantissa,-Float)
cvtFloat(-Sign,-Exponent,-Mantissa,+Float)

interconverts between a sign/exponent/mantissa representation of a float
and an XSB float.  sign/exponent and mantissa are all integers.

   sign is 1 if Float is negative, 0 if positive
   exponent is the integer power of 2 of the Float
   mantissa is an integer of 23 bits which are the bits of the
     mantissa in IEEE 754 floating point standard (without the extra 1
     to the left of the binary point.)

[These routines work for sign and exponent for any representation.  The
 mantissa representation is special for single precision floats and
 would need to be changed (to include another mantissa integer) to work
 for doubles.]
**/

% See two related predicates above, as they need to vary among Prologs: float_to_intrep and extractMantissa

cvtFloat(Sign,Power,Mantissa,Float) :-
	(nonvar(Float)
         ->     float_to_intrep(Sign,Power,Mantissa,Float)
         ; nonvar(Sign),nonvar(Power),nonvar(Mantissa)
         ->     intrep_to_float(Sign,Power,Mantissa,Float)
         ;      write('Instantiation Error: in cvtFloat/4'), nl,
	        fail
        ).

intrep_to_float(Sign,Power,Mantissa,Float) :-
	(Power < 0
	 ->   PowerA is - Power,
	      generatePower2(PowerA,2.0,1.0,PfA),
	      Pf is 1.0 / PfA
	 ;    generatePower2(Power,2.0,1.0,Pf)
        ),
	generateMantissa(23,Mantissa,0.0,Mf1),
	Mf is Mf1 + 1.0,
	FloatP is Pf * Mf,
	(Sign > 0 -> Float is - FloatP ; Float = FloatP).

generatePower2(N,P,Fi,Fo) :-
	N > 0
         ->     B is N /\ 1,
	        N1 is N >> 1,
		(B > 0 -> Fi1 is Fi*P ; Fi1 = Fi),
		P1 is P*P,
		generatePower2(N1,P1,Fi1,Fo)
	 ;      Fo=Fi.


generateMantissa(K,N,Fi,Fo) :-
	K =< 0
         ->     Fo = Fi
         ;      K1 is K-1,
	        N1 is N>>1,
		(N/\1 > 0
	         ->     Fi1 is (Fi+1.0)/2
	         ;      Fi1 is Fi/2
	        ),
		generateMantissa(K1,N1,Fi1,Fo).


extractPower(Float,Rem,PowI,Power) :-
	Float < 1.0
         ->     extractPowerM(Float,Rem,PowI,Power)
         ;      extractPowerP(Float,Rem,PowI,Power).

extractPowerP(Float,Rem,PowI,Power) :-
	(Float < 2.0
         ->     Rem = Float,
	        Power = PowI
	 ;      Float1 is Float / 2,
	        Power1 is PowI+1,
		extractPowerP(Float1,Rem,Power1,Power)
	).

extractPowerM(Float,Rem,PowI,Power) :-
	(Float >= 1.0
         ->     Rem = Float,
	        Power = PowI
	 ;      Float1 is Float * 2,
	        Power1 is PowI-1,
		extractPowerM(Float1,Rem,Power1,Power)
	).


/****** Object term analysis (cf. also ipProcessExamples and ipcompareTerms) ******/

% ipAnalyseTerm(Term,Template,Names,Vars,Subs)
% A helper predicate to obtain variable names and types
% This depends on the Java object grammar; it might perhaps be produced by partial evaluation
% of a grammar lookahead...

ipAnalyseTerm(
	object(class(Name,VUID,ClassInfo),Data),
	object(class(Name,VUID,ClassInfo),TData),
	Names,Vars,Subs
	) :- !,
	ipAnalyseFields(ClassInfo,Data,TData,Names,Vars,Subs).
ipAnalyseTerm(
	arrayObject(class(Name,VUID,Info),Values),
	arrayObject(class(Name,VUID,Info),VarValues),
	[arrayof-Name],[VarValues],[Values]
	) :- !.
ipAnalyseTerm(T,T,[],[],[]).

ipAnalyseFields(classDescInfo(Fields,_,null),[]+ThisData, []+TemplateData,Names,Vars,Subs) :-
	!,
	ipAnalyseClassFields(Fields,ThisData,TemplateData,Names,Vars,Subs).
ipAnalyseFields(classDescInfo(Fields,_,/*Super*/
		class(_SName,_SVUID,SuperInfo)), SuperData+ThisData, TemplateSuper+TemplateThis,Names,Vars,Subs
	) :-
	ipAnalyseFields(/*Super*/ SuperInfo,SuperData,TemplateSuper,Names1,Vars1,Subs1),
	ipAnalyseClassFields(Fields,ThisData,TemplateThis,Names2,Vars2,Subs2),
	append(Names1,Names2,Names),
	append(Vars1,Vars2,Vars),
	append(Subs1,Subs2,Subs).

% ipAnalyseClassFields(Fields,ThisData,TemplateThis,Names2,Vars2,Subs2): do it for one class
ipAnalyseClassFields([],[],[],[],[],[]) :- !.
ipAnalyseClassFields([Field1|Fields],[Data1|Data],[Var1|Templates],[Field1|Names],[Var1|Vars],[Data1|Subs]) :-
	ipAnalyseClassFields(Fields,Data,Templates,Names,Vars,Subs).




% ipObjectSpec(ClassName,VarValues,Object)
% This predicate can be used both to build new objects and to introspect its variables
% Keep in mind we are talking about object specifications on the Prolog side, not real Java objects
% A ClassName instance must have been taught with teachMoreObjects
% If the class is an array, VarValues will be a list simply with the array values
% Else:
% VarValueList is a list [VarName1=Value1,...,VarNameN=ValueN]
% Each VarName must be an atom, the name of a Java instance variable of the class
% If var(Object) then all values must be ground, and Object will be bound to a specification
% of an object similar to the Class prototype that was taught except for the different values in the list
% If nonvar(Object) then the unbound values in the list will be bound to the object variable values
% Each Value must be compatible with the corresponding object field; this is only partially checked


ipObjectSpec(Class,Values,arrayObject(_D,Values)) :-
	ipObjectTemplate(Class,arrayObject(_D,Values),_,_,_),
	!,
	is_list(Values).

ipObjectSpec(Class,VarValueList,Object) :-
	atom(Class), is_list(VarValueList),
	ipObjectTemplate(Class,Object,ANames,TVars,TSubs),
	bindObjectVars(VarValueList,ANames,TVars),
	bindFreeVarsWith(TVars,TSubs).

bindFreeVarsWith([],[]) :- !.
bindFreeVarsWith([V|Vn],[_|Xn]) :- nonvar(V), !, bindFreeVarsWith(Vn,Xn).
bindFreeVarsWith([X|Vn],[X|Xn]) :- bindFreeVarsWith(Vn,Xn).


bindObjectVars([],_ANames,_TVars) :- !.
bindObjectVars([VarName=X|VarValues],ANames,TVars) :-
	findCheckVarAndType(ANames,TVars,VarName,X),
	bindObjectVars(VarValues,ANames,TVars).


% findCheckVarAndType(Names,Vars,VarName,Value)
findCheckVarAndType([Term|_],[Value|_],VarName,Value) :-
	typeNameTerm(Term,VarName,Value), !.
findCheckVarAndType([_|Terms],[_|FreeVars],VarName,Value) :-
	findCheckVarAndType(Terms,FreeVars,VarName,Value).

% typeNameTerm(Term,VarName,Value)
% succeeds if Term denotes an object field with name VarName, to which a
% assignment of Value can be performed; this test is incomplete, as (1) the Java class hierarchy is not available
% and (2) we are not dissecting and checking object type strings
% cf. cases for valueOfField in grammar
typeNameTerm(byte(VarName),VarName,X) :- !, integer(X), X >= -128, X =< 127.
typeNameTerm(short(VarName),VarName,X) :- !, integer(X), X >= -32768, X =< 32767.
typeNameTerm(int(VarName),VarName,X) :- !, integer(X).
typeNameTerm(long(VarName),VarName,X) :- !, X=long(_,_,_,_).
typeNameTerm(boolean(VarName),VarName,X) :- !, integer(X), X>=0, X=<1.
typeNameTerm(char(VarName),VarName,X) :- !, integer(X), X>=0, X=<255.
typeNameTerm(float(VarName),VarName,X) :- !, float(X).
typeNameTerm(double(VarName),VarName,X) :- !, X=double(_B7,_B6,_B5,_B4,_B3,_B2,_B1,_B0).
typeNameTerm(objectField(VarName,_Type),VarName,Value) :- !,
	(Value = object(_D,_V) ; Value = class(_,_,_) ; Value = null ; Value = string(A), atom(A)).
typeNameTerm(arrayField(VarName,_Type),VarName,Value) :- !, Value = arrayObject(_D,Values), is_list(Values).


/******************************************************************/
/* 2-3 tree routines, taken initially from I. Bratko, Prolog      */
/* Programming for AI, and modified by David S. Warren.           */
/******************************************************************/
addkey([],X,V,l(X,V)) :- !.
addkey(Tree,X,V,Tree1) :-
	ins2(Tree,X,V,Trees),
	cmb0(Trees,Tree1).


find(l(X,V),Xs,V) :- X == Xs.
find(n2(T1,M,T2),X,V) :-
	M @=< X
	 ->	find(T2,X,V)
	 ;	find(T1,X,V).
find(n3(T1,M2,T2,M3,T3),X,V) :-
	M2 @=< X
	 ->	(M3 @=< X
		 ->	find(T3,X,V)
		 ;	find(T2,X,V)
		)
	 ;	find(T1,X,V).




ins2(n2(T1,M,T2),X,V,Tree) :-
	M @=< X
	 ->	ins2(T2,X,V,Tree1),
		cmb2(Tree1,T1,M,Tree)
	 ;	ins2(T1,X,V,Tree1),
		cmb1(Tree1,M,T2,Tree).
ins2(n3(T1,M2,T2,M3,T3),X,V,Tree) :-
	M2 @=< X
	 ->	(M3 @=< X
		 ->	ins2(T3,X,V,Tree1),
			cmb4(Tree1,T1,M2,T2,M3,Tree)
		 ;	ins2(T2,X,V,Tree1),
			cmb5(Tree1,T1,M2,M3,T3,Tree)
		)
	 ;	ins2(T1,X,V,Tree1),
		cmb3(Tree1,M2,T2,M3,T3,Tree).
ins2(l(A,V),X,Vn,Tree) :-
	A @=< X
	 ->	(X @=< A
		 ->	fail
		 ;	Tree = t(l(A,V),X,l(X,Vn))
		)
	 ;	Tree = t(l(X,Vn),A,l(A,V)).

cmb0(t(Tree),Tree).
cmb0(t(T1,M,T2),n2(T1,M,T2)).

cmb1(t(NT1),M,T2,t(n2(NT1,M,T2))).
cmb1(t(NT1a,Mb,NT1b),M,T2,t(n3(NT1a,Mb,NT1b,M,T2))).

cmb2(t(NT2),T1,M,t(n2(T1,M,NT2))).
cmb2(t(NT2a,Mb,NT2b),T1,M,t(n3(T1,M,NT2a,Mb,NT2b))).

cmb3(t(NT1),M2,T2,M3,T3,t(n3(NT1,M2,T2,M3,T3))).
cmb3(t(NT1a,Mb,NT1b),M2,T2,M3,T3,t(n2(NT1a,Mb,NT1b),M2,n2(T2,M3,T3))).

cmb4(t(NT3),T1,M2,T2,M3,t(n3(T1,M2,T2,M3,NT3))).
cmb4(t(NT3a,Mb,NT3b),T1,M2,T2,M3,t(n2(T1,M2,T2),M3,n2(NT3a,Mb,NT3b))).

cmb5(t(NT2),T1,M2,M3,T3,t(n3(T1,M2,NT2,M3,T3))).
cmb5(t(NT2a,Mb,NT2b),T1,M2,M3,T3,t(n2(T1,M2,NT2a),Mb,n2(NT2b,M3,T3))).
