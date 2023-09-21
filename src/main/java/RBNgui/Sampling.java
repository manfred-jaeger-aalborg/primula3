/*
 * Sampling.java
 * 
 * Copyright (C) 2005 Aalborg University
 *
 * contact:
 * jaeger@cs.aau.dk   http://www.cs.aau.dk/~jaeger/Primula.html
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package RBNgui;

import RBNpackage.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import java.util.*;

class Sampling extends Thread{

	/**
	 * @uml.property  name="running"
	 */
	private boolean running;
//	private int test;
	/**
	 * @uml.property  name="temp"
	 */
	private int temp;
	/**
	 * @uml.property  name="testdata"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	TestData testdata;
	/**
	 * @uml.property  name="pause"
	 */
	private boolean pause;
	/**
	 * @uml.property  name="queryAtomSize"
	 */
	private int queryAtomSize;
	/**
	 * @uml.property  name="test" multiplicity="(0 -1)" dimension="1"
	 */
	int[] test;
	/**
	 * @uml.property  name="tal"
	 */
	int tal = 0;
	public Sampling(Observer evidence, int queryAtomSize){
//		test = 0;
		temp = 0;
		running = true;
		this.queryAtomSize = queryAtomSize;
		testdata = new TestData();
		testdata.addObserver(evidence);
		pause = false;
		test = new int[queryAtomSize];

	}

	public void run(){
		while(running){
			try{
				while(pause){
					Thread.sleep((int)(Math.random()*3));	
				}				
				Thread.sleep((int)(Math.random()*3));				
			}
			catch(InterruptedException e){
				System.err.println(e.toString());
			}

			for(int i=0; i<queryAtomSize; i++){
//				System.out.println("HER");
				test[i] = tal++;
			}
			temp++;

			testdata.setData(test);
//			System.out.println("test "+test);	
			if(temp == 50 || running == false){
				temp = 0;
				testdata.notifyObservers();
			}

		}
	}

	/**
	 * @param running
	 * @uml.property  name="running"
	 */
	public void setRunning(boolean running){
		this.running = running;
	}

	/**
	 * @param pause
	 * @uml.property  name="pause"
	 */
	public void setPause(boolean pause){
		this.pause = pause;
	}

}
