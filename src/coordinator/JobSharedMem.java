package coordinator;

/**
 * JobSharedMem.java
 * 
 * Data shared between GUIThread and WorkerThread about the current job
 * 
 * @author Lee
 *
 */

public class JobSharedMem {
	
	public int num_workers;
	public int num_pops;
	public int num_pops_remaining;
	public char[] params;
	public char[] preview;
	public char[] status;
	public int status_size;
	public char[] results;
	public int results_size;
	public int num_results_remaining;
	public int pop_size;
	public boolean cancelled;
	
	public JobSharedMem(int MAX_PARAMS_SIZE, int MAX_PREVIEW_SIZE, int MAX_STATUS_SIZE,int MAX_RESULTS_SIZE){
		params = new char[MAX_PARAMS_SIZE];
		preview = new char [MAX_PREVIEW_SIZE];
		status = new char[MAX_STATUS_SIZE];
		results = new char[MAX_RESULTS_SIZE];
		
		num_workers = 0;
		num_pops = 0;
		num_pops_remaining = 0;
		results_size = 0;
		num_results_remaining = 0;
		pop_size = 0;
		boolean cancelled = false;
	}
	
}
