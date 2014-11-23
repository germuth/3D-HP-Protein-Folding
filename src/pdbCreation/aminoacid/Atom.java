package pdbCreation.aminoacid;
/**
 * Atom
 * 
 * Represents an atom of an amino acid. Atom's fields are store in FixedLengthStrings to allow easy printing to 
 * the protein data base file.
 * @author Aaron Germuth
 *
 */
public class Atom {
	/**
	 * Title Tag used in protein date base files
	 */
	private static FixedLengthString title = new FixedLengthString("ATOM  ", 6);
	
	/**
	 * The Number of this atom. Atom number used in pdb file to keep track
	 * of bonds. 
	 */
	private FixedLengthString number = new FixedLengthString(5);
	/**
	 * Name of this atom. Names are usually one letter abbreviations.
	 * For example, C = Carbon, O = Oxygen, H = Hydrogen
	 */
	private FixedLengthString name = new FixedLengthString(3);
	/**
	 * Alternate Location tag of pdb file. Not Used.
	 */
	private FixedLengthString alternateLocation = new FixedLengthString(3);
	/**
	 * Names the amino acid this atom is part of. Names are in the form of 
	 * three letter amino acid abbreviations such as LYS, VAL, or GLY
	 */
	private FixedLengthString residueName = new FixedLengthString(3);
	/**
	 * Chain Identifier tag of pdb file. Not Used.
	 */
	private FixedLengthString chainIdentifier = new FixedLengthString(2);
	/**
	 * The Number that this amino acid is in the protein sequence. Numbers 
	 * are kept in "000x" form. For example, 0002 or 0032
	 */
	private FixedLengthString residueSequenceNumber = new FixedLengthString(4);
	/**
	 * Code for insertion of residues tag. Not used.
	 */
	private FixedLengthString codeForInsertionOfResidues = new FixedLengthString(4);
	/**
	 * The x-position of this atom to four decimal places.
	 */
	private FixedLengthString xPosition = new FixedLengthString(8);
	/**
	 * The y-position of this atom to four decimal places.
	 */
	private FixedLengthString yPosition = new FixedLengthString(8);
	/**
	 * The z-position of this atom to four decimal places
	 */
	private FixedLengthString zPosition = new FixedLengthString(8);
	/**
	 * A Connection object holding all the atoms this atom is bonded too
	 */
	private Connection bonds;
	
	/**
	 * constructor for Atom
	 * @param number, the atom number of this atom
	 * @param name, the name of this atom
	 * @param residueName, name of amino acid this atom is a part of
	 * @param residueSequenceNumber, number of amino acid this atom is a part of
	 * @param coords, the coordinates of this atom
	 * @param bonds, the Connection object for this atom
	 */
	public Atom(String number, String name, String residueName, 
			String residueSequenceNumber, Coords coords, Connection bonds){
		this.number.setString(number);
		this.name.setString(name);
		this.residueName.setString(residueName);
		this.residueSequenceNumber.setString(residueSequenceNumber);
		//coords.set automatically formats to 4 decimals places
		this.xPosition.setString(coords.getX() + "");
		this.yPosition.setString(coords.getY() + "");
		this.zPosition.setString(coords.getZ() + "");
		
		this.bonds = bonds;
	}
	
	/**
	 * Print atom
	 * Used to print this atom. Returns a string of pdb format
	 * @return
	 */
	public String printAtom(){
		String answer = "";
		answer += Atom.title.print();
		answer += this.number.print();
		answer += this.name.print();
		answer += this.alternateLocation.print();
		answer += this.residueName.print();
		answer += this.chainIdentifier.print();
		answer += this.residueSequenceNumber.print();
		answer += this.codeForInsertionOfResidues.print();
		answer += this.xPosition.print();
		answer += this.yPosition.print();
		answer += this.zPosition.print();
		return answer;
	}
	
	/**
	 * Translate atom by taking it's position, and subtracting offests given
	 */
	public void translate(double xOffset, double yOffset, double zOffset){
		double x = this.xPosition.getDouble();
		double y = this.yPosition.getDouble();
		double z = this.zPosition.getDouble();
		
		double x2 = x - xOffset;
		double y2 = y - yOffset;
		double z2 = z - zOffset;
		
		// make a coords object to format doubles
		Coords temp = new Coords(x2, y2, z2);
		// set new coordinates
		this.xPosition.setString(temp.getX() + "");
		this.yPosition.setString(temp.getY() + "");
		this.zPosition.setString(temp.getZ() + "");

	}

	/**
	 * Rotates this atom around either the x y or z axis, by an amount in radians
	 * @param axis, the axis you are rotating around, may be 'X', 'Y', or 'Z'
	 * @param radians, the degree in radians you want to rotate around it
	 */
	public void rotate(char axis, double radians) {
		double x = this.xPosition.getDouble();
		double y = this.yPosition.getDouble();
		double z = this.zPosition.getDouble();
		
		double x2 = x;
		double y2 = y;
		double z2 = z;
		
		if (axis == 'X') {
			// y' = y*cos q - z*sin q
			// z' = y*sin q + z*cos q
			// x' = x
			y2 = y * Math.cos(radians) - z * Math.sin(radians);
			z2 = y * Math.sin(radians) + z * Math.cos(radians);
		}
		else if(axis == 'Y'){
			//z' = z*cos q - x*sin q
			//x' = z*sin q + x*cos q
			//y' = y
			z2 = z * Math.cos(radians) - x * Math.sin(radians);
			x2 = z * Math.sin(radians) + x * Math.cos(radians);
		}
		else if(axis == 'Z'){
			//x' = x*cos q - y*sin q
			//y' = x*sin q + y*cos q 
			//z' = z
			x2 = x * Math.cos(radians) - y * Math.sin(radians);
			y2 = x * Math.sin(radians) + y * Math.cos(radians);
		}
		// make a coords object to format doubles
		Coords temp = new Coords(x2, y2, z2);
		// set new coordinates
		this.xPosition.setString(temp.getX() + "");
		this.yPosition.setString(temp.getY() + "");
		this.zPosition.setString(temp.getZ() + "");

	}

	public Connection getConnection(){
		return this.bonds;
	}

	public FixedLengthString getxPosition() {
		return xPosition;
	}

	public void setxPosition(FixedLengthString xPosition) {
		this.xPosition = xPosition;
	}

	public FixedLengthString getyPosition() {
		return yPosition;
	}

	public void setyPosition(FixedLengthString yPosition) {
		this.yPosition = yPosition;
	}

	public FixedLengthString getzPosition() {
		return zPosition;
	}

	public void setzPosition(FixedLengthString zPosition) {
		this.zPosition = zPosition;
	}

	public FixedLengthString getAlternateLocation() {
		return alternateLocation;
	}

	public void setAlternateLocation(FixedLengthString alternateLocation) {
		this.alternateLocation = alternateLocation;
	}
	
	
	
}
