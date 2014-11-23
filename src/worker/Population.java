package worker;

import java.util.ArrayList;

/**
 * 
 * Population.java
 *
 * This class represents a population of proteins, used in the genetic algorithm.
 * It is responsible for generating and storing proteins
 *
 * @author Aaron Germuth
 * @date 2013-03-16
 */
public class Population {
	private int type;
	private int length;
	private ProteinList proteinList;
	
	public Population(int type){
		this.type = type;
		this.proteinList = new ProteinList();
	}
	
	public void printList(){
		proteinList.printList();
	}
	
	/**
	 * Generates a list of population with random SS structures
	 * @param type
	 * @param length
	 */
	public Population(int type, int length) {
		this.type = type;
		this.length = length;
		this.proteinList = new ProteinList();
		for (int i = 0; i < ParametersFromGUI.getPopulationSize(); i++) {
			Protein baby = new Protein(this.type);
			FoldModel alive = new FoldModel();
			alive.buildSS(this.length);
			baby.assign(alive);
			add(baby);
		}
	}
	
	public Population(int type, String charges, MetaPop metaPop){
		if(type != ParametersFromGUI.getpType()){
			System.err.println("Population constructor, wrong constructor used");
		}
		this.type = type;
		this.length = charges.length();
		this.proteinList = new ProteinList();
		
		//Scan the changes for possible secondary structures
		ArrayList<SSInfo> possibleSSList = new ArrayList<SSInfo>();
		int ssLength = 0;
		int ssPosition = 0;
		int lengthBefore = charges.length();

		charges += 'p'; // add sentinel value
		for (int i = 0; i < charges.length(); i++) {
			if (charges.charAt(i) == 'h') {
				if (ssLength == 0) {
					ssPosition = i;
				}
				ssLength++;
			} 
			else {
				if (ssLength > 1 && metaPop.hasLength(ssLength)) {
					SSInfo ssInfo = new SSInfo(ssPosition, ssLength, false);
					possibleSSList.add(ssInfo);
				}
				ssLength = 0;
			}
		}
		charges = charges.substring(0, charges.length() - 1); // remove sentinel
		int lengthAfter = charges.length();
		assert(lengthBefore == lengthAfter);
		
		//Fill the population with random P structures
		for(int growth = 0; growth < ParametersFromGUI.getPopulationSize(); growth++){
			Protein baby = new Protein(type);
			FoldModel alive = new FoldModel();
			alive.buildP(charges, metaPop, possibleSSList);
			baby.assign(alive);
			this.add(baby);
		}
	}
	
	public String toString(){
		return "Population Type: " + this.type + ", Length: " + this.length;
	}

	public void add(Protein protein){
		if(protein.getType() != this.type){
			System.err.println("Protein is the incorrect type. Population.add");
		}
		this.proteinList.insert(protein);
	}
	
	public boolean tryAdd(Protein protein){
		if(protein.getType() != this.type){
			System.err.println("Protein is the incorrect type. Population.tryAdd");
		}
		if(protein.getHealth() > this.proteinList.getWeakest().getHealth()){
			this.proteinList.insert(protein);
			this.proteinList.removeWeakest();
			return true;
		}
		return false;
	}
	
	public Protein getRand(){
		return this.proteinList.getRandom();
	}
	
	public Protein getNext(){
		return this.proteinList.getNext();
	}
	
	public Protein getBest(){
		return this.proteinList.getBest();
	}
	
	public Population getSubPopulation(int size){
		Population best = new Population(this.type);
		for(int pos = 0; pos < size; pos++){
			System.err.println("This method shouldn't ever be called anymore. Population.getSubPopulation");
		}
		return best;
	}
	
	/**
	 * Returns two random members of the population. These two members will become parents 
	 * to a new protein in the GA
	 */
	public ProteinPair getParents(){
		ProteinPair parents;
		Protein parent1 = new Protein(this.type);
		Protein parent2 = new Protein(this.type);
		parent1 = this.proteinList.getRandom();
		parent2 = this.proteinList.getRandom();
		parents = new ProteinPair(parent1, parent2);
		return parents;
	}
	/**
	 * Returns a random protein from population
	 * @return
	 */
	public Protein getSS(){
		//if(this.type != SS_TYPE)
		return this.proteinList.getRandom();
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public ProteinList getProteinList() {
		return proteinList;
	}
	
	public int getProteinListSize(){
		return proteinList.getLength();
	}
	
	public void setProteinList(ProteinList proteinList) {
		this.proteinList = proteinList;
	}
	
	
	
}
