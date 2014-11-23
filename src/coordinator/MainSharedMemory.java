package coordinator;

/**
 * MainSharedMemory.java
 * 
 * Shared Data between the GUIThread and WorkerThread.
 * 
 * This data has to do with multiple workers on different jobs 
 * and contains information about the number of workers, number
 * of current jobs etc.
 * 
 * @author Lee Foster
 *
 */

public class MainSharedMemory {

	public int num_clients;
	public int[] client_list;
	public int num_workers;
	public int[] worker_list;
	public int num_jobs;
	public int[] job_list;
	
	
	
	public MainSharedMemory(int MAX_CLIENTS, int MAX_WORKERS, int MAX_JOBS){
		client_list = new int[MAX_CLIENTS];
		worker_list = new int[MAX_WORKERS];
		job_list = new int[MAX_JOBS];
		num_clients = 0;
		num_workers = 0;
		num_jobs = 0;
	}
	
}
