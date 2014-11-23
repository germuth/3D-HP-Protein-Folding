package pdbCreation.aminoacid.aminoacids;

import pdbCreation.aminoacid.AminoAcid;
import pdbCreation.aminoacid.Atom;
import pdbCreation.aminoacid.Connection;
import pdbCreation.aminoacid.Coords;

public class Serine extends AminoAcid{
	private Coords carbon;
	private Coords firstHydrogen;
	private Coords secondHydrogen;
	private Coords oxygen;
	private Coords oxyHydrogen;
	private static int rgroupAtomNumber = 5;
	public Serine(Coords chiralCarbon, int atomNumber, int aminoAcidNumber) {
		super(chiralCarbon, atomNumber, aminoAcidNumber, "SER");
		
		//main CH3 carbon
		this.carbon = new Coords(
				-0.933 + chiralCarbon.getX(),
				-1.232 + chiralCarbon.getY(),
				-0.136 + chiralCarbon.getZ());
		atomNumber += super.getNumberAtoms();
		
		Connection ch3 = new Connection(atomNumber - 1, super.getChiralCarbonNumber(), atomNumber, atomNumber+1, atomNumber+2);
		Atom carbon = new Atom((atomNumber - 1) + "", "C", "SER", super.getNumber(), this.carbon, ch3);
		
		//first ch2o hydrogen
		this.firstHydrogen = new Coords(
				-0.351+ chiralCarbon.getX(),
				-2.161 + chiralCarbon.getY(),
				-0.037 + chiralCarbon.getZ());
		Connection hydrogen = new Connection(atomNumber, atomNumber - 1);
		Atom h = new Atom((atomNumber) + "", "H", "SER", super.getNumber(), this.firstHydrogen, hydrogen);
		
		//second ch2o hydrogen
		this.secondHydrogen = new Coords(
				-1.433 + chiralCarbon.getX(),
				-1.231 + chiralCarbon.getY(),
				-1.117 + chiralCarbon.getZ());
		Connection hydrogen2 = new Connection(atomNumber + 1, atomNumber - 1);
		Atom h2 = new Atom((atomNumber + 1) + "", "H", "SER", super.getNumber(),
				this.secondHydrogen, hydrogen2);

		// oxygen
		this.oxygen = new Coords(
				-1.703 + chiralCarbon.getX(),
				-1.215 + chiralCarbon.getY(),
				0.651 + chiralCarbon.getZ());
		Connection hydrogen3 = new Connection(atomNumber + 2, atomNumber - 1);
		Atom ox = new Atom((atomNumber + 2) + "", "O", "SER", super.getNumber(),
				this.oxygen, hydrogen3);
		
		// third and final ch3 hydrogen
		this.oxyHydrogen = new Coords(
				-1.983 + chiralCarbon.getX(),
				-1.974 + chiralCarbon.getY(), 
			   	 1.276 + chiralCarbon.getZ());
		Connection oxHydro = new Connection(atomNumber + 3, atomNumber + 2);
		Atom oxh = new Atom((atomNumber + 3) + "", "H", "SER", super.getNumber(),
				this.oxyHydrogen, oxHydro);

		Atom[] parent = super.getAtoms();
		Atom[] SER = new Atom[parent.length + rgroupAtomNumber];
		System.arraycopy(parent, 0, SER, 0, parent.length);
		SER[SER.length - 5] = carbon;
		SER[SER.length - 4] = h;
		SER[SER.length - 3] = h2;
		SER[SER.length - 2] = ox;
		SER[SER.length - 1] = oxh;
		super.setAtoms(SER);
		super.setNumberAtoms(super.getNumberAtoms() + rgroupAtomNumber);
		
	}
}
