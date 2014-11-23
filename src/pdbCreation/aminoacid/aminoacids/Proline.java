package pdbCreation.aminoacid.aminoacids;

import pdbCreation.aminoacid.AcidGroup;
import pdbCreation.aminoacid.AminoAcid;
import pdbCreation.aminoacid.Atom;
import pdbCreation.aminoacid.Connection;
import pdbCreation.aminoacid.Coords;

public class Proline extends AminoAcid{
	private Coords hydrogen;
	
	private Coords carbon1;
	private Coords carbon1hydrogen1;
	private Coords carbon1hydrogen2;
	private Coords carbon2;
	private Coords carbon2hydrogen1;
	private Coords carbon2hydrogen2;
	private Coords carbon3;
	private Coords carbon3hydrogen1;
	private Coords carbon3hydrogen2;
	private Coords nitrogen;
	private Coords nitrogenhydrogen;
	private AcidGroup acid;
	private int nitrogenNumber;
	private int acidCarbonNumber;

	public Proline(Coords chiralCarbon, int atomNumber, int aminoAcidNumber) {
		super(chiralCarbon, atomNumber, aminoAcidNumber, "PRO");
		this.acidCarbonNumber = atomNumber;
		Coords acidCarbon = new Coords(
				1.240 + chiralCarbon.getX(),
				0.048 + chiralCarbon.getY(),
				-0.851 + chiralCarbon.getZ());
		this.acid = new AcidGroup(acidCarbon, atomNumber, this.getNumber(), "PRO", aminoAcidNumber + 1==AminoAcid.getNumAminoAcids());
		
		atomNumber += this.acid.getNumberAtoms() + 4;

		Connection maincon = new Connection(atomNumber - 4, atomNumber -3, acid.getAcidCarbonNumber(), atomNumber + 7);
		Atom cmain = new Atom((atomNumber - 4) + "", "C", "PRO", super.getNumber(), chiralCarbon, maincon);
		atomNumber--;
		
		this.hydrogen = new Coords(
				 0.377 + chiralCarbon.getX(),
				-0.094 + chiralCarbon.getY(),
				 1.031 + chiralCarbon.getZ());
		Connection hydrogen = new Connection(atomNumber -2, atomNumber - 3);
		Atom h = new Atom((atomNumber -2) + "", "H", "PRO", super.getNumber(), this.hydrogen, hydrogen);
		
		this.carbon1 = new Coords(-0.563 + chiralCarbon.getX(), -1.366
				+ chiralCarbon.getY(), -0.463 + chiralCarbon.getZ());
		
		
		Connection ch3 = new Connection(atomNumber - 1,
				atomNumber - 3, atomNumber, atomNumber + 1,
				atomNumber + 2);
		Atom carbon = new Atom((atomNumber - 1) + "", "C", "PRO",
				super.getNumber(), this.carbon1, ch3);
		this.carbon1hydrogen1 = new Coords(-0.648 + chiralCarbon.getX(), -1.391
				+ chiralCarbon.getY(), -1.562 + chiralCarbon.getZ());
		Connection hydr = new Connection(atomNumber, atomNumber - 1);
		Atom c1h1 = new Atom((atomNumber) + "", "H", "PRO", super.getNumber(),
				this.carbon1hydrogen1, hydr);
		this.carbon1hydrogen2 = new Coords(0.046 + chiralCarbon.getX(), -2.222
				+ chiralCarbon.getY(), -0.129 + chiralCarbon.getZ());
		Connection hydrogen2 = new Connection(atomNumber + 1, atomNumber - 1);
		Atom c1h2 = new Atom((atomNumber + 1) + "", "H", "PRO",
				super.getNumber(), this.carbon1hydrogen2, hydrogen2);

		
		this.carbon2 = new Coords(-1.975 + chiralCarbon.getX(), -1.346
				+ chiralCarbon.getY(), 0.152 + chiralCarbon.getZ());
		Connection carbon2 = new Connection(atomNumber + 2, atomNumber - 1,
				atomNumber + 3, atomNumber + 4, atomNumber + 5);
		Atom c2 = new Atom((atomNumber + 2) + "", "C", "PRO",
				super.getNumber(), this.carbon2, carbon2);
		this.carbon2hydrogen1 = new Coords(-1.908 + chiralCarbon.getX(), -1.513
				+ chiralCarbon.getY(), 1.241 + chiralCarbon.getZ());
		Connection c2h1con = new Connection(atomNumber + 3, atomNumber + 2);
		Atom c2h1 = new Atom((atomNumber + 3) + "", "H", "PRO", super.getNumber(),
				this.carbon2hydrogen1, c2h1con);
		this.carbon2hydrogen2 = new Coords(-2.655 + chiralCarbon.getX(), -2.094
				+ chiralCarbon.getY(), -0.288 + chiralCarbon.getZ());
		Connection c2h2con = new Connection(atomNumber + 4, atomNumber + 2);
		Atom c2h2 = new Atom((atomNumber + 4) + "", "H", "PRO",
				super.getNumber(), this.carbon2hydrogen2, c2h2con);


		this.carbon3 = new Coords(-2.402 + chiralCarbon.getX(), 0.112
				+ chiralCarbon.getY(), -0.129 + chiralCarbon.getZ());
		Connection carbon3 = new Connection(atomNumber + 5, atomNumber + 2,
				atomNumber + 6, atomNumber + 7, atomNumber + 8);
		Atom c3 = new Atom((atomNumber + 5) + "", "C", "PRO",
				super.getNumber(), this.carbon3, carbon3);
		this.carbon3hydrogen1 = new Coords(-2.758 + chiralCarbon.getX(), 0.191
				+ chiralCarbon.getY(), -1.172 + chiralCarbon.getZ());
		Connection c3h1con = new Connection(atomNumber + 6, atomNumber + 5);
		Atom c3h1 = new Atom((atomNumber + 6) + "", "H", "PRO",
				super.getNumber(), this.carbon3hydrogen1, c3h1con);
		this.carbon3hydrogen2 = new Coords(-3.204 + chiralCarbon.getX(), 0.445
				+ chiralCarbon.getY(), 0.548 + chiralCarbon.getZ());
		Connection c3h2con = new Connection(atomNumber + 7, atomNumber + 5);
		Atom c3h2 = new Atom((atomNumber + 7) + "", "H", "PRO",
				super.getNumber(), this.carbon3hydrogen2, c3h2con);
		
		this.nitrogenNumber = atomNumber + 8;
		
		
		if (aminoAcidNumber == 0) {
			
			this.nitrogen = new Coords(-1.169 + chiralCarbon.getX(), 0.907
					+ chiralCarbon.getY(), 0.074 + chiralCarbon.getZ());
			Connection ncon = new Connection(atomNumber + 8, atomNumber + 5,
					atomNumber + 9, atomNumber - 3);
			Atom n1 = new Atom((atomNumber + 8) + "", "N", "PRO",
					super.getNumber(), this.nitrogen, ncon);
			
			this.nitrogenhydrogen = new Coords(-1.099 + chiralCarbon.getX(),
					1.714 + chiralCarbon.getY(), -0.644 + chiralCarbon.getZ());
			Connection n1h1con = new Connection(atomNumber + 9, atomNumber + 8);
			Atom n1h1 = new Atom((atomNumber + 9) + "", "H", "PRO",
					super.getNumber(), this.nitrogenhydrogen, n1h1con);

			Atom[] acids = acid.getAtoms();
			Atom[] pro = new Atom[acids.length + 13];
			System.arraycopy(acids, 0, pro, 0, acids.length);
			// get chiral carbon
			pro[pro.length - 13] = cmain;
			pro[pro.length - 12] = h;
			pro[pro.length - 11] = carbon;
			pro[pro.length - 10] = c1h1;
			pro[pro.length - 9] = c1h2;
			pro[pro.length - 8] = c2;
			pro[pro.length - 7] = c2h1;
			pro[pro.length - 6] = c2h2;
			pro[pro.length - 5] = c3;
			pro[pro.length - 4] = c3h1;
			pro[pro.length - 3] = c3h2;
			pro[pro.length - 2] = n1;
			pro[pro.length - 1] = n1h1;
			super.setAtoms(pro);
			super.setNumberAtoms(pro.length + 2);
		}
		else{
			this.nitrogen = new Coords(-1.169 + chiralCarbon.getX(), 0.907
					+ chiralCarbon.getY(), 0.074 + chiralCarbon.getZ());
			Connection ncon = new Connection(atomNumber + 8, atomNumber + 5,
					 atomNumber - 3);
			Atom n1 = new Atom((atomNumber + 8) + "", "N", "PRO",
					super.getNumber(), this.nitrogen, ncon);
			Atom[] acids = acid.getAtoms();
			Atom[] pro = new Atom[acids.length + 12];
			System.arraycopy(acids, 0, pro, 0, acids.length);
			// get chiral carbon
			pro[pro.length - 12] = cmain;
			pro[pro.length - 11] = h;
			pro[pro.length - 10] = carbon;
			pro[pro.length - 9] = c1h1;
			pro[pro.length - 8] = c1h2;
			pro[pro.length - 7] = c2;
			pro[pro.length - 6] = c2h1;
			pro[pro.length - 5] = c2h2;
			pro[pro.length - 4] = c3;
			pro[pro.length - 3] = c3h1;
			pro[pro.length - 2] = c3h2;
			pro[pro.length - 1] = n1;
			super.setAtoms(pro);
			super.setNumberAtoms(pro.length + 2);
		}
	}
	@Override
	public int getNitrogenNumber() {
		return this.nitrogenNumber;
	}
	@Override
	public int getAcidCarbonNumber() {
		return this.acidCarbonNumber;
	}
	
	
}
