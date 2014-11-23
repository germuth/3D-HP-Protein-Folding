/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gui;

/**
 *
 * @author hartsho
 */
public class cords {

    public cords(String directions){
        this.directions = directions;
        mastercord = new int[3];
        avg = new float[3];
        avg[0] = 0;
        avg[1] = 0;
        avg[2] = 0;
        max = new float[3];
        max[0] = 0;
        max[1] = 0;
        max[2] = 0;
        cord = new int[this.directions.length()+1][3];
        cord[0][0] = 0;
        cord[0][1] = 0;
        cord[0][2] = 0;
        start =0;
        has_next=false;
    }
    
    public void resetCords(String directions){
        this.directions = directions;
        mastercord = new int[3];
        avg = new float[3];
        avg[0] = 0;
        avg[1] = 0;
        avg[2] = 0;
        max = new float[3];
        max[0] = 0;
        max[1] = 0;
        max[2] = 0;
        cord = new int[this.directions.length()+1][3];
        cord[0][0] = 0;
        cord[0][1] = 0;
        cord[0][2] = 0;
        start =0;
        has_next=false;
    }

    public void start(){
        start = 0;
        has_next = false;
    }

    public float getAvgX(){
        return avg[0];
    }
    public float getAvgY(){
        return avg[1];
    }
    public float getAvgZ(){
        return avg[2];
    }
    public float getX(){
        return cord[start][0];
    }
    public float getY(){
        return cord[start][1];
    }
    public float getZ(){
        return cord[start][2];
    }
    public float maxX(){
        return max[0];
    }
    public float maxY(){
        return max[1];
    }
    public float maxZ(){
        return max[2];
    }
    
    public float seeNextX(){
        return cord[start+1][0];
    }
    public float seeNextY(){
        return cord[start+1][1];
    }
    public float seeNextZ(){
        return cord[start+1][2];
    }

    public void restart(){
        start =0;
        has_next = true;
    }
    public void getNext(){
        start++;
    }

    
    public boolean hasNext(){

        if(has_next && start <= this.directions.length()){
            return true;
        }
        else{
            return false;
        }

    }

        public boolean haslookahead(){

        if(has_next && start < this.directions.length()){
            return true;
        }
        else{
            return false;
        }

    }

    public int[][] setCords(String directions){
        has_next = true;
        int x,y,z;
        int curx=0, cury=0, curz=0;
        int maxx=0, maxy=0, maxz=0, minx=0, miny=0, minz=0;
        float ctx =0,cty=0,ctz=0;
        for(int i=1; i<=directions.length();i++){
            x = mastercord[0];
            y = mastercord[1];
            z = mastercord[2];
            switch(directions.charAt(i-1)){
                case 'r': mastercord[0] = x+1;
                          cord[i][0]= x+1;
                          cord[i][1]= y;
                          cord[i][2]= z;
                          avg[0] = avg[0]+cord[i][0];
                          if(minx == 0){
                              maxx++; 
                              max[0] = Math.max(max[0],maxx);}
                          else{minx--;}
                          ctx++;
                                        break;
                case 'l': mastercord[0] = x-1;
                          cord[i][0]= x-1;
                          cord[i][1]= y;
                          cord[i][2]= z;
                          avg[0] = avg[0]+cord[i][0];
                          if(maxx == 0){
                              maxx++; 
                              max[0] = Math.max(max[0],maxx);}
                          else{minx--;}
                          ctx++;
                                        break;
                case 'u': mastercord[1] = y+1;
                          cord[i][0]= x;
                          cord[i][1]= y+1;
                          cord[i][2]= z;
                          avg[1] = avg[1]+cord[i][1];
                          if(miny == 0){
                              maxy++; 
                              max[1] = Math.max(max[1],maxy);}
                          else{miny--;}
                          cty++;
                                        break;
                case 'd': mastercord[1] = y-1;
                          cord[i][0]= x;
                          cord[i][1]= y-1;
                          cord[i][2]= z;
                          avg[1] = avg[1]+cord[i][1];
                          if(maxy == 0){
                              miny++; 
                              max[1] = Math.max(max[1],miny);}
                          else{maxy--;}
                          cty++;
                                        break;
                case 'i': mastercord[2] = z+1;
                          cord[i][0]= x;
                          cord[i][1]= y;
                          cord[i][2]= z+1;
                          avg[2] = avg[2]+cord[i][2];
                          if(minz == 0){
                              maxz++; 
                              max[2] = Math.max(max[2],maxz);}
                          else{minz--;}
                          ctz++;
                                        break;
                case 'o': mastercord[2] = z-1;
                          cord[i][0]= x;
                          cord[i][1]= y;
                          cord[i][2]= z-1;
                          avg[2] = avg[2] + cord[i][2];
                          if(maxz == 0){
                              minz++; 
                              max[2] = Math.max(max[2],minz);}
                          else{maxz--;}
                          ctz++;
                                        break;
            }
           }
           if(ctx!=0)avg[0] = avg[0]/ctx;
           if(cty!=0)avg[1] = avg[1]/cty;
           if(ctz!=0)avg[2] = avg[2]/ctz;
           return cord;
    }

    public void printCords(){
        Message show_cords = new Message(null, null);
        show_cords.setVisible(true);
        show_cords.appendMessage("I contain the folowing cords\n");
        for(int i=0; i<directions.length();i++){
            show_cords.appendMessage("(" + cord[i][0] + "," + cord[i][1] + "," +cord[i][2] + ")\n");
        }
    }
    
    public int[][] getCoords(){
    	return this.cord;
    }

    private String directions;
    private int mastercord[];
    private int cord[][];
    private int start;
    private float avg[];
    private float max[];
    private boolean has_next;
}
