package RBNpackage;

import RBNutilities.rbnutilities;

public class CConstrPlus extends CConstr {

	

   CConstr C1;
   CConstr C2;
   
   /** Creates new CConstrAnd */
   public CConstrPlus(CConstr c1,CConstr c2) 
   {
       SSymbs = rbnutilities.arraymerge(c1.SSymbs,c2.SSymbs);
       C1 = c1;
       C2 = c2;
   }
   
   public String[] freevars()
   {
       return rbnutilities.arraymerge(C1.freevars(),C2.freevars());
       
   }
   
   public CConstr substitute(String[] vars, int[] args)
   {
       return new CConstrAnd(C1.substitute(vars,args),C2.substitute(vars,args));
   }
   
    public CConstr substitute(String[] vars, String[] args)
   {
       return new CConstrAnd(C1.substitute(vars,args),C2.substitute(vars,args));
   }
   

   public String asString()
   {
       String result;
       result = "(" + C1.asString() + " + " + C2.asString() + ")";
       return result;
   }
   
   public String asString(RelStruc A)
   {
       String result;
       result = "(" + C1.asString(A) + " + " + C2.asString(A) + ")";
       return result;
   }
	


}
