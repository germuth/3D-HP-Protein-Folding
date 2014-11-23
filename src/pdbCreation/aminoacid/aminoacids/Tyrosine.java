package pdbCreation.aminoacid.aminoacids;

import pdbCreation.aminoacid.AminoAcid;
import pdbCreation.aminoacid.Atom;
import pdbCreation.aminoacid.Connection;
import pdbCreation.aminoacid.Coords;

public class Tyrosine extends AminoAcid{
	private Coords carbon;
	private Coords hydrogen;
	private Coords hydrogen2;
	private Coords carbon1;
	private Coords carbon2;
	private Coords carbon2hydrogen1;
	private Coords carbon3;
	private Coords carbon3hydrogen1;
	private Coords carbon4;
	private Coords oxygen;
	private Coords oxyHydrogen;
	private Coords carbon5;
	private Coords carbon5hydrogen1;
	private Coords carbon6;
	private Coords carbon6hydrogen1;
	private static int rgroupAtomNumber = 15;
	public Tyrosine(Coords chiralCarbon, int atomNumber, int aminoAcidNumber) {
		super(chiralCarbon, atomNumber, aminoAcidNumber, "TYR");
		
		//main CH3 carbon
		this.carbon = new Coords(
				-0.631 + chiralCarbon.getX(),
				-1.411 + chiralCarbon.getY(),
				-0.146 + chiralCarbon.getZ());
		atomNumber += super.getNumberAtoms();
		
		Connection ch3 = new Connection(atomNumber - 1, super.getChiralCarbonNumber(), atomNumber, atomNumber+1, atomNumber+2);
		Atom carbon = new Atom((atomNumber - 1) + "", "C", "TYR", super.getNumber(), this.carbon, ch3);
		
		//first ch2o hydrogen
		this.hydrogen = new Coords(
				-1.457 + chiralCarbon.getX(),
				-1.524 + chiralCarbon.getY(),
				 0.574 + chiralCarbon.getZ());
		Connection hydrogen = new Connection(atomNumber, atomNumber - 1);
		Atom h = new Atom((atomNumber) + "", "H", "TYR", super.getNumber(), this.hydrogen, hydrogen);
		
		//second ch2o hydrogen
		this.hydrogen2 = new Coords(
				 0.119 + chiralCarbon.getX(),
				-2.183 + chiralCarbon.getY(),
				 0.087 + chiralCarbon.getZ());
		Connection hydrogen2 = new Connection(atomNumber + 1, atomNumber - 1);
		Atom h2 = new Atom((atomNumber + 1) + "", "H", "TYR", super.getNumber(),
				this.hydrogen2, hydrogen2);
		
		this.carbon1 = new Coords(
				-1.177 + chiralCarbon.getX(),
				-1.610 + chiralCarbon.getY(),
				-1.567 + chiralCarbon.getZ());
		
		Connection c1c = new Connection(atomNumber + 2, atomNumber - 1, atomNumber + 3, atomNumber+11, atomNumber+11);
		Atom c1 = new Atom((atomNumber + 2) + "", "C", "TYR", super.getNumber(), this.carbon1, c1c);
		
		//second ch3
		this.carbon2 = new Coords(
				-2.393 + chiralCarbon.getX(),
				-1.023 + chiralCarbon.getY(),
				-1.935 + chiralCarbon.getZ());
		Connection carbon2 = new Connection(atomNumber + 3, atomNumber + 2, atomNumber + 4, atomNumber + 5, atomNumber + 5);
		Atom c2 = new Atom((atomNumber + 3 )+ "", "C", "TYR", super.getNumber(),
				this.carbon2, carbon2);
		
		//first ch3 hydrogen
		this.carbon2hydrogen1 = new Coords(
				-2.951 + chiralCarbon.getX(),
				-0.431 + chiralCarbon.getY(),
				-1.217 + chiralCarbon.getZ());
		Connection c2h1con = new Connection(atomNumber + 4, atomNumber + 3);
		Atom c2h1 = new Atom((atomNumber + 4) + "", "H", "TYR", super.getNumber(),
				this.carbon2hydrogen1, c2h1con);
		
		//second ch3
				this.carbon3 = new Coords(
						-2.898 + chiralCarbon.getX(),
						-1.193 + chiralCarbon.getY(),
						-3.228 + chiralCarbon.getZ());
				Connection carbon3 = new Connection(atomNumber + 5, atomNumber + 3, atomNumber + 3, atomNumber + 6, atomNumber + 7);
				Atom c3 = new Atom((atomNumber + 5 )+ "", "C", "TYR", super.getNumber(),
						this.carbon3, carbon3);

		
				
				//first ch3 hydrogen
				this.carbon3hydrogen1 = new Coords(
						-3.840 + chiralCarbon.getX(),
						-0.732 + chiralCarbon.getY(),
						-3.508 + chiralCarbon.getZ());
				Connection c3h1con = new Connection(atomNumber + 6, atomNumber + 5);
				Atom c3h1 = new Atom((atomNumber + 6) + "", "H", "TYR", super.getNumber(),
						this.carbon3hydrogen1, c3h1con);

				
				//second ch2o hydrogen
				this.carbon4 = new Coords(
						-2.190 + chiralCarbon.getX(),
						-1.957 + chiralCarbon.getY(),
						-4.160 + chiralCarbon.getZ());
				Connection carbon4 = new Connection(atomNumber + 7, atomNumber + 5, atomNumber + 9, atomNumber + 9, atomNumber + 8);
				Atom c4 = new Atom((atomNumber + 7) + "", "C", "TYR", super.getNumber(),
						this.carbon4, carbon4);
				
				//first ch3 hydrogen
				this.oxygen = new Coords(
						-2.731 + chiralCarbon.getX(),
						-2.061 + chiralCarbon.getY(),
						-5.470 + chiralCarbon.getZ());
				Connection oxy = new Connection(atomNumber + 8, atomNumber + 7);
				Atom ox = new Atom((atomNumber + 8) + "", "O", "TYR", super.getNumber(),
						this.oxygen, oxy);
				
				//first ch3 hydrogen
				this.oxyHydrogen = new Coords(
						-2.179 + chiralCarbon.getX(),
						-2.555 + chiralCarbon.getY(),
						-6.066 + chiralCarbon.getZ());
				Connection oxyh = new Connection(atomNumber + 13, atomNumber + 8);
				Atom oxh = new Atom((atomNumber + 13) + "", "H", "TYR", super.getNumber(),
						this.oxyHydrogen, oxyh);
				
				//second ch2o hydrogen
				this.carbon5 = new Coords(
						-0.977 + chiralCarbon.getX(),
						-2.547 + chiralCarbon.getY(),
						-3.798 + chiralCarbon.getZ());
				Connection carbon5 = new Connection(atomNumber + 9, atomNumber + 7, atomNumber + 7, atomNumber + 10, atomNumber + 11);
				Atom c5 = new Atom((atomNumber + 9) + "", "C", "TYR", super.getNumber(),
						this.carbon5, carbon5);
				
				//first ch3 hydrogen
				this.carbon5hydrogen1 = new Coords(
						-0.426 + chiralCarbon.getX(),
						-3.413 + chiralCarbon.getY(),
						-4.520 + chiralCarbon.getZ());
				Connection c5h1con = new Connection(atomNumber + 10, atomNumber + 9);
				Atom c5h1 = new Atom((atomNumber + 10) + "", "H", "TYR", super.getNumber(),
						this.carbon5hydrogen1, c5h1con);
				
				//second ch2o hydrogen
				this.carbon6 = new Coords(
						-0.470 + chiralCarbon.getX(),
						-2.371 + chiralCarbon.getY(),
						-2.506 + chiralCarbon.getZ());
				Connection carbon6 = new Connection(atomNumber + 11, atomNumber + 12, atomNumber + 9, atomNumber + 2, atomNumber + 2);
				Atom c6 = new Atom((atomNumber + 11) + "", "C", "TYR", super.getNumber(),
						this.carbon6, carbon6);
				
				//first ch3 hydrogen
				this.carbon6hydrogen1 = new Coords(
						 0.477 + chiralCarbon.getX(),
						-2.828 + chiralCarbon.getY(),
						-2.240 + chiralCarbon.getZ());
				Connection c6h1con = new Connection(atomNumber + 12, atomNumber + 11);
				Atom c6h1 = new Atom((atomNumber + 12) + "", "H", "TYR", super.getNumber(),
						this.carbon6hydrogen1, c6h1con);
				

		Atom[] parent = super.getAtoms();
		Atom[] TYR = new Atom[parent.length + rgroupAtomNumber];
		System.arraycopy(parent, 0, TYR, 0, parent.length);
		TYR[TYR.length - 15] = carbon;
		TYR[TYR.length - 14] = c1;
		TYR[TYR.length - 13] = h;
		TYR[TYR.length - 12] = h2;
		TYR[TYR.length - 11] = c2;
		TYR[TYR.length - 10] = c2h1;
		TYR[TYR.length - 9] = c3;
		TYR[TYR.length - 8] = c3h1;
		TYR[TYR.length - 7] = c4;
		TYR[TYR.length - 6] = ox;
		TYR[TYR.length - 5] = oxh;
		TYR[TYR.length - 4] = c5;
		TYR[TYR.length - 3] = c5h1;
		TYR[TYR.length - 2] = c6;
		TYR[TYR.length - 1] = c6h1;
		super.setAtoms(TYR);
		super.setNumberAtoms(super.getNumberAtoms() + rgroupAtomNumber);
		
	}
}
