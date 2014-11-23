/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gui;

import java.util.ArrayList;
import javax.swing.JProgressBar;
/**
 *
 * @author hartsho
 */
public class Info extends Thread{

    
    Info(Gui sourceui){
        start =0;
        big_view=0;
        this.ui = sourceui;
        
        directions = new String[4];
        directions[0]="uuuuddrruuddddrruuuurrddllddlddllllrrrrrrrrlllll";
        directions[1]="ildidlldlidiuriiuorolodrodiliruiiidlorodoldiiuld";
        directions[2]="uuuuddrruuddddrruuuurrddllddlddllllrrrrrrrrlllll";
        directions[3]="luridluurrdddlloouuuirrdddloruuoriiiilllloooorri";
   
        protien_info = new String[4];
        protien_info[0]="Welcome to 3DHP\nInteractive protien folder\n\nPlease enter charges\nExample: hhhphphp ";
        protien_info[1]="Welcome to 3DHP\nInteractive protien folder\n\nExample 1";
        protien_info[2]="Welcome to 3DHP\nInteractive protien folder\n\nBen Hartshorn\nMike Peters";
        protien_info[3]="Welcome to 3DHP\nInteractive protien folder\n\nExample 2";
    }
    
    public String getInfo(int view){    
        if(view==0) {
            String no_info = new String("Protein Information\nnot available yet");
            return no_info;
        }
        return protien_info[0];
    }
 
    public String getView1(){
        protien_info[0] = protien_info[1];
        directions[0] = directions[1];
        return directions[1];
    } 
    public String getView2(){
        protien_info[0] = protien_info[2];
        directions[0] = directions[2];
        return directions[2];
    }  
    public String getView3(){
        protien_info[0] = protien_info[3];
        directions[0] = directions[3];
        return directions[3];
    }
    public String getBigView(){
        return directions[0];
    } 
    //public void   setBigView(int view){
    //    big_view = view;
    //}
  
    public void   next(){
        String message = new String();
        message = server.getNextProt();
        if (message !=null){
           directions[1] = directions[2];
           directions[2] = directions[3];
           protien_info[1] = protien_info[2];
           protien_info[2] = protien_info[3];
           parseMessage(message,3);  
        }    
    }
    public void   prev(){
        String message = new String();
        message = server.getPrevProt();
        if (message !=null){
           directions[3] = directions[2];
           directions[2] = directions[1];
           protien_info[3] = protien_info[2];
           protien_info[2] = protien_info[1];
           parseMessage(message,1);  
        }    
    }

    public String getTag(String xml_message){
        String tag = new String();
        int ptr=0;
        while( ptr < xml_message.length() &&
              (xml_message.charAt(ptr) == ' '   ||
               xml_message.charAt(ptr) == '\n'  ||
               xml_message.charAt(ptr) == '\t') ){
            ptr++;
        }
        if (ptr < xml_message.length() && xml_message.charAt(ptr) == '<'){
            ptr++;
            while (xml_message.charAt(ptr) !='>' && ptr < xml_message.length()){
                tag+=xml_message.charAt(ptr);
                ptr++;
            } 
            ptr++;
        }
        return tag;
    }
    
    private String getTag(){
        String tag = new String();
        int ptr=0;
        while( ptr < message.length() &&
              (message.charAt(ptr) == ' '   ||
               message.charAt(ptr) == '\n'  ||
               message.charAt(ptr) == '\t') ){
            ptr++;
        }
        if (ptr < message.length() && message.charAt(ptr) == '<'){
            ptr++;
            while (message.charAt(ptr) !='>' && ptr < message.length()){
                tag+=message.charAt(ptr);
                ptr++;
            } 
            ptr++;
        }
        message = message.substring(ptr);
        return tag;
    }
    
    private String getData(){
        String tag = new String();
        int ptr=0;
        while((message.charAt(ptr) == ' '   ||
               message.charAt(ptr) == '\n'  ||
               message.charAt(ptr) == '\t') &&
               ptr < message.length()){
            ptr++;
        }
        while (message.charAt(ptr) !='<' && ptr < message.length()){
            tag+=message.charAt(ptr);
            ptr++;
        }
        message = message.substring(ptr);
        return tag;
    }
    
    public String getData(String xml_message){
        String tag = new String();
        int ptr=0;
        while((xml_message.charAt(ptr) == ' '   ||
               xml_message.charAt(ptr) == '\n'  ||
               xml_message.charAt(ptr) == '\t') &&
               ptr < xml_message.length()){
            ptr++;
        }
        while (xml_message.charAt(ptr) !='<' && ptr < xml_message.length()){
            tag+=xml_message.charAt(ptr);
            ptr++;
        }
        return tag;
    }
    
