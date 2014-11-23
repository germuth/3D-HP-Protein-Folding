package pdbCreation.aminoacid.aminoacids;

import pdbCreation.aminoacid.AminoAcid;
import pdbCreation.aminoacid.Atom;
import pdbCreation.aminoacid.Connection;
import pdbCreation.aminoacid.Coords;
/**
 * Alanine
 * 
 * This class represents Alanine, One of the Hydrophobic Amino acids.
 * @author Aaron Germuth
 *
 */
public class Alanine extends AminoAcid{
	private Coords carbon;
	private Coords firstHydrogen;
	private Coords secondHydrogen;
	private Coords thirdHydrogen;
	private static int rgroupAtomNumber = 4;
	public Alanine(Coords chiralCarbon, int atomNumber, int aminoAcidNumber) {
		super(chiralCarbon, atomNumber, aminoAcidNumber, "ALA");
		
		//main CH3 carbon
		this.carbon = new Coords(
				-0.933 + chiralCarbon.getX(),
				-1.232 + chiralCarbon.getY(),
				-0.136 + chiralCarbon.getZ());
		atomNumber += super.getNumberAtoms();
		
		Connection ch3 = new Connection(atomNumber - 1, super.getChiralCarbonNumber(), atomNumber, atomNumber+1, atomNumber+2);
		Atom carbon = new Atom((atomNumber - 1) + "", "C", "ALA", super.getNumber(), this.carbon, ch3);
		
		//first ch3 hydrogen
		this.firstHydrogen = new Coords(
				-1.703 + chiralCarbon.getX(),
				-1.215 + chiralCarbon.getY(),
				0.651 + chiralCarbon.getZ());
		Connection hydrogen = new Connection(atomNumber, atomNumber - 1);
		Atom h = new Atom((atomNumber) + "", "H", "ALA", super.getNumber(), this.firstHydrogen, hydrogen);
		
		//second ch3 hydrogen
		this.secondHydrogen = new Coords(
				-0.351+ chiralCarbon.getX(),
				-2.161 + chiralCarbon.getY(),
				-0.037 + chiralCarbon.getZ());
		Connection hydrogen2 = new Connection(atomNumber + 1, atomNumber - 1);
		Atom h2 = new Atom((atomNumber + 1) + "", "H", "ALA", super.getNumber(),
				this.secondHydrogen, hydrogen2);

		// third and final ch3 hydrogen
		this.thirdHydrogen = new Coords(
				-1.433 + chiralCarbon.getX(),
				-1.231 + chiralCarbon.getY(),
				-1.117 + chiralCarbon.getZ());
		Connection hydrogen3 = new Connection(atomNumber + 2, atomNumber - 1);
		Atom h3 = new Atom((atomNumber + 2) + "", "H", "ALA", super.getNumber(),
				this.thirdHydrogen, hydrogen3);

		Atom[] parent = super.getAtoms();
		Atom[] ala = new Atom[parent.length + rgroupAtomNumber];
		System.arraycopy(parent, 0, ala, 0, parent.length);
		ala[ala.length - 4] = carbon;
		ala[ala.length - 3] = h;
		ala[ala.length - 2] = h2;
		ala[ala.length - 1] = h3;
		super.setAtoms(ala);
		super.setNumberAtoms(super.getNumberAtoms() + rgroupAtomNumber);
		
	}
}
