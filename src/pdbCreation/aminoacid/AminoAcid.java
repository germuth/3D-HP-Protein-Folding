package pdbCreation.aminoacid;

import pdbCreation.RotationParameter;

/**
 * This class reprents a basic amino acid. 
 * It makes use of an amino group, and acid group and is extended to create the individual r group of each amino acid
 * @author Aaron
 *
 */
public class AminoAcid {
	/**
	 * Amino group of this amino acid
	 */
	private AminoGroup amino;
	/**
	 * Acid group of this amino acid
	 */
	private AcidGroup acid;
	/**
	 * chiral (middle) carbon of this amino acid.
	 * Connects to the amino group, acid group, extra hydrogen and r group
	 */
	private Coords chiralCarbon; 
	/**
	 * Extra hydrogen attached to chiral carbon
	 */
	private Coords hydrogen;
	/**
	 * The atom number of the chiral carbon. Used for maintaining correct
	 * atom numbers
	 */
	private int chiralCarbonNumber;
	/**
	 * The number of atoms in this amino acid
	 */
	private int numberAtoms;
	/**
	 * Array of all atoms in this amino acid
	 */
	private Atom[] atoms;
	/**
	 * The three letter code of this amino acid.
	 */
	private String name;;
	/**
	 * The number of this amino acid in the protein. Stored in a string
	 * with pre pending zeros up to a size of 4
	 */
	private String number;
	/**
	 * How many amino acids are in this protein
	 */
	private static int numAminoAcids;
	/**
	 * Constructor for amino acid
	 * @param chiralCarbon, the coordinates of the chiral carbon. From these coordinates the rest of the 
	 * atoms coordinates can be figured out
	 * @param atomNumber, the starting atom number of this amino acid
	 * @param aminoAcidNumber, which number in the protein this amino acid is
	 * @param name, the three letter code of this amino acid. set by extending object
	 */
	public AminoAcid(Coords chiralCarbon, int atomNumber, int aminoAcidNumber, String name){
		this.setName( name );
		
		// for loops start at 0
		// amino acid numbers start at 1
		aminoAcidNumber++;
		this.number = getAANumber(aminoAcidNumber);

		//atoms 1, 2, 3
		//create amino group at relative coords
		Coords aminoNitrogen = new Coords(
				-0.914 + chiralCarbon.getX(), 
				1.094 + chiralCarbon.getY(),
				-0.387 + chiralCarbon.getZ());
		this.amino = new AminoGroup(aminoNitrogen, atomNumber, this.getNumber(), name, aminoAcidNumber==1);
		
		atomNumber += this.amino.getNumberAtoms();
		
		//atoms 4, 5, 6, 7
		//create acid group at relative coords
		Coords acidCarbon = new Coords(
				1.240 + chiralCarbon.getX(),
				0.048 + chiralCarbon.getY(),
				-0.851 + chiralCarbon.getZ());
		this.acid = new AcidGroup(acidCarbon, atomNumber, this.getNumber(), name, aminoAcidNumber==numAminoAcids);
		
		atomNumber += this.amino.getNumberAtoms() + 1;
		
		//atom 8
		//chiral carbon
		this.chiralCarbon = chiralCarbon;
		this.chiralCarbonNumber = atomNumber;
		Connection main = new Connection(atomNumber, this.amino.getNitrogenNumber(), this.acid.getAcidCarbonNumber(), atomNumber + 1, atomNumber + 2);
		Atom mainCarbon = new Atom((atomNumber) + "", "C", name, this.number, this.chiralCarbon, main);
		
		atomNumber++;
		
		//atom 9
		//hydrogen
		this.hydrogen = new Coords(
				0.266 + chiralCarbon.getX(),
				0.077 + chiralCarbon.getY(),
				1.068 + chiralCarbon.getZ());
		Connection connection = new Connection(atomNumber, atomNumber - 1);
		Atom hydrogen = new Atom((atomNumber) + "", "H", name, this.number, this.hydrogen, connection);
		
		//create atom array-----------------------------------------
		Atom[] firsta = amino.getAtoms();
		Atom[] second = acid.getAtoms();
		Atom[] all = new Atom[firsta.length + second.length + 2];
		
		//Atom[] all = new Atom[first.length + 2];
		System.arraycopy(firsta, 0, all, 0, firsta.length);
		System.arraycopy(second, 0, all, firsta.length, second.length);
		
		all[all.length - 2] = mainCarbon;
		all[all.length - 1] = hydrogen;
		this.atoms = all;
		
		this.numberAtoms = this.amino.getNumberAtoms() + this.acid.getNumberAtoms() + 3;
	}
	
	/**
	 * Takes in an integer and returns a string of the from "0xxx"
	 * where there are four places.
	 * @param aminoAcidNumber, integer you want formatted
	 * @return
	 */
	private String getAANumber(int aminoAcidNumber) {
		if(aminoAcidNumber > 9999){
			System.err.println("Invalid amino acid number. It is 10000 or greater");
		}
		String num = aminoAcidNumber + "";
		
		while(num.length() != 4){
			num = "0" + num;
		}
		return num;
	}
	/**
	 * Rotates an entire amino acid. Does this by translating the amino acid to the origin, rotating, and 
	 * then translating back to original spot
	 * @param axis
	 * @param radians
	 */
	public void rotate(RotationParameter rp) {
		//remember original
		double xOffset = this.chiralCarbon.getX();
		double yOffset = this.chiralCarbon.getY();
		double zOffset = this.chiralCarbon.getZ();
		
		//translate to origin
		Atom[] atoms = this.getAtoms();
		for(int i = 0; i < atoms.length; i++){
			Atom current = atoms[i];
			current.translate(xOffset, yOffset, zOffset);
		}
		
		//rotate
		for(int i = 0; i < atoms.length; i++){
			Atom current = atoms[i];
			current.rotate(rp.getAxis(), rp.getRadians());
		}
		
		//translate back
		for(int i = 0; i < atoms.length; i++){
			Atom current = atoms[i];
			current.translate(-xOffset, -yOffset, -zOffset);
		}
		
	}

	public Atom[] getAtoms(){
		return this.atoms;
	}
	
	public void setAtoms(Atom[] a){
		this.atoms = a;
	}
	
	public void setNumberAtoms(int num){
		this.numberAtoms = num;
	}
	
	public int getNumberAtoms(){
		return this.numberAtoms;
	}
	
	public int getNitrogenNumber(){
		return this.amino.getNitrogenNumber();
	}
	
	public int getAcidCarbonNumber(){
		return this.acid.getAcidCarbonNumber();
	}
	
	public int getChiralCarbonNumber(){
		return this.chiralCarbonNumber;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		if(name.length() != 3){
			System.out.println("Invalid name used. Must be of the form \"XXX\"");
		}
		this.name = name;
	}

	public static int getNumAminoAcids() {
		return numAminoAcids;
	}

	public static void setNumAminoAcids(int numAminoAcids) {
		AminoAcid.numAminoAcids = numAminoAcids;
	}

	public String getNumber() {
		return number;
	}

	public void setNumber(String number) {
		this.number = number;
	}

}
