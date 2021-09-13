package RBNpackage;

public class CombFuncSum extends CombFunc {

    public CombFuncSum() {
        name = "sum";
    }
    
	public double evaluate(double[] args)
	{
		double sum = 0;
		for (int i=0; i<args.length; i++)
			sum = sum + args[i];
		return sum;
	}

	public  int evaluatesTo(int[] args){
		if (args.length == 0 ) return 0;
		if (args.length ==1 && args[0]==1) return 1;
		else{
			boolean allzeros = true;
			for (int i=0;i<args.length;i++){
				if (args[i]!=0) allzeros = false;
			}
			if (allzeros) return 0;
			return -1;
		}
	}

}
