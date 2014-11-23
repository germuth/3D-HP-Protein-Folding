package pdbCreation.aminoacid.aminoacids;

import pdbCreation.aminoacid.AminoAcid;
import pdbCreation.aminoacid.Atom;
import pdbCreation.aminoacid.Connection;
import pdbCreation.aminoacid.Coords;

public class Arginine extends AminoAcid{
	private Coords carbon;
	private Coords hydrogen;
	private Coords hydrogen2;
	private Coords carbon2;
	private Coords carbon2hydrogen1;
	private Coords carbon2hydrogen2;
	private Coords carbon3;
	private Coords carbon3hydrogen1;
	private Coords carbon3hydrogen2;
	private Coords nitrogen1;
	private Coords nitrogen1hydrogen1;
	private Coords carbon4;
	private Coords nitrogen2;
	private Coords nitrogen2hydrogen1;
	private Coords nitrogen2hydrogen2;
	private Coords nitrogen3;
	private Coords nitrogen3hydrogen1;
	
	private static int rgroupAtomNumber = 17;
	public Arginine(Coords chiralCarbon, int atomNumber, int aminoAcidNumber) {
		super(chiralCarbon, atomNumber, aminoAcidNumber, "ARG");
		
		//main CH3 carbon
		this.carbon = new Coords(
				-0.866 + chiralCarbon.getX(),
				-1.284 + chiralCarbon.getY(),
				-0.132 + chiralCarbon.getZ());
		atomNumber += super.getNumberAtoms();
		
		Connection ch3 = new Connection(atomNumber - 1, super.getChiralCarbonNumber(), atomNumber, atomNumber+1, atomNumber+2);
		Atom carbon = new Atom((atomNumber - 1) + "", "C", "ARG", super.getNumber(), this.carbon, ch3);
		
		//first ch2o hydrogen
		this.hydrogen = new Coords(
				-1.659 + chiralCarbon.getX(),
				-1.253 + chiralCarbon.getY(),
				 0.633 + chiralCarbon.getZ());
		Connection hydrogen = new Connection(atomNumber, atomNumber - 1);
		Atom h = new Atom((atomNumber) + "", "H", "ARG", super.getNumber(), this.hydrogen, hydrogen);
		
		//second ch2o hydrogen
		this.hydrogen2 = new Coords(
				-0.236 + chiralCarbon.getX(),
				-2.166 + chiralCarbon.getY(),
				 0.067 + chiralCarbon.getZ());
		Connection hydrogen2 = new Connection(atomNumber + 1, atomNumber - 1);
		Atom h2 = new Atom((atomNumber + 1) + "", "H", "ARG", super.getNumber(),
				this.hydrogen2, hydrogen2);
		
		//second ch3
		this.carbon2 = new Coords(
				-1.522 + chiralCarbon.getX(),
				-1.423 + chiralCarbon.getY(),
				-1.534 + chiralCarbon.getZ());
		Connection carbon2 = new Connection(atomNumber + 2, atomNumber - 1, atomNumber + 3, atomNumber + 4, atomNumber + 5);
		Atom c2 = new Atom((atomNumber + 2 )+ "", "C", "ARG", super.getNumber(),
				this.carbon2, carbon2);
		
		this.carbon2hydrogen1 = new Coords(
				-0.740 + chiralCarbon.getX(),
				-1.540 + chiralCarbon.getY(),
				-2.302 + chiralCarbon.getZ());
		Connection c2h1con = new Connection(atomNumber + 3, atomNumber + 2);
		Atom c2h1 = new Atom((atomNumber + 3) + "", "H", "ARG", super.getNumber(),
				this.carbon2hydrogen1, c2h1con);
		
		this.carbon2hydrogen2 = new Coords(
				-2.097 + chiralCarbon.getX(),
				-0.511 + chiralCarbon.getY(),
				-1.756 + chiralCarbon.getZ());
		Connection c2h2con = new Connection(atomNumber + 4, atomNumber + 2);
		Atom c2h2 = new Atom((atomNumber + 4) + "", "H", "ARG", super.getNumber(),
				this.carbon2hydrogen2, c2h2con);
		
	
				this.carbon3 = new Coords(
						-2.463 + chiralCarbon.getX(),
						-2.658 + chiralCarbon.getY(),
						-1.597 + chiralCarbon.getZ());
				Connection carbon3 = new Connection(atomNumber + 5, atomNumber + 2, atomNumber + 8, atomNumber + 6, atomNumber + 7);
				Atom c3 = new Atom((atomNumber + 5 )+ "", "C", "ARG", super.getNumber(),
						this.carbon3, carbon3);

		
				
				//first ch3 hydrogen
				this.carbon3hydrogen1 = new Coords(
						-1.9 + chiralCarbon.getX(),
						-3.561 + chiralCarbon.getY(),
						-1.312 + chiralCarbon.getZ());
				Connection c3h1con = new Connection(atomNumber + 6, atomNumber + 5);
				Atom c3h1 = new Atom((atomNumber + 6) + "", "H", "ARG", super.getNumber(),
						this.carbon3hydrogen1, c3h1con);

				// second ch3 hydrogen
				this.carbon3hydrogen2 = new Coords(
						-3.287 + chiralCarbon.getX(),
						-2.538 + chiralCarbon.getY(),
						-0.874 + chiralCarbon.getZ());
				Connection c3h2con = new Connection(atomNumber + 7, atomNumber + 5);
				Atom c3h2 = new Atom((atomNumber + 7) + "", "H", "ARG", super.getNumber(),
						this.carbon3hydrogen2, c3h2con);
				
				//second ch2o hydrogen
				this.nitrogen1 = new Coords(
						-3.027 + chiralCarbon.getX(),
						-2.886 + chiralCarbon.getY(),
						-2.947 + chiralCarbon.getZ());
				Connection nitrogen1 = new Connection(atomNumber + 8, atomNumber + 9, atomNumber + 5, atomNumber + 10);
				Atom n1 = new Atom((atomNumber + 8) + "", "N", "ARG", super.getNumber(),
						this.nitrogen1, nitrogen1);
				
				// second ch3 hydrogen
				this.nitrogen1hydrogen1 = new Coords(
						-2.251 + chiralCarbon.getX(),
						-3.098 + chiralCarbon.getY(),
						-3.670 + chiralCarbon.getZ());
				Connection n1h1con = new Connection(atomNumber + 9, atomNumber + 8);
				Atom n1h1 = new Atom((atomNumber + 9) + "", "H", "ARG", super.getNumber(),
						this.nitrogen1hydrogen1, n1h1con);
				
				//second ch2o hydrogen
				this.carbon4 = new Coords(
						-3.812 + chiralCarbon.getX(),
						-1.891 + chiralCarbon.getY(),
						-3.364 + chiralCarbon.getZ());
				Connection carbon4 = new Connection(atomNumber + 10, atomNumber + 14, atomNumber + 14, atomNumber + 11, atomNumber + 8);
				Atom c4 = new Atom((atomNumber + 10) + "", "C", "ARG", super.getNumber(),
						this.carbon4, carbon4);
				
				
				this.nitrogen2 = new Coords(
						-5.081 + chiralCarbon.getX(),
						-1.96 + chiralCarbon.getY(),
						-2.972 + chiralCarbon.getZ());
				Connection nitrogen2 = new Connection(atomNumber + 11, atomNumber + 12, atomNumber + 13, atomNumber + 10);
				Atom n2 = new Atom((atomNumber + 11) + "", "N", "ARG", super.getNumber(),
						this.nitrogen2, nitrogen2);
				

				this.nitrogen2hydrogen1 = new Coords(
						-5.57 + chiralCarbon.getX(),
						-1.018 + chiralCarbon.getY(),
						-3.172 + chiralCarbon.getZ());
				Connection n2h1con = new Connection(atomNumber + 12, atomNumber + 11);
				Atom n2h1 = new Atom((atomNumber + 12) + "", "H", "ARG", super.getNumber(),
						this.nitrogen2hydrogen1, n2h1con);
				

				this.nitrogen2hydrogen2 = new Coords(
						-5.581 + chiralCarbon.getX(),
						-2.748 + chiralCarbon.getY(),
						-3.513 + chiralCarbon.getZ());
				Connection n2h2con = new Connection(atomNumber + 13, atomNumber + 11);
				Atom n2h2 = new Atom((atomNumber + 13) + "", "H", "ARG", super.getNumber(),
						this.nitrogen2hydrogen2, n2h2con);
				
				this.nitrogen3 = new Coords(
						-3.407 + chiralCarbon.getX(),
						-0.928 + chiralCarbon.getY(),
						-4.089 + chiralCarbon.getZ());
				Connection nitrogen3 = new Connection(atomNumber + 14, atomNumber + 10, atomNumber + 10, atomNumber + 15);
				Atom n3 = new Atom((atomNumber + 14) + "", "N", "ARG", super.getNumber(),
						this.nitrogen3, nitrogen3);
				

				this.nitrogen3hydrogen1 = new Coords(
						-2.447 + chiralCarbon.getX(),
						-0.865 + chiralCarbon.getY(),
						-4.390 + chiralCarbon.getZ());
				Connection n3h1con = new Connection(atomNumber + 15, atomNumber + 14);
				Atom n3h1 = new Atom((atomNumber + 15) + "", "H", "ARG", super.getNumber(),
						this.nitrogen3hydrogen1, n3h1con);

				

		Atom[] parent = super.getAtoms();
		Atom[] ARG = new Atom[parent.length + rgroupAtomNumber];
		System.arraycopy(parent, 0, ARG, 0, parent.length);
		ARG[ARG.length - 17] = carbon;
		ARG[ARG.length - 16] = h;
		ARG[ARG.length - 15] = h2;
		ARG[ARG.length - 14] = c2;
		ARG[ARG.length - 13] = c2h1;
		ARG[ARG.length - 12] = c2h2;
		ARG[ARG.length - 11] = c3;
		ARG[ARG.length - 10] = c3h1;
		ARG[ARG.length - 9] = c3h2;
		ARG[ARG.length - 8] = n1;
		ARG[ARG.length - 7] = n1h1;
		ARG[ARG.length - 6] = c4;
		ARG[ARG.length - 5] = n2;
		ARG[ARG.length - 4] = n2h1;
		ARG[ARG.length - 3] = n2h2;
		ARG[ARG.length - 2] = n3;
		ARG[ARG.length - 1] = n3h1;
		super.setAtoms(ARG);
		super.setNumberAtoms(super.getNumberAtoms() + rgroupAtomNumber);
		
	}
}
