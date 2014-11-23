package pdbCreation.pointoffset;
import java.util.Scanner;

/**
 * Ofsett Calculator
 * 
 * Temporary Class to calculate relative positions of amino acids
 * @author Administrator
 *
 */
public class OffsetCalculator {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub	
		Scanner s = new Scanner(System.in);
		String input = s.nextLine();
		
		Scanner s2 = new Scanner(input);
		double x = s2.nextDouble();
		double y = s2.nextDouble();
		double z = s2.nextDouble();
		
		while(s.hasNextLine()){
			String line = s.nextLine();
			while(line.isEmpty()){
				line = s.nextLine();
			}
			s2 = new Scanner(line);
			System.out.print(s2.nextDouble() - x);
			System.out.print(" ");
			System.out.print(s2.nextDouble() - y);
			System.out.print(" ");
			System.out.print(s2.nextDouble() - z);
			System.out.print(" ");
			System.out.println(" " );
		}
		s.close();
		s2.close();
	}

}
