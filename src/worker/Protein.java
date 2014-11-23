package worker;

import java.util.ArrayList;
import java.util.Random;

/**
 * 
 * Protein.java
 * 
 * A protein. Protein has a type, which indicates whether it is the full protein, or a subprotein representing 
 * a secondary structure of the full protein. Also has health, how stable the protein's directions are, and it's directions.
 * 
 * @author Aaron Germuth
 * @date 2013-03-16
 */
public class Protein {
	private int type;
	private int health;
	private String directions;
	/**
	 * List of (possible?) second structures
	 */
	private ArrayList<SSInfo> ssList;

	/**
	 * default constructor, sets, type and health to -1
	 */
	public Protein() {
		this.type = -1;
		this.health = -1;
	}

	/**
	 * sets health to -1
	 * 
	 * @param type
	 */
	public Protein(int type) {
		this.type = type;
		this.health = -1;
	}

	public Protein(int type, int health, String direction) {
		this.type = type;
		this.health = health;
		this.directions = direction;
	}

	public Protein(int type, int health, String direction, ArrayList<SSInfo> ssList) {
		this.type = type;
		this.health = health;
		this.directions = direction;
		this.ssList = ssList;
	}
	
	public Protein(Protein copy){
		this.directions = copy.directions;
		this.health = copy.health;
		this.ssList = copy.ssList;
		this.type = copy.type;
	}
	
	//don't think this is necessary anymore
	public Protein(FoldModel rhs){
		this.assign(rhs);
		
	}
	
	public String toString(){
		return "Protein Type: " + this.type + ", Directions: " + this.directions + ", Health: " + this.health;
	}
	
	/**
	 * kind of the operator overload seen in C++
	 * TODO check if this works
	 * @param rhs
	 * @return
	 */
	public Protein equals(FoldModel rhs){
		this.assign(rhs);
		return this;
	}

	/**
	 * This method mutates a protein by changing it's structure
	 * @param metaPop
	 */
	public void mutate(MetaPop metaPop) {
		//
		int[] offset = { 0, 0, 0 };
		//
		int hits = 0;
		//
		String newSS;

		// ensure protein is right type
		if (this.type != ParametersFromGUI.getSsType() && this.type != ParametersFromGUI.getpType() ) {
			System.err.println("Protein:Mutate Protein type invalid");
		}
		// Random number generator
		Random r = new Random();

		// for each direction
		for (int pos = 0; pos < this.directions.length(); pos++) {
			// the secondary structure
			if (this.type == ParametersFromGUI.getSsType()) {
				// if random number from 0 - 99 greater than mutateprobability,
				// then mutate!
				if (r.nextInt(100) < ParametersFromGUI.getMutateProbability()) {
					// update the offsets, not sure why random number from 0 - 3
					offset[0] += r.nextInt(4);
					offset[1] += r.nextInt(4);
					offset[2] += r.nextInt(4);
				}

				// modify the directions according to the offsets
				char[] direction = this.directions.toCharArray();

				direction[pos] = rotateX(this.directions.charAt(pos), offset[0]);
				direction[pos] = rotateY(this.directions.charAt(pos), offset[0]);
				direction[pos] = rotateZ(this.directions.charAt(pos), offset[0]);
				
				this.directions = "";
				
				for(int i = 0; i < direction.length; i++){
					this.directions += direction[i];
				}
			}
			//Protein
			else{
				// If the ucrrent position is not the start of a secondary structure that is turned on
				if( this.ssList.size() <= hits || this.ssList.get(hits).getPosition() != pos || !this.ssList.get(hits).isOn() ){
					//Allowed to mutate
					if( r.nextInt(100) < ParametersFromGUI.getMutateProbability() ){
						//update offsets
						offset[0] += r.nextInt(4);
						offset[1] += r.nextInt(4);
						offset[2] += r.nextInt(4);
					}
					
					// modify the directions according to the offsets
					char[] direction = this.directions.toCharArray();

					direction[pos] = rotateX(this.directions.charAt(pos), offset[0]);
					direction[pos] = rotateY(this.directions.charAt(pos), offset[0]);
					direction[pos] = rotateZ(this.directions.charAt(pos), offset[0]);
					
					this.directions = "";
					
					for(int i = 0; i < direction.length; i++){
						this.directions += direction[i];
					}
					
					//for an 'h' sequence that is not a secondary structure
					if (this.ssList.size() > hits
							&& this.ssList.get(hits).getPosition() == pos) {
						hits++;
					}
				} else {
					// at the start of secondary structures; not allowed to
					// mutate
					// chance of swapping secondary structures
					if (r.nextInt(100) < ParametersFromGUI.getSSSwapProbability()) {
						// get new secondary structure directions
						newSS = metaPop.getSS(this.ssList.get(hits).getLength()).getDirection();
						if (newSS.length() == this.ssList.get(hits).getLength()) {
							
							char[] direction = this.directions.toCharArray();
							// String method replacing C++ method
							direction = ParametersFromGUI.replace( direction, pos, this.ssList.get(hits).getLength(), newSS );
							
							this.directions = "";
							
							for(int i = 0; i < direction.length; i++){
								this.directions += direction[i];
							}
							
						}
						// clear new SS
						newSS = "";
					}
					// skip the rest of the secondary structure
					pos += this.ssList.get(hits).getLength() - 1;
					hits++;
				}
			}
		}
	}

