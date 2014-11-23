package pdbCreation.aminoacid.aminoacids;

import pdbCreation.aminoacid.AminoAcid;
import pdbCreation.aminoacid.Atom;
import pdbCreation.aminoacid.Connection;
import pdbCreation.aminoacid.Coords;

public class Aspartate extends AminoAcid{
	private Coords carbon;
	private Coords hydrogen;
	private Coords hydrogen2;
	private Coords carbon2;
	private Coords doubleoxygen;
	private Coords singleoxygen;
	private Coords singleoxygenhydrogen;
	
	private static int rgroupAtomNumber = 7;
	public Aspartate(Coords chiralCarbon, int atomNumber, int aminoAcidNumber) {
		super(chiralCarbon, atomNumber, aminoAcidNumber, "ASP");
		
		//main CH3 carbon
		this.carbon = new Coords(
				-0.635 + chiralCarbon.getX(),
				-1.412 + chiralCarbon.getY(),
				-0.111 + chiralCarbon.getZ());
		atomNumber += super.getNumberAtoms();
		
		Connection ch3 = new Connection(atomNumber - 1, super.getChiralCarbonNumber(), atomNumber, atomNumber+1, atomNumber+2);
		Atom carbon = new Atom((atomNumber - 1) + "", "C", "ASP", super.getNumber(), this.carbon, ch3);
		
		//first ch2o hydrogen
		this.hydrogen = new Coords(
				-1.473 + chiralCarbon.getX(),
				-1.502 + chiralCarbon.getY(),
				 0.599 + chiralCarbon.getZ());
		Connection hydrogen = new Connection(atomNumber, atomNumber - 1);
		Atom h = new Atom((atomNumber) + "", "H", "ASP", super.getNumber(), this.hydrogen, hydrogen);
		
		//second ch2o hydrogen
		this.hydrogen2 = new Coords(
				 0.117 + chiralCarbon.getX(),
				-2.173 + chiralCarbon.getY(),
				 0.153 + chiralCarbon.getZ());
		Connection hydrogen2 = new Connection(atomNumber + 1, atomNumber - 1);
		Atom h2 = new Atom((atomNumber + 1) + "", "H", "ASP", super.getNumber(),
				this.hydrogen2, hydrogen2);
		
		//second ch3
		this.carbon2 = new Coords(
				-1.156 + chiralCarbon.getX(),
				-1.670 + chiralCarbon.getY(),
				-1.501 + chiralCarbon.getZ());
		Connection carbon2 = new Connection(atomNumber + 2, atomNumber - 1, atomNumber + 3, atomNumber + 3, atomNumber + 4);
		Atom c2 = new Atom((atomNumber + 2 )+ "", "C", "ASP", super.getNumber(),
				this.carbon2, carbon2);
		
		this.doubleoxygen = new Coords(
				-0.683 + chiralCarbon.getX(),
				-2.526 + chiralCarbon.getY(),
				-2.231 + chiralCarbon.getZ());
		Connection doxy = new Connection(atomNumber + 3, atomNumber + 2, atomNumber + 2);
		Atom dox = new Atom((atomNumber + 3) + "", "O", "ASP", super.getNumber(),
				this.doubleoxygen, doxy);
		
		this.singleoxygen = new Coords(
				-2.168 + chiralCarbon.getX(),
				-0.906 + chiralCarbon.getY(),
				-1.901 + chiralCarbon.getZ());
		Connection oxyy = new Connection(atomNumber + 4, atomNumber + 2);
		Atom ox = new Atom((atomNumber + 4) + "", "O", "ASP", super.getNumber(),
				this.singleoxygen, oxyy);
		
	
				this.singleoxygenhydrogen = new Coords(
						-2.416 + chiralCarbon.getX(),
						-1.147 + chiralCarbon.getY(),
						-2.786 + chiralCarbon.getZ());
				Connection oxhy = new Connection(atomNumber + 5, atomNumber + 4);
				Atom oxh = new Atom((atomNumber + 5 )+ "", "H", "ASP", super.getNumber(),
						this.singleoxygenhydrogen, oxhy);
				

		Atom[] parent = super.getAtoms();
		Atom[] ASP = new Atom[parent.length + rgroupAtomNumber];
		System.arraycopy(parent, 0, ASP, 0, parent.length);
		ASP[ASP.length - 7] = carbon;
		ASP[ASP.length - 6] = h;
		ASP[ASP.length - 5] = h2;
		ASP[ASP.length - 4] = c2;
		ASP[ASP.length - 3] = dox;
		ASP[ASP.length - 2] = ox;
		ASP[ASP.length - 1] = oxh;
		super.setAtoms(ASP);
		super.setNumberAtoms(super.getNumberAtoms() + rgroupAtomNumber);
		
	}
}
