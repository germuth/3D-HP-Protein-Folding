package pdbCreation.aminoacid.aminoacids;

import pdbCreation.aminoacid.AminoAcid;
import pdbCreation.aminoacid.Atom;
import pdbCreation.aminoacid.Connection;
import pdbCreation.aminoacid.Coords;

public class Leucine extends AminoAcid{
	private Coords carbon;
	private Coords hydrogen;
	private Coords hydrogen2;
	private Coords carbon2;
	private Coords carbon2hydrogen1;
	private Coords carbon3;
	private Coords carbon3hydrogen1;
	private Coords carbon3hydrogen2;
	private Coords carbon3hydrogen3;
	private Coords carbon4;
	private Coords carbon4hydrogen1;
	private Coords carbon4hydrogen2;
	private Coords carbon4hydrogen3;
	
	private static int rgroupAtomNumber = 13;
	public Leucine(Coords chiralCarbon, int atomNumber, int aminoAcidNumber) {
		super(chiralCarbon, atomNumber, aminoAcidNumber, "LEU");
		
		//main CH3 carbon
		this.carbon = new Coords(
				-0.848 + chiralCarbon.getX(),
				-1.263 + chiralCarbon.getY(),
				-0.329 + chiralCarbon.getZ());
		atomNumber += super.getNumberAtoms();
		
		Connection ch3 = new Connection(atomNumber - 1, super.getChiralCarbonNumber(), atomNumber, atomNumber+1, atomNumber+2);
		Atom carbon = new Atom((atomNumber - 1) + "", "C", "LEU", super.getNumber(), this.carbon, ch3);
		
		//first ch2o hydrogen
		this.hydrogen = new Coords(
				-0.266 + chiralCarbon.getX(),
				-2.140 + chiralCarbon.getY(),
				 0.008 + chiralCarbon.getZ());
		Connection hydrogen = new Connection(atomNumber, atomNumber - 1);
		Atom h = new Atom((atomNumber) + "", "H", "LEU", super.getNumber(), this.hydrogen, hydrogen);
		
		//second ch2o hydrogen
		this.hydrogen2 = new Coords(
				-1.778 + chiralCarbon.getX(),
				-1.229 + chiralCarbon.getY(),
				 0.259 + chiralCarbon.getZ());
		Connection hydrogen2 = new Connection(atomNumber + 1, atomNumber - 1);
		Atom h2 = new Atom((atomNumber + 1) + "", "H", "LEU", super.getNumber(),
				this.hydrogen2, hydrogen2);
		
		//second ch3
		this.carbon2 = new Coords(
				-1.199 + chiralCarbon.getX(),
				-1.470 + chiralCarbon.getY(),
				-1.836 + chiralCarbon.getZ());
		Connection carbon2 = new Connection(atomNumber + 2, atomNumber - 1, atomNumber + 3, atomNumber + 4, atomNumber + 8);
		Atom c2 = new Atom((atomNumber + 2 )+ "", "C", "LEU", super.getNumber(),
				this.carbon2, carbon2);
		
		//first ch3 hydrogen
		this.carbon2hydrogen1 = new Coords(
				-0.266 + chiralCarbon.getX(),
				-1.571 + chiralCarbon.getY(),
				-2.417 + chiralCarbon.getZ());
		Connection c2h1con = new Connection(atomNumber + 3, atomNumber + 2);
		Atom c2h1 = new Atom((atomNumber + 3) + "", "H", "LEU", super.getNumber(),
				this.carbon2hydrogen1, c2h1con);
		
		//second ch3
				this.carbon3 = new Coords(
						-1.990 + chiralCarbon.getX(),
						-2.798 + chiralCarbon.getY(),
						-2.005 + chiralCarbon.getZ());
				Connection carbon3 = new Connection(atomNumber + 4, atomNumber + 2, atomNumber + 5, atomNumber + 6, atomNumber + 7);
				Atom c3 = new Atom((atomNumber + 4 )+ "", "C", "LEU", super.getNumber(),
						this.carbon3, carbon3);

		
				
				//first ch3 hydrogen
				this.carbon3hydrogen1 = new Coords(
						-1.413 + chiralCarbon.getX(),
						-3.650 + chiralCarbon.getY(),
						-1.616 + chiralCarbon.getZ());
				Connection c3h1con = new Connection(atomNumber + 5, atomNumber + 4);
				Atom c3h1 = new Atom((atomNumber + 5) + "", "H", "LEU", super.getNumber(),
						this.carbon3hydrogen1, c3h1con);

				// second ch3 hydrogen
				this.carbon3hydrogen2 = new Coords(
						-2.205 + chiralCarbon.getX(),
						-2.991 + chiralCarbon.getY(),
						-3.069 + chiralCarbon.getZ());
				Connection c3h2con = new Connection(atomNumber + 6, atomNumber + 4);
				Atom c3h2 = new Atom((atomNumber + 6) + "", "H", "LEU", super.getNumber(),
						this.carbon3hydrogen2, c3h2con);
				
				// second ch3 hydrogen
				this.carbon3hydrogen3 = new Coords(
						-2.947 + chiralCarbon.getX(),
						-2.747 + chiralCarbon.getY(),
						-1.461 + chiralCarbon.getZ());
				Connection c3h3con = new Connection(atomNumber + 7, atomNumber + 4);
				Atom c3h3 = new Atom((atomNumber + 7) + "", "H", "LEU", super.getNumber(),
						this.carbon3hydrogen3, c3h3con);
				
				//second ch2o hydrogen
				this.carbon4 = new Coords(
						-2.025 + chiralCarbon.getX(),
						-0.294 + chiralCarbon.getY(),
						-2.429 + chiralCarbon.getZ());
				Connection carbon4 = new Connection(atomNumber + 8, atomNumber + 2, atomNumber + 9, atomNumber + 10, atomNumber + 11);
				Atom c4 = new Atom((atomNumber + 8) + "", "C", "LEU", super.getNumber(),
						this.carbon4, carbon4);
				
				//first ch3 hydrogen
				this.carbon4hydrogen1 = new Coords(
						-1.429 + chiralCarbon.getX(),
						 0.627 + chiralCarbon.getY(),
						-2.486 + chiralCarbon.getZ());
				Connection c4h1con = new Connection(atomNumber + 9, atomNumber + 8);
				Atom c4h1 = new Atom((atomNumber + 9) + "", "H", "LEU", super.getNumber(),
						this.carbon4hydrogen1, c4h1con);

				// second ch3 hydrogen
				this.carbon4hydrogen2 = new Coords(
						-2.918 + chiralCarbon.getX(),
						-0.104+ chiralCarbon.getY(),
						-1.813 + chiralCarbon.getZ());
				Connection c4h2con = new Connection(atomNumber + 10, atomNumber + 8);
				Atom c4h2 = new Atom((atomNumber + 10) + "", "H", "LEU", super.getNumber(),
						this.carbon4hydrogen2, c4h2con);

				// third ch3 hydrogen
				this.carbon4hydrogen3 = new Coords(
						-2.352 + chiralCarbon.getX(),
						-0.535 + chiralCarbon.getY(),
						-3.452 + chiralCarbon.getZ());
				Connection c4h3con = new Connection(atomNumber + 11, atomNumber + 8);
				Atom c4h3 = new Atom((atomNumber + 11) + "", "H", "LEU", super.getNumber(),
						this.carbon4hydrogen3, c4h3con);

				

		Atom[] parent = super.getAtoms();
		Atom[] LEU = new Atom[parent.length + rgroupAtomNumber];
		System.arraycopy(parent, 0, LEU, 0, parent.length);
		LEU[LEU.length - 13] = carbon;
		LEU[LEU.length - 12] = h;
		LEU[LEU.length - 11] = h2;
		LEU[LEU.length - 10] = c2;
		LEU[LEU.length - 9] = c2h1;
		LEU[LEU.length - 8] = c3;
		LEU[LEU.length - 7] = c3h1;
		LEU[LEU.length - 6] = c3h2;
		LEU[LEU.length - 5] = c3h3;
		LEU[LEU.length - 4] = c4;
		LEU[LEU.length - 3] = c4h1;
		LEU[LEU.length - 2] = c4h2;
		LEU[LEU.length - 1] = c4h3;
		super.setAtoms(LEU);
		super.setNumberAtoms(super.getNumberAtoms() + rgroupAtomNumber);
		
	}
}
