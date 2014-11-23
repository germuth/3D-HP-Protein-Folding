package worker;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

/**
 * 
 * GeneticAlgorithm.java
 *
 * This class represents a genetic algorithm used to fold proteins. It makes populations, grab two random parents,
 * mutates them, fixes them, and crossover's them to create a child.
 *
 * @author Aaron Germuth
 * @date 2013-03-16
 */
public class GeneticAlgorithm {
	/**
	 * This method checks if a secondary structure made in this algorithm meets the inputed requirements given in the GUI
	 * Right now it's private, might have to change to public
	 * @param length
	 * @return
	 */
	private static boolean isUsableSS(int length){
		int globalMinLength = ParametersFromGUI.getMinSSLength();
		int globalMaxLength = ParametersFromGUI.getMaxSSLength();
		if( length < globalMinLength || length > globalMaxLength){
			return false;
		}
		return true;
	}

	public static Set<Integer> findSSPositions() {
		boolean inSS = false;
		int start = 0;
		int end = 0;
		Set<Integer> ssLengths = new TreeSet<Integer>();
		String charges = ParametersFromGUI.getCharges();

		// if user opts to use only their SS-lenghts, return
		if (!ParametersFromGUI.isUserSSOnly()) {

			// add sentinel
			charges += 'p';

			// when user does not specify lengths
			if (!ParametersFromGUI.isUserSSOnly()) {
				for (int i = 0; i < charges.length(); i++) {
					if (charges.charAt(i) == 'h' && inSS == false) {
						start = i;
						inSS = true;
					}
					if (charges.charAt(i) == 'p' && inSS == true) {
						end = i;
						// if the SS meets requirements, add length to list
						if (isUsableSS(end - start)) {
							ssLengths.add(end - start);
							inSS = false;
						}
					}
				}
			}
			//remove sentinel
			charges = charges.substring(0, charges.length() - 1);
		}
		
		while(ssLengths.size() > ParametersFromGUI.getMaxSSPops() + ParametersFromGUI.getUserSSLengths().size() ){
			//TODO i think this is erasing first element of list
			//ssLengths.erase(ssLenghts.begin() )
			ssLengths.remove(0);
		}
		
		
		//TODO still have no clude why this was needed
		/**
		//TODO
		//I think this is adding the last element of global to first position of string
		// ss_lengths.insert(global::user_ss_lengths.begin(),global::user_ss_lengths.end());
		// .insert(int position, string)
		Set<Integer> list = ParametersFromGUI.getUserSSLengths();
		Object[] array = list.toArray();
		//ssLengths.add(0, (int) array[array.length - 1]);
		ssLengths.add((int) array[array.length - 1]);
		*/
		
		
		return ssLengths;
	}
	
	/**
	 * Predict
	 */
	public static Population predict(ParentSocket socket){
		
		Set<Integer> ssLengths = findSSPositions();
		
		//Secondary structure populations
		MetaPop metaPop = new MetaPop(ParametersFromGUI.getSsType());
		Iterator<Integer> i = ssLengths.iterator();
		
		while(i.hasNext()){
			int current = (Integer) i.next();
			System.out.println("Generating initial SS population of length " + current);
			Population pop = new Population(ParametersFromGUI.getSsType(), current);
			System.out.println("Running Genetic Algorithm on SS population");
			if( ! geneticAlgorithm(socket, pop, metaPop) ){
				return null;
			}
			metaPop.addPopulation(pop);
		}
		
		
		
		System.out.println("Generating main population");
		Population pop = new Population(ParametersFromGUI.getpType(), 
				ParametersFromGUI.getCharges(), metaPop);
		System.out.println("Running Genetic Algorithm on main population");
		if( !geneticAlgorithm(socket, pop, metaPop)){
			return null;
		}
		return pop;
		
	}

