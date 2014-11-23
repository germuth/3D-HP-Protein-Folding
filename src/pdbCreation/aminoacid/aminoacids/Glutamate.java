package pdbCreation.aminoacid.aminoacids;

import pdbCreation.aminoacid.AminoAcid;
import pdbCreation.aminoacid.Atom;
import pdbCreation.aminoacid.Connection;
import pdbCreation.aminoacid.Coords;

public class Glutamate extends AminoAcid{
	private Coords carbon;
	private Coords hydrogen;
	private Coords hydrogen2;
	private Coords carbon2;
	private Coords carbon2hydrogen1;
	private Coords carbon2hydrogen2;
	private Coords carbon3;
	private Coords doubleoxygen;
	private Coords singleoxygen;
	private Coords singleoxygenhydrogen;
	
	private static int rgroupAtomNumber = 10;
	public Glutamate(Coords chiralCarbon, int atomNumber, int aminoAcidNumber) {
		super(chiralCarbon, atomNumber, aminoAcidNumber, "GLU");
		

		this.carbon = new Coords(
				-0.666 + chiralCarbon.getX(),
				-1.401 + chiralCarbon.getY(),
				-0.086 + chiralCarbon.getZ());
		atomNumber += super.getNumberAtoms();
		
		Connection ch3 = new Connection(atomNumber - 1, super.getChiralCarbonNumber(), atomNumber, atomNumber+1, atomNumber+2);
		Atom carbon = new Atom((atomNumber - 1) + "", "C", "GLU", super.getNumber(), this.carbon, ch3);
		

		this.hydrogen = new Coords(
				-1.554 + chiralCarbon.getX(),
				-1.409 + chiralCarbon.getY(),
				 0.568 + chiralCarbon.getZ());
		Connection hydrogen = new Connection(atomNumber, atomNumber - 1);
		Atom h = new Atom((atomNumber) + "", "H", "GLU", super.getNumber(), this.hydrogen, hydrogen);
		

		this.hydrogen2 = new Coords(
				 0.038 + chiralCarbon.getX(),
				-2.163 + chiralCarbon.getY(),
				 0.289 + chiralCarbon.getZ());
		Connection hydrogen2 = new Connection(atomNumber + 1, atomNumber - 1);
		Atom h2 = new Atom((atomNumber + 1) + "", "H", "GLU", super.getNumber(),
				this.hydrogen2, hydrogen2);
		

		this.carbon2 = new Coords(
				-1.095 + chiralCarbon.getX(),
				-1.772 + chiralCarbon.getY(),
				-1.53 + chiralCarbon.getZ());
		Connection carbon2 = new Connection(atomNumber + 2, atomNumber - 1, atomNumber + 3, atomNumber + 4, atomNumber + 5);
		Atom c2 = new Atom((atomNumber + 2 )+ "", "C", "GLU", super.getNumber(),
				this.carbon2, carbon2);
		

		this.carbon2hydrogen1 = new Coords(-0.214 + chiralCarbon.getX(), -1.83
				+ chiralCarbon.getY(), -2.189 + chiralCarbon.getZ());
		Connection c2h1con = new Connection(atomNumber + 3, atomNumber + 2);
		Atom c2h1 = new Atom((atomNumber + 3) + "", "H", "GLU", super.getNumber(),
				this.carbon2hydrogen1, c2h1con);


		this.carbon2hydrogen2 = new Coords(-1.779 + chiralCarbon.getX(), -1.009
				+ chiralCarbon.getY(), -1.933 + chiralCarbon.getZ());
		Connection c2h2con = new Connection(atomNumber + 4, atomNumber + 2);
		Atom c2h2 = new Atom((atomNumber + 4) + "", "H", "GLU",
				super.getNumber(), this.carbon2hydrogen2, c2h2con);

		this.carbon3 = new Coords(-1.802 + chiralCarbon.getX(), -3.103
				+ chiralCarbon.getY(), -1.542 + chiralCarbon.getZ());
		Connection c3con = new Connection(atomNumber + 5, atomNumber + 2,
				atomNumber + 6, atomNumber + 6, atomNumber + 7);
		Atom c3 = new Atom((atomNumber + 5) + "", "C", "GLU",
				super.getNumber(), this.carbon3, c3con);

		this.doubleoxygen = new Coords(
				-1.338 + chiralCarbon.getX(),
				-4.091 + chiralCarbon.getY(),
				-2.09 + chiralCarbon.getZ());
		Connection doxy = new Connection(atomNumber + 6, atomNumber + 5, atomNumber + 5);
		Atom dox = new Atom((atomNumber + 6) + "", "O", "GLU", super.getNumber(),
				this.doubleoxygen, doxy);
		
		this.singleoxygen = new Coords(
				-2.975 + chiralCarbon.getX(),
				-3.149 + chiralCarbon.getY(),
				-0.915 + chiralCarbon.getZ());
		Connection oxyy = new Connection(atomNumber + 7, atomNumber + 5, atomNumber + 8);
		Atom ox = new Atom((atomNumber + 7) + "", "O", "GLU", super.getNumber(),
				this.singleoxygen, oxyy);
		
	
				this.singleoxygenhydrogen = new Coords(
						-3.334 + chiralCarbon.getX(),
						-4.026 + chiralCarbon.getY(),
						-0.98 + chiralCarbon.getZ());
				Connection oxhy = new Connection(atomNumber + 8, atomNumber + 7);
				Atom oxh = new Atom((atomNumber + 8 )+ "", "H", "GLU", super.getNumber(),
						this.singleoxygenhydrogen, oxhy);
				

		Atom[] parent = super.getAtoms();
		Atom[] GLU = new Atom[parent.length + rgroupAtomNumber];
		System.arraycopy(parent, 0, GLU, 0, parent.length);
		GLU[GLU.length - 10] = carbon;
		GLU[GLU.length - 9] = h;
		GLU[GLU.length - 8] = h2;
		GLU[GLU.length - 7] = c2;
		GLU[GLU.length - 6] = c2h1;
		GLU[GLU.length - 5] = c2h2;
		GLU[GLU.length - 4] = c3;
		GLU[GLU.length - 3] = dox;
		GLU[GLU.length - 2] = ox;
		GLU[GLU.length - 1] = oxh;
		super.setAtoms(GLU);
		super.setNumberAtoms(super.getNumberAtoms() + rgroupAtomNumber);
		
	}
}
