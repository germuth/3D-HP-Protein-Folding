package pdbCreation.aminoacid.aminoacids;

import pdbCreation.aminoacid.AminoAcid;
import pdbCreation.aminoacid.Atom;
import pdbCreation.aminoacid.Connection;
import pdbCreation.aminoacid.Coords;

public class Histidine extends AminoAcid{
	private Coords carbon;
	private Coords hydrogen;
	private Coords hydrogen2;
	private Coords carbon2;
	private Coords carbon3;
	private Coords carbon3hydrogen1;
	private Coords nitrogen1;
	private Coords nitrogen1hydrogen1;
	private Coords carbon4;
	private Coords carbon4hydrogen1;
	private Coords nitrogen2;
	
	private static int rgroupAtomNumber = 11;
	public Histidine(Coords chiralCarbon, int atomNumber, int aminoAcidNumber) {
		super(chiralCarbon, atomNumber, aminoAcidNumber, "HIS");
		

		this.carbon = new Coords(
				-0.395 + chiralCarbon.getX(),
				-1.502 + chiralCarbon.getY(),
				-0.024 + chiralCarbon.getZ());
		atomNumber += super.getNumberAtoms();
		
		Connection ch3 = new Connection(atomNumber - 1, super.getChiralCarbonNumber(), atomNumber, atomNumber+1, atomNumber+2);
		Atom carbon = new Atom((atomNumber - 1) + "", "C", "HIS", super.getNumber(), this.carbon, ch3);
		

		this.hydrogen = new Coords(
				 0.519 + chiralCarbon.getX(),
				-2.115 + chiralCarbon.getY(),
				 0.029 + chiralCarbon.getZ());
		Connection hydrogen = new Connection(atomNumber, atomNumber - 1);
		Atom h = new Atom((atomNumber) + "", "H", "HIS", super.getNumber(), this.hydrogen, hydrogen);
		

		this.hydrogen2 = new Coords(
				-0.924 + chiralCarbon.getX(),
				-1.76 + chiralCarbon.getY(),
				-0.954 + chiralCarbon.getZ());
		Connection hydrogen2 = new Connection(atomNumber + 1, atomNumber - 1);
		Atom h2 = new Atom((atomNumber + 1) + "", "H", "HIS", super.getNumber(),
				this.hydrogen2, hydrogen2);
		

		this.carbon2 = new Coords(
				-1.296 + chiralCarbon.getX(),
				-1.827 + chiralCarbon.getY(),
				 1.141 + chiralCarbon.getZ());
		Connection carbon2 = new Connection(atomNumber + 2, atomNumber - 1, atomNumber + 3, atomNumber + 3, atomNumber + 9);
		Atom c2 = new Atom((atomNumber + 2 )+ "", "C", "HIS", super.getNumber(),
				this.carbon2, carbon2);

		this.carbon3 = new Coords(
				-1.012 + chiralCarbon.getX(),
				-2.673 + chiralCarbon.getY(),
				 2.138 + chiralCarbon.getZ());
		Connection c3con = new Connection(atomNumber + 3, atomNumber + 2, atomNumber + 2, atomNumber + 4, atomNumber + 5);
		Atom c3 = new Atom((atomNumber + 3) + "", "C", "HIS",
				super.getNumber(), this.carbon3, c3con);
		
		this.carbon3hydrogen1 = new Coords(
				-0.132 + chiralCarbon.getX(),
				-3.287 + chiralCarbon.getY(),
				 2.326 + chiralCarbon.getZ());
		Connection c3h1con = new Connection(atomNumber + 4, atomNumber + 3);
		Atom c3h1 = new Atom((atomNumber + 4) + "", "H", "HIS", super.getNumber(),
				this.carbon3hydrogen1, c3h1con);
		
		this.nitrogen1 = new Coords(
				-2.215 + chiralCarbon.getX(), 
				-2.528 + chiralCarbon.getY(), 
				 2.898 + chiralCarbon.getZ());
		Connection n1con = new Connection(atomNumber + 5, atomNumber + 7, atomNumber + 7, atomNumber + 3);
		Atom n1 = new Atom((atomNumber + 5) + "", "N", "TRP",
				super.getNumber(), this.nitrogen1, n1con);


		this.nitrogen1hydrogen1 = new Coords(
				-3.128 + chiralCarbon.getX(), 
				-1.534 + chiralCarbon.getY(),
				 0.418 + chiralCarbon.getZ());
		Connection n1hcon = new Connection(atomNumber + 6, atomNumber + 9);
		Atom n1h1 = new Atom((atomNumber + 6) + "", "H", "TRP",
				super.getNumber(), this.nitrogen1hydrogen1, n1hcon);

		this.carbon4 = new Coords(
				-3.036 + chiralCarbon.getX(), 
				-1.719 + chiralCarbon.getY(), 
				 2.387 + chiralCarbon.getZ());
		Connection c4con = new Connection(atomNumber + 7, atomNumber + 5, atomNumber + 5, atomNumber + 8, atomNumber + 9);
		Atom c4 = new Atom((atomNumber + 7) + "", "C", "HIS",
				super.getNumber(), this.carbon4, c4con);
		
		this.carbon4hydrogen1 = new Coords(
				-4.008 + chiralCarbon.getX(), 
				-1.454 + chiralCarbon.getY(), 
				 2.804 + chiralCarbon.getZ());
		Connection c4h1con = new Connection(atomNumber + 8, atomNumber + 7);
		Atom c4h1 = new Atom((atomNumber + 8) + "", "H", "HIS", super.getNumber(),
				this.carbon4hydrogen1, c4h1con);
				
		this.nitrogen2 = new Coords(
				-2.504 + chiralCarbon.getX(), 
				-1.248 + chiralCarbon.getY(), 
				 1.255 + chiralCarbon.getZ());
		Connection n2con = new Connection(atomNumber + 9, atomNumber + 2, atomNumber + 6, atomNumber + 7);
		Atom n2 = new Atom((atomNumber + 9) + "", "N", "TRP",
				super.getNumber(), this.nitrogen2, n2con);

		Atom[] parent = super.getAtoms();
		Atom[] HIS = new Atom[parent.length + rgroupAtomNumber];
		System.arraycopy(parent, 0, HIS, 0, parent.length);
		HIS[HIS.length - 11] = carbon;
		HIS[HIS.length - 10] = h;
		HIS[HIS.length - 9] = h2;
		HIS[HIS.length - 8] = c2;
		HIS[HIS.length - 7] = c3;
		HIS[HIS.length - 6] = c3h1;
		HIS[HIS.length - 5] = n1;
		HIS[HIS.length - 4] = n1h1;
		HIS[HIS.length - 3] = c4;
		HIS[HIS.length - 2] = c4h1;
		HIS[HIS.length - 1] = n2;
		super.setAtoms(HIS);
		super.setNumberAtoms(super.getNumberAtoms() + rgroupAtomNumber);
		
	}
}
