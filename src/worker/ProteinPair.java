package worker;

import java.util.ArrayList;
import java.util.Random;

/**
 * 
 * ProteinPair.java
 *
 * This class represents a pair of proteins. The main use of this class is the crossover function used in the genetic algorithm
 * and the mutate function
 *
 * @author Aaron Germuth
 * @date 2013-03-16
 */
public class ProteinPair {
	private Protein protein1;
	private Protein protein2;
	
	public ProteinPair(){
		this.protein1 = null;
		this.protein2 = null;
	}
	
	public ProteinPair(Protein one, Protein two){
		this.protein1 = one;
		this.protein2 = two;
	}
	
	public String toString(){
		return "Protein Pair: Protein1: " + this.protein1 + ", Protein2 " + this.protein2;
	}
	
	/**
	 * This method performs the crossover step of the genetic algorithm. It takes two proteins
	 * (the two of the protein pair and merges their "genomes" to produce a child. It does this
	 * by picking a random position in the proteins direction string. Then it every character after the string
	 * to the other protein and vice versa. This change is also made for each proteins ssList
	 */
	public void crossover(){
		// the direction of first protein
		String direction1 = this.protein1.getDirection();
		// direction of second protein
		String direction2 = this.protein2.getDirection();

		// ssList of first protein
		ArrayList<SSInfo> ssInfoList1 = this.protein1.getSsList();
		// ssList of second protein
		ArrayList<SSInfo> ssInfoList2 = this.protein2.getSsList();
		
		// a random point in the proteins direction where crossover will happen on either side
		int cross = 0;
		// the length of the string containing the proteins direction
		int length = 0;
		// random number generator
		Random r = new Random();
		
		// ensure the proteins are of the same type
		if(this.protein1.getType() != this.protein2.getType()){
			System.err.println("ProteinPair.crossover(): Two proteins with different types were attempted to be crossover-ed");
		}
		
		if(this.protein1.getType() != ParametersFromGUI.getSsType() && this.protein1.getType() != ParametersFromGUI.getpType() ){
			System.err.println("ProteinPair.crossover(): Protein types are invalid");
		}
		
		// This whole if/else statement is pretty confusing, why should we even continue of the proteins types don't match?
		if(this.protein1.getType() == ParametersFromGUI.getSsType()){
			length = this.protein1.getDirection().length();
			cross = r.nextInt(length);
		}
		else{
			do{
			length = this.protein1.getDirection().length();
			cross = r.nextInt(length);
			} while(this.inSS(cross));
			
		}
		
		// actually perform the crossover
		// after picking a random cross point, it merges the two strings.
		// protein 1 gets the first "half" of its original direction and the second "half" of protein 2s direction
		// substring method works different in C++ and java
		this.protein1.setDirection( direction1.substring(0, cross) + 
				direction2.substring(cross, direction2.length() ) );
		// protein 2 gets the opposite of above
		this.protein2.setDirection( direction2.substring(0, cross) + 
				direction1.substring( cross, direction1.length() ) );
		
		// iterate through the list
		// if we are at a point after the cross, then switch the ssInfo in each list to match the crossover we just performed
		for( int i = 0; i < ssInfoList1.size(); i++){
			if( ssInfoList1.get(i).getPosition() > cross ){
				SSInfo temp = ssInfoList1.get(i);
				ssInfoList1.set(i, ssInfoList2.get(i));
				ssInfoList2.set(i, temp);
			}
		}
		
		this.protein1.setSsList(ssInfoList1);
		this.protein2.setSsList(ssInfoList2);
	}
	
	/**
	 * I honestly have no idea what this method does. It seems to only be called 
	 * when two proteins whose type do not match are being crossover-ed.
	 *
	 * @param cross
	 * @return
	 */
	private boolean inSS(int cross) {
		ArrayList<SSInfo> ssList1 = this.protein1.getSsList();
		ArrayList<SSInfo> ssList2 = this.protein2.getSsList();
		for( int i = 0; i < ssList1.size(); i++ ){
			if( ssList1.get(i).isOn() || ssList2.get(i).isOn() ){
				if( ( cross >= ssList1.get(i).getPosition() ) 
						&& ( cross < ssList2.get(i).getPosition() + ssList1.get(i).getLength() ) ){
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * This method calls the mutate method on each protein of the protein pair
	 */
	public void mutate(MetaPop metaPop){
		this.protein1.mutate(metaPop);
		this.protein2.mutate(metaPop);
	}
	
	/**
	 * This method calls the fix method on each protein of the protein pair
	 * @param charges
	 * @return
	 */
	public boolean fix(String charges) {
		this.protein1.fix(charges);
		this.protein2.fix(charges);
		return true;
	}
	

	public Protein getProtein1() {
		return protein1;
	}

	public void setProtein1(Protein protein1) {
		this.protein1 = protein1;
	}

	public Protein getProtein2() {
		return protein2;
	}

	public void setProtein2(Protein protein2) {
		this.protein2 = protein2;
	}

	
	
}
