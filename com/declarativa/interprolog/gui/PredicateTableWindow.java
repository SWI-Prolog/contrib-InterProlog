/* 
** Author(s): Miguel Calejo
** Contact:   interprolog@declarativa.com, http://www.declarativa.com
** Copyright (C) Declarativa, Portugal, 2000-2002
** Use and distribution, without any warranties, under the terms of the 
** GNU Library General Public License, readable in http://www.fsf.org/copyleft/lgpl.html
*/
package com.declarativa.interprolog.gui;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;
import java.awt.*;

public class PredicateTableWindow extends JFrame{
	public PredicateTableWindow(PredicateTableModel ptm){
		super(ptm.toString());
		JTable table = new JTable(ptm);
		table.setColumnSelectionAllowed(true); // prettier...
		JScrollPane scroller = new JScrollPane(table); 
		Dimension screenSize = getToolkit().getScreenSize();
		int x = screenSize.width; int y = screenSize.height;
		Dimension ps = table.getPreferredScrollableViewportSize();
		if (ps.width<x) x=ps.width;
		if (ps.height<y) y=ps.height;
		getContentPane().add("Center",scroller);
		setSize(x,y);
		setVisible(true);
		//System.out.println("Created window for PredicateTableModel "+ptm);
	}
}
