package worker;

import java.util.Random;

/**
 * 
 * Directions.java
 * 
 * This class holds a direction. It also contains the functionality to mirror, rotate, and
 * reflect the directions. Each direction specifies where the next node is in relation to the last one.
 * for example
 * 'l' - to the left
 * 'r' - to the right
 * 'u' - up
 * 'd' - down
 * 'i' - in, in the screen, farther away
 * 'o' - out, out of the screen, coming at you
 * 
 * (From C++) 
 * 
 * Know issues - mirror is_odd retruns an invalid solutions that
 * garantee to be invalid (contain overlaps) eg udu, ioi... FIXED - inificinet
 * initialiser, inisializes for both rotate and mirror. (efficiency) - rotation
 * returns many results granteed to overlap with current directions.. and
 * duplicate rotations... FIXED - rotation assumes all combinations eg
 * rotate(llll) tries all 20 combos yet only 4 exist for this string
 * (efficiency) - odd and even cases are reversed (line 167)
 * 
 * @author Aaron Germuth
 * @date 2013-03-18
 */
public class Directions {
	//
	private int[] mirrorKey;
	//
	private int[] rotateKey;
	//
	private int attempt;
	//
	private boolean mirrored;
	//
	private boolean rotated; 
	//
	private String reflectedDirections;
	//
	private String rotatedDirections;
	//
	private String directions;
	//
	private boolean swapIO;
	//
	private boolean swapUD;
	//
	private boolean swapRL;
	//default constructor
	public Directions(){
		
	}
	
	public Directions(String directions){
		setDirections(directions);
	}
	
	public String toString(){
		return "Directions: directions: " + this.directions + ", reflected: " + this.reflectedDirections + ", rotated: " + this.rotatedDirections;
	}
	
	public void setDirections(String directions){
		this.directions = directions;
		this.mirrored = false;
		this.rotated = false;
		this.attempt = 0;
		this.mirrorKey = new int[6];
		this.rotateKey = new int[24];
		
		this.rotatedDirections = "";
		this.reflectedDirections = "";
		
		//randomize the possible rotation options
		setRotateKey();
		//randomize the possible mirror options
		setMirrorKey();
	}
	/**
	 * This method randomizes the possible rotation options
	 */
	private void setMirrorKey() {
		//create random number generator
		Random r = new Random();
		for(int i = 0; i < 6; i++){
			this.mirrorKey[i] = i + 1;
		}
		for(int i = 0; i < 6; i++){
			int value = mirrorKey[i];
			int index = r.nextInt(6);
			mirrorKey[i] = mirrorKey[index];
			mirrorKey[index] = value;
		}
		
	}
	/**
	 * THis method randomized the possible mirror options. 
	 * There are 64 combinations
	 * 20 are unique and valid 0-7, 12-19, 24-27
	 */
	private void setRotateKey() {
		Random r = new Random();
		for(int i = 0; i < 8; i++){
			rotateKey[i] = i;
		}
		for(int i = 0; i < 16; i++){
			rotateKey[i] = i+4;
		}
		for(int i = 16; i < 20; i++){
			rotateKey[i] = i + 8;
		}
		
		//scramble all 20 options
		for(int i = 0; i < 20; i ++){
			int value = rotateKey[i];
			int index = r.nextInt(20);
			rotateKey[i] = rotateKey[index];
			rotateKey[index] = value;
		}
		
	}
	
	/**
	 * 
	 * @return
	 */
	public boolean rotate(){
		if(this.mirrored || attempt >= 20){
			return false; 
		}
		this.rotatedDirections = "";
		
		//convert base 10 to base 4
		int base4[] = {0, 0, 0}; 
		int x = rotateKey[attempt]; 
		for(int pos = 2; pos >= 0; pos--){
			base4[pos] = x % 4;
			x /= 4;
		}
		
		//be careful with strings, when they are returned they aren't changed, in C++ they are
		char dir;
		for(int pos = 0; pos < this.directions.length(); pos++){
			dir = rotateX(this.directions.charAt(pos), base4[2]);
			dir = rotateY(dir, base4[1]);
			dir = rotateZ(dir, base4[0]);
			//add direction to string
			rotatedDirections += dir;
		}
		
		this.attempt++;
		this.rotated = true;
		return true;
	}

