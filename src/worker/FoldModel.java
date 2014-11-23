package worker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

/**
 * 
 * FoldModel.java
 * 
 * FoldModel actually folds the protein. It reads in a charge sequence
 * and gives that protein a direction. 
 * Fold model class provides a model for creating valid protein structures.
 * Multiple nodes are not allowed to exist at the same coordinate. Provides
 * functions useful for fixing mutated proteins main method is build().
 * 
 * @author Aaron Germuth
 * @date 2013-03-16
 */
public class FoldModel {
	private static int BUILD_INEXACT = 0;
	private static int BUILD_EXACT = 1;
	private int type;
	private String directions;
	private ArrayList<SSInfo> ssList;
	private int health;
	private Node first;
	private Node last;
	/**
	 * A list of all nodes and their coordinates. Used to ensure only one
	 * node is ever placed at one position.
	 */
	private LinkedList<CoordsNodePair> allCoords;

	public FoldModel() {
		this.type = -1;
		this.directions = "";
		this.ssList = new ArrayList<SSInfo>();
		this.health = -1;
		this.first = null;
		this.last = null;
		this.allCoords = new LinkedList<CoordsNodePair>();
	}
	
	public String toString(){
		return "FoldModel Type: " + this.type + ", directions: " + this.directions + ", ssList: " + this.ssList + ", health: " + this.health;
	}

	/**
	 * Wrapper for buildSS
	 * 
	 * @param charges
	 * @param directions
	 * @param ssList2
	 */
	public boolean buildSSFromDirections(String proposedDirections) {
		int length = proposedDirections.length() + 1;
		return buildSS(length, proposedDirections);
	}

	public boolean buildSS(int length){
		return buildSS(length, "");
	}
	
	/**
	 * Gives Directions to a secondary structure. 
	 * 
	 * BuildSS if proposed directions are supplied, uses the first half of
	 * proposed directions to (inexactly) build the firsh half, otherwise
	 * randomly builds the first half and mirrors the first half to create the
	 * second half
	 * 
	 * @param length
	 * @param proposedDirections
	 * @return
	 */
	public boolean buildSS(int length, String proposedDirections) {
		
		String[] firstHalfDirections = new String[1];
		String[] secondHalfDirections = new String[1];
		
		int firstHalfLength;
		boolean success = false;
		
		//TODO buildSS isOdd doesn't make sense
		//TODO changed isODD so it makes sense
		
		/**
		boolean isOdd = (length % 2 != 0);
		// determine the length of the first half( if directions are odd, mirror
		// decides middle direction)
		if (isOdd) {
			firstHalfLength = length / 2;
		} else {
			firstHalfLength = (length / 2) - 1;
		}
		*/
		
		boolean isOdd;
		if( length % 2 == 0){
			isOdd = false;
			//in order to get one less direction than charges
			firstHalfLength = (length / 2) - 1;
		}
		else{
			isOdd = true;
			//odd needs to grab the middle one
			firstHalfLength = (length / 2);
		}
		// generate the first half of the charges
		String firstHalfCharges = stringOfHs(firstHalfLength);
		// Only use the first half of proposed directions
		if (proposedDirections.length() != 0) {
			proposedDirections = proposedDirections.substring(0, firstHalfCharges.length());
		}

		do {
			clear();
			// manually add the first node
			// TODO sub method should automatically add an x
			addNode('h');
			success = ( build(firstHalfCharges, proposedDirections, firstHalfDirections ) 
					&& 
					mirror( firstHalfDirections, isOdd, secondHalfDirections) );

		} while (!success && proposedDirections.length() == 0);

		if (!success) {
			return false;
		}

		assert (success);
		this.type = ParametersFromGUI.getSsType();
		this.directions = firstHalfDirections[0] + secondHalfDirections[0];
		assert this.directions.length() == ( length - 1 ) ;
		//TODO what is wrong assert
		this.health = getSSScore();
		return true;
	}

