package coordinator;

/**
 * WorkerThread.java
 * 
 * Made by coordinator to serve a worker that connects to it
 * 
 * @author Lee Foster
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;

public class WorkerThread extends Thread{
	private Socket worker;
	private BufferedReader in;
	private PrintWriter out;
	
	public WorkerThread(Socket s, BufferedReader i, PrintWriter o){
		worker = s;
		in = i;
		out = o;
	}
	
	public void run(){
		int job = 0;
		int results_stored = 0;
		CoordinatorStates current_state = CoordinatorStates.WAIT_FOR_JOB;
		String received = "";
		//Array used so we can pass by pointer
		String[] error_msg = new String[1];
		int attempts_remaining = Coordinator.getMaxattempts();
		
		//Identify itself to Coordinator
		out.println("3DHP Coordinator");
		
		try{
			while(current_state != CoordinatorStates.DONE && attempts_remaining > 0){
			  switch(current_state){
				case WAIT_FOR_JOB:
					this.sleep(1);
					Coordinator.semMainMem.acquire();
					//Will loop through until GUIThread changes the number of jobs
					if(Coordinator.main_shm.num_jobs > 0){
						out.println("fold");
						received = in.readLine();
						//Worker accepted job
						if(received.equals("give parameters")){
							//increment the number of workers on the job
							job = Coordinator.main_shm.job_list[0];
							Coordinator.semJobShm.acquire();
							Coordinator.job_shm.num_workers++;
							Coordinator.semJobShm.release();
							if(Coordinator.job_shm.num_workers >= Coordinator.job_shm.num_pops_remaining){
								//Job has enough workers: remove job from list
								Coordinator.main_shm.num_jobs--;
								for(int i = 0; i <Coordinator.main_shm.num_jobs; i++){
									Coordinator.main_shm.job_list[i] = Coordinator.main_shm.job_list[i+1];
								}
							}
							current_state = CoordinatorStates.SEND_PARAMETERS;
						}
						//Worker is dead
						else if(received.length() <= 0){
							System.exit(0);
						}
						//worker replies inappropriately
						else{
							attempts_remaining--;
						}
					}
					Coordinator.semMainMem.release();
					break;
			case SEND_PARAMETERS: 
								  out.println(Coordinator.job_shm.params);
								  attempts_remaining = Coordinator.getMaxattempts();
								  current_state = CoordinatorStates.RECEIVE_PARAMETERS_ACCEPT;
								  break;
			case RECEIVE_PARAMETERS_ACCEPT: received = in.readLine();
											if(received.equals("bye") || received.equals("")){
												//Worker is dead decrement the number of workers on the job
												Coordinator.semJobShm.acquire();
												Coordinator.job_shm.num_workers--;
												Coordinator.semJobShm.release();
												//TODO see if we need the add job to front thing
												System.exit(0);												
											}
											else if(received.equals("ok")){
												//Worker accepted parameters
												current_state = CoordinatorStates.RECEIVE_STATUS;
											}
											else{
												attempts_remaining--;
											}
											break;
			case RECEIVE_STATUS: received = in.readLine();
								 System.out.println("Worker Receive_Status: " + received);
								if(received.equals("bye") || received.equals("")){
									//Worker is dead decrement the number of workers on the job
									Coordinator.semJobShm.acquire();
									Coordinator.job_shm.num_workers--;
									Coordinator.semJobShm.release();
									System.exit(0);												
								}
								else if(received.equals("done")){
									results_stored= 0;
									current_state = CoordinatorStates.GET_RESULT;
								}
								else if(received.startsWith("<result>")){
									//Worker replies with preview
									Coordinator.semJobShm.acquire();
									Coordinator.job_shm.preview = received.toCharArray();
									Coordinator.semJobShm.release();
								}
								//Here worker replies with a status which will update the progress bar in the GUI
								else{
									//TODO Major Change from C++ here to get preview to work
									//If there is any array out of bounds problems check here
									Coordinator.semJobShm.acquire();									
									Coordinator.job_shm.status = received.toCharArray();
									Coordinator.job_shm.status_size = received.length();
									if(Coordinator.job_shm.cancelled){
										out.println("abort");
										current_state = CoordinatorStates.WAIT_FOR_JOB;
										}
										else{
											out.println("ok");
										}
									Coordinator.semJobShm.release();
								}
								break;
			case GET_RESULT: boolean success = false;
							 out.println("get next result");
							 received = in.readLine();
							 System.out.println("Worker Get Result: " + received);
							 if(received.equals("bye") || received.equals("")){
									//Worker is dead decrement the number of workers on the job
									Coordinator.semJobShm.acquire();
									Coordinator.job_shm.num_workers--;
									Coordinator.semJobShm.release();
									System.exit(0);												
								}
							 //Worker replies with result
							 else{
									//TODO Major Change from C++ here to get preview to work
									//If there is any array out of bounds problems check here
								 Coordinator.semJobShm.acquire();
								 //System.out.println("workerthread num_results_remaining: " + Coordinator.job_shm.num_results_remaining);								
								 Coordinator.job_shm.results = received.toCharArray();
								 Coordinator.job_shm.results_size += received.length();
								 Coordinator.job_shm.num_results_remaining--;
								 Coordinator.semJobShm.release();
								 results_stored++;
								 //System.out.println("results_stored: " + results_stored);
								 
								 Coordinator.semJobShm.acquire();
								 //If there are no results remaining or this worker has done its share
								 if(Coordinator.job_shm.num_results_remaining < 1|| results_stored >= Coordinator.job_shm.pop_size/Coordinator.job_shm.num_pops){
									 Coordinator.job_shm.num_workers--;
									 Coordinator.job_shm.num_pops_remaining--;
									 attempts_remaining = Coordinator.getMaxattempts();
									 current_state = CoordinatorStates.WAIT_FOR_JOB;
								 }
								 Coordinator.semJobShm.release();
							 }
							 break;
				}
			  if(attempts_remaining <= 0){
				  out.println("error: too many failed attempts");
			  }
			}
		}
		catch(IOException e){
			System.out.println("Reading from line problem in worker");
		}
		catch(InterruptedException e){
			System.out.println("Semaphore Problem in Worker");
		}
	}
}