    public void parseMessage(String XMLmessage, int prot_num){
        
        //data variables neede
        this.message = XMLmessage;
        ArrayList<Integer> ss_starts = new ArrayList<Integer>();
        ArrayList<Integer> ss_lengths = new ArrayList<Integer>();
        boolean resultEndTag = false;
        String tag = new String();
        String health = new String();
        String comments = new String();
        String rating = new String();
        Message pars_message = new Message(this, null);
        
        if(getTag().compareTo("result") == 0){
            while(resultEndTag == false && message.length()>0){
                infinate_loop_detector = message.length();
                tag = getTag();
                if(tag.compareTo("directions") == 0){
                    directions[prot_num] = getData();
                    tag = getTag();
                    if (tag.compareTo("/directions") !=0){  
                        pars_message.appendMessage("XML pasing error: expected /directions recived: "+tag+"\n");
                        pars_message.setVisible(true);
                }   }
                if(tag.compareTo("worker") == 0){
                    System.out.println(getData());
                    tag = getTag();
                    if (tag.compareTo("/worker") !=0){  
                        pars_message.appendMessage("XML pasing error: expected /worker: "+tag+"\n");
                        pars_message.setVisible(true);
                }   }
                if(tag.compareTo("health") == 0){
                    health = "Protein Health: "+ getData();
                    tag = getTag();
                    if (tag.compareTo("/health") !=0){
                        pars_message.appendMessage("XML pasing error: expected /health recived: "+tag+"\n");
                        pars_message.setVisible(true);
                }   }
                if(tag.compareTo("comments") == 0){
                    comments = "User Comments: " + getData();
                    tag = getTag();
                    if (tag.compareTo("/comments") !=0){
                        pars_message.appendMessage("XML pasing error: expected /comments recived: "+tag+"\n");
                        pars_message.setVisible(true);
                }   }
                if(tag.compareTo("rating") == 0){
                    rating = "Average Rating: " + getData();
                    tag = getTag();
                    if (tag.compareTo("/rating") !=0){
                        pars_message.appendMessage("XML pasing error: expected /rating recived: "+tag+"\n");
                        pars_message.setVisible(true);
                }   }
                if(tag.compareTo("ss") == 0){
                    try{
                        for(int ss=0; ss<2;ss++){
                          tag = getTag();
                            if(tag.compareTo("position") == 0){
                                ss_starts.add(Integer.valueOf(getData()));
                                tag = getTag();
                                if (tag.compareTo("/position") !=0){
                                    pars_message.appendMessage("XML pasing error: expected /pos recived: "+tag+"\n");
                                    pars_message.setVisible(true);
                            }   }             
                            if(tag.compareTo("length") == 0){
                                ss_lengths.add(Integer.valueOf(getData()));
                                tag = getTag();
                                if (tag.compareTo("/length") !=0){
                                    pars_message.appendMessage("XML pasing error: expected /length recived: "+tag+"\n");
                                    pars_message.setVisible(true);
                    }   }   }   }
                    catch(Exception e){
                        pars_message.appendMessage("XML pasing error, Invalid integer: "+tag+"\n");
                        pars_message.setVisible(true);
                }    } 
                if(tag.compareTo("/result") == 0){
                    resultEndTag = true;
                }
                if (infinate_loop_detector == message.length()){
                    pars_message.appendMessage("Unable to parse: "+ message+"\n");
                    pars_message.setVisible(true);
                    resultEndTag=true;
        }   }   }
        protien_info[prot_num] = (health +"\n\n"+"Secondary Structured: "+ss_starts.size()+
                                "\n"+comments+"\n"+rating);
        System.out.println(protien_info[prot_num]);
    }                        

    public String generateFoldXML(Gui data){
        int user_def_ss[] = data.getUsrSSdef();
        send_message ="";
        send_message+="<parameters>";
        send_message+="<charges>"+data.getCharges()+"</charges>";
        send_message+="<mutate>"+data.getMutProb()+"</mutate>";
        send_message+="<ss-intro>"+data.getAddSS()+"</ss-intro>";
        send_message+="<ss-swap>"+data.getSwapSS()+"</ss-swap>";
        send_message+="<pop-size>"+data.getPopSize()+"</pop-size>";
        send_message+="<ga-iterations>"+data.getGAcycles()+"</ga-iterations>";
        send_message+="<min-ss-length>"+data.getMinSSLen()+"</min-ss-length>";
        send_message+="<max-ss-length>"+data.getMaxSSLen()+"</max-ss-length>";
        send_message+="<max-ss-pops>"+data.getSSqt()+"</max-ss-pops>";
        send_message+="<user-ss-only>"+data.getUsrSSonly()+"</user-ss-only>";
        send_message+="<num-pops>"+data.getNumPops()+"</num-pops>";
        if(user_def_ss.length >0){
            send_message+="<ss-list>";
            for(int i =0; i<user_def_ss.length; i++){
                send_message+="<ss>"+user_def_ss[i]+"</ss>";         
            }
            send_message+="</ss-list>";
        }
        send_message+="</parameters>";
        return(send_message);
    }
    
    public void run(){
        server =new Communicate(ui.getServerName(), ui);
        server.fold(generateFoldXML(ui),this, ui);
        boolean success =true;
        String message = new String();
        if (server.failed()==false){
            for(int i=0; i<3; i++){
                message = server.getNextProt();
                if (message !=null){ parseMessage(message,i+1); }
                else{ success = false; }
            }
            directions[0]=directions[1];
            if (success){ 
                directions[0]=directions[1];
                dir = directions[1];
                protien_info[0]=protien_info[1];
                ui.redraw(directions[1], directions[2], directions[3], protien_info[0]);
            }
        }
    }
    
    Gui ui;
    Communicate server;
    int infinate_loop_detector;
    String message;
    String send_message;
    
    //private String charges;
    
    //old code for testing
    int big_view;
    int start;
    private String[] protien_info; 
    private String[] directions;
    public String dir;
    
}
