package pdbCreation.pointoffset;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

import pdbCreation.aminoacid.Atom;
import pdbCreation.aminoacid.Coords;

/**
 * Rotate
 * 
 * Temporary class to rotate given pdb's to match format used here
 * @author Aaron
 *
 */
public class Rotate{
	
	public static void main(String[] args) {
		rotate();
	}

	public static void rotate() {
		FileWriter fw = null;
		Scanner s = null;
		Scanner s2 = null;
		try {
			File f = new File("leu1.pdb");
			File f2 = new File("leu2.pdb");
			fw = new FileWriter(f2);
			s = new Scanner(f);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		while(s.hasNextLine()){
			String line = s.nextLine();
			s2 = new Scanner(line);
			if(!line.isEmpty() && s2.next().equals("ATOM")){
				char axis = 'Y';
				double radians = -Math.PI / 15;
				s2.next();
				s2.next();
				s2.next();
				s2.next();
				double x = s2.nextDouble();
				double y = s2.nextDouble();
				double z = s2.nextDouble();
				
				double x2 = x;
				double y2 = y;
				double z2 = z;
				
				if (axis == 'X') {
					// y' = y*cos q - z*sin q
					// z' = y*sin q + z*cos q
					// x' = x
					y2 = y * Math.cos(radians) - z * Math.sin(radians);
					z2 = y * Math.sin(radians) + z * Math.cos(radians);
				} else if (axis == 'Y') {
					// z' = z*cos q - x*sin q
					// x' = z*sin q + x*cos q
					// y' = y
					z2 = z * Math.cos(radians) - x * Math.sin(radians);
					x2 = z * Math.sin(radians) + x * Math.cos(radians);
				} else if (axis == 'Z') {
					// x' = x*cos q - y*sin q
					// y' = x*sin q + y*cos q
					// z' = z
					x2 = x * Math.cos(radians) - y * Math.sin(radians);
					y2 = x * Math.sin(radians) + y * Math.cos(radians);
				}
				// make a coords object to format doubles
				Coords temp = new Coords(x2, y2, z2);
				line = line.substring(0, 30);
				Atom n = new Atom("1", "C", "TRP", "0001", temp, null);
				line += n.getxPosition().print() + n.getyPosition().print() + n.getzPosition().print();
			}
			try {
				fw.write(line);
				fw.write(System.getProperty("line.separator"));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
		}
		try {
			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		s2.close();
	}
}