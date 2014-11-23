package pdbCreation.aminoacid.aminoacids;

import pdbCreation.aminoacid.AminoAcid;
import pdbCreation.aminoacid.Atom;
import pdbCreation.aminoacid.Connection;
import pdbCreation.aminoacid.Coords;

public class Isoleucine extends AminoAcid{
	private Coords carbon;
	private Coords hydrogen;
	private Coords carbon2;
	private Coords carbon2hydrogen1;
	private Coords carbon2hydrogen2;
	private Coords carbon2hydrogen3;
	private Coords carbon3;
	private Coords carbon3hydrogen1;
	private Coords carbon3hydrogen2;
	private Coords carbon4;
	private Coords carbon4hydrogen1;
	private Coords carbon4hydrogen2;
	private Coords carbon4hydrogen3;
	
	private static int rgroupAtomNumber = 13;
	public Isoleucine(Coords chiralCarbon, int atomNumber, int aminoAcidNumber) {
		super(chiralCarbon, atomNumber, aminoAcidNumber, "ILE");
		
		//main CH3 carbon
		this.carbon = new Coords(
				-0.811 + chiralCarbon.getX(),
				-1.331 + chiralCarbon.getY(),
				-0.079 + chiralCarbon.getZ());
		atomNumber += super.getNumberAtoms();
		
		Connection ch3 = new Connection(atomNumber - 1, super.getChiralCarbonNumber(), atomNumber, atomNumber+1, atomNumber+5);
		Atom carbon = new Atom((atomNumber - 1) + "", "C", "ILE", super.getNumber(), this.carbon, ch3);
		
		//first ch2o hydrogen
		this.hydrogen = new Coords(
				-0.084 + chiralCarbon.getX(),
				-2.143 + chiralCarbon.getY(),
				 0.260 + chiralCarbon.getZ());
		Connection hydrogen = new Connection(atomNumber, atomNumber - 1);
		Atom h = new Atom((atomNumber) + "", "H", "ILE", super.getNumber(), this.hydrogen, hydrogen);
		
		//second ch2o hydrogen
		this.carbon2 = new Coords(
				-1.794 + chiralCarbon.getX(),
				-1.322 + chiralCarbon.getY(),
				 1.286 + chiralCarbon.getZ());
		Connection carbon2 = new Connection(atomNumber + 1, atomNumber - 1, atomNumber + 2, atomNumber + 3, atomNumber + 4);
		Atom c2 = new Atom((atomNumber + 1) + "", "C", "ILE", super.getNumber(),
				this.carbon2, carbon2);
		
		//first ch3 hydrogen
		this.carbon2hydrogen1 = new Coords(
				-1.287 + chiralCarbon.getX(),
				-0.970 + chiralCarbon.getY(),
				 2.197 + chiralCarbon.getZ());
		Connection c2h1con = new Connection(atomNumber + 2, atomNumber + 1);
		Atom c2h1 = new Atom((atomNumber + 2) + "", "H", "ILE", super.getNumber(),
				this.carbon2hydrogen1, c2h1con);

		// second ch3 hydrogen
		this.carbon2hydrogen2 = new Coords(
				-2.170 + chiralCarbon.getX(),
				-2.336 + chiralCarbon.getY(),
				 1.492 + chiralCarbon.getZ());
		Connection c2h2con = new Connection(atomNumber + 3, atomNumber + 1);
		Atom c2h2 = new Atom((atomNumber + 3) + "", "H", "ILE", super.getNumber(),
				this.carbon2hydrogen2, c2h2con);

		// third ch3 hydrogen
		this.carbon2hydrogen3 = new Coords(
				-2.656 + chiralCarbon.getX(),
				-0.668 + chiralCarbon.getY(),
				 1.089 + chiralCarbon.getZ());
		Connection c2h3con = new Connection(atomNumber + 4, atomNumber + 1);
		Atom c2h3 = new Atom((atomNumber + 4) + "", "H", "ILE", super.getNumber(),
				this.carbon2hydrogen3, c2h3con);
		
		//second ch3
				this.carbon3 = new Coords(
						-1.545 + chiralCarbon.getX(),
						-1.620 + chiralCarbon.getY(),
						-1.266 + chiralCarbon.getZ());
				Connection carbon3 = new Connection(atomNumber + 5, atomNumber - 1, atomNumber + 6, atomNumber + 7, atomNumber + 8);
				Atom c3 = new Atom((atomNumber + 5 )+ "", "C", "ILE", super.getNumber(),
						this.carbon3, carbon3);
				
				//first ch3 hydrogen
				this.carbon3hydrogen1 = new Coords(
						-0.803 + chiralCarbon.getX(),
						-1.685 + chiralCarbon.getY(),
						-2.077 + chiralCarbon.getZ());
				Connection c3h1con = new Connection(atomNumber + 6, atomNumber + 5);
				Atom c3h1 = new Atom((atomNumber + 6) + "", "H", "ILE", super.getNumber(),
						this.carbon3hydrogen1, c3h1con);

				// second ch3 hydrogen
				this.carbon3hydrogen2 = new Coords(
						-2.234 + chiralCarbon.getX(),
						-0.792 + chiralCarbon.getY(),
						-1.500 + chiralCarbon.getZ());
				Connection c3h2con = new Connection(atomNumber + 7, atomNumber + 5);
				Atom c3h2 = new Atom((atomNumber + 7) + "", "H", "ILE", super.getNumber(),
						this.carbon3hydrogen2, c3h2con);
				
				//second ch2o hydrogen
				this.carbon4 = new Coords(
						-2.343 + chiralCarbon.getX(),
						-2.951 + chiralCarbon.getY(),
						-1.266 + chiralCarbon.getZ());
				Connection carbon4 = new Connection(atomNumber + 8, atomNumber + 5, atomNumber + 9, atomNumber + 10, atomNumber + 11);
				Atom c4 = new Atom((atomNumber + 8) + "", "C", "ILE", super.getNumber(),
						this.carbon4, carbon4);
				
				//first ch3 hydrogen
				this.carbon4hydrogen1 = new Coords(
						-1.697 + chiralCarbon.getX(),
						-3.787 + chiralCarbon.getY(),
						-0.957 + chiralCarbon.getZ());
				Connection c4h1con = new Connection(atomNumber + 9, atomNumber + 8);
				Atom c4h1 = new Atom((atomNumber + 9) + "", "H", "ILE", super.getNumber(),
						this.carbon4hydrogen1, c4h1con);

				// second ch3 hydrogen
				this.carbon4hydrogen2 = new Coords(
						-2.722 + chiralCarbon.getX(),
						-3.16 + chiralCarbon.getY(),
						-2.280 + chiralCarbon.getZ());
				Connection c4h2con = new Connection(atomNumber + 10, atomNumber + 8);
				Atom c4h2 = new Atom((atomNumber + 10) + "", "H", "ILE", super.getNumber(),
						this.carbon4hydrogen2, c4h2con);

				// third ch3 hydrogen
				this.carbon4hydrogen3 = new Coords(
						-3.210 + chiralCarbon.getX(),
						-2.898 + chiralCarbon.getY(),
						-0.590 + chiralCarbon.getZ());
				Connection c4h3con = new Connection(atomNumber + 11, atomNumber + 8);
				Atom c4h3 = new Atom((atomNumber + 11) + "", "H", "ILE", super.getNumber(),
						this.carbon4hydrogen3, c4h3con);

				

		Atom[] parent = super.getAtoms();
		Atom[] ILE = new Atom[parent.length + rgroupAtomNumber];
		System.arraycopy(parent, 0, ILE, 0, parent.length);
		ILE[ILE.length - 13] = carbon;
		ILE[ILE.length - 12] = h;
		ILE[ILE.length - 11] = c2;
		ILE[ILE.length - 10] = c2h1;
		ILE[ILE.length - 9] = c2h2;
		ILE[ILE.length - 8] = c2h3;
		ILE[ILE.length - 7] = c3;
		ILE[ILE.length - 6] = c3h1;
		ILE[ILE.length - 5] = c3h2;
		ILE[ILE.length - 4] = c4;
		ILE[ILE.length - 3] = c4h1;
		ILE[ILE.length - 2] = c4h2;
		ILE[ILE.length - 1] = c4h3;
		super.setAtoms(ILE);
		super.setNumberAtoms(super.getNumberAtoms() + rgroupAtomNumber);
		
	}
}
