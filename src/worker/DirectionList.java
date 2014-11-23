package worker;
/**
 * DirectionList.java
 * 
 * A list of possible 3D directions useful for choosing directions at random
 * 
 * @author Lee Foster
 */
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;

public class DirectionList {

	private LinkedList<Character> direction_list;
	
	//Sets up the possible directions and shuffles them
	public DirectionList(){
		direction_list = new LinkedList<Character>();
		
		//Puts the directions into an array for easy shuffling
		char directions[] = {'u', 'r', 'd', 'l', 'i', 'o'};
		Random r = new Random();
		for(int i = 0; i < 6;i++){
			int j = r.nextInt(6);
			char temp = directions[j];
			directions[j] = directions[i];
			directions[i] = temp;
		}
		//Store the directions (now shuffled) in a list
		for(int i = 0; i<6; i++){
			direction_list.add(directions[i]);
		}
	}
	
	//removes a random direction from the list and returns it
	//because the direction list is shuffled at construction we 
	//can simply take from the front of the list
	public char randomTake(){
		if(direction_list.size() == 0){
			System.out.println("error DirectionList.randomTake: list is empty");
			System.exit(1);
		}
		char temp = direction_list.pop();
		return temp;
	}
	
	//removes the specified direction from the list and returns it
	public char take(char c){
		Iterator<Character> it = direction_list.iterator();
		while(it.hasNext()){
			char current = (char) it.next();
			if(current == c){
				direction_list.remove(new Character(current));
				return c;
			}
		}
		System.out.println("error DirectionList.take: direction not found");
		System.exit(1);
		return ' ';
	}
	
	//Returns true if the list is empty, false otherwise
	public boolean isEmpty(){
		if(direction_list.size() == 0){
			return true;
		}
		else{
			return false;
		}
	}
	
	//Empties the list of directions
	public void clear(){
		direction_list.clear();
	}
	
	public String toString(){
		return this.direction_list.toString();
	}
}
