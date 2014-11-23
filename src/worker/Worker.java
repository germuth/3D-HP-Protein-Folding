package worker;

/**
 * Worker.java
 * 
 * Worker class that connects to Coordinator and receives a job
 * to fold.  This is the class that does the actual protein folding.
 * 
 * @author Lee Foster
 *
 */

public class Worker {
	private static final int PORT_NUM = 7777;
	private static final int MAX_ATTEMPTS = 5;

	//Finite State Machine that serves the client
	public static void serveClient(ClientSocket client){
		State currentState = State.START;
		String received = "";
		String[] error_msg = new String[1];
		int progress = 0;
		int attempts_remaining = MAX_ATTEMPTS;
		Population pop = null;
		
		//Finite State Machine that controls what the worker is doing
		while(currentState != State.DONE && attempts_remaining > 0){
			switch(currentState){
			  case START: 
				  client.sendMessage("3DHP Worker");
				  currentState = State.RECEIVE_CLIENT_INFO;
				  break;
			  case RECEIVE_CLIENT_INFO: 
				  received = client.receiveMessage();
				  if(received.equals("bye.")) 
					  currentState = State.DONE;
				  else 
					  currentState = State.RECEIVE_JOB;
				  break;
			  case RECEIVE_JOB: 
				  received = client.receiveMessage();
				  if(received.equals("bye.")) 
					  currentState = State.DONE;
				  else if(received.equals("fold")){
					  attempts_remaining = MAX_ATTEMPTS;
					  currentState = State.SEND_PARAM_REQUEST;
				  }
				  else{
					  attempts_remaining--;
					  client.sendError(received);
				  }
				  break;
			  case SEND_PARAM_REQUEST: 
				  client.sendMessage("give parameters");
				  currentState = State.RECEIVE_PARAMS;
				  break;
			  case RECEIVE_PARAMS: 
				  received = client.receiveMessage();
				  if(received.equals("bye")) 
					  currentState = State.DONE;
				  else 
					  currentState = State.PARSE_PARAMS;
				  break;
				 
				//Parses the parameters from the GUI to the global variables used throughout the genetic algorithm  
			  case PARSE_PARAMS: 
				  System.out.println(received);
				  if(ParseParams.parse(received, error_msg)){
					  attempts_remaining = MAX_ATTEMPTS;
					  client.sendMessage("ok");
					  currentState = State.FOLD_JOB;
				  }
				  else{
					  attempts_remaining--;
					  client.sendMessage(error_msg[0]);
					  currentState = State.RECEIVE_PARAMS;
				  }
				  break;
			  case FOLD_JOB:
				  //TODO Make Population class so this works
				  //TODO Make GA class with predict in it				  
				  pop = GeneticAlgorithm.predict(client);
				  System.out.println("Worker: The Size of the populations is: " +pop.getProteinListSize());
				  //pop.printList();
				  if(pop == null)
					  currentState = State.RECEIVE_JOB;
				  else{
					  client.sendMessage("done");
					  currentState = State.RECEIVE_RESULT_REQUEST;
				  }
				  break;
			  case RECEIVE_RESULT_REQUEST: 
				  received = client.receiveMessage();
				  if(received.equals("get next result") || received.equals("get previous result")){
					  attempts_remaining = MAX_ATTEMPTS;
					  currentState = State.SEND_RESULT;
				  }
				  else if(received.equals("fold")){
					  attempts_remaining = MAX_ATTEMPTS;
					  currentState = State.SEND_PARAM_REQUEST;
				  }
				  else if(received.equals("bye")){
					  currentState = State.DONE;
				  }
				  else{
					  attempts_remaining--;
				  }
				  break;
			  case SEND_RESULT:
				  Protein protein = pop.getNext();
				  client.sendProtein(protein);
				  currentState = State.RECEIVE_RESULT_REQUEST;
				  break;
			}
		}
		if(attempts_remaining < 0){
			client.sendMessage("error: too many failed attempts");
		}
		client.sendMessage("bye");
	}
	
	public static void main(String[] args){
		String host = "";
		ClientSocket s = new ClientSocket();
		
		//sets the host to local if there is nothing in args
		if(args.length == 0)
			host = "localhost";
		else{
			host = args[1];
		}
		
		//tries to connect exits if it cannot
		if(!s.connectTo(host,PORT_NUM)){
			System.out.println("Could not connect");
			System.exit(0);
		}
		serveClient(s);
		s.disconnect();
		System.exit(0);
	}
}
