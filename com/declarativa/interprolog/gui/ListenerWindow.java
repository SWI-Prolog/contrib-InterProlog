/* 
** Author(s): Miguel Calejo
** Contact:   interprolog@declarativa.com, http://www.declarativa.com
** Copyright (C) Declarativa, Portugal, 2000-2002
** Use and distribution, without any warranties, under the terms of the 
** GNU Library General Public License, readable in http://www.fsf.org/copyleft/lgpl.html
*/
package com.declarativa.interprolog.gui;
import com.declarativa.interprolog.*;
import com.declarativa.interprolog.util.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.table.*;
import javax.swing.event.*;
import java.awt.dnd.*;
import java.awt.datatransfer.*;

/** A simple Prolog listener, with a consult menu and an history mechanism. 
This should be sub-classed, in order to define sendToProlog()*/
public abstract class ListenerWindow extends JFrame implements WindowListener{	
	public JTextArea prologOutput, prologInput; 
	JMenu historyMenu, fileMenu; 
	Vector loadedFiles;
	private static int topLevelCount = 0;
	public AbstractPrologEngine engine = null;
	
	public ListenerWindow(AbstractPrologEngine e){
		this(e,true);
	}
	public ListenerWindow(AbstractPrologEngine e, boolean autoDisplay){
		super("PrologEngine listener (Swing)");		
		if (e!=null) engine=e;
		else throw new IPException("missing Prolog engine");
		String VF = e.getImplementationPeer().visualizationFilename();
		if (engine.getLoadFromJar()) engine.consultFromPackage(VF,ListenerWindow.class);
		else engine.consultRelative(VF,ListenerWindow.class);
		engine.teachMoreObjects(guiExamples());
				
		if (engine==null) dispose(); // no interface object permitted!
		else topLevelCount++;
		debug=engine.isDebug();
		
		loadedFiles = new Vector();
		
		constructWindowContents();
		constructMenu();
		
				
		addWindowListener(this);
		prologInput.addKeyListener(new KeyAdapter(){
			public void keyPressed(KeyEvent e){
				if (e.getKeyCode()==KeyEvent.VK_ENTER) {
					sendToProlog();
					e.consume();
				}
			}
		});
		prologOutput.append("Welcome to an InterProlog top level\n"+e.getPrologVersion() + "\n\n");
        if (autoDisplay) {
		    setVisible(true);
		    focusInput();	
		}
		
	}
			
	// WindowListener methods
	public void windowOpened(WindowEvent e){}
	public void windowClosed(WindowEvent e){}
	public void windowIconified(WindowEvent e){}
	public void windowClosing(WindowEvent e){
		dispose();
		engine.shutdown();
		topLevelCount--;
		if (topLevelCount <= 0) System.exit(0);
			// should check whether any relevant windows are changed...
	}
	public void windowActivated(WindowEvent e){
		prologInput.requestFocus();
	}
	public void windowDeactivated(WindowEvent e){}
	public void windowDeiconified(WindowEvent e){}
	
	public static ObjectExamplePair[] guiExamples() {
		ObjectExamplePair[] examples = {
			PredicateTableModel.example(),
			TermListModel.example(),
			TermTreeModel.example(),
			new ObjectExamplePair("ArrayOfTermTreeModel",new TermTreeModel[0]),
			XSBTableModel.example(),
		};
		return examples;
	}
	
	void constructWindowContents(){
		Font prologFont = new Font("Courier",Font.PLAIN,12);
		Container c = getContentPane();
		c.setLayout(new BorderLayout());
		prologOutput = new JTextArea(20,80); prologOutput.setFont(prologFont); 
		prologOutput.setEditable(false); 
		prologOutput.setToolTipText("Here's Prolog console output");
		prologOutput.setLineWrap(true);  // Swing used to crash with large ammounts of text...
		prologOutput.setDoubleBuffered(true); // Use Swing double screen buffer
        prologOutput.getAccessibleContext().setAccessibleName("Prolog Console Output");
		JScrollPane piscroller = new JScrollPane();
		prologInput = new JTextArea(4,80); prologInput.setFont(prologFont); prologInput.setLineWrap(true);
		prologInput.setToolTipText("Prolog input, sent when you press enter. Drag and drop .P files here to reconsult them");
        prologInput.getAccessibleContext().setAccessibleName("Prolog Input");
		piscroller.getViewport().add(prologInput); 

		JScrollPane scroller = new JScrollPane();
		scroller.getViewport().add(prologOutput);
		JSplitPane j = new JSplitPane (JSplitPane.VERTICAL_SPLIT, scroller, prologInput);
		c.add("Center",j);
		setSize(600,600);
		j.setDividerLocation(500);
		//j.resetToPreferredSizes();
		validate();
		
		DropTargetListener dropHandler = new DropTargetListener(){
			public void dragOver(DropTargetDragEvent dtde){}
			public void dropActionChanged(DropTargetDragEvent dtde){}
			public void dragExit(DropTargetEvent dte){}
			public void drop(DropTargetDropEvent dtde){
				handlePrologInputDnD(dtde);
			}
			public void dragEnter(DropTargetDragEvent dtde){
				// System.out.println("dragEnter:"+dtde);
			}
		};
		new DropTarget(prologInput,dropHandler);
	}
	
