package pdbCreation.aminoacid.aminoacids;

import pdbCreation.aminoacid.AminoAcid;
import pdbCreation.aminoacid.Atom;
import pdbCreation.aminoacid.Connection;
import pdbCreation.aminoacid.Coords;

public class Asparagine extends AminoAcid{
	private Coords carbon;
	private Coords hydrogen;
	private Coords hydrogen2;
	private Coords carbon2;
	private Coords doubleoxygen;
	private Coords nitrogen;
	private Coords nitrogen1hydrogen1;
	private Coords nitrogen1hydrogen2;

	private static int rgroupAtomNumber = 8;

	public Asparagine(Coords chiralCarbon, int atomNumber, int aminoAcidNumber) {
		super(chiralCarbon, atomNumber, aminoAcidNumber, "ASN");

		// main CH3 carbon
		this.carbon = new Coords(-0.739 + chiralCarbon.getX(), -1.367
				+ chiralCarbon.getY(), -0.027 + chiralCarbon.getZ());
		atomNumber += super.getNumberAtoms();

		Connection ch3 = new Connection(atomNumber - 1,
				super.getChiralCarbonNumber(), atomNumber, atomNumber + 1,
				atomNumber + 2);
		Atom carbon = new Atom((atomNumber - 1) + "", "C", "ASN",
				super.getNumber(), this.carbon, ch3);

		// first ch2o hydrogen
		this.hydrogen = new Coords(-1.456 + chiralCarbon.getX(), -1.415
				+ chiralCarbon.getY(), 0.807 + chiralCarbon.getZ());
		Connection hydrogen = new Connection(atomNumber, atomNumber - 1);
		Atom h = new Atom((atomNumber) + "", "H", "ASN", super.getNumber(),
				this.hydrogen, hydrogen);

		// second ch2o hydrogen
		this.hydrogen2 = new Coords(-0.002 + chiralCarbon.getX(), -2.173
				+ chiralCarbon.getY(), 0.126 + chiralCarbon.getZ());
		Connection hydrogen2 = new Connection(atomNumber + 1, atomNumber - 1);
		Atom h2 = new Atom((atomNumber + 1) + "", "H", "ASN",
				super.getNumber(), this.hydrogen2, hydrogen2);

		// second ch3
		this.carbon2 = new Coords(-1.465 + chiralCarbon.getX(), -1.605
				+ chiralCarbon.getY(), -1.331 + chiralCarbon.getZ());
		Connection carbon2 = new Connection(atomNumber + 2, atomNumber - 1,
				atomNumber + 3, atomNumber + 3, atomNumber + 4);
		Atom c2 = new Atom((atomNumber + 2) + "", "C", "ASN",
				super.getNumber(), this.carbon2, carbon2);

		this.doubleoxygen = new Coords(-0.91 + chiralCarbon.getX(), -2.236
				+ chiralCarbon.getY(), -2.216 + chiralCarbon.getZ());
		Connection doxy = new Connection(atomNumber + 3, atomNumber + 2,
				atomNumber + 2);
		Atom dox = new Atom((atomNumber + 3) + "", "O", "ASN",
				super.getNumber(), this.doubleoxygen, doxy);

		this.nitrogen = new Coords(-2.711 + chiralCarbon.getX(), -1.118
				+ chiralCarbon.getY(), -1.499 + chiralCarbon.getZ());
		Connection ncon = new Connection(atomNumber + 4, atomNumber + 2);
		Atom n = new Atom((atomNumber + 4) + "", "N", "ASN",
				super.getNumber(), this.nitrogen, ncon);

		this.nitrogen1hydrogen1 = new Coords(-3.184 + chiralCarbon.getX(),
				-0.606 + chiralCarbon.getY(), -0.781 + chiralCarbon.getZ());
		Connection n1h1con = new Connection(atomNumber + 5, atomNumber + 4);
		Atom n1h1 = new Atom((atomNumber + 5) + "", "H", "ASN",
				super.getNumber(), this.nitrogen1hydrogen1, n1h1con);
		
		this.nitrogen1hydrogen2 = new Coords(-3.171 + chiralCarbon.getX(),
				-1.282 + chiralCarbon.getY(), -2.372 + chiralCarbon.getZ());
		Connection n1h2con = new Connection(atomNumber + 6, atomNumber + 4);
		Atom n1h2 = new Atom((atomNumber + 6) + "", "H", "ASN",
				super.getNumber(), this.nitrogen1hydrogen2, n1h2con);

		Atom[] parent = super.getAtoms();
		Atom[] ASN = new Atom[parent.length + rgroupAtomNumber];
		System.arraycopy(parent, 0, ASN, 0, parent.length);
		ASN[ASN.length - 8] = carbon;
		ASN[ASN.length - 7] = h;
		ASN[ASN.length - 6] = h2;
		ASN[ASN.length - 5] = c2;
		ASN[ASN.length - 4] = dox;
		ASN[ASN.length - 3] = n;
		ASN[ASN.length - 2] = n1h1;
		ASN[ASN.length - 1] = n1h2;
		super.setAtoms(ASN);
		super.setNumberAtoms(super.getNumberAtoms() + rgroupAtomNumber);

	}
}
