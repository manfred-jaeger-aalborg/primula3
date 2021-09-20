/*
 * Atom.java
 * 
 * Copyright (C) 2003 Max-Planck-Institut fuer Informatik,
 *                    Helsinki Institute for Information Technology
 *
 * contact:
 * jaeger@cs.auc.dk   www.cs.auc.dk/~jaeger/Primula.html
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

package RBNpackage;

import myio.StringOps;
import RBNutilities.*;



public class GroundAtom extends java.lang.Object {


    /**
	 * @uml.property  name="rel"
	 * @uml.associationEnd  
	 */
    public Rel rel;
    /**
	 * @uml.property  name="args" multiplicity="(0 -1)" dimension="1"
	 */
    public int[] args;
    //Thrane
    /**
	 * @uml.property  name="owner"
	 */
    private String owner = "";
    //Thrane

    public GroundAtom(){
    }

    /* constructs an atom out of a string representation 
     * with arguments given by their names in A
     * Input string is assumed to be of the form
     * relname(argname1,...,argnamek)
     * 
     * This is the converse of asString(RelStruc)
     * 
     * The last argument specifies the type of the relation 
     */
    public GroundAtom(String atstring, RelStruc A, int type){
    	int leftpar = atstring.indexOf("(");
    	String relname = atstring.substring(0, leftpar);
    	args =  A.getIndexes(StringOps.stringToStringArray(atstring.substring(leftpar)));
    	int arity = args.length;
    	
    	switch (type) {
    	case Rel.BOOLEAN: rel = new BoolRel(relname,arity); break;
    	case Rel.NUMERIC: rel = new NumRel(relname,arity); break;
    	default: rel = new Rel(relname,arity);	
    	} 	
    }
    
    public GroundAtom(Rel r, int[] a){
	rel = r;
	args = a;
	//Thrane
	for(int i =0; i<args.length; i++ ){
	    if(i==0){
		owner = owner + args[i];
	    }
	    else{
		owner = owner+ ", " + args[i];
	    }

	}
    }

    public GroundAtom(Rel r, int[] a, String owner){
  	rel = r;
  	args = a;
  	this.owner = owner;
  	System.out.println("owner "+owner );
    }


    public int[] args(){
	return args;
    }

    /**
	 * @return
	 * @uml.property  name="owner"
	 */
    public String getOwner(){
	return owner;
    }



    //Thrane
    public boolean equals( GroundAtom a ){
	return this.rel.equals( a.rel ) && java.util.Arrays.equals( this.args, a.args ) && this.owner.equals( a.getOwner() );
    }

    /** @author keith cascio
	@since 20061020 */
    public boolean isIdenticalTo( GroundAtom atom ){
	return this.rel.equals( atom.rel ) && java.util.Arrays.equals( this.args, atom.args );
    }

    /** @author keith cascio
	@since 20061020 */
    protected int hashCodeImpl(){
//    	return rel.name.name.hashCode() + rel.arity + java.util.Arrays.hashCode( args );
    	return this.asString().hashCode();
    }

    /** @author keith cascio
	@since 20061020 */
    final public int hashCode(){
    	return (myHashCode == 0) ? (myHashCode = hashCodeImpl()) : myHashCode;
    }

    /**
	 * @author     keith cascio
	 * @since     20061020
	 * @uml.property  name="myHashCode"
	 */
    private int myHashCode = 0;

    public boolean inArgument(int a){
	return rbnutilities.inArray(args,a);
    }


    /** returns the Rel of this atom */
    public Rel rel(){
	return rel;
    }


    /** Returns the name of the relation of this atom
     */
    public String relname(){
	return rel.name.name;
    }

    public String asString(){
	return rel.name.name + "(" + rbnutilities.arrayToString(args) + ")";
    }

    /* with int-arguments substituted by elementnames according
     * to RelStruc A:
     */
    public String asString(RelStruc A){
	String result = rel.name.name + A.namesAt(args);
	return result;
    }
    
    public static String relnameFromString(String atomstring){
    	return atomstring.substring(0,atomstring.indexOf("("));
    }
}
