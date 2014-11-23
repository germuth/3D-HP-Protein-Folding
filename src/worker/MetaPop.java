package worker;
import java.util.LinkedList;
import java.util.List;
/**
 * 
 * MetaPop.java
 *
 * This class is a container for populations of the same type. 
 *
 * @author Aaron Germuth
 * @date 2013-03-16
 */

public class MetaPop {
	private int type;
	private Population originalPopulation;
	private List<Population> populationList;
	
	public MetaPop(int type){
		this.type = type;
		this.originalPopulation = null;
		this.populationList = new LinkedList<Population>();
	}
	
	public String toString(){
		return "MetaPop Type: " + this.type;
	}
	/**
	 * Used to set original population
	 * @param pop
	 */
	public void addOriginal(Population pop){
		//make sure population matches type
		if(this.type != pop.getLength()){
			System.err.println("Trying to add " + pop + " as origanal population to " + this + " when their types do not match");
		}
		this.originalPopulation = pop;
	}

	/**
	 * Add a population to MetaPop's populationList
	 */
	public void addPopulation(Population pop) {
		// make sure population matches type
		if (this.type != pop.getType()) {
			System.err.println("Trying to add " + pop
					+ " to population list of " + this
					+ " when their types do not match");
		}
		populationList.add(pop);
	}
	
	/**
	 * Returns a randomly selected Secondary Structure of specified length
	 * 
	 * @param length
	 * @return
	 */
	public Protein getSS(int length) {
		// if (this.type != SS_TYPE)
		// error("MetaPop::getSS: population is not SS_TYPE");

		for (int i = 0; i < this.populationList.size(); i++) {
			if (length == this.populationList.get(i).getLength()) {
				return this.populationList.get(i).getSS();
			}
		}

		return null;
	}
	
	/**
	 * getSuperPop - returns a population of specified size containing the best proteins
	 */
	public Population getSuperPop(int size){
		Population best = new Population(this.type);
		for(int i= 0; i < this.populationList.size(); i++){
			//best = best + 
		}
		System.err.println("I don't think this method is implement anymore. MetaPop.getSuperPop");
		return best; 
	}
	 
	 
	 
	
	/**
	 * if metapop is SS_type,
	 * Returns true if the metapop contains a population with proteins of specified length
	 */
	public boolean hasLength(int length){
		//if (this->type != SS_TYPE) error("MetaPop::getSizes: population is not SS_TYPE");
		for(int i = 0; i < this.populationList.size(); i++){
			if(this.populationList.get(i).getLength() == length){
				return true;
			}
		}
		return false;
	}
	public int getType() {
		return type;
	}
	public void setType(int type) {
		this.type = type;
	}
	public Population getOriginalPopulation() {
		return originalPopulation;
	}
	public void setOriginalPopulation(Population originalPopulation) {
		this.originalPopulation = originalPopulation;
	}
	public List<Population> getPopulationList() {
		return populationList;
	}
	public void setPopulationList(List<Population> populationList) {
		this.populationList = populationList;
	}
	
	
}
