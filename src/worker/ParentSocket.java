package worker;

/**
 * ParentSocket.java
 * 
 * General Socket class that deals with sending and receiving messages and sending a protein.
 * 
 * @author Lee Foster
 * 
 */
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;


public class ParentSocket {
	private final int MAXDATASIZE = 100;
	protected boolean connected;
	public Socket socket;
	protected BufferedReader in;
	protected PrintWriter out;
	
	//sends a message to the server
	public boolean sendMessage(String msg){
		if(!connected){
			System.out.println("Not Connected");
			return false;
		}
		//System.out.println("Sending message " + msg);
		try{
			out.println(msg);
		}
		catch(Exception e){
			System.out.println("Sending Message Error");
			connected = false;
			return false;
		}
		return true;
	}
	
	public boolean sendPolicy(){
		return sendMessage("<cross-domain-policy><allow-access-from domain=\"*\" to-ports=\"*\" /></cross-domain-policy>\0");
	}
	
	public String receiveMessage(){
		String msg = "";
		try{
			msg = in.readLine();
			//System.out.println("recieved: " + msg);
			return msg;
		}
		catch(IOException e){
			System.out.println("Error Recieving");
			connected = false;
			return "";
		}
		
	}
	
	public boolean sendProtein(Protein protein){
		ArrayList<SSInfo> ss_info_list = protein.getSsList();
		String str = "";
		str += "<result>";
		//TODO find getpid()
		//str += "<worker>" + getpid() + "</worker>";
		str += "<worker>" + 12345 + "</worker>";
		str += "<directions>" + protein.getDirection() + "</directions>";
		str += "<health>" + protein.getHealth() + "</health>";
		for(int i = 0; i<ss_info_list.size(); i++){
			if(ss_info_list.get(i).isOn()){
				str += "<ss>";
				str +="<length>" + ss_info_list.get(i).getLength() + "</length>";
				str +="<position>" + ss_info_list.get(i).getPosition() + "</position>";
				str += "</ss>";
			}
		}
		str += "<comments>0</comments>";
		str += "<rating>0</rating>";
		str += "</result>";
		return sendMessage(str);
	}
	
	//Sends an error back to the server
	public boolean sendError(String received_msg){
		String msg = "error: I don't understand your message: ";
		msg.concat(received_msg);
		return sendMessage(msg);
	}
	
	public boolean isConnected(){
		return connected;
	}
}
