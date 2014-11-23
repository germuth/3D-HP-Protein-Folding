package pdbCreation.aminoacid.aminoacids;

import pdbCreation.aminoacid.AminoAcid;
import pdbCreation.aminoacid.Atom;
import pdbCreation.aminoacid.Connection;
import pdbCreation.aminoacid.Coords;

public class Methionine extends AminoAcid{
	private Coords carbon;
	private Coords hydrogen;
	private Coords hydrogen2;
	private Coords carbon2;
	private Coords carbon2hydrogen1;
	private Coords carbon2hydrogen2;
	private Coords sulphur;
	private Coords carbon3;
	private Coords carbon3hydrogen1;
	private Coords carbon3hydrogen2;
	private Coords carbon3hydrogen3;
	
	private static int rgroupAtomNumber = 11;
	
	public Methionine(Coords chiralCarbon, int atomNumber, int aminoAcidNumber) {
		super(chiralCarbon, atomNumber, aminoAcidNumber, "MET");
		
		//main CH3 carbon
		this.carbon = new Coords(
				-0.710 + chiralCarbon.getX(),
				-1.278 + chiralCarbon.getY(),
				-0.527 + chiralCarbon.getZ());
		atomNumber += super.getNumberAtoms();
		
		Connection ch3 = new Connection(atomNumber - 1, super.getChiralCarbonNumber(), atomNumber, atomNumber+1, atomNumber+2);
		Atom carbon = new Atom((atomNumber - 1) + "", "C", "MET", super.getNumber(), this.carbon, ch3);
		
		//first ch2o hydrogen
		this.hydrogen = new Coords(
				-1.573 + chiralCarbon.getX(),
				-1.499 + chiralCarbon.getY(),
				 0.121 + chiralCarbon.getZ());
		Connection hydrogen = new Connection(atomNumber, atomNumber - 1);
		Atom h = new Atom((atomNumber) + "", "H", "MET", super.getNumber(), this.hydrogen, hydrogen);
		
		//second ch2o hydrogen
		this.hydrogen2 = new Coords(
				-0.014 + chiralCarbon.getX(),
				-2.131 + chiralCarbon.getY(),
				-0.471 + chiralCarbon.getZ());
		Connection hydrogen2 = new Connection(atomNumber + 1, atomNumber - 1);
		Atom h2 = new Atom((atomNumber + 1) + "", "H", "MET", super.getNumber(),
				this.hydrogen2, hydrogen2);
		
		//second ch3
		this.carbon2 = new Coords(
				-1.210 + chiralCarbon.getX(),
				-1.122+ chiralCarbon.getY(),
				-1.986 + chiralCarbon.getZ());
		Connection carbon2 = new Connection(atomNumber + 2, atomNumber - 1, atomNumber + 3, atomNumber + 4, atomNumber + 5);
		Atom c2 = new Atom((atomNumber + 2 )+ "", "C", "MET", super.getNumber(),
				this.carbon2, carbon2);
		
		//first ch3 hydrogen
		this.carbon2hydrogen1 = new Coords(
				-0.366 + chiralCarbon.getX(),
				-0.991 + chiralCarbon.getY(),
				-2.681 + chiralCarbon.getZ());
		Connection c2h1con = new Connection(atomNumber + 3, atomNumber + 2);
		Atom c2h1 = new Atom((atomNumber + 3) + "", "H", "MET", super.getNumber(),
				this.carbon2hydrogen1, c2h1con);
		
		//first ch3 hydrogen
				this.carbon2hydrogen2 = new Coords(
						-1.888 + chiralCarbon.getX(),
						-0.259 + chiralCarbon.getY(),
						-2.074 + chiralCarbon.getZ());
				Connection c2h2con = new Connection(atomNumber + 4, atomNumber + 2);
				Atom c2h2 = new Atom((atomNumber + 4) + "", "H", "MET", super.getNumber(),
						this.carbon2hydrogen2, c2h2con);
				
				//first ch3 hydrogen
				this.sulphur = new Coords(
						-2.114 + chiralCarbon.getX(),
						-2.631 + chiralCarbon.getY(),
						-2.464 + chiralCarbon.getZ());
				Connection scon = new Connection(atomNumber +5 , atomNumber + 2, atomNumber + 6);
				Atom s = new Atom((atomNumber + 5) + "", "S", "MET", super.getNumber(),
						this.sulphur, scon);
		
		//second ch3
				this.carbon3 = new Coords(
						-2.592 + chiralCarbon.getX(),
						-2.191 + chiralCarbon.getY(),
						-4.164 + chiralCarbon.getZ());
				Connection carbon3 = new Connection(atomNumber + 6, atomNumber + 5, atomNumber + 7, atomNumber + 8, atomNumber +9);
				Atom c3 = new Atom((atomNumber + 6 )+ "", "C", "MET", super.getNumber(),
						this.carbon3, carbon3);

		
				
				//first ch3 hydrogen
				this.carbon3hydrogen1 = new Coords(
						-3.164 + chiralCarbon.getX(),
						-3.022 + chiralCarbon.getY(),
						-4.603 + chiralCarbon.getZ());
				Connection c3h1con = new Connection(atomNumber + 7, atomNumber + 6);
				Atom c3h1 = new Atom((atomNumber + 7) + "", "H", "MET", super.getNumber(),
						this.carbon3hydrogen1, c3h1con);

				// second ch3 hydrogen
				this.carbon3hydrogen2 = new Coords(
						-1.692 + chiralCarbon.getX(),
						-2.009 + chiralCarbon.getY(),
						-4.769 + chiralCarbon.getZ());
				Connection c3h2con = new Connection(atomNumber + 8, atomNumber + 6);
				Atom c3h2 = new Atom((atomNumber +8) + "", "H", "MET", super.getNumber(),
						this.carbon3hydrogen2, c3h2con);
				
				// second ch3 hydrogen
				this.carbon3hydrogen3 = new Coords(
						-3.218 + chiralCarbon.getX(),
						-1.287 + chiralCarbon.getY(),
						-4.153 + chiralCarbon.getZ());
				Connection c3h3con = new Connection(atomNumber + 9, atomNumber + 6);
				Atom c3h3 = new Atom((atomNumber + 9) + "", "H", "MET", super.getNumber(),
						this.carbon3hydrogen3, c3h3con);
				

		Atom[] parent = super.getAtoms();
		Atom[] MET = new Atom[parent.length + rgroupAtomNumber];
		System.arraycopy(parent, 0, MET, 0, parent.length);
		MET[MET.length - 11] = carbon;
		MET[MET.length - 10] = h;
		MET[MET.length - 9] = h2;
		MET[MET.length - 8] = c2;
		MET[MET.length - 7] = c2h1;
		MET[MET.length - 6] = c2h2;
		MET[MET.length - 5] = s;
		MET[MET.length - 4] = c3;
		MET[MET.length - 3] = c3h1;
		MET[MET.length - 2] = c3h2;
		MET[MET.length - 1] = c3h3;
		super.setAtoms(MET);
		super.setNumberAtoms(super.getNumberAtoms() + rgroupAtomNumber);
		
	}
}
