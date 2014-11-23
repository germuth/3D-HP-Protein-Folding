package pdbCreation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Scanner;

import pdbCreation.aminoacid.Atom;
import pdbCreation.aminoacid.Coords;

/**
 * PDB Editor
 * 
 * This class parses through a recently generated PDB file and makes some small adjustments.
 * The Main adjustment made now, is to scan through the file and locate the chiral carbon and the nitrogen
 * of two adjacent amino acids. It then uses their coordinates to better determine the proper location of
 * the oxygen in between them, and then moves the oxygen to that position. This process is repeated 
 * inbetween every adjacent amino acids to align all peptide bond oxygen's more correctly.
 * Also done for hydrogen atoms of amino nitrogens (not r group ones)
 * 
 * This is currently done not very efficient. The Editor parses through the entire file each time to make one adjustment, so
 * making multiple oxygen adjustments means scanning the file several times.
 * @author Aaron
 *
 */
public class PDBEditor {
	/**
	 * PDB File created in PDDCreater. Will be read (not edited) to create new editer pdb
	 */
	private File preProteinFile;
	/**
	 * PDB file created here
	 */
	private File proteinFile;
	/**
	 * Scanner used to read preProtein File
	 */
	private Scanner preProteinScanner;
	/**
	 * Scanner used to parse strings to get at certain words one at a time
	 */
	private Scanner lineScanner;
	/**
	 * Hash map used to store the new resulting position of edited atoms based on their unique atom number
	 */
	private HashMap<Integer, Coords> atomNumToVector = new HashMap<Integer, Coords>();
	/**
	 * The length of polypeptide in amino acids residues
	 */
	private int proteinLength;
	/**
	 * Holds the coordinates of the chiral Carbon found in the amino acid after the one we are currently visiting
	 */
	private Coords nextChiralCarbon;
	/**
	 * The bond length in between an acid carbon and it's double bonded oxygen in anstroms.
	 */
	private static final double DOUBLE_BONDED_OXYGEN_LENGTH = 1.2204056702588693;
	/**
	 * The bond length in between an amino nitrogen and it's hydrogen in anstroms
	 */
	private static final double NITROGEN_TO_HYDROGEN_LENGTH = 1.08175;
	
