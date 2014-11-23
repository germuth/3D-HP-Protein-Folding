package pdbCreation.aminoacid;

public class AcidGroup {
	private Coords carbon;
	private Coords doubleOxy;
	private Coords singleOxy;
	private Coords hydrogen;
	private Atom[] atoms;
	private int numberAtoms;
	private int acidCarbonNumber;
	
	/**
	 * Constructor for acid group
	 * oxygen is 1.2204056702588693 away
	 * @param carbon, coordinates of acid carbon 
	 * @param atomNumber, atom number
	 * @param amicoAcidNumber, amino acid number
	 * @param last, whether this is part of the last amino acid
	 */
	public AcidGroup(Coords carbon, int atomNumber, String aminoAcidNumber, String name, boolean last){
		if(last){
			this.atoms = new Atom[4];
			this.numberAtoms = 4;
		}
		else{
			this.atoms = new Atom[2];
			this.numberAtoms = 4;
		}
		//add carbon
		this.carbon = carbon;
		Connection one = new Connection(atomNumber, atomNumber+1, atomNumber+1, atomNumber+2);
		this.atoms[0] = new Atom(atomNumber + "", "C", name, aminoAcidNumber, this.carbon, one);
		this.acidCarbonNumber = atomNumber;
		
		//add double bonded oxy
		this.doubleOxy = new Coords( 1.125 + carbon.getX(),
				0.006 + carbon.getY(),
				0.473 + carbon.getZ() );
		Connection two = new Connection(atomNumber + 1, atomNumber);
		this.atoms[1] = new Atom((atomNumber + 1 )+ "", "O", name, aminoAcidNumber, doubleOxy, two);
		
		//only if the last amino acid in chain
		//otherwise atoms are removed to connect to next amino acid
		if(last){
			//add single bonded oxy
			this.singleOxy = new Coords( -0.199 + carbon.getX(),
				0.032 + carbon.getY(),
				-1.315 + carbon.getZ() );
			Connection three = new Connection(atomNumber + 2, atomNumber);
			this.atoms[2] = new Atom((atomNumber + 2) + "", "O", name, aminoAcidNumber, singleOxy, three);
		
			//add hydrogen on single bonded oxygen
			this.hydrogen = new Coords( 0.640 + carbon.getX(),
				0.059 + carbon.getY(),
				-1.760 + carbon.getZ() );
			Connection four = new Connection(atomNumber + 3, atomNumber + 2);
			this.atoms[3] = new Atom((atomNumber + 3 )+ "", "H", name, aminoAcidNumber, hydrogen, four);
		}
	}
	
	public Atom[] getAtoms() {
		return this.atoms;
	}

	public int getNumberAtoms() {
		return numberAtoms;
	}

	public int getAcidCarbonNumber() {
		return acidCarbonNumber;
	}
}
