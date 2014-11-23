package worker;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

/**
 * 
 * ParametersFromGUI.java
 *
 * This class represents all the parameters send from the GUI to the Worker. 
 * For now it just holds variables, but some other class will populate it values from the Socket/semaphore?
 *
 * @author Aaron Germuth, Lee Foster
 * @date 2013-03-16
 */
public class ParametersFromGUI {
	/**
	 * This string represents the protein charges
	 * ie. "hhphphphphpppphh"
	 */
	private static String charges = "";
	private static int mutateProbability = 20;
	private static int SSSwapProbability = 0;
	private static int SSIntroProbability = 50;
	private static int populationSize = 5;
	private static int geneticAlgorithmIterations = 2;
	private static int minSSLength = 4;
	private static int maxSSLength = 50;
	private static int maxSSPops = 10;
	private static int numPopulations = 1;
	private static boolean debug = false;
	private static boolean userSSOnly = false;
	private static Set<Integer> userSSLengths = new TreeSet<Integer>();
	/**
	 * How often the GA sends the protein structure to the GUI for it to be displayed
	 */
	private static int updateGUIInterval = 5;
	/**
	 * How often the GA sends it's current progress to the GUI
	 * Progress I believe meaning how far through the progress bar we are
	 */
	private static int updateGUIProgressInterval = 2;
	/**
	 * the current secondary structure type. Not exactly sure about this yet
	 * @return
	 */
	private static int ssType = 0;
	private static int pType = 1; 
	
	//Checks to see if the values from the GUI are allowable
	public static boolean checkParameters(String[] error_msg){
		error_msg[0] = "";
		if(userSSLengths != null){
		Iterator it = userSSLengths.iterator();
			while(it.hasNext()){
				int element = (Integer)it.next();
				if(element < 4){
			    	error_msg[0] = "ss-lengths must be integers greater than 4";
			    	return false;
				}
			}
		}
		
		if(mutateProbability < 0 || mutateProbability > 100){
		    error_msg[0] = "mutate probability must be an integer between 0 and 100 (inclusive)";
		    return false;
		}
		if(SSSwapProbability < 0 || SSSwapProbability > 100){
		    error_msg[0] = "ss-swap probability requires an integer between 0 and 100 (inclusive)";
		    return false;
		}
		if(SSIntroProbability < 0 || SSIntroProbability > 100){
		    error_msg[0] = "ss-intro probability requires an integer between 0 and 100 (inclusive)";
		    return false;
		}
		if(populationSize < 100){
		    error_msg[0] = "pop-size requires an integer greater than or equal to 100";
		    return false;
		}
		if(geneticAlgorithmIterations < 1){
		    error_msg[0] = "ga-iterations requires an integer greater than 0";
		    return false;
		}
		if(minSSLength < 3){
		    error_msg[0] = "min-ss-length requires an integer greater than 3";
		    return false;
		}
		if(maxSSLength < 5){
		    error_msg[0] = "max-ss-length requires an integer greater than 4";
		    return false;
		}
		if(maxSSPops < 0){
		    error_msg[0] = "max-ss-pops requires an integer greater or equal to 0";
		    return false;
		}
		if(numPopulations < 1){
		    error_msg[0] = "num_populations requires an integer greater than 0";
		    return false;
		}
		if(charges.length() == 0){
		    error_msg[0] = "charges must be a string of length greater than 0";
		    return false;
		}
		for(int i = 0; i <charges.length(); i++){
			if(charges.charAt(i) != 'h' && charges.charAt(i) != 'p'){
			      error_msg[0] = "charges must be a string consisting only of h's and p's";
			      return false;
			}
		}
		return true;		
	}
	/**
	 * This class manipulates Strings. a new string is inserted within the
	 * starting string at position start and copys over all of lengthReplaced.
	 * If the inputted string is longer than length Replaced, the extra
	 * characters are added anyways
	 * 
	 * @param directions
	 * @param start
	 * @param lengthReplaced
	 * @param newString
	 * @return
	 */
	public static char[] replace(char[] directions, int start, int lengthReplaced, String newString) {
		String starting = "";
		for (int i = 0; i < directions.length; i++) {
			starting += directions[i];
		}
		String answer = starting.substring(0, start);
		answer += newString;
		answer += starting.substring(start + lengthReplaced, starting.length());

		return answer.toCharArray();

	}
	