	/**
	 * Constructor
	 * @param length, the length of the protein in amino acid residues
	 */
	public PDBEditor(int length){
		this.proteinLength = length;
		this.nextChiralCarbon = null;
		preProteinFile = new File("preprotein.pdb");
		try {
			preProteinScanner = new Scanner(preProteinFile);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Parses through pdb file mulitple times and edits connections every time
	 * Method is messy, should be cleaned up
	 */
	public void edit() {
		for (int i = 1; i < proteinLength; i++) {
			try {
				preProteinFile = new File("preprotein.pdb");
				preProteinScanner = new Scanner(preProteinFile);
			} 
			catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			//variables should be declared where they are used, not all here
			boolean proline = false;
			// find atoms we need to find		
			Coords hydrogen = null;
			int hydrogenAtomNumber = -1;
			boolean hydrogenFound = false;
			
			int carbonAtomNumber = -1;
			Coords carbon = null;
			int oxygenAtomNumber = -1;
			Coords oxygen = null;
			int nitrogenAtomNumber = -1;
			Coords nitrogen = null;
			int chiralCarbonAtomNumber = -1;
			Coords chiralCarbon = null;
			boolean carbonFound = false;
			boolean oxygenFound = false;
			boolean nitrogenFound = false;
			boolean foundCCarbon = false;
			boolean nextAtom = false;
			bigwhile:
			while (preProteinScanner.hasNextLine()) {
				String line = preProteinScanner.nextLine();
				lineScanner = new Scanner(line);
				String tag = lineScanner.next();
				
				if(line.isEmpty()){
					continue;
				}
				
				if (tag.equals("ATOM")) {
					Scanner temp = new Scanner(line);
					temp.next();
					temp.next();
					temp.next();
					temp.next();
					int num = temp.nextInt();
					if (num != i) {
						line = nextAA(preProteinScanner);
						lineScanner = new Scanner(line);
						tag = lineScanner.next();
						
					}
				}
				if (!line.isEmpty() && tag.equals("ATOM")) {
					int atomNumber = lineScanner.nextInt();
					String in = lineScanner.next();
					
					if (!carbonFound && in.equals("C")) {
						carbonAtomNumber = atomNumber;
						carbonFound = true;
						if(lineScanner.next().equals("PRO")){
							proline = true;
						}
						lineScanner.next();
						carbon = new Coords(lineScanner.nextDouble(), lineScanner.nextDouble(),
								lineScanner.nextDouble());
					} else if (carbonFound && !foundCCarbon && in.equals("C")) {
						chiralCarbonAtomNumber = atomNumber;
						foundCCarbon = true;
						if(lineScanner.next().equals("PRO")){
							proline = true;
						}
						lineScanner.next();
						chiralCarbon = new Coords(lineScanner.nextDouble(),
								lineScanner.nextDouble(), lineScanner.nextDouble());
						
						String line2 = preProteinScanner.nextLine();
						Scanner s3 = new Scanner(line2);
						while(s3.next().equals("ATOM")){
							String line3 = preProteinScanner.nextLine();
							s3 = new Scanner(line3);
						}
						//search next amino acid and grab nitrogen and chiralcarbon and hydrogen on nitrogen
						boolean cFound = false;
						int carbons = 0;
						boolean nFound = false;
						boolean hFound = false;
						while(preProteinScanner.hasNextLine()){
							String l = preProteinScanner.nextLine();
							Scanner s4 = new Scanner(l);
							if(s4.next().equals("ATOM")){
								atomNumber = s4.nextInt();
								String name = s4.next();
								if(name.equals("N") && !nFound){
									if(s4.next().equals("PRO")){
										proline = true;
									}
									s4.next();
									nitrogen = new Coords(s4.nextDouble(), s4.nextDouble(),
											s4.nextDouble());
									nFound = true;
								}
								if(name.equals("H") && !hFound){
									if(s4.next().equals("PRO")){
										proline = true;
									}
									s4.next();
									hydrogenAtomNumber = atomNumber;
									hydrogen = new Coords(s4.nextDouble(), s4.nextDouble(),
											s4.nextDouble());
									hFound = true;
								}
								if(name.equals("C")){
									carbons++;
									if(carbons == 2){
										cFound = true;
										if(s4.next().equals("PRO")){
											proline = true;
										}
										s4.next();
										this.nextChiralCarbon = new Coords(s4.nextDouble(), s4.nextDouble(),
												s4.nextDouble());
										cFound = true;
									}
								}
								if(cFound && nFound && hFound){
									break bigwhile;
								}
							}
						}
					} else if (!oxygenFound && carbonFound && in.equals("O")) {
						oxygenAtomNumber = atomNumber;
						if(lineScanner.next().equals("PRO")){
							proline = true;
						}
						lineScanner.next();
						oxygen = new Coords(lineScanner.nextDouble(), lineScanner.nextDouble(),
								lineScanner.nextDouble());
						oxygenFound = true;
					}
				}
				if (!line.isEmpty() && tag.equals("CONECT")) {
					nextAtom = true;
				}
			}
			
			//adjust nitrogen
			adjustAtom(chiralCarbon, nitrogen, carbon, PDBEditor.DOUBLE_BONDED_OXYGEN_LENGTH, oxygenAtomNumber);
			
			// now deal with hydrogen
			// not done on proline because nitrogen has no extra hyrogen in bonded case
			if (!proline){ //this.nextChiralCarbon != null) {
				adjustAtom(carbon, this.nextChiralCarbon, nitrogen,
						PDBEditor.NITROGEN_TO_HYDROGEN_LENGTH, hydrogenAtomNumber);
			}
			
		}
		try {
			makeAdjustmentsInFile();
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * This method reads from the hash map set up earlier to determine what atoms if any to translate, and
	 * then translates them
	 * @throws IOException
	 */
	private void makeAdjustmentsInFile() throws IOException {
		proteinFile = new File("protein.pdb");
		FileWriter fw = new FileWriter(proteinFile);
		preProteinScanner = new Scanner(preProteinFile);
		while (preProteinScanner.hasNextLine()) {
			String line = preProteinScanner.nextLine();
			lineScanner = new Scanner(line);
			if (!line.isEmpty() && lineScanner.next().equals("ATOM")) {
				int nerd = lineScanner.nextInt();
				if (this.atomNumToVector.containsKey(nerd)) {
					line = line.substring(0, 30);
					Atom n = new Atom("1", "C", "TRP", "0001",
							this.atomNumToVector.get(nerd), null);
					line += n.getxPosition().print()
							+ n.getyPosition().print()
							+ n.getzPosition().print();
				}
			}
			fw.write(line);
			fw.write(System.getProperty("line.separator"));
		}
		fw.close();
		
	}

	/**
	 * This method adjusts an atom from it's original position to adopt a trigonal planar configuration.
	 * Currently used to move oxygens in the acid group of an amino acid, and hydrogens involved in amino groups
	 * @param left, Coords of atom on the left ex. chiral Carbon
	 * @param right, Coords of atom on the right ex. nitrogen
	 * @param middle, Coorsd of middle atom, ex. acid carbon
	 * @param bondLength, bondlength of bond, static variable of this class
	 * @param atomNumber, the number the atom we are moving in is in the pdb file
	 */
	private void adjustAtom(Coords left, Coords right, Coords middle,
			double bondLength, int atomNumber) {
		// below we do some vector math to determine the best position of an atom
		// first we consider the middle's position to be the origin. To do
		// this, we simply translate left and right by the origin
		
		// However, we don't want to edit the objects them selves
		// Translating both of them is equaivalent to subtracting the middle coordinates on each of them
		// left - middle and right - middle
		// but in the next step we add the together so it becomes
		// left - middle + right - middle or
		// left + right - 2 middle

		// We add the two vectors representing the left and the right
		// Vectors add tail to tip, so this resembles a long vector in between the two
		Coords together = new Coords(
				left.getX() + right.getX() - 2 * middle.getX(),
				left.getY() + right.getY() - 2 * middle.getY(), 
				left.getZ() + right.getZ() - 2 * middle.getZ());

		// if we take the negative of the vector, it now points the other way,
		// forming a "triangle"
		// This is an excellent position for the oxygen as it is the exact 3d opposite
		// of where the other two atoms are located, forming a trigonal planar arangement
		// which is seen in nitrogen atoms or in sp2 carbon atoms
		Coords longVector = new Coords(
				-together.getX(), 
				-together.getY(),
				-together.getZ());

		// this vector will be too long, so we divide it scalar-ly by the 
		// actual bond length it should be
		double magnitude = Math.sqrt(Math.pow(longVector.getX(), 2)
				+ Math.pow(longVector.getY(), 2)
				+ Math.pow(longVector.getZ(), 2));
		double scalar = bondLength / magnitude;
		// this gets us our resulting vector of proper length
		Coords finalVec = new Coords(longVector.getX() * scalar,
				longVector.getY() * scalar, longVector.getZ() * scalar);

		// however, we must translate back away from origin
		Coords adjustedVector = new Coords(finalVec.getX() + middle.getX(),
				finalVec.getY() + middle.getY(), finalVec.getZ()
						+ middle.getZ());
		//place resulting Coordinate in hash map to remember later
		this.atomNumToVector.put(atomNumber, adjustedVector);
	}

	/**
	 * Moves the scanner s3 to the next atom in the file. Returns the first line of the next atom
	 * @param s3, Scanner being moved
	 * @return str, the first line of the next atom
	 */
	private String nextAA(Scanner s3) {
		String line = null;
		while(s3.hasNextLine()){
			String temp = s3.nextLine();
			Scanner s = new Scanner(temp);
			while(s.next().equals("ATOM")){
				s = new Scanner(s3.nextLine());
			}
			line = s3.nextLine();
			s = new Scanner(line);
			String temp2 = s.next();
			while(temp2.equals("CONECT")){
				line = s3.nextLine();
				s = new Scanner(line);
				temp2 = s.next();
			}
			return line;
		}
		return line;
	}
	
	public File getFile(){
		return this.proteinFile;
	}
}
