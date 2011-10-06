/* 
** Author(s): Miguel Calejo
** Contact:   interprolog@declarativa.com, http://www.declarativa.com
** Copyright (C) Declarativa, Portugal, 2000-2005
** Use and distribution, without any warranties, under the terms of the 
** GNU Library General Public License, readable in http://www.fsf.org/copyleft/lgpl.html
*/
package com.declarativa.interprolog.examples;
import com.declarativa.interprolog.*;
import com.declarativa.interprolog.util.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
/**
From Prolog in a InterProlog listener window:
1) assert( (greetat(TextID) :- javaMessage( TextID, setText(string('Hello world!')) )) ).
2) call this to create the window:
ipPrologEngine(Engine), javaMessage('com.declarativa.interprolog.examples.HelloWindow','HelloWindow'(Engine)).
*/
public class HelloWindow extends JFrame{
	PrologEngine myEngine;
	public HelloWindow(PrologEngine pe){
		super("Java-Prolog-Java call example");
		myEngine = pe;
		JTextField text = new JTextField(15);
		final Object fieldObject = myEngine.makeInvisible(text);
		text.setBorder(BorderFactory.createTitledBorder("text"));
		JButton button = new JButton("Greet");
		Box box = new Box(BoxLayout.Y_AXIS);
		box.add(text); box.add(button);
		getContentPane().add(box);
		setSize(200,100); setVisible(true);
		
		button.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent e){
					myEngine.deterministicGoal("greetat(Obj)","[Obj]",new Object[]{fieldObject});
				}
			});
	}
}
