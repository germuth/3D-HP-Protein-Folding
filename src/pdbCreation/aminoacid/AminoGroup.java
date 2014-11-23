package pdbCreation.aminoacid;

public class AminoGroup {
	private Coords nitrogen = new Coords();
	private int nitrogenNumber;
	private Coords firstHydrogen = new Coords( (-1.135 + 1.606), (1.772 - 0.811), (0.518 - 0.361));
	private Coords secondHydrogen = new Coords( (-2.523 + 1.606), (0.762 - 0.811), (0.933 - 0.361));
	private Atom[] atoms;
	private int numberAtoms;
	
	/**
	 * Amino group constructor
	 * @param nitrogen, coordinates of nitrogen atom
	 * @param atomNumber, starting atom number
	 * @param aminoAcidNumber, number of amino acid in the sequence
	 * @param first, whether this is the first amino acid in the sequence
	 */
	public AminoGroup(Coords nitrogen, int atomNumber, String aminoAcidNumber, String name, boolean first){
		if(first){
			this.atoms = new Atom[3];
			this.numberAtoms = 3;
		}
		else{
			this.atoms = new Atom[2];
			this.numberAtoms = 3;
		}
		this.nitrogen = nitrogen;
		this.firstHydrogen = new Coords( (0.471 + this.nitrogen.getX()),
				(0.961 + nitrogen.getY()),
				(0.157 + nitrogen.getZ()));
		this.secondHydrogen = new Coords( (-0.917 + nitrogen.getX()), 
				(-0.050 + nitrogen.getY()),
				(0.572 + nitrogen.getZ()));
		
		//add nitrogen
		Connection one = new Connection(atomNumber, atomNumber+1, atomNumber+2);
		this.atoms[0] = new Atom(atomNumber + "", "N", name, aminoAcidNumber, nitrogen, one);
		this.nitrogenNumber = atomNumber;
		
		//add first H
		Connection two = new Connection(atomNumber+1, atomNumber);
		this.atoms[1] = new Atom((atomNumber + 1 )+ "", "H", name, aminoAcidNumber, firstHydrogen, two);
		
		//if the first amino acid add extra hydrogen
		//else this hydrogen is removed to bond with previous amino acid
		if(first){
			//add final H
			Connection three = new Connection(atomNumber+2, atomNumber);
			this.atoms[2] = new Atom((atomNumber + 2) + "", "H", name, aminoAcidNumber, secondHydrogen, three);
		}
	}

	public Atom[] getAtoms() {
		return atoms;
	}

	public int getNumberAtoms() {
		return numberAtoms;
	}
	
	public int getNitrogenNumber(){
		return this.nitrogenNumber;
	}

}