	/**
	 * The genetic algorithm
	 * 
	 * I think we should change the limit of noImprovementStreak from hard coded
	 * 10000, to a user defined variable
	 * 
	 * @param the
	 *            Socket to send progress to
	 * @param population
	 *            we are optimizing
	 * @param metaPop
	 *            , a container for our populations
	 * @return
	 */
	public static boolean geneticAlgorithm(ParentSocket socket, Population population, MetaPop metaPop){
		// the best health of a protein encountered so far
		int bestHealth;
		// how many iterations we have done without improvement
		int noImprovementStreak = 0;
		// current iteration of genetic algorithm
		int i = 0;
		// how many seconds between sending client results
		int updateInterval = ParametersFromGUI.getUpdateGUIInterval();
		long updateIntervalLastUpdate = System.currentTimeMillis()/1000;
		long updateIntervalCurrentTime;
		//
		int updateTime = 0;
		// how many seconds between sending client progress (%)
		int progressInterval = ParametersFromGUI.getUpdateGUIProgressInterval();
		long progressIntervalLastUpdate = System.currentTimeMillis()/1000;
		long progressIntervalCurrentTime;
		// iterations of Genetic Alogirithm
		int gaIterations = ParametersFromGUI.getGeneticAlgorithmIterations();
		// percent completed the genetic algorithm
		int percent;
		// 
		ProteinPair children;
		// a time object?, might have to create a time object
		int seconds = 0; 
		//
		String receivedMsg;
		
		while( i < gaIterations && noImprovementStreak < 10000){
			// this should all be put in one method, called sendProgressReport
			progressIntervalCurrentTime = System.currentTimeMillis()/1000;
			if( progressIntervalCurrentTime - progressIntervalLastUpdate >= progressInterval ){
				percent = (int) ( (double) i / (double) gaIterations*100 );
				progressIntervalLastUpdate = progressIntervalCurrentTime;
				//
		    	String task;
		    	// not sure how metapop evaluates to a boolean, weird C++ stuff
		    	//if (!metaPop) task = "ss"; else task = "p";
		    	// will assume this means if metaPop not null
		    	if( metaPop == null){
		    		task = "ss";
		    	}
		    	else{
		    		task = "p";
		    	}
		    	// build message to be sent to client
		    	String progressMsg = "";
		    	progressMsg += "<status>";
		    		progressMsg += "<worker>";
		    		// fake process ID
		    		progressMsg += "1";
		    		progressMsg += "</worker>";
		    		
		    		progressMsg += "<task>";
		    		progressMsg += task;
		    		progressMsg += "</task>";
		    		
		    		progressMsg += "<percent>";
		    		progressMsg += percent;
		    		progressMsg += "</percent>";
		    	progressMsg += "</status>";
		      
				socket.sendMessage(progressMsg);

				boolean next;

				//TODO
				//this was a loop in C++ program, should make sure this works
				receivedMsg = socket.receiveMessage();
				if (!receivedMsg.equals("ok")) {
					System.out.println("Aborting job");
					return false;
				}
			}
			
			//grab two random parents
			children = population.getParents();
			Protein child1 = new Protein( children.getProtein1() );
			Protein child2 = new Protein( children.getProtein2() );
			
			// and crosses them over
			//TODO documentation mentioned crossover no longer performed on 3d structure
			//children.crossover();
			
			//mutate both parents (change the 3D structure)
			children.mutate(metaPop);
			
			if(!children.fix(ParametersFromGUI.getCharges())){
				continue;
			}
			
			//add original protein if better than mutated
			if(child1.getHealth() > children.getProtein1().getHealth()){
				population.getProteinList().insert(child1);
				population.getProteinList().removeWeakest();
			}
			if(child2.getHealth() > children.getProtein2().getHealth()){
				population.getProteinList().insert(child2);
				population.getProteinList().removeWeakest();
			}
			
			if(population.tryAdd(children.getProtein1())){
				updateIntervalCurrentTime = System.currentTimeMillis()/1000;
				if( updateIntervalCurrentTime - updateIntervalLastUpdate >= updateInterval && metaPop != null){
					socket.sendProtein(children.getProtein1());
					updateIntervalLastUpdate = updateIntervalCurrentTime;
				}
				noImprovementStreak = 0;
			}
			else{
				noImprovementStreak++;
			}
			
			if(population.tryAdd(children.getProtein2())){
				if( seconds >= updateTime && metaPop != null){
					socket.sendProtein(children.getProtein2());
					updateTime = seconds + updateInterval;
				}
				noImprovementStreak = 0;
			}
			else{
				noImprovementStreak++;
			}
			
			//seconds = time(null) ??
			seconds = 0;
			i++;
		}
		
		String task;
		if(metaPop == null){
			task = "ss"; 
		}
		else{
			task = "p";
		}
		String progressMsg = "";
		
		progressMsg += "<status>";
			progressMsg += "<worker>";
			// fake process ID
			progressMsg += "1";
			progressMsg += "</worker>";
		
			progressMsg += "<task>";
			progressMsg += task;
			progressMsg += "</task>";
		
			progressMsg += "<percent>";
			progressMsg += 100;
			progressMsg += "</percent>";
		progressMsg += "</status>";
		
		socket.sendMessage(progressMsg);
		
		return true;
	}
}
