package RBNLearning;

public class Profiler {
	
	public static final int NUM_EVALUATE_OLD =0;
	public static final int NUM_EVALUATE_NEW =1;
	public static final int TIME_STOCH_GRAD =2;
	public static final int TIME_OLDLOOKUP =3;
	public static final int TIME_GETSATISFYING = 4;
	public static final int TIME_PFEVALUATE = 5;
	public static final int NUM_EPOCHS = 6;
	public static final int TIME_HASHCREATE = 7;
	public static final int TIME_ARRAYCREATE = 8;
	public static final int TIME_COMPGRAD = 9;
	public static final int TIME_XXX = 10;
	public static final int TIME_RESTARTS = 11;
	
	public static final String timers_names[] = 
		{"NUM_EVALUATE_OLD",
			"NUM_EVALUATE_NEW",
			"TIME_STOCH_GRAD",
			"TIME_OLDLOOKUP",
			"TIME_GETSATISFYING",
			"TIME_PFEVALUATE",
			"NUM_EPOCHS",
			"TIME_HASHCREATE",
			"TIME_ARRAYCREATE",
			"TIME_COMPGRAD",
			"TIME_XXX",
			"TIME_RESTARTS"};
	
	
	private int numtimers = timers_names.length;
	
	long[] timers_vals;
	
	public Profiler() {
		timers_vals = new long[numtimers];
	}
	
	public void initTimers() {
		timers_vals = new long[numtimers];
	}
	
	public void addTime(int i, double t) {
		timers_vals[i]+= t;
	}
	
	public long getTime(int i) {
		return timers_vals[i];
	}
	
	public String showTimers() {
		String result = "";
		for (int i=0;i<numtimers;i++) {
			result += "#  " + timers_names[i] + " " + timers_vals[i] + '\n';
		}
		return result;
	}
}
