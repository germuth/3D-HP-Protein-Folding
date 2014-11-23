package pdbCreation.aminoacid;
/**
 * Fixed Length String
 * 
 * This class represents a string, with a fixed length. If not enough characters are supplied to fill
 * the string, the string is pre-pended with empty spaces until it is full.
 * This is used to comply with pdb format
 * @author Aaron Germuth
 *
 */
public class FixedLengthString {
	/**
	 * Array of characters used to hold the string
	 */
	private char[] string;
	/**
	 * Length of string
	 */
	private final int length;
	
	/**
	 * Constructor. if no string is supplied, the fixed string is initially empty
	 * @param length, length of fixed string
	 */
	public FixedLengthString(int length){
		this.string = new char[length];
		this.length = length;
		
		//fill with empties
		for(int i = 0; i < this.string.length; i++){
			this.string[i] = ' ';
		}
	}
	
	/**
	 * Normal constructor.
	 * @param str, string you want to have fixed length
	 * @param length, length you want it to have.
	 * length must be equal or larger than str.length
	 */
	public FixedLengthString(String str, int length){
		this.length = length;
		this.string = new char[length];
		//calculate start of string
		int start = length - str.length();
		if(start < 0){
			System.err.println("Negative start point of fixed length string");
		}
		
		//fill with blank spaces
		for(int i = 0; i < start; i++){
			string[i] = ' ';
		}
		
		//fill in with string
		int k = 0;
		for(int i = start; i < string.length; i++){
			string[i] = str.charAt(k);
			k++;
		}
	}
	
	/**
	 * Print the fixed length string object. 
	 * If empty spaces appear before the string, they are outputted
	 * @return
	 */
	public String print(){
		String answer = "";
		for(int i = 0; i < string.length; i++){
			answer += this.string[i];
		}
		return answer;
	}

	/**
	 * Parses a double from a fixed length string. Only
	 * to be used when fixed length string holds an x y z coordinate
	 * @return, the parsed double 
	 */
	public double getDouble(){
		String answer = "";
		for(int i = 0; i < this.length; i++){
			answer += this.string[i];
		}
		answer = answer.trim();
		double d = Double.parseDouble(answer);
		return d;
	}
	
	public char[] getCharArray() {
		return string;
	}
	
	/**
	 * Replaces current contents with str
	 * @param str
	 */
	public void setString(String str) {
		int start = length - str.length();
		if(start < 0){
			System.err.println("Negative start point of fixed length string");
		}
		
		//fill with blank spaces
		for(int i = 0; i < start; i++){
			string[i] = ' ';
		}
		
		//fill in with string
		int k = 0;
		for(int i = start; i < string.length; i++){
			string[i] = str.charAt(k);
			k++;
		}
	}
}


