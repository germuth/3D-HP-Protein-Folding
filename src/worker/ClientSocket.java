package worker;

/** 
 * ClientSocket.java
 * 
 * Socket used by worker to communicate with the coordinator
 * 
 * @author Lee Foster
 */
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientSocket extends ParentSocket{
		
	public ClientSocket(){
		connected = false;
	}
	
	/** Connects to the server specified by host and port
	 * 
	 * @param host Name of the host
	 * @param port Port Number
	 * @return true if connected, false if it could not connect
	 */
	public boolean connectTo(String host, int port){
		
		try{
		 socket = new Socket(host, port);
		 out = new PrintWriter(socket.getOutputStream(), true);
         in = new BufferedReader(new InputStreamReader(
                                     socket.getInputStream()));
     } catch (Exception e) {
         System.out.println("client: failed to connect");
         return false;
     }	
		connected = true;
		return true;
	}
	
	/** Disconnects from the server closing the BufferedReader and PrintWriter
	 * 
	 */
	public void disconnect(){
		try{
			out.close();
			in.close();
			socket.close();
		}
		catch(IOException e){
			System.out.println("Error Disconnecting");
		}
	}
}