	//Prints out the parameters User to check to make sure the values were parsed propperly
	public static void printParameters(){
		System.out.println("charges: " + charges);
		System.out.println("mutateProbability: " + mutateProbability);
		System.out.println("SSSwapProbability: " + SSSwapProbability);
		System.out.println("SSIntroProbability: " + SSIntroProbability);
		System.out.println("PopulationSize: " + populationSize);
		System.out.println("GeneticAlgorithmIterations: " + geneticAlgorithmIterations);
		System.out.println("MinSSLength: " + minSSLength);
		System.out.println("MaxSSLength: " + maxSSLength);
		System.out.println("MaxSSPops: " + maxSSPops);
		System.out.println("UserSSOnly: " + userSSOnly);
		System.out.println("Debug: " + debug);
		System.out.print("UserSSLengths: ");
		
		Iterator it = userSSLengths.iterator();
		while(it.hasNext()){
			int element = (Integer)it.next();
			System.out.print(element + " ");
		}
		System.out.println("");
		
	}
	
	public static int getpType() {
		return pType;
	}

	public static void setpType(int pType) {
		ParametersFromGUI.pType = pType;
	}

	public static void insert(int n){
		userSSLengths.add(n);
	}
	
	public static String getCharges() {
		return charges;
	}
	public static void setCharges(String charges) {
		ParametersFromGUI.charges = charges;
	}
	public static int getMutateProbability() {
		return mutateProbability;
	}
	public static void setMutateProbability(int mutateProbability) {
		ParametersFromGUI.mutateProbability = mutateProbability;
	}
	public static int getSSSwapProbability() {
		return SSSwapProbability;
	}
	public static void setSSSwapProbability(int sSSwapProbability) {
		SSSwapProbability = sSSwapProbability;
	}
	public static int getSSIntroProbability() {
		return SSIntroProbability;
	}
	public static void setSSIntroProbability(int sSIntroProbability) {
		SSIntroProbability = sSIntroProbability;
	}
	public static int getPopulationSize() {
		return populationSize;
	}
	public static void setPopulationSize(int populationSize) {
		ParametersFromGUI.populationSize = populationSize;
	}
	public static int getGeneticAlgorithmIterations() {
		return geneticAlgorithmIterations;
	}
	public static void setGeneticAlgorithmIterations(int geneticAlgorithmIterations) {
		ParametersFromGUI.geneticAlgorithmIterations = geneticAlgorithmIterations;
	}
	public static int getMinSSLength() {
		return minSSLength;
	}
	public static void setMinSSLength(int minSSLength) {
		ParametersFromGUI.minSSLength = minSSLength;
	}
	public static int getMaxSSLength() {
		return maxSSLength;
	}
	public static void setMaxSSLength(int maxSSLength) {
		ParametersFromGUI.maxSSLength = maxSSLength;
	}
	public static int getMaxSSPops() {
		return maxSSPops;
	}
	public static void setMaxSSPops(int maxSSPops) {
		ParametersFromGUI.maxSSPops = maxSSPops;
	}
	public static int getNumPopulations() {
		return numPopulations;
	}
	public static void setNumPopulations(int numPopulations) {
		ParametersFromGUI.numPopulations = numPopulations;
	}
	public static boolean isDebug() {
		return debug;
	}
	public static void setDebug(boolean debug) {
		debug = debug;
	}
	public static boolean isUserSSOnly() {
		return userSSOnly;
	}
	public static void setUserSSOnly(boolean userSSOnly) {
		 userSSOnly = userSSOnly;
	}
	public static int getUpdateGUIInterval() {
		return updateGUIInterval;
	}
	public static void setUpdateGUIInterval(int updateGUIInterval) {
		ParametersFromGUI.updateGUIInterval = updateGUIInterval;
	}
	public static int getUpdateGUIProgressInterval() {
		return updateGUIProgressInterval;
	}
	public static void setUpdateGUIProgressInterval(int updateGUIProgressInterval) {
		ParametersFromGUI.updateGUIProgressInterval = updateGUIProgressInterval;
	}
	public static int getSsType() {
		return ssType;
	}
	public static void setSsType(int ssType) {
		ParametersFromGUI.ssType = ssType;
	}

	
	public static Set<Integer> getUserSSLengths() {
		return userSSLengths;
	}
		 
	public static void setUserSSLengths(Set<Integer> userSSLengths) {
		ParametersFromGUI.userSSLengths = userSSLengths;
	}

	
}
