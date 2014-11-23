/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;


/**
 *
 * @author hartsho
 */
public class Communicate {
    
    public Communicate(String server_name, Gui ui){
        this.ui = ui;
        this.server_name = server_name;
        max_wait = 10000;
        failed = false;
        try{  
        	server = new Socket(server_name, 7777);
            //server = new Socket(InetAddress.getByName(server_name),7777);
            //recieved = server.getInputStream();
            out = new PrintWriter(server.getOutputStream(), true);
            recieved = new BufferedReader(new InputStreamReader(server.getInputStream())); 
            sent = server.getOutputStream();
            message = new OutputStreamWriter(sent);
            server.setSoTimeout(max_wait);
        }
        catch(Exception e){
        	System.out.println(e.getMessage());
            failed = true;
        } 
    }
   
    public void fold(String user_data, Info info, Gui ui){
        try{ server.setSoTimeout(120000);}
        catch(Exception e){}
        Message fold_message = new Message(info, ui);
        
        if(!ui.usedFile()){
            fold_message.fileNotUsed();
        }
        if(ui.hasAminoAcids() && ui.getAminoAcidSeqLength() == ui.getUserChargesLength()){
            fold_message.enableCreatePDB();
        }
        Progress prog_bar = new Progress();
        prog_bar.setVisible(true);
        prog_bar.updateMessage("Waiting for Server");
        failed = false;
        String data = new String();
        String task = new String();
        String amount = new String();
        String worker= new String();
        fold_message.appendMessage("Requesting server to fold protein\n");
        try{
            //introductions
            data = "";
            out.println("3DHP Client (Java-GUI v0.6");
            //out.println("3DHP Client (Java-Gui v0.6)");
            // 
            data =readFromServer();
            fold_message.appendMessage("conected to: "+data+"\n");
            //reply to server intoducing myself
            //out.println("3DHP Client (Java-Gui v0.64)");
            // 
            
            
            //try three times to deal with parameters
            for(int tries=0; data.compareTo("give parameters")!=0 && tries<4; tries++){
                if(tries == 3){throw new SocketException("Unable to request job");}
                
                //System.out.println("fold" +tries);
                fold_message.appendMessage("fold\n");
                out.println("fold");
                //out.println("fold");
                // 
                data =readFromServer();
                fold_message.appendMessage(data+"\n");
                
            }
          
            for(int tries=0; data.compareTo("ok")!=0 && tries<4; tries++){
                if(tries == 3){throw new SocketException("server not accepting parameters");}
                
                //send User parameters as XML
                
                fold_message.appendMessage(user_data+"\n");
                out.println(user_data);
                //get confirmation that parameters are o.k
                data = readFromServer();
                fold_message.appendMessage(data+"\n");  
            }
            
            while(data.compareTo("done") !=0){
                data =readFromServer();
                fold_message.appendMessage(data+"\n");

                task = info.getTag(data);
                if(task.compareTo("status") ==0){
                    //out.println("ok\n");
                    // 
                    amount = data.substring(data.indexOf("<percent>")+9,data.indexOf("</percent>"));
                    worker = data.substring(data.indexOf("<worker>")+8,data.indexOf("</worker>"));
                    task = data.substring(data.indexOf("<task>")+6,data.indexOf("</task>"));
                    if (task.compareTo("ss")==0){      task = "Building Secondary Strucutre... ";  }
                    else if (task.compareTo("p")==0){  task = "Building main population... ";      }
                    else{                              task = "Server Running... ";                }
                    prog_bar.setProgBar(Integer.valueOf(amount));
                    prog_bar.updateMessage(task + Integer.valueOf(amount)+"% (worker: "+worker+ ")" );
                }
                else if(task.compareTo("result") ==0){
                	System.out.println("got a result");
                    info.parseMessage(data, 3);
                    ui.redrawBigWindow(info.getView3(), info.getInfo(3));
                    System.out.println(info.getView3());
                }   
            }
            //prog_bar.setProgBar(100);
            try{ server.setSoTimeout(10000);}
            catch(Exception e){}
        }
        catch(Exception error_folding){
            failed = true;
            try{ server.setSoTimeout(10000);}
            catch(Exception e){}
            try{
                out.println("bye\n");
                 
            }
            catch(Exception error_bye_failed){
                fold_message.appendMessage(error_bye_failed.toString()+"\n");
            }
            fold_message.appendMessage(error_folding.toString()+"\n");
        }
        prog_bar.close();
        fold_message.setVisible(true);
    }
    
    public String readFromServer() throws IOException{
        String data =new String();
        data = recieved.readLine();  
        return data;
    }
    
public String getNextProt(){
        //if(!server.isConnected()){reConnect();}
        failed = false;
        String data = new String();
        try {
            out.println("get next result");
             
            data =readFromServer();
            
        } catch (IOException ex) {
            failed = true;
            Message error  = new Message(null, null);
            error.appendMessage("Atempted to get protein information from server");
            error.appendMessage(ex.toString());
            error.setVisible(true);
        }
        return data;
    }
public String getPrevProt(){
        //if(!server.isConnected()){reConnect();}
        failed = false;
        String data = new String();
        try {
            out.println("get previous result");
             
            data =readFromServer();
            
        } catch (IOException ex) {
            failed = true;
            Message error  = new Message(null, null);
            error.appendMessage("Atempted to get protein information from server");
            error.appendMessage(ex.toString());
            error.setVisible(true);
        }
        return data;
    }
    
    public void close(){
        try {
            server.close();
        } catch (IOException ex) {
            failed = true;
            new Message("Failed to close").setVisible(true);
        }
    }    
    
    public String testAddress(){
        try {
            out.println("3DHP Client (Java-Gui v0.6)");
             
            String server_message =readFromServer();
            out.println("bye");
             
            close();
            return "found: " + server_message;
        } catch (Exception ex) {
            //return (ex.toString());
            return ("Unable To connect to: " + server_name);
        }
    }
            
    public static void main(String args[]){
        //new Communicate();
    }
       
    public boolean failed(){
        return failed;
    }
    
    private Gui ui;
    private String server_name;
    private int max_wait;
    private boolean failed;
    private Socket server;
    //private InputStream recieved;
    private BufferedReader recieved;
    private OutputStreamWriter message;
    private OutputStream sent;
    private PrintWriter out;
    int MAXDATASIZE;
}