	/**
	 * get Secondary Structure Score
	 */
	private int getSSScore() {
		int count = 0;
		// TODO this node was final in C++
		Node foundNode;
		LinkedList<Coords> externalNodeList = new LinkedList<Coords>();

		for (Node n = this.first; n != null; n = n.getNext()) {
			if (n.getCharge() == 'h') {
				for (int i = -1; i <= 1; i += 2) { // i = {-1, 1}
					for (int j = 0; j < 3; j++) {// j = {0, 1, 2}
						Coords coords = new Coords(n.getCoords().getX(), n.getCoords().getY(), n.getCoords().getZ());
						switch (j) {
							case 0:
								coords.setX(coords.getX() + i);
								break;
							case 1:
								coords.setY(coords.getY() + i);
								break;
							case 2:
								coords.setZ(coords.getZ() + i);
								break;
						}

						foundNode = nodeExists(coords);
						if (foundNode != null) {
							// if node exists at coords and is not the previous
							// or next node, increase count
							if (foundNode != null && foundNode != n.getPrevious() && foundNode != n.getNext()) {

								if (foundNode.getCharge() == 'h') {
									count++;
								}
							}
						} else {
							// add to the list and maintain sorted order
							externalNodeList.add(coords);
							Collections.sort(externalNodeList, new Comparator<Coords>() {
								@Override
								public int compare(Coords one, Coords two) {
									if (one.getX() < two.getX()) {
										return -1;
									}
									if (one.getX() == two.getX()) {
										if (one.getY() < two.getY()) {
											return -1;
										}
										if (one.getY() == two.getY()) {
											if (one.getZ() < two.getZ()) {
												return -1;
											}
											if (one.getZ() == one.getY()) {
												return 0;
											}
										}
									}
									return 1;
								}
							});
						}
					}
				}
			}
		}
		return count / 2;
	}

	/**
	 * Mirror
	 * 
	 * @param firstHalfDirections2
	 * @param b
	 * @param secondHalfDirections2
	 * @return
	 */
	private boolean mirror(String[] firstHalfDirections, boolean isOdd, String[] secondHalfDirections) {
		int lengthBefore = this.allCoords.size();
		boolean success = false;
		Directions directions = new Directions(firstHalfDirections[0]);
		while (!success && directions.mirror(isOdd)) {
			String charges = stringOfHs( directions.get().length() );
			success = build(charges, directions.get(), secondHalfDirections, null, FoldModel.BUILD_EXACT);
		}
		int lengthAfter = this.allCoords.size();
		assert (success || lengthBefore == lengthAfter);
		return success;

	}

	// wrapper methods for build
	private boolean build(String charges, String proposedDirections, String[] newDirections) {
		return build(charges, proposedDirections, newDirections, null);
	}

	private boolean build(String charges, String proposedDirections, String[] newDirections, MetaPop metapop) {
		return build(charges, proposedDirections, newDirections, metapop, FoldModel.BUILD_INEXACT);
	}

	private boolean build(String charges, String proposedDirections, String[] newDirections, MetaPop metapop, int mode) {
		return build(charges, proposedDirections, newDirections, metapop, mode, 0);
	}

	// TODO this is where we are getting an error almost certainly
	private boolean build(String charges, String proposedDirections, String[] newDirections, MetaPop metaPop, int mode,
			int position) {
		if (this.isEmpty()) {
			System.err.println("Node list can't be empty. FoldModel.build");
		}

		// Base case where changes string is empty
		if (charges.equals("")) {
			newDirections[0] = "";
			return true;
		}

		DirectionList directionList = new DirectionList();
		Directions rotateModel = new Directions();
		Node newNode = null;
		//this should be changed before it is ever used
		char direction = '!';
		boolean success = false;
		boolean nodeAdded = false;
		boolean isSSStart = false;
		int ssLength;
		int attempt = 1;

		// Try adding the current node until successful or all possible
		// directions have been exhausted
		while (!success && !directionList.isEmpty()) {
			success = false;

			// First attempt
			if (attempt == 1) {
				// at the start of secondary structure
				ssLength = isSSStart(position);
				
				if (ssLength != 0) {
					// not building from directions, so get an SS from the
					// meta population
					if (proposedDirections.length() == 0) {
						assert (metaPop != null);
						proposedDirections = metaPop.getSS(ssLength).getDirection();
					}
					rotateModel.setDirections(proposedDirections.substring(0, ssLength - 1));
					isSSStart = true;
				}

				if (proposedDirections.length() != 0) {
					direction = directionList.take(proposedDirections.charAt(0));
					//TODO not sure about this
					proposedDirections = proposedDirections.substring(1);
				} else {
					direction = directionList.randomTake();
				}
			}
			// not the first attempt
			else {
				// if at the start of SS, rotate and replace the SS in proposed
				// Directions
				ssLength = isSSStart(position);
				if (ssLength != 0) {
					if (!rotateModel.rotate()) {
						return false;
					}

					char[] array = ParametersFromGUI.replace(proposedDirections.toCharArray(), 0, ssLength - 1,
							rotateModel.get());
					// TODO hopefully toString() works
					proposedDirections = array.toString();

					direction = proposedDirections.charAt(0);

				}
				// otherwise choose a random direction because
				// proposedDirections failed
				else {
					assert (!inSS(direction));
					direction = directionList.randomTake();

				}
			}
			// if we are in an SS or building exact, clear the direction list
			if (inSS(position) || mode == FoldModel.BUILD_EXACT) {
				directionList.clear();
			}

			// attempt to add the node
			// changed direction to an array so nodeAdded can change the direction
			char[] directionArray = { direction };
			nodeAdded = addNode(charges.charAt(0), directionArray);
			direction = directionArray[0];
			
			if (nodeAdded) {
				
				//TODO I think this code was causing problems comment it out
				// length proposed directions so that substr doesn't throw error
				/**
				if (proposedDirections.length() == 0) {
					proposedDirections = " ";
				}
				*/
				
				success = build(charges.substring(1), proposedDirections, newDirections, metaPop, mode, position + 1);
			}
			if (nodeAdded && !success) {
				deleteLastNode();
			}
			attempt++;
		}

		if (!success) {
			return false;
		}
		newDirections[0] = direction + newDirections[0];
		return true;
	}