	void handlePrologInputDnD(DropTargetDropEvent dtde){
		//System.out.println("drop:"+dtde);
		try{
			Transferable transferable = dtde.getTransferable();
			/*
			DataFlavor[] flavors = transferable.getTransferDataFlavors();
			for (int f=0;f<flavors.length;f++)
				System.out.println("Flavor:"+flavors[f]);*/
			int action = dtde.getDropAction();
			if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)){	
				if (engine.isIdle()){
					dtde.acceptDrop(action);
					final java.util.List files = (java.util.List)transferable.getTransferData(DataFlavor.javaFileListFlavor);
					dtde.getDropTargetContext().dropComplete(true);
					boolean allPs = true;
					for (int f=0;f<files.size();f++){
						String filename = ((File)files.get(f)).getName();
						int dot = filename.lastIndexOf('.');
						if (!filename.endsWith(".P")) {
							allPs=false; break;
						}
					}
					if(!allPs) 
						errorMessage("All dragged files must be Prolog source files (with a .P extension)");
					else {
						prologOutput.append("\nReconsulting "+((files.size()>1 ? files.size()+" files...\n" : files.size()+" file...\n")));
						Runnable r = new Runnable(){
							public void run(){
								boolean crashed = false;
								Toolkit.getDefaultToolkit().sync();
								for (int f=0;f<files.size() && !crashed;f++){
									File file = (File)files.get(f);
									if (!processDraggedFile(file)) crashed = true;
								}
								if (crashed) prologOutput.append("...terminated with errors.\n");
								else prologOutput.append("...done.\n");
							}
						};
						SwingUtilities.invokeLater(r);
					}
				} else {
					dtde.rejectDrop();
					errorMessage("You can not consult files while Prolog is working");
				}
			} else dtde.rejectDrop();
		} catch (Exception e){
			throw new IPException("Problem dropping:"+e);
		}
	}
	
	public boolean processDraggedFile(File f){
		if (engine.consultAbsolute(f)) {
			addToReloaders(f,"consult");
			return true;
		} else {
			errorMessage("Problems reconsulting "+f.getName());
			return false;
		}
	}
	
	public void errorMessage(String m){
		beep();
		JOptionPane.showMessageDialog(this,m,"Error",JOptionPane.ERROR_MESSAGE);
	}
	
	void constructMenu(){
		JMenuBar mb; 
		mb = new JMenuBar(); setJMenuBar(mb);
		
		fileMenu = new JMenu("File"); 
                fileMenu.setMnemonic('F');
                mb.add(fileMenu);
		
		addItemToMenu(fileMenu,"Consult...",'C', new ActionListener(){
			public void actionPerformed(ActionEvent e){
				reconsultFile();
			}
		});
		
		if (engine.getImplementationPeer() instanceof XSBPeer)
			addItemToMenu(fileMenu,"Load dynamically...",'L',new ActionListener(){
				public void actionPerformed(ActionEvent e){
					load_dynFile();
				}
			});
				
		fileMenu.addSeparator();
		
		JMenu toolMenu = new JMenu("Tools"); 
                toolMenu.setMnemonic('T');
                mb.add(toolMenu);
		
		final JCheckBoxMenuItem debugging = new JCheckBoxMenuItem("Engine debugging");
		toolMenu.add(debugging);
		debugging.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				engine.setDebug(debugging.isSelected());
			}
		});

		addItemToMenu(toolMenu,"See Object Specifications",'S',new ActionListener(){
			public void actionPerformed(ActionEvent e){
				engine.command("showObjectVariables");
			}
		});
		
				
		addItemToMenu(toolMenu,"Interrupt Prolog",'I',new ActionListener(){
			public void actionPerformed(ActionEvent e){
				engine.interrupt();
			}
		});
		/*
		addItemToMenu(toolMenu,"Serialize JFrame",new ActionListener(){
			public void actionPerformed(ActionEvent e){
				Object [] toSerialize = {new JFrame("My window")};
				engine.setDebug(true);
				System.out.println(engine.deterministicGoal("true","[Object]",toSerialize));
			}
		});*/
		
		historyMenu = new JMenu("History",true); 
                historyMenu.setMnemonic('H');
                mb.add(historyMenu); 
		historyMenu.addSeparator(); // to avoid Swing bug handling key events
	}
	
	class HistoryListener implements ActionListener{
		JTextComponent targetText;
		String memory;
		HistoryListener(JTextComponent t,String s){
			targetText=t; memory=s;
		}
		public void actionPerformed(ActionEvent e){
			targetText.replaceSelection(memory);
		}
	}
	
	static void addItemToMenu(JMenu menu,String item,ActionListener handler) {
		JMenuItem menuItem = new JMenuItem(item);
		menu.add(menuItem);
		menuItem.addActionListener(handler);
	}
	
    static void addItemToMenu(JMenu menu, String item, char mnemonics, ActionListener handler) {
		JMenuItem menuItem = new JMenuItem(item);
                menuItem.setMnemonic(mnemonics);
		menu.add(menuItem);
		menuItem.addActionListener(handler);
	}
	
	public abstract void sendToProlog();
	
	protected void addToHistory(){
		JMenuItem item;
		String goal = prologInput.getText();
		if (goal.equals(";")) return; // not worthy remembering
		if (goal.length()>20) historyMenu.add(item = new JMenuItem(goal.substring(0,19)+"..."));
		else historyMenu.add(item = new JMenuItem(goal.substring(0,goal.length())));
		item.addActionListener(new HistoryListener(prologInput,goal));
	}
	
	static class LoadedFile{
		File file; String method;
		LoadedFile(File file,String method){
			this.file=file; this.method=method;
			if (!(method.equals("consult") || method.equals("load_dyn")))
			throw new IPException("bad load method");
		}
		public boolean equals(LoadedFile o){
			return file.equals(o.file) && method.equals(o.method);
		}
	}
	
	void addToReloaders(File file,String method){
		final LoadedFile lf = new LoadedFile(file,method);
		if (!loadedFiles.contains(lf)){
			loadedFiles.addElement(lf);
			addItemToMenu(fileMenu,file.getName(),new ActionListener(){
				public void actionPerformed(ActionEvent e){
					engine.command(lf.method+"('"+engine.unescapedFilePath(lf.file.getAbsolutePath())+ "')");
				}
			});
		}
	}
	
	public boolean successfulCommand(String s){
		try {
			return engine.command(s);
		}
		catch(Exception e){}
		return false;
	}
	
	void reconsultFile(){
		String nome,directorio; File filetoreconsult=null;
		FileDialog d = new FileDialog(this,"Consult file...");
		d.setVisible(true);
		nome = d.getFile(); directorio = d.getDirectory();
		if (nome!=null) {
			filetoreconsult = new File(directorio,nome);
			if (engine.consultAbsolute(filetoreconsult))
				addToReloaders(filetoreconsult,"consult");
		}
	}

	/** For XSB only */
	void load_dynFile(){
		String nome,directorio; File filetoreconsult=null;
		FileDialog d = new FileDialog(this,"load_dyn file...");
		d.show();
		nome = d.getFile(); directorio = d.getDirectory();
		if (nome!=null) {
			filetoreconsult = new File(directorio,nome);
			if (successfulCommand("load_dyn('"+engine.unescapedFilePath(filetoreconsult.getAbsolutePath())+ "')"))
				addToReloaders(filetoreconsult,"load_dyn");
		}
	}

	public void focusInput(){
		prologInput.selectAll();
		prologInput.requestFocus();
	}
		
	public void scrollToBottom(){
		if (prologOutput.isShowing()) {
			prologOutput.setCaretPosition(prologOutput.getDocument().getEndPosition().getOffset()-1 /* OBOB hack */);
			try {
				// If we're in a JScrollPane, force scrolling to bottom and left
				JScrollBar scrollbarV = ((JScrollPane)((JViewport)(prologOutput.getParent())).getParent()).getVerticalScrollBar();
				scrollbarV.setValue(scrollbarV.getMaximum());
				JScrollBar scrollbarH = ((JScrollPane)((JViewport)(prologOutput.getParent())).getParent()).getHorizontalScrollBar();
				scrollbarH.setValue(scrollbarH.getMinimum());
			} catch (Exception e) {/* We're not in a JScrollPane, forget it! */};
		}
	}
	
	public static boolean debug = false;
	public static String prologStartCommand=null;
	public static boolean loadFromJar = true;

	public static void commonMain(String args[]) {
		commonGreeting();
		if (args.length>=1){
			int i=0;
			while(i<args.length){
				if (args[i].toLowerCase().startsWith("-d")) {
					debug=true;
					i++;
				} else if (args[i].toLowerCase().startsWith("-nojar")){
					loadFromJar=false;
					i++;
				} else {
					prologStartCommand = remainingArgs(args,i);
					break;
				}
			}
		} else throw new IPException("Missing arguments in command line");

	}
	
	public static void commonGreeting(){
		System.out.println("Welcome "+System.getProperty("user.name")+" to InterProlog "+AbstractPrologEngine.version+" on Java "+
			System.getProperty("java.version") + " ("+
			System.getProperty("java.vendor") + "), "+ 
			System.getProperty("os.name") + " "+
			System.getProperty("os.version"));
	}
	
	public static String commandArgs(String[] args){
		return remainingArgs(args,0);
	}
	public static String remainingArgs(String[] args,int first){
		if (args.length==0) throw new IPException("Missing arguments in command line");
		StringBuffer temp = new StringBuffer();
		for (int i=first;i<args.length;i++){
			if (i>first) temp.append(" ");
			temp.append(args[i]);
		}
		return temp.toString();
	}
	
	public static void beep(){
		Toolkit.getDefaultToolkit().beep();
	}
}