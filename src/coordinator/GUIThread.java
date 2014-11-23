package coordinator;

/**
 * GUIThread.java
 * 
 * Created by Coordinator when the GUI connects. Uses a finite state machine
 * to talk back and forth with the GUI
 * 
 * @author Lee Foster
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;

public class GUIThread extends Thread{
	private Socket GUI;
	private BufferedReader in;
	private PrintWriter out;
	
	public GUIThread(Socket s, BufferedReader i, PrintWriter o){
		GUI = s;
		in = i;
		out = o;
	}
	
	public void run(){
		CoordinatorStates currentState = CoordinatorStates.RECEIVE_JOB;
		int result_index = 0;
		int preview_interval = 5;
		long lastUpdate = System.currentTimeMillis()/1000;
		long currentSeconds;
		int attempts_remaining = Coordinator.getMaxattempts();
		int job = 0; 	
		String received = "";
		String[] error_msg = new String[1];
		ArrayList<String> results = new ArrayList<String>();
		
		out.println("3DHP Coordinator");
		try{
			while(currentState != CoordinatorStates.DONE && attempts_remaining > 0){
				//Client is dead
				if(!GUI.isConnected()){
					Coordinator.semMainMem.acquire();
					Coordinator.main_shm.num_jobs--;
					Coordinator.main_shm.num_clients--;
					Coordinator.semMainMem.release();
					if(job == 1){
						Coordinator.semJobShm.acquire();
						Coordinator.job_shm.cancelled = true;
						Coordinator.semJobShm.release();
					}
					break;
				}

				switch(currentState){
				case RECEIVE_JOB: received = in.readLine();
								  if(received.equals("bye") || received.equals(""))
								    currentState = CoordinatorStates.DONE;
								  else if(received.equals("fold")){
									  attempts_remaining = Coordinator.getMaxattempts();
									  currentState = CoordinatorStates.SEND_PARAM_REQUEST;
								  }
								  else{
									  attempts_remaining--;
									  System.out.println("error trying again");
								  }
								  break;
				case SEND_PARAM_REQUEST: out.println("give parameters");
										 currentState = CoordinatorStates.RECEIVE_PARAMS;
										 break;
				case RECEIVE_PARAMS: received = in.readLine();
									 if(received.equals("bye") || received.equals(""))
										 currentState = CoordinatorStates.DONE;
									 else
										 currentState = CoordinatorStates.PARSE_PARAMS;
									 break;
				case PARSE_PARAMS: if(ParseParams.parse(received, error_msg)){
									
									
									//Set up job shared memory
									Coordinator.semJobShm.acquire();
									Coordinator.job_shm.params = received.toCharArray();
									Coordinator.job_shm.results_size = 0;
									Coordinator.job_shm.num_workers = 0;
									Coordinator.job_shm.num_pops = ParametersFromGUI.getNumPopulations();
									Coordinator.job_shm.num_pops_remaining = ParametersFromGUI.getNumPopulations();
									Coordinator.job_shm.pop_size = ParametersFromGUI.getPopulationSize();
									Coordinator.job_shm.num_results_remaining = ParametersFromGUI.getPopulationSize();
									Coordinator.semJobShm.release();
									
									//Increase the number of current jobs
									//TODO check to see if i need the getpid() from c++ here
									Coordinator.semMainMem.acquire();
									Coordinator.main_shm.num_jobs++;
									Coordinator.semMainMem.release();
									
									out.println("ok");
								   	attempts_remaining = Coordinator.getMaxattempts();
								   	currentState = CoordinatorStates.FOLD_JOB;
								   }
								   else{
									   attempts_remaining--;
									   out.println(error_msg[0]);
									   currentState = CoordinatorStates.RECEIVE_PARAMS;
								   }
								   break;
				case FOLD_JOB:	currentSeconds = System.currentTimeMillis()/1000;
								if(Coordinator.job_shm.preview.length >= 1 && currentSeconds - lastUpdate >= preview_interval){
									Coordinator.semJobShm.acquire();
									error_msg[0] = new String(Coordinator.job_shm.preview);
									Coordinator.semJobShm.release();
									System.out.println("GUIThreadSending Preview: " + error_msg[0]);
									out.println(error_msg[0]);
									lastUpdate = currentSeconds;
								}
								
								
								if(Coordinator.job_shm.status_size > 0){
									//Read Status from job shared memory
									Coordinator.semJobShm.acquire();
									error_msg[0] = new String(Coordinator.job_shm.status);
									Coordinator.job_shm.status_size = 0;
									Coordinator.semJobShm.release();
									out.println(error_msg[0]);
								}
								if(Coordinator.job_shm.results_size > 0){
									Coordinator.semJobShm.acquire();
									received = new String(Coordinator.job_shm.results);
									Coordinator.job_shm.results_size = 0;
									Coordinator.semJobShm.release();
									//TODO Could break here in c++ it splits it on \n
									//TODO toString is not giving the string of characters 
									results.add(new String(Coordinator.job_shm.results));
								}
								if(Coordinator.job_shm.num_workers < 1 && Coordinator.job_shm.num_pops_remaining < 1){
									out.println("done");
									System.out.println("Coordinator Results Size: " + results.size());
									result_index = -1;
									attempts_remaining = Coordinator.getMaxattempts();
									currentState = CoordinatorStates.RECEIVE_RESULT_REQUEST;
								}
								break;
				case RECEIVE_RESULT_REQUEST: received = in.readLine();
											if(received.equals("bye") || received.equals("")){
												currentState = CoordinatorStates.DONE;
											}
											else if(received.equals("get next result")){
												currentState = CoordinatorStates.SEND_NEXT_RESULT;
											}
											else if(received.equals("get previous result")){
												currentState = CoordinatorStates.SEND_PREVIOUS_RESULT;
											}
											else if(received.equals("fold")){
												attempts_remaining = Coordinator.getMaxattempts();
												currentState = CoordinatorStates.SEND_PARAM_REQUEST;
											}
											else{
												attempts_remaining--;
											}
											break;
				case SEND_NEXT_RESULT: 	result_index++;
										if(result_index > results.size()-1)
											result_index = 0;
										out.println(results.get(result_index));
										attempts_remaining = Coordinator.getMaxattempts();
										currentState = CoordinatorStates.RECEIVE_RESULT_REQUEST;
										break;
				case SEND_PREVIOUS_RESULT: result_index--;
											if(result_index < 0)
												result_index = results.size() -1;
											out.println(results.get(result_index));
											attempts_remaining = Coordinator.getMaxattempts();
											currentState = CoordinatorStates.RECEIVE_RESULT_REQUEST;
											break;
				}
				
			}
		}
		catch(IOException e){
			System.out.println("Error Talking With Worker");
		}
		catch(InterruptedException e){
			System.out.println("Semaphore Problem");
		}
	}
}