	/**
	 * build P from directions wrapper for build P
	 */
	public void buildPFromDirections(String charges, String proposedDirections, ArrayList<SSInfo> ssList) {
		buildP(charges, null, ssList, proposedDirections);
	}
	
	/**
	 * wrapper for buildP
	 * @param charges
	 * @param metaPop
	 * @param possibleSSlist
	 */
	public void buildP(String charges, MetaPop metaPop, ArrayList<SSInfo> possibleSSlist){
		buildP(charges, metaPop, possibleSSlist, "");
	}

	/**
	 * buildP if proposed direcitons are supplied, uses build to (inexactly)
	 * fold the protein, otherwise randomly selects secondary structures and
	 * uses build to randomly fold the protein
	 * 
	 * currently does not build the beginnign in reverse
	 */
	public void buildP(String charges, MetaPop metaPop, ArrayList<SSInfo> possibleSSList, String proposedDirections) {
		boolean success = false;
		//TODO check this initializations
		String newDirections = "";
		String pDirections = "";
		String pCharges;
		ArrayList<SSInfo> chosenSSList = new ArrayList<SSInfo>();
		Random r = new Random();

		// Generate the secondary structure list
		if (proposedDirections.length() == 0) {
			// Not building from directions, randomly turn secondary structures
			// on
			for (int i = 0; i < possibleSSList.size(); i++) {
				if (r.nextInt(100) < ParametersFromGUI.getSSIntroProbability()) {
					possibleSSList.get(i).setOn(true);
					chosenSSList.add(possibleSSList.get(i));
				}
			}
		} 
		else {
			// building from directions, use provided secondary structure info
			for (int i = 0; i < possibleSSList.size(); i++) {
				if (possibleSSList.get(i).isOn()) {
					chosenSSList.add(possibleSSList.get(i));
				}
			}
		}
		this.ssList = possibleSSList;
		String[] array = new String[1];
		
		int attempts = 0;
		do {
			// Manually add the first node
			clear();
			addNode(charges.charAt(0));
			// Build
			array[0] = newDirections;
			success = build(charges.substring(1), proposedDirections, array, metaPop, FoldModel.BUILD_INEXACT);
			if (!success && attempts == 50) {
				System.err.println("FOldModel buildP has failed 50 attempts, latest attempt was to build "
						+ proposedDirections);
			}
			attempts++;
		} while (!success);

		newDirections = pDirections + array[0];
		assert (success);
		this.type = ParametersFromGUI.getpType();
		this.directions = newDirections;
		assert ((this.directions).length() == charges.length() - 1);
		this.health = getPScore();
	}

	private boolean inSS(int position) {
		for (int i = 0; i < this.ssList.size(); i++) {
			if (this.ssList.get(i).isOn() && position >= this.ssList.get(i).getPosition()
					&& position < this.ssList.get(i).getPosition() + this.ssList.get(i).getLength()) {
				return true;
			}
		}
		return false;
	}

	private int isSSStart(int position) {
		for (int i = 0; i < this.ssList.size(); i++) {
			if (this.ssList.get(i).isOn() && position == ssList.get(i).getPosition()) {
				return this.ssList.get(i).getLength();
			}
		}
		return 0;
	}

	private int countAdjacentNodes(Node node, char charge) {
		int count = 0;
		Node foundNode;
		for (int i = 1; i <= 1; i += 2) {
			for (int j = 0; j < 3; j++) {
				Coords coords = new Coords(node.getCoords().getX(), node.getCoords().getY(), node.getCoords().getZ());
				switch (j) {
					case 0:
						coords.setX(coords.getX() + i);
						break;
					case 1:
						coords.setY(coords.getY() + i);
						break;
					case 2:
						coords.setZ(coords.getZ() + i);
						break;
				}
				foundNode = nodeExists(coords);
				// If node exists at coords and is not the previous or next
				// node, increase count
				if (foundNode != null && foundNode != node.getPrevious() && foundNode != node.getNext()) {
					if (charge == 'h' && foundNode.getCharge() == 'h')
						count = count + 6;
					if (charge == 'p' && foundNode.getCharge() == 'h') {
						count = count + 1;
					}
				} else if (foundNode == null && charge == 'h') {
					count = count - 2;
				}
			}
		}
		return count;
	}
	
