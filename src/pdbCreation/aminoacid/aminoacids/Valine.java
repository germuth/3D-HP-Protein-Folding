package pdbCreation.aminoacid.aminoacids;

import pdbCreation.aminoacid.AminoAcid;
import pdbCreation.aminoacid.Atom;
import pdbCreation.aminoacid.Connection;
import pdbCreation.aminoacid.Coords;

public class Valine extends AminoAcid{
	private Coords carbon;
	private Coords hydrogen;
	private Coords carbon2;
	private Coords carbon2hydrogen1;
	private Coords carbon2hydrogen2;
	private Coords carbon2hydrogen3;
	private Coords carbon3;
	private Coords carbon3hydrogen1;
	private Coords carbon3hydrogen2;
	private Coords carbon3hydrogen3;
	private static int rgroupAtomNumber = 10;
	public Valine(Coords chiralCarbon, int atomNumber, int aminoAcidNumber) {
		super(chiralCarbon, atomNumber, aminoAcidNumber, "VAL");
		
		//main CH3 carbon
		this.carbon = new Coords(
				-0.837 + chiralCarbon.getX(),
				-1.304 + chiralCarbon.getY(),
				-0.158 + chiralCarbon.getZ());
		atomNumber += super.getNumberAtoms();
		
		Connection ch3 = new Connection(atomNumber - 1, super.getChiralCarbonNumber(), atomNumber, atomNumber+1, atomNumber+5);
		Atom carbon = new Atom((atomNumber - 1) + "", "C", "VAL", super.getNumber(), this.carbon, ch3);
		
		//first ch2o hydrogen
		this.hydrogen = new Coords(
				-1.672 + chiralCarbon.getX(),
				-1.256 + chiralCarbon.getY(),
				 0.563 + chiralCarbon.getZ());
		Connection hydrogen = new Connection(atomNumber, atomNumber - 1);
		Atom h = new Atom((atomNumber) + "", "H", "VAL", super.getNumber(), this.hydrogen, hydrogen);
		
		//second ch2o hydrogen
		this.carbon2 = new Coords(
				 0.009 + chiralCarbon.getX(),
				-2.561 + chiralCarbon.getY(),
				 0.185 + chiralCarbon.getZ());
		Connection carbon2 = new Connection(atomNumber + 1, atomNumber - 1, atomNumber + 2, atomNumber + 3, atomNumber + 4);
		Atom c2 = new Atom((atomNumber + 1) + "", "C", "VAL", super.getNumber(),
				this.carbon2, carbon2);
		
		//first ch3 hydrogen
		this.carbon2hydrogen1 = new Coords(
				 0.838 + chiralCarbon.getX(),
				-2.683 + chiralCarbon.getY(),
				-0.528 + chiralCarbon.getZ());
		Connection c2h1con = new Connection(atomNumber + 2, atomNumber + 1);
		Atom c2h1 = new Atom((atomNumber + 2) + "", "H", "VAL", super.getNumber(),
				this.carbon2hydrogen1, c2h1con);

		// second ch3 hydrogen
		this.carbon2hydrogen2 = new Coords(
				-0.618 + chiralCarbon.getX(),
				-3.466 + chiralCarbon.getY(),
				 0.145 + chiralCarbon.getZ());
		Connection c2h2con = new Connection(atomNumber + 3, atomNumber + 1);
		Atom c2h2 = new Atom((atomNumber + 3) + "", "H", "VAL", super.getNumber(),
				this.carbon2hydrogen2, c2h2con);

		// third ch3 hydrogen
		this.carbon2hydrogen3 = new Coords(
				 0.429 + chiralCarbon.getX(),
				-2.477 + chiralCarbon.getY(),
				 1.200 + chiralCarbon.getZ());
		Connection c2h3con = new Connection(atomNumber + 4, atomNumber + 1);
		Atom c2h3 = new Atom((atomNumber + 4) + "", "H", "VAL", super.getNumber(),
				this.carbon2hydrogen3, c2h3con);
		
		//second ch3
				this.carbon3 = new Coords(
						-1.448 + chiralCarbon.getX(),
						-1.435 + chiralCarbon.getY(),
						-1.581 + chiralCarbon.getZ());
				Connection carbon3 = new Connection(atomNumber + 5, atomNumber - 1, atomNumber + 6, atomNumber + 7, atomNumber + 8);
				Atom c3 = new Atom((atomNumber + 5 )+ "", "C", "VAL", super.getNumber(),
						this.carbon3, carbon3);
				
				//first ch3 hydrogen
				this.carbon3hydrogen1 = new Coords(
						-0.661 + chiralCarbon.getX(),
						-1.567 + chiralCarbon.getY(),
						-2.338 + chiralCarbon.getZ());
				Connection c3h1con = new Connection(atomNumber + 6, atomNumber + 5);
				Atom c3h1 = new Atom((atomNumber + 6) + "", "H", "VAL", super.getNumber(),
						this.carbon3hydrogen1, c3h1con);

				// second ch3 hydrogen
				this.carbon3hydrogen2 = new Coords(
						-2.036 + chiralCarbon.getX(),
						-0.541 + chiralCarbon.getY(),
						-1.836 + chiralCarbon.getZ());
				Connection c3h2con = new Connection(atomNumber + 7, atomNumber + 5);
				Atom c3h2 = new Atom((atomNumber + 7) + "", "H", "VAL", super.getNumber(),
						this.carbon3hydrogen2, c3h2con);

				// third ch3 hydrogen
				this.carbon3hydrogen3 = new Coords(
						-2.116 + chiralCarbon.getX(),
						-2.310 + chiralCarbon.getY(),
						-1.626 + chiralCarbon.getZ());
				Connection c3h3con = new Connection(atomNumber + 8, atomNumber + 5);
				Atom c3h3 = new Atom((atomNumber + 8) + "", "H", "VAL", super.getNumber(),
						this.carbon3hydrogen3, c3h3con);

		Atom[] parent = super.getAtoms();
		Atom[] VAL = new Atom[parent.length + rgroupAtomNumber];
		System.arraycopy(parent, 0, VAL, 0, parent.length);
		VAL[VAL.length - 10] = carbon;
		VAL[VAL.length - 9] = h;
		VAL[VAL.length - 8] = c2;
		VAL[VAL.length - 7] = c2h1;
		VAL[VAL.length - 6] = c2h2;
		VAL[VAL.length - 5] = c2h3;
		VAL[VAL.length - 4] = c3;
		VAL[VAL.length - 3] = c3h1;
		VAL[VAL.length - 2] = c3h2;
		VAL[VAL.length - 1] = c3h3;
		super.setAtoms(VAL);
		super.setNumberAtoms(super.getNumberAtoms() + rgroupAtomNumber);
		
	}
}
