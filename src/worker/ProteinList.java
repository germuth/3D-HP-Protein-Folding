package worker;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * 
 * ProteinList.java
 *
 * This class represents a list of proteins Objects. 
 * They are ordered based on their health
 * beginning of list is most unhealthy, 
 * end of list is healthiest
 * 
 *
 * @author Aaron Germuth
 * @date 2013-03-16
 */
public class ProteinList {
	/**
	 * The list of proteins
	 * 
	 * currently a linkedList
	 */
	private List<Protein> theList;
	/**
	 * Iterator for list
	 */
	private int iterator;
	
	
	/**
	 * default constructor
	 */
	public ProteinList(){
		this.theList = new LinkedList<Protein>();
		this.iterator = 0;
	}
	
	public void printList(){
		for(int i = 0; i<theList.size(); i++){
			System.out.println(theList.get(i).toString());
		}
	}
	
	/**
	 * Insert Protein in the list, it maintains sorted order
	 * right now this is done not the best
	 * 
	 * @param p
	 */
	public void insert(Protein p) {
		this.theList.add(p);
		Collections.sort(this.theList, new Comparator<Protein>() {
			@Override
			public int compare(Protein one, Protein two) {

				if (one.getHealth() < two.getHealth()) {
					return -1;
				}
				if (one.getHealth() > two.getHealth()) {
					return 1;
				}
				return 0;
			}
		});
	}
	
	/**
	 * returns the protein at the list iterator, and increments the iterator
	 * @return
	 */
	public Protein getNext(){
		Protein p = this.theList.get(this.iterator);
		this.iterator++;
		return p;
	}
	
	/**
	 * returns random element of the list
	 * @return
	 */
	public Protein getRandom(){
		Random r = new Random();
		int random = r.nextInt(theList.size());
		return this.theList.get(random);
	}
	
	/**
	 * returns highest health protein in the list
	 * @return
	 */
	public Protein getBest(){
		return this.theList.get(this.theList.size() - 1);
	}
	
	/**
	 * get the weakest health protein
	 */
	public Protein getWeakest(){
		return this.theList.get(0);
	}

	/**
	 * remove weakest health protein
	 */
	public void removeWeakest(){
		this.theList.remove(0);
	}
	/**
	 * Temp toString method to make sure list is always sorted by health
	 */
	public String toString(){
		String answer = "Protein List: ";
		for(int i = 0; i < this.theList.size(); i++){
			answer += this.theList.get(i).getHealth() + ", "; 
		}
		return answer;
		
	}
	
	public int getLength(){
		return theList.size();
	}
}
