package pdbCreation.aminoacid.aminoacids;

import pdbCreation.aminoacid.AminoAcid;
import pdbCreation.aminoacid.Atom;
import pdbCreation.aminoacid.Connection;
import pdbCreation.aminoacid.Coords;

public class Glutamine extends AminoAcid{
	private Coords carbon;
	private Coords hydrogen;
	private Coords hydrogen2;
	private Coords carbon2;
	private Coords carbon2hydrogen1;
	private Coords carbon2hydrogen2;
	private Coords carbon3;
	private Coords doubleoxygen;
	private Coords nitrogen;
	private Coords nitrogen1hydrogen1;
	private Coords nitrogen1hydrogen2;

	private static int rgroupAtomNumber = 11;

	public Glutamine(Coords chiralCarbon, int atomNumber, int aminoAcidNumber) {
		super(chiralCarbon, atomNumber, aminoAcidNumber, "GLN");

		// main CH3 carbon
		this.carbon = new Coords(-0.83 + chiralCarbon.getX(), -1.306
				+ chiralCarbon.getY(), -0.160 + chiralCarbon.getZ());
		atomNumber += super.getNumberAtoms();

		Connection ch3 = new Connection(atomNumber - 1,
				super.getChiralCarbonNumber(), atomNumber, atomNumber + 1,
				atomNumber + 2);
		Atom carbon = new Atom((atomNumber - 1) + "", "C", "GLN",
				super.getNumber(), this.carbon, ch3);

		// first ch2o hydrogen
		this.hydrogen = new Coords(-1.687 + chiralCarbon.getX(), -1.257
				+ chiralCarbon.getY(), 0.531 + chiralCarbon.getZ());
		Connection hydrogen = new Connection(atomNumber, atomNumber - 1);
		Atom h = new Atom((atomNumber) + "", "H", "GLN", super.getNumber(),
				this.hydrogen, hydrogen);

		// second ch2o hydrogen
		this.hydrogen2 = new Coords(-0.198 + chiralCarbon.getX(), -2.159
				+ chiralCarbon.getY(), 0.140 + chiralCarbon.getZ());
		Connection hydrogen2 = new Connection(atomNumber + 1, atomNumber - 1);
		Atom h2 = new Atom((atomNumber + 1) + "", "H", "GLN",
				super.getNumber(), this.hydrogen2, hydrogen2);

		// second ch3
		this.carbon2 = new Coords(-1.344 + chiralCarbon.getX(), -1.571
				+ chiralCarbon.getY(), -1.601 + chiralCarbon.getZ());
		Connection carbon2 = new Connection(atomNumber + 2, atomNumber - 1,
				atomNumber + 3, atomNumber + 4, atomNumber + 5);
		Atom c2 = new Atom((atomNumber + 2) + "", "C", "GLN",
				super.getNumber(), this.carbon2, carbon2);

		// first ch2o hydrogen
		this.carbon2hydrogen1 = new Coords(-0.498 + chiralCarbon.getX(), -1.607
				+ chiralCarbon.getY(), -2.305 + chiralCarbon.getZ());
		Connection c2h1con = new Connection(atomNumber + 3, atomNumber + 2);
		Atom c2h1 = new Atom((atomNumber + 3) + "", "H", "GLN", super.getNumber(),
				this.carbon2hydrogen1, c2h1con);

		// second ch2o hydrogen
		this.carbon2hydrogen2 = new Coords(-2.028 + chiralCarbon.getX(), -0.774
				+ chiralCarbon.getY(), -1.928 + chiralCarbon.getZ());
		Connection c2h2con = new Connection(atomNumber + 4, atomNumber + 2);
		Atom c2h2 = new Atom((atomNumber + 4) + "", "H", "GLN",
				super.getNumber(), this.carbon2hydrogen2, c2h2con);

		// second ch3
		this.carbon3 = new Coords(-2.088 + chiralCarbon.getX(), -2.885
				+ chiralCarbon.getY(), -1.656 + chiralCarbon.getZ());
		Connection carbon3 = new Connection(atomNumber + 5, atomNumber + 2,
				atomNumber + 6, atomNumber + 6, atomNumber + 7);
		Atom c3 = new Atom((atomNumber + 5) + "", "C", "GLN",
				super.getNumber(), this.carbon3, carbon3);

		this.doubleoxygen = new Coords(-1.571 + chiralCarbon.getX(), -3.849
				+ chiralCarbon.getY(), -2.197 + chiralCarbon.getZ());
		Connection doxy = new Connection(atomNumber + 6, atomNumber + 5,
				atomNumber + 5);
		Atom dox = new Atom((atomNumber + 6) + "", "O", "GLN",
				super.getNumber(), this.doubleoxygen, doxy);

		this.nitrogen = new Coords(-3.314 + chiralCarbon.getX(), -2.969
				+ chiralCarbon.getY(), -1.099 + chiralCarbon.getZ());
		Connection ncon = new Connection(atomNumber + 7, atomNumber + 5, atomNumber + 8, atomNumber + 9);
		Atom n = new Atom((atomNumber + 7) + "", "N", "GLN", super.getNumber(),
				this.nitrogen, ncon);

		this.nitrogen1hydrogen1 = new Coords(-3.75 + chiralCarbon.getX(),
				-2.184 + chiralCarbon.getY(), -0.658 + chiralCarbon.getZ());
		Connection n1h1con = new Connection(atomNumber + 8, atomNumber + 7);
		Atom n1h1 = new Atom((atomNumber + 8) + "", "H", "GLN",
				super.getNumber(), this.nitrogen1hydrogen1, n1h1con);

		this.nitrogen1hydrogen2 = new Coords(-3.795 + chiralCarbon.getX(),
				-3.844 + chiralCarbon.getY(), -1.138 + chiralCarbon.getZ());
		Connection n1h2con = new Connection(atomNumber + 9, atomNumber + 7);
		Atom n1h2 = new Atom((atomNumber + 9) + "", "H", "GLN",
				super.getNumber(), this.nitrogen1hydrogen2, n1h2con);

		Atom[] parent = super.getAtoms();
		Atom[] GLN = new Atom[parent.length + rgroupAtomNumber];
		System.arraycopy(parent, 0, GLN, 0, parent.length);
		GLN[GLN.length - 11] = carbon;
		GLN[GLN.length - 10] = h;
		GLN[GLN.length - 9] = h2;
		GLN[GLN.length - 8] = c2;
		GLN[GLN.length - 7] = c2h1;
		GLN[GLN.length - 6] = c2h2;
		GLN[GLN.length - 5] = c3;
		GLN[GLN.length - 4] = dox;
		GLN[GLN.length - 3] = n;
		GLN[GLN.length - 2] = n1h1;
		GLN[GLN.length - 1] = n1h2;
		super.setAtoms(GLN);
		super.setNumberAtoms(super.getNumberAtoms() + rgroupAtomNumber);

	}

}
