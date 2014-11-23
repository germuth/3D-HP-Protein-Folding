package pdbCreation.aminoacid.aminoacids;

import pdbCreation.aminoacid.AminoAcid;
import pdbCreation.aminoacid.Atom;
import pdbCreation.aminoacid.Connection;
import pdbCreation.aminoacid.Coords;

public class Glycine extends AminoAcid{
	private Coords hydrogen;
	private static int rgroupAtomNumber = 1;
	public Glycine(Coords chiralCarbon, int atomNumber, int aminoAcidNumber) {
		super(chiralCarbon, atomNumber, aminoAcidNumber, "GLY");
		this.hydrogen = new Coords(
				-0.496 + chiralCarbon.getX(),
				-0.970 + chiralCarbon.getY(),
				-0.167 + chiralCarbon.getZ());
		Connection hydrogen = new Connection(atomNumber + 9, atomNumber + 7);
		Atom h = new Atom((atomNumber + 9) + "", "H", "GLY", super.getNumber(), this.hydrogen, hydrogen);
		Atom[] parent = super.getAtoms();
		Atom[] gly = new Atom[parent.length + rgroupAtomNumber];
		System.arraycopy(parent, 0, gly, 0, parent.length);
		gly[gly.length - 1] = h;
		super.setAtoms(gly);
		super.setNumberAtoms(super.getNumberAtoms() + rgroupAtomNumber);
	}
}
