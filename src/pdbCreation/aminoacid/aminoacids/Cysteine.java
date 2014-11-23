package pdbCreation.aminoacid.aminoacids;

import pdbCreation.aminoacid.AminoAcid;
import pdbCreation.aminoacid.Atom;
import pdbCreation.aminoacid.Connection;
import pdbCreation.aminoacid.Coords;

public class Cysteine extends AminoAcid{
	private Coords carbon;
	private Coords sulphur;
	private Coords sulphurHydrogen;
	private Coords firstHydrogen;
	private Coords secondHydrogen;
	private static int rgroupAtomNumber = 5;
	public Cysteine(Coords chiralCarbon, int atomNumber, int aminoAcidNumber) {
		super(chiralCarbon, atomNumber, aminoAcidNumber, "CYS");
		
		//carbon
		this.carbon = new Coords(
				//-1.310 + chiralCarbon.getX(),
				// 0.715 + chiralCarbon.getY(),
				//-0.419 + chiralCarbon.getZ());
				-0.933 + chiralCarbon.getX(),
				-1.232 + chiralCarbon.getY(),
				-0.136 + chiralCarbon.getZ());
		atomNumber += super.getNumberAtoms();
		
		Connection c = new Connection(atomNumber - 1, super.getChiralCarbonNumber(), atomNumber, atomNumber+2, atomNumber+3);
		Atom carbon = new Atom((atomNumber - 1) + "", "C", "CYS", super.getNumber(), this.carbon, c);
		
		//sulphur
		this.sulphur = new Coords(
				//-1.170 + chiralCarbon.getX(),
				// 1.316 + chiralCarbon.getY(),
				//-2.136 + chiralCarbon.getZ());
				-1.703 + chiralCarbon.getX(),
				-1.215 + chiralCarbon.getY(),
				0.651 + chiralCarbon.getZ());
		Connection sulphur = new Connection(atomNumber, atomNumber - 1, atomNumber + 1);
		Atom s = new Atom((atomNumber) + "", "S", "CYS", super.getNumber(), this.sulphur, sulphur);
		
		//sulphur hydrogen
		this.sulphurHydrogen = new Coords(
				//-2.083 + chiralCarbon.getX(),
				//1.735 + chiralCarbon.getY(),
				//-2.220 + chiralCarbon.getZ());
				-1.983 + chiralCarbon.getX(),
				-1.974 + chiralCarbon.getY(),
				 1.276 + chiralCarbon.getZ());
		Connection sHydrogen = new Connection(atomNumber + 1, atomNumber);
		Atom sh = new Atom((atomNumber + 1) + "", "H", "CYS", super.getNumber(),
				this.sulphurHydrogen, sHydrogen);

		// carbonHydrogen
		this.firstHydrogen = new Coords(
				//-2.163 + chiralCarbon.getX(),
				// 0.021 + chiralCarbon.getY(), 
				//-0.340 + chiralCarbon.getZ());
				-0.351+ chiralCarbon.getX(),
				-2.161 + chiralCarbon.getY(),
				-0.037 + chiralCarbon.getZ());
		Connection hydrogen1 = new Connection(atomNumber + 2, atomNumber - 1);
		Atom h1 = new Atom((atomNumber + 2) + "", "H", "CYS", super.getNumber(),
				this.firstHydrogen, hydrogen1);
		
		// 2nd carbonHydrogen
		this.secondHydrogen = new Coords(
				//-1.495 + chiralCarbon.getX(),
				// 1.578 + chiralCarbon.getY(),
				// 0.241 + chiralCarbon.getZ());
				-1.433 + chiralCarbon.getX(),
				-1.231 + chiralCarbon.getY(),
				-1.117 + chiralCarbon.getZ());
		Connection hydrogen2 = new Connection(atomNumber + 3, atomNumber - 1);
		Atom h2 = new Atom((atomNumber + 3) + "", "H", "CYS", super.getNumber(),
				this.secondHydrogen, hydrogen2);

		Atom[] parent = super.getAtoms();
		Atom[] cys = new Atom[parent.length + rgroupAtomNumber];
		System.arraycopy(parent, 0, cys, 0, parent.length);
		cys[cys.length - 5] = carbon;
		cys[cys.length - 4] = s;
		cys[cys.length - 3] = sh;
		cys[cys.length - 2] = h1;
		cys[cys.length - 1] = h2;
		super.setAtoms(cys);
		super.setNumberAtoms(super.getNumberAtoms() + rgroupAtomNumber);
		
	}
}