	private int getPScore(){
		int score = 0;
		for(Node n = this.first; n != null; n = n.getNext()){
			score += countAdjacentNodes(n, n.getCharge());
		}
		return score/2;
	}
	
	private boolean deleteLastNode(){
		boolean success = false;
		
		if(this.isEmpty()){
			System.err.println("Can't delete if a node doesn't exist");
			return false;
		}
		
		Node secondLast = this.last.getPrevious();
		
		//TODO make sure it could be deleted
		//should deleted no matter what node is because .equals() method only checks coords
		for(int i = 0; i < this.allCoords.size(); i++){
			Coords current = this.allCoords.get(i).getCoords();
			if(current.equals(last.getCoords())){
				this.allCoords.remove(i);
				success = true;
			}
		}
		
		if( !success ){
			return false;
		}
		
		if( secondLast != null){
			secondLast.setNext(null);
		}
		else{
			this.first = null;
		}
		this.last = secondLast;
		return true;
	}
	
	private boolean addNode(char charge){
		char[] array = { 'x' };
		return addNode(charge, array);
	}

	/**
	 * This method adds a node
	 * 
	 * @param c
	 */
	private boolean addNode(char charge, char[] direction) {
		boolean second;

		if (isEmpty()) {
			if (direction[0] != 'x') {
				System.err.println("Direction ignored when adding first node");
			}
			Coords newCoords = new Coords(0, 0, 0);
			Node newNode = new Node(charge, newCoords);
			
			this.allCoords.add( new CoordsNodePair(newCoords, newNode) );
			// make first and last point to same node
			this.first = newNode;
			this.last = newNode;
		} 
		else {
			if (direction[0] == 'x') {
				System.err.println("Actual direction required");
			}
		
			boolean nodeAdded = false;
			
			while(!nodeAdded){
				nodeAdded = true;
				// not adding first node
				// determine new coords based on previous node and direction
				int x = this.last.getCoords().getX();
				int y = this.last.getCoords().getY();
				int z = this.last.getCoords().getZ();
				switch (direction[0]) {
				case 'u':
					y++;
					break;
				case 'r':
					x++;
					break;
				case 'd':
					y--;
					break;
				case 'l':
					x--;
					break;
				case 'i':
					z--;
					break;
				case 'o':
					z++;
					break;
				default:
					System.err
							.println("invalid direction in FoldModel.addnode()");
					return false;
				}

				Coords newCoords = new Coords(x, y, z);
				Node newNode = new Node(charge, newCoords);

				// TODO added code to ensure to nodes aren't inserted at the
				// same position
				for (CoordsNodePair p : this.allCoords) {
					if (p.getCoords().equals(newCoords)) {
						nodeAdded = false;
						direction[0] = new DirectionList().randomTake();
					}
				}
				if(nodeAdded){
					this.allCoords.add(new CoordsNodePair(newCoords, newNode));
					Node secondLast = this.last;
					this.last = newNode;
					secondLast.setNext(this.last);
					this.last.setPrevious(secondLast);
				}
			}
			// TODO C++ had some weird stuff to make sure the insertion was
			// correct

		}
		return true;

	}

	private void clear() {
		this.allCoords.clear();
		this.first = null;
		this.last = null;
	}

	private String stringOfHs(int firstHalfLength) {
		String answer = "";
		for (int i = 0; i < firstHalfLength; i++) {
			answer += "h";
		}
		return answer;
	}

	private boolean isEmpty() {
		if (first == null) {
			return true;
		}
		return false;
	}

	/**
	 * replaces nodeExists() from C++ searches has map based on coords for a
	 * matching node, if it exists it returns it if it doesn't exist, it return
	 * NULL
	 * 
	 * @return
	 */
	private Node searchforNode(Coords coords) {
		for (CoordsNodePair p : this.allCoords) {
			if (p.equals(new CoordsNodePair(coords, null))) {
				return p.getNode();
			}
		}
		return null;

	}

	/**
	 * wrapper so we can use the same method name as C++, just so i don't
	 * confuse lee by changing all the names
	 * 
	 * @param coords
	 * @return
	 */
	private Node nodeExists(Coords coords) {
		return this.searchforNode(coords);
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
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

	public int getHealth() {
		return health;
	}

	public void setHealth(int health) {
		this.health = health;
	}

	public Node getFirst() {
		return first;
	}

	public void setFirst(Node first) {
		this.first = first;
	}

	public Node getLast() {
		return last;
	}

	public void setLast(Node last) {
		this.last = last;
	}
}
