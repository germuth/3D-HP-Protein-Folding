package pdbCreation.aminoacid.aminoacids;

import pdbCreation.aminoacid.AminoAcid;
import pdbCreation.aminoacid.Atom;
import pdbCreation.aminoacid.Connection;
import pdbCreation.aminoacid.Coords;

public class Threonine extends AminoAcid{
	private Coords carbon;
	private Coords hydrogen;
	private Coords carbon2;
	private Coords oxygen;
	private Coords oxyHydrogen;
	private Coords carbon2hydrogen1;
	private Coords carbon2hydrogen2;
	private Coords carbon2hydrogen3;
	private static int rgroupAtomNumber = 8;
	public Threonine(Coords chiralCarbon, int atomNumber, int aminoAcidNumber) {
		super(chiralCarbon, atomNumber, aminoAcidNumber, "THR");
		
		//main CH3 carbon
		this.carbon = new Coords(
				-0.858 + chiralCarbon.getX(),
				-1.225 + chiralCarbon.getY(),
				-0.427 + chiralCarbon.getZ());
		atomNumber += super.getNumberAtoms();
		
		Connection ch3 = new Connection(atomNumber - 1, super.getChiralCarbonNumber(), atomNumber, atomNumber+1, atomNumber+2);
		Atom carbon = new Atom((atomNumber - 1) + "", "C", "THR", super.getNumber(), this.carbon, ch3);
		
		//first ch2o hydrogen
		this.hydrogen = new Coords(
				-0.200 + chiralCarbon.getX(),
				-2.113 + chiralCarbon.getY(),
				-0.438 + chiralCarbon.getZ());
		Connection hydrogen = new Connection(atomNumber, atomNumber - 1);
		Atom h = new Atom((atomNumber) + "", "H", "THR", super.getNumber(), this.hydrogen, hydrogen);
		
		//second ch2o hydrogen
		this.carbon2 = new Coords(
				-2.031 + chiralCarbon.getX(),
				-1.490 + chiralCarbon.getY(),
				 0.556 + chiralCarbon.getZ());
		Connection carbon2 = new Connection(atomNumber + 1, atomNumber - 1, atomNumber + 4, atomNumber + 5, atomNumber + 6);
		Atom c2 = new Atom((atomNumber + 1) + "", "C", "THR", super.getNumber(),
				this.carbon2, carbon2);

		// oxygen
		this.oxygen = new Coords(
				-1.364 + chiralCarbon.getX(),
				-0.983 + chiralCarbon.getY(),
				-1.752 + chiralCarbon.getZ());
		Connection oxy = new Connection(atomNumber + 2, atomNumber - 1);
		Atom ox = new Atom((atomNumber + 2) + "", "O", "THR", super.getNumber(),
				this.oxygen, oxy);
		
		// third and final ch3 hydrogen
		this.oxyHydrogen = new Coords(
				-1.867 + chiralCarbon.getX(),
				-1.715 + chiralCarbon.getY(), 
			   	-2.094 + chiralCarbon.getZ());
		Connection oxHydro = new Connection(atomNumber + 3, atomNumber + 2);
		Atom oxh = new Atom((atomNumber + 3) + "", "H", "THR", super.getNumber(),
				this.oxyHydrogen, oxHydro);
		
		//first ch3 hydrogen
		this.carbon2hydrogen1 = new Coords(
				-1.658 + chiralCarbon.getX(),
				-1.588 + chiralCarbon.getY(),
				 1.587 + chiralCarbon.getZ());
		Connection c2h1con = new Connection(atomNumber + 4, atomNumber + 1);
		Atom c2h1 = new Atom((atomNumber + 4) + "", "H", "THR", super.getNumber(),
				this.carbon2hydrogen1, c2h1con);

		// second ch3 hydrogen
		this.carbon2hydrogen2 = new Coords(
				-2.546 + chiralCarbon.getX(),
				-2.424 + chiralCarbon.getY(),
				 0.283 + chiralCarbon.getZ());
		Connection c2h2con = new Connection(atomNumber + 5, atomNumber + 1);
		Atom c2h2 = new Atom((atomNumber + 5) + "", "H", "THR", super.getNumber(),
				this.carbon2hydrogen2, c2h2con);

		// third ch3 hydrogen
		this.carbon2hydrogen3 = new Coords(
				-2.764 + chiralCarbon.getX(),
				-0.671 + chiralCarbon.getY(),
				 0.516 + chiralCarbon.getZ());
		Connection c2h3con = new Connection(atomNumber + 6, atomNumber + 1);
		Atom c2h3 = new Atom((atomNumber + 6) + "", "H", "THR", super.getNumber(),
				this.carbon2hydrogen3, c2h3con);

		Atom[] parent = super.getAtoms();
		Atom[] THR = new Atom[parent.length + rgroupAtomNumber];
		System.arraycopy(parent, 0, THR, 0, parent.length);
		THR[THR.length - 8] = carbon;
		THR[THR.length - 7] = ox;
		THR[THR.length - 6] = oxh;
		THR[THR.length - 5] = h;
		THR[THR.length - 4] = c2;
		THR[THR.length - 3] = c2h1;
		THR[THR.length - 2] = c2h2;
		THR[THR.length - 1] = c2h3;
		super.setAtoms(THR);
		super.setNumberAtoms(super.getNumberAtoms() + rgroupAtomNumber);
		
	}
}