	/**
	 * rotates directions around the x axis by a given offset
	 * @param charAt
	 * @param i
	 * @return
	 */
	private char rotateX(char direction, int offset) {
		//possible directions, up, in, down, out
		char dir[] = {'u', 'i', 'd', 'o'};
		switch(direction){
			case 'u': return dir[( 0 + offset) % 4];
			case 'i': return dir[( 1 + offset) % 4];
			case 'd': return dir[( 2 + offset) % 4];
			case 'o': return dir[( 3 + offset) % 4];
		default: 
			return direction; 
		}
	}
	/**
	 * rotates directions around the y axis by a given offset
	 * @param charAt
	 * @param i
	 * @return
	 */
	private char rotateY(char direction, int offset) {
		//possible directions, up, in, down, out
		char dir[] = {'i', 'r', 'o', 'l'};
		switch(direction){
			case 'i': return dir[( 0 + offset) % 4];
			case 'r': return dir[( 1 + offset) % 4];
			case 'o': return dir[( 2 + offset) % 4];
			case 'l': return dir[( 3 + offset) % 4];
		default: 
			return direction; 
		}
	}
	/**
	 * rotates directions around the z axis by a given offset
	 * @param charAt
	 * @param i
	 * @return
	 */
	private char rotateZ(char direction, int offset) {
		//possible directions, up, in, down, out
		char dir[] = {'u', 'r', 'd', 'l'};
		switch(direction){
			case 'u': return dir[( 0 + offset) % 4];
			case 'r': return dir[( 1 + offset) % 4];
			case 'd': return dir[( 2 + offset) % 4];
			case 'l': return dir[( 3 + offset) % 4];
		default: 
			return direction; 
		}
	}
	
	/**
	 * This metod makes a FoldModel and calls buildFormDirections and updates proteins shape
	 * @param charges
	 */
	public boolean fix(String charges) {
		//no constructor call here, but I think we need one
		//TODO make sure it works
		//FoldModel myself
		FoldModel myself = new FoldModel();
		if(this.type == ParametersFromGUI.getpType()){
			myself.buildPFromDirections(charges, this.directions, this.ssList);
		}
		else if(type == ParametersFromGUI.getSsType()){
			if(!myself.buildSSFromDirections(this.directions)){
				return false;
			}
		}
		else{
			System.err.println("Invalid type found in Protein: fix");
		}
		assign(myself);
		return true;
		
	}
	
	public void assign(FoldModel model){
		this.type = model.getType();
		this.health = model.getHealth();
		this.directions = model.getDirection();
		this.ssList = model.getSsList();
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public int getHealth() {
		return health;
	}

	public void setHealth(int health) {
		this.health = health;
	}

	public String getDirection() {
		return directions;
	}

	public void setDirection(String direction) {
		this.directions = direction;
	}

	public ArrayList<SSInfo> getSsList() {
		return ssList;
	}

	public void setSsList(ArrayList<SSInfo> ssList) {
		this.ssList = ssList;
	}

	public int getNumberSS(){
		return this.ssList.size();
	}

}
