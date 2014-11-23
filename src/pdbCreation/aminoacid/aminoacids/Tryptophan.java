package pdbCreation.aminoacid.aminoacids;

import pdbCreation.aminoacid.AminoAcid;
import pdbCreation.aminoacid.Atom;
import pdbCreation.aminoacid.Connection;
import pdbCreation.aminoacid.Coords;

public class Tryptophan extends AminoAcid {

	private Coords carbon1;
	private Coords carbon1hydrogen1;
	private Coords carbon1hydrogen2;
	private Coords carbon2;
	private Coords carbon3;
	private Coords carbon3hydrogen1;
	private Coords nitrogen1;
	private Coords nitrogen1hydrogen1;
	private Coords carbon4;
	private Coords carbon5;
	private Coords carbon5hydrogen1;
	private Coords carbon6;
	private Coords carbon6hydrogen1;
	private Coords carbon7;
	private Coords carbon7hydrogen1;
	private Coords carbon8;
	private Coords carbon8hydrogen1;
	private Coords carbon9;
	private static int rgroupAtomNumber = 18;

	public Tryptophan(Coords chiralCarbon, int atomNumber, int aminoAcidNumber) {
		super(chiralCarbon, atomNumber, aminoAcidNumber, "TRP");

		// main CH3 carbon
		this.carbon1 = new Coords(-0.758 + chiralCarbon.getX(), -1.189
				+ chiralCarbon.getY(), -0.897 + chiralCarbon.getZ());
		atomNumber += super.getNumberAtoms();

		Connection ch3 = new Connection(atomNumber - 1,
				super.getChiralCarbonNumber(), atomNumber, atomNumber + 1,
				atomNumber + 2);
		Atom carbon = new Atom((atomNumber - 1) + "", "C", "TRP",
				super.getNumber(), this.carbon1, ch3);

		// first ch2o hydrogen
		this.carbon1hydrogen1 = new Coords(-0.049 + chiralCarbon.getX(), -1.995
				+ chiralCarbon.getY(), -0.897 + chiralCarbon.getZ());
		Connection hydrogen = new Connection(atomNumber, atomNumber - 1);
		Atom h = new Atom((atomNumber) + "", "H", "TRP", super.getNumber(),
				this.carbon1hydrogen1, hydrogen);

		// second ch2o hydrogen
		this.carbon1hydrogen2 = new Coords(-1.213 + chiralCarbon.getX(), -0.837
				+ chiralCarbon.getY(), -1.589 + chiralCarbon.getZ());
		Connection hydrogen2 = new Connection(atomNumber + 1, atomNumber - 1);
		Atom h2 = new Atom((atomNumber + 1) + "", "H", "TRP",
				super.getNumber(), this.carbon1hydrogen2, hydrogen2);

		this.carbon2 = new Coords(-1.853 + chiralCarbon.getX(), -1.787
				+ chiralCarbon.getY(), 0.198 + chiralCarbon.getZ());
		Connection c2c = new Connection(atomNumber + 2, atomNumber - 1,
				atomNumber + 3, atomNumber + 3, atomNumber + 16);
		Atom c2 = new Atom((atomNumber + 2) + "", "C", "TRP",
				super.getNumber(), this.carbon2, c2c);


		this.carbon3 = new Coords(-1.979 + chiralCarbon.getX(), -1.795
				+ chiralCarbon.getY(), 1.537 + chiralCarbon.getZ());
		Connection carbon3 = new Connection(atomNumber + 3, atomNumber + 2,
				atomNumber + 2, atomNumber + 4, atomNumber + 5);
		Atom c3 = new Atom((atomNumber + 3) + "", "C", "TRP",
				super.getNumber(), this.carbon3, carbon3);

		this.carbon3hydrogen1 = new Coords(-1.287 + chiralCarbon.getX(), -1.357
				+ chiralCarbon.getY(), 2.256 + chiralCarbon.getZ());
		Connection c3h1con = new Connection(atomNumber + 4, atomNumber + 3);
		Atom c3h1 = new Atom((atomNumber + 4) + "", "H", "TRP",
				super.getNumber(), this.carbon3hydrogen1, c3h1con);


		this.nitrogen1 = new Coords(-3.080 + chiralCarbon.getX(), -2.418
				+ chiralCarbon.getY(), 1.870 + chiralCarbon.getZ());
		Connection n1con = new Connection(atomNumber + 5, atomNumber + 3,
				atomNumber + 6, atomNumber + 7);
		Atom n1 = new Atom((atomNumber + 5) + "", "N", "TRP",
				super.getNumber(), this.nitrogen1, n1con);


		this.nitrogen1hydrogen1 = new Coords(-3.398 + chiralCarbon.getX(), -2.555
				+ chiralCarbon.getY(), 2.842 + chiralCarbon.getZ());
		Connection n1hcon = new Connection(atomNumber + 6, atomNumber + 5);
		Atom n1h1 = new Atom((atomNumber + 6) + "", "H", "TRP",
				super.getNumber(), this.nitrogen1hydrogen1, n1hcon);


		this.carbon4 = new Coords(-3.749 + chiralCarbon.getX(), -2.864
				+ chiralCarbon.getY(), 0.788 + chiralCarbon.getZ());
		Connection carbon4 = new Connection(atomNumber + 7, atomNumber + 5,
				atomNumber + 16, atomNumber + 16, atomNumber + 8);
		Atom c4 = new Atom((atomNumber + 7) + "", "C", "TRP",
				super.getNumber(), this.carbon4, carbon4);


		// second ch2o hydrogen
		this.carbon5 = new Coords(-4.953 + chiralCarbon.getX(), -3.564
				+ chiralCarbon.getY(), 0.728 + chiralCarbon.getZ());
		Connection carbon5 = new Connection(atomNumber + 8, atomNumber + 7,
				atomNumber + 9, atomNumber + 10, atomNumber + 10);
		Atom c5 = new Atom((atomNumber + 8) + "", "C", "TRP",
				super.getNumber(), this.carbon5, carbon5);

		// first ch3 hydrogen
		this.carbon5hydrogen1 = new Coords(-5.499 + chiralCarbon.getX(), -3.826
				+ chiralCarbon.getY(), 1.628 + chiralCarbon.getZ());
		Connection c5h1con = new Connection(atomNumber + 9, atomNumber + 8);
		Atom c5h1 = new Atom((atomNumber + 9) + "", "H", "TRP",
				super.getNumber(), this.carbon5hydrogen1, c5h1con);

		// second ch2o hydrogen
		this.carbon6 = new Coords(-5.424 + chiralCarbon.getX(), -3.908
				+ chiralCarbon.getY(), -0.546 + chiralCarbon.getZ());
		Connection carbon6 = new Connection(atomNumber + 10, atomNumber + 8,
				atomNumber + 8, atomNumber + 11, atomNumber + 12);
		Atom c6 = new Atom((atomNumber + 10) + "", "C", "TRP",
				super.getNumber(), this.carbon6, carbon6);

		// first ch3 hydrogen
		this.carbon6hydrogen1 = new Coords(-6.36 + chiralCarbon.getX(), -4.449
				+ chiralCarbon.getY(), -0.6358 + chiralCarbon.getZ());
		Connection c6h1con = new Connection(atomNumber + 11, atomNumber + 10);
		Atom c6h1 = new Atom((atomNumber + 11) + "", "H", "TRP",
				super.getNumber(), this.carbon6hydrogen1, c6h1con);

		// second ch2o hydrogen
		this.carbon7 = new Coords(-4.708 + chiralCarbon.getX(), -3.565
				+ chiralCarbon.getY(), -1.704 + chiralCarbon.getZ());
		Connection carbon7 = new Connection(atomNumber + 12, atomNumber + 10,
				atomNumber + 13, atomNumber + 14, atomNumber + 14);
		Atom c7 = new Atom((atomNumber + 12) + "", "C", "TRP",
				super.getNumber(), this.carbon7, carbon7);

		// first ch3 hydrogen
		this.carbon7hydrogen1 = new Coords(-5.097 + chiralCarbon.getX(), -3.849
				+ chiralCarbon.getY(), -2.678 + chiralCarbon.getZ());
		Connection c7h1con = new Connection(atomNumber + 13, atomNumber + 12);
		Atom c7h1 = new Atom((atomNumber + 13) + "", "H", "TRP",
				super.getNumber(), this.carbon7hydrogen1, c7h1con);


		this.carbon8 = new Coords(-3.498 + chiralCarbon.getX(), -2.849
				+ chiralCarbon.getY(), -1.628 + chiralCarbon.getZ());
		Connection carbon8 = new Connection(atomNumber + 14, atomNumber + 12,
				atomNumber + 12, atomNumber + 15, atomNumber + 16);
		Atom c8 = new Atom((atomNumber + 14) + "", "C", "TRP",
				super.getNumber(), this.carbon8, carbon8);


		this.carbon8hydrogen1 = new Coords(-2.935 + chiralCarbon.getX(), -2.596
				+ chiralCarbon.getY(), -2.515 + chiralCarbon.getZ());
		Connection c8h1con = new Connection(atomNumber + 15, atomNumber + 14);
		Atom c8h1 = new Atom((atomNumber + 15) + "", "H", "TRP",
				super.getNumber(), this.carbon8hydrogen1, c8h1con);

		this.carbon9 = new Coords(-3.049 + chiralCarbon.getX(), -2.523
				+ chiralCarbon.getY(), -0.353 + chiralCarbon.getZ());
		Connection carbon9 = new Connection(atomNumber + 16, atomNumber + 7,
				atomNumber + 7, atomNumber + 14, atomNumber + 2);
		Atom c9 = new Atom((atomNumber + 16) + "", "C", "TRP",
				super.getNumber(), this.carbon9, carbon9);

		Atom[] parent = super.getAtoms();
		Atom[] TRP = new Atom[parent.length + rgroupAtomNumber];
		System.arraycopy(parent, 0, TRP, 0, parent.length);
		TRP[TRP.length - 18] = carbon;
		TRP[TRP.length - 17] = h;
		TRP[TRP.length - 16] = h2;
		TRP[TRP.length - 15] = c2;
		TRP[TRP.length - 14] = c3;
		TRP[TRP.length - 13] = c3h1;
		TRP[TRP.length - 12] = n1;
		TRP[TRP.length - 11] = n1h1;
		TRP[TRP.length - 10] = c4;
		TRP[TRP.length - 9] = c5;
		TRP[TRP.length - 8] = c5h1;
		TRP[TRP.length - 7] = c6;
		TRP[TRP.length - 6] = c6h1;
		TRP[TRP.length - 5] = c7;
		TRP[TRP.length - 4] = c7h1;
		TRP[TRP.length - 3] = c8;
		TRP[TRP.length - 2] = c8h1;
		TRP[TRP.length - 1] = c9;
		super.setAtoms(TRP);
		super.setNumberAtoms(super.getNumberAtoms() + rgroupAtomNumber);

	}
}
