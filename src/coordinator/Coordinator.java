package coordinator;

/**
 * Coordinator that communicates between the GUI and the Worker using sockets.
 * Creates a GUIThread to deal with the GUI and a WorkerThread to deal with
 * worker.
 * 
 * The two threads communicate using the JobSharedMem and MainSharedMemory classes
 * which hold different variables that have to do with the job parameters and
 * how far along the job is.  A semaphore is used to protect access to these variables
 * 
 * @author Lee Foster
 */

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Semaphore;

public class Coordinator {

	private static final int MAXATTEMPTS = 5;

	private static final int MAX_CLIENTS = 50;
	private static final int MAX_WORKERS = 50;
	private static final int MAX_JOBS = 50;

	private static final int MAX_PREVIEW_SIZE = 500;
	private static final int MAX_PARAMS_SIZE = 5000;
	private static final int MAX_STATUS_SIZE = 500;
	private static final int MAX_RESULTS_SIZE = 500;
	
	
	public static MainSharedMemory main_shm;
	public static JobSharedMem job_shm;
	public static Semaphore semMainMem;
	public static Semaphore semJobShm;
	
	public Coordinator(){
		//blank
	}
	
	public static void serve(ServerSocket s){
		try{
			while(true){
				System.out.println("Waiting for a new Connection");
				Socket client = s.accept();
				PrintWriter out = new PrintWriter(client.getOutputStream(), true);
	            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
	            String clientType = in.readLine();
	            
	            //Serve Worker
	            if(clientType.equals("3DHP Worker")){
	            	System.out.println("Got a worker");
	            	WorkerThread t = new WorkerThread(client, in, out);
	            	t.start();
	            }
	            //Serves GUI
	            if(clientType.startsWith("3DHP Client")){
	            	System.out.println("Got the gui");
	            	GUIThread t = new GUIThread(client,in,out);
	            	t.start();
	            }
	            
			}
		}
		catch(Exception e){
			
		}
	}
	
	public static int getMaxattempts() {
		return MAXATTEMPTS;
	}

	public static int getMaxClients() {
		return MAX_CLIENTS;
	}

	public static int getMaxWorkers() {
		return MAX_WORKERS;
	}

	public static int getMaxJobs() {
		return MAX_JOBS;
	}

	public static int getMaxPreviewSize() {
		return MAX_PREVIEW_SIZE;
	}

	public static int getMaxParamsSize() {
		return MAX_PARAMS_SIZE;
	}

	public static int getMaxStatusSize() {
		return MAX_STATUS_SIZE;
	}

	public static int getMaxResultsSize() {
		return MAX_RESULTS_SIZE;
	}
	
	public static void main(String[] args){
		int portNum = 7777;
		//Create the semaphore and the MainSharedMemory and JobSharedMemory
		//to be used throughout the jobs
		main_shm = new MainSharedMemory(MAX_CLIENTS, MAX_WORKERS, MAX_JOBS);
		job_shm = new JobSharedMem(MAX_PARAMS_SIZE, MAX_PREVIEW_SIZE, MAX_STATUS_SIZE, MAX_RESULTS_SIZE);
		semMainMem = new Semaphore(1);
		semJobShm = new Semaphore(1);
		
		try{
			//Set up and run the server
			ServerSocket s = new ServerSocket(portNum);
			serve(s);			
		}
		catch(Exception e){
			System.out.println("Problem Listening on port " + portNum);
		}
		
	}
}