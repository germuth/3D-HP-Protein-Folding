package pdbCreation.aminoacid.aminoacids;

import pdbCreation.aminoacid.AminoAcid;
import pdbCreation.aminoacid.Atom;
import pdbCreation.aminoacid.Connection;
import pdbCreation.aminoacid.Coords;

public class Lysine extends AminoAcid{
	private Coords carbon;
	private Coords hydrogen;
	private Coords hydrogen2;
	private Coords carbon2;
	private Coords carbon2hydrogen1;
	private Coords carbon2hydrogen2;
	private Coords carbon3;
	private Coords carbon3hydrogen1;
	private Coords carbon3hydrogen2;
	private Coords carbon4;
	private Coords carbon4hydrogen1;
	private Coords carbon4hydrogen2;
	private Coords nitrogen;
	private Coords nitrogen1hydrogen1;
	private Coords nitrogen1hydrogen2;
	private static int rgroupAtomNumber = 15;

	public Lysine(Coords chiralCarbon, int atomNumber, int aminoAcidNumber) {
		super(chiralCarbon, atomNumber, aminoAcidNumber, "LYS");


		this.carbon = new Coords(-0.824 + chiralCarbon.getX(), -1.315
				+ chiralCarbon.getY(), -0.074 + chiralCarbon.getZ());
		atomNumber += super.getNumberAtoms();
		Connection ch3 = new Connection(atomNumber - 1,
				super.getChiralCarbonNumber(), atomNumber, atomNumber + 1,
				atomNumber + 2);
		Atom carbon = new Atom((atomNumber - 1) + "", "C", "LYS",
				super.getNumber(), this.carbon, ch3);
		this.hydrogen = new Coords(-1.611 + chiralCarbon.getX(), -1.28
				+ chiralCarbon.getY(), 0.697 + chiralCarbon.getZ());
		Connection hydrogen = new Connection(atomNumber, atomNumber - 1);
		Atom h = new Atom((atomNumber) + "", "H", "LYS", super.getNumber(),
				this.hydrogen, hydrogen);
		this.hydrogen2 = new Coords(-0.164 + chiralCarbon.getX(), -2.169
				+ chiralCarbon.getY(), 0.154 + chiralCarbon.getZ());
		Connection hydrogen2 = new Connection(atomNumber + 1, atomNumber - 1);
		Atom h2 = new Atom((atomNumber + 1) + "", "H", "LYS",
				super.getNumber(), this.hydrogen2, hydrogen2);


		
		
		this.carbon2 = new Coords(-1.484 + chiralCarbon.getX(), -1.533
				+ chiralCarbon.getY(), -1.464 + chiralCarbon.getZ());
		Connection carbon2 = new Connection(atomNumber + 2, atomNumber - 1,
				atomNumber + 3, atomNumber + 4, atomNumber + 5);
		Atom c2 = new Atom((atomNumber + 2) + "", "C", "LYS",
				super.getNumber(), this.carbon2, carbon2);
		this.carbon2hydrogen1 = new Coords(-0.703 + chiralCarbon.getX(), -1.648
				+ chiralCarbon.getY(), -2.232 + chiralCarbon.getZ());
		Connection c2h1con = new Connection(atomNumber + 3, atomNumber + 2);
		Atom c2h1 = new Atom((atomNumber + 3) + "", "H", "LYS", super.getNumber(),
				this.carbon2hydrogen1, c2h1con);
		this.carbon2hydrogen2 = new Coords(-2.099 + chiralCarbon.getX(), -0.655
				+ chiralCarbon.getY(), -1.722 + chiralCarbon.getZ());
		Connection c2h2con = new Connection(atomNumber + 4, atomNumber + 2);
		Atom c2h2 = new Atom((atomNumber + 4) + "", "H", "LYS",
				super.getNumber(), this.carbon2hydrogen2, c2h2con);


		this.carbon3 = new Coords(-2.381 + chiralCarbon.getX(), -2.801
				+ chiralCarbon.getY(), -1.471 + chiralCarbon.getZ());
		Connection carbon3 = new Connection(atomNumber + 5, atomNumber + 2,
				atomNumber + 6, atomNumber + 7, atomNumber + 8);
		Atom c3 = new Atom((atomNumber + 5) + "", "C", "LYS",
				super.getNumber(), this.carbon3, carbon3);
		this.carbon3hydrogen1 = new Coords(-1.776 + chiralCarbon.getX(), -3.685
				+ chiralCarbon.getY(), -1.213 + chiralCarbon.getZ());
		Connection c3h1con = new Connection(atomNumber + 6, atomNumber + 5);
		Atom c3h1 = new Atom((atomNumber + 6) + "", "H", "LYS",
				super.getNumber(), this.carbon3hydrogen1, c3h1con);
		this.carbon3hydrogen2 = new Coords(-3.174 + chiralCarbon.getX(), -2.693
				+ chiralCarbon.getY(), -0.713 + chiralCarbon.getZ());
		Connection c3h2con = new Connection(atomNumber + 7, atomNumber + 5);
		Atom c3h2 = new Atom((atomNumber + 7) + "", "H", "LYS",
				super.getNumber(), this.carbon3hydrogen2, c3h2con);

		this.carbon4 = new Coords(-3.034 + chiralCarbon.getX(), -3.031
				+ chiralCarbon.getY(), -2.860 + chiralCarbon.getZ());
		Connection carbon4 = new Connection(atomNumber + 8, atomNumber + 5,
				atomNumber + 9, atomNumber + 10, atomNumber + 11);
		Atom c4 = new Atom((atomNumber + 8) + "", "C", "LYS",
				super.getNumber(), this.carbon4, carbon4);
		this.carbon4hydrogen1 = new Coords(-2.241 + chiralCarbon.getX(), -3.139
				+ chiralCarbon.getY(), -3.618 + chiralCarbon.getZ());
		Connection c4h1con = new Connection(atomNumber + 9, atomNumber + 8);
		Atom c4h1 = new Atom((atomNumber + 9) + "", "H", "LYS",
				super.getNumber(), this.carbon4hydrogen1, c4h1con);
		this.carbon4hydrogen2 = new Coords(-3.659 + chiralCarbon.getX(), -2.163
				+ chiralCarbon.getY(), -3.312 + chiralCarbon.getZ());
		Connection c4h2con = new Connection(atomNumber + 10, atomNumber + 8);
		Atom c4h2 = new Atom((atomNumber + 10) + "", "H", "LYS",
				super.getNumber(), this.carbon4hydrogen2, c4h2con);


		this.nitrogen = new Coords(-3.848 + chiralCarbon.getX(), -4.265
				+ chiralCarbon.getY(), -2.846 + chiralCarbon.getZ());
		Connection ncon = new Connection(atomNumber + 11, atomNumber + 8,
				atomNumber + 12, atomNumber + 13);
		Atom n1 = new Atom((atomNumber + 11) + "", "N", "LYS",
				super.getNumber(), this.nitrogen, ncon);
		this.nitrogen1hydrogen1 = new Coords(-4.659 + chiralCarbon.getX(), -4.18
				+ chiralCarbon.getY(), -2.136 + chiralCarbon.getZ());
		Connection n1h1con = new Connection(atomNumber + 12, atomNumber + 11);
		Atom n1h1 = new Atom((atomNumber + 12) + "", "H", "LYS",
				super.getNumber(), this.nitrogen1hydrogen1, n1h1con);	
		this.nitrogen1hydrogen2 = new Coords(-4.252 + chiralCarbon.getX(), -4.465
				+ chiralCarbon.getY(), -3.83 + chiralCarbon.getZ());
		Connection n1h2con = new Connection(atomNumber + 13, atomNumber + 11);
		Atom n1h2 = new Atom((atomNumber + 13) + "", "H", "LYS",
				super.getNumber(), this.nitrogen1hydrogen2, n1h2con);
		
		Atom[] parent = super.getAtoms();
		Atom[] LYS = new Atom[parent.length + rgroupAtomNumber];
		System.arraycopy(parent, 0, LYS, 0, parent.length);
		LYS[LYS.length - 15] = carbon;
		LYS[LYS.length - 14] = h;
		LYS[LYS.length - 13] = h2;
		LYS[LYS.length - 12] = c2;
		LYS[LYS.length - 11] = c2h1;
		LYS[LYS.length - 10] = c2h2;
		LYS[LYS.length - 9] = c3;
		LYS[LYS.length - 8] = c3h1;
		LYS[LYS.length - 7] = c3h2;
		LYS[LYS.length - 6] = c4;
		LYS[LYS.length - 5] = c4h1;
		LYS[LYS.length - 4] = c4h2;
		LYS[LYS.length - 3] = n1;
		LYS[LYS.length - 2] = n1h1;
		LYS[LYS.length - 1] = n1h2;
		super.setAtoms(LYS);
		super.setNumberAtoms(super.getNumberAtoms() + rgroupAtomNumber);

	}

}