	/**
	 * rotates directions around the x axis by a given offset
	 * @param charAt
	 * @param i
	 * @return
	 */
	public char rotateX(char direction, int offset) {
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
	public char rotateY(char direction, int offset) {
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
	public char rotateZ(char direction, int offset) {
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
	 * This method mirrors a direction
	 * @param whether the directions string is odd
	 * @return
	 */
	public boolean mirror(boolean isOdd){
		if( (isOdd && attempt > 5) || (!isOdd && attempt > 0) || rotated == true ){
			return false;
		}
		this.swapUD = false;
		this.swapRL = false;
		this.swapIO = false;
		boolean ok = true;
		
		if(isOdd){
			ok = setCaseIsOdd();
		}
		else{
			ok = setCaseIsEven();
		}
		if(!ok){
			return false;
		}
		
		for(int pos = directions.length() - 1; pos >= 0; pos--){
			switch(directions.charAt(pos)){
				case 'u': 
					if(swapUD){
						reflectedDirections += 'd'; 
					}
					else{
						reflectedDirections += 'u';
					}
					break;
				case 'r': 
					if(swapRL){
						reflectedDirections += 'l'; 
					}
					else{
						reflectedDirections += 'r';
					}
					break;
				case 'd': 
					if(swapUD){
						reflectedDirections += 'u'; 
					}
					else{
						reflectedDirections += 'd';
					}
					break;
				case 'l': 
					if(swapUD){
						reflectedDirections += 'r'; 
					}
					else{
						reflectedDirections += 'l';
					}
					break;
				case 'i': 
					if(swapUD){
						reflectedDirections += 'o'; 
					}
					else{
						reflectedDirections += 'i';
					}
					break;
				case 'o': 
					if(swapUD){
						reflectedDirections += 'i'; 
					}
					else{
						reflectedDirections += 'o';
					}
					break;
				default: System.err.println("Directions:Mirror invalid character");
				break;
			
			}
		}
		this.mirrored = true;
		this.attempt++;
		return true;
		
	}
	
	/**
	 * setCaseIsOdd
	 * This function sets the mirror options used by mirror.
	 * Methods are stored as private member variables rather than local variables because in C++
	 * they are changed when they are passed into the method
	 * @return
	 */
	public boolean setCaseIsEven(){
		int middle = directions.length() - 1;
		switch(mirrorKey[attempt]){
			case 1:
				if(this.directions.charAt(middle) == 'o' || this.directions.charAt(middle) == 'i'){
					attempt++;
					return setCaseIsEven();
				}
				this.swapUD = true;
				this.swapRL = true;
				this.reflectedDirections += "i";
				break;
			case 2:
				if(this.directions.charAt(middle) == 'o' || this.directions.charAt(middle) == 'i'){
					attempt++;
					return setCaseIsEven();
				}
				this.swapUD = true;
				this.swapRL = true;
				this.reflectedDirections += "o";
				break;
			case 3:
				if(this.directions.charAt(middle) == 'r' || this.directions.charAt(middle) == 'l'){
					attempt++;
					return setCaseIsEven();
				}
				this.swapUD = true;
				this.swapIO = true;
				this.reflectedDirections += "l";
				break;
			case 4:
				if(this.directions.charAt(middle) == 'r' || this.directions.charAt(middle) == 'l'){
					attempt++;
					return setCaseIsEven();
				}
				this.swapUD = true;
				this.swapIO = true;
				this.reflectedDirections += "r";
				break;
			case 5:
				if(this.directions.charAt(middle) == 'd' || this.directions.charAt(middle) == 'u'){
					attempt++;
					return setCaseIsEven();
				}
				this.swapIO = true;
				this.swapRL = true;
				this.reflectedDirections += "u";
				break;
			case 6:
				if(this.directions.charAt(middle) == 'd' || this.directions.charAt(middle) == 'u'){
					attempt++;
					return setCaseIsEven();
				}
				this.swapIO = true;
				this.swapRL = true;
				this.reflectedDirections += "d";
				break;
			default:
				return false;
		}
		return true;
	}
	
	/**
	 * Set case is even
	 * this method determines which mirror option is the valid one for mirror()
	 * @return
	 */
	private boolean setCaseIsOdd(){
		int middle = directions.length()-1;
		if (this.directions.charAt(middle) == 'i' || this.directions.charAt(middle) == 'o'){
			this.swapUD = true;
			this.swapRL = true;
		}
		else if (this.directions.charAt(middle) == 'u' || this.directions.charAt(middle) == 'd'){
			this.swapIO = true;
			this.swapRL = true;
		}
		else if (this.directions.charAt(middle) == 'r' || this.directions.charAt(middle) == 'l'){
			this.swapUD = true;
			this.swapIO = true;
		}
		else{
			System.err.println("isOdd = false did not find match in directions.mirror");
			return false;
		}
		return true;
	}
	/**
	 * this method gets the current direction, mirror, rotated or however it may be
	 * @return
	 */
	public String get(){
		if(rotated){
			return this.rotatedDirections;
		}
		if(mirrored){
			return this.reflectedDirections;
		}
		return this.directions;
	}

}

