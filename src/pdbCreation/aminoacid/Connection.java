package pdbCreation.aminoacid;
/**
 * Connection
 * 
 * This class represents a bond between two atoms. Fields are stored in fixed length strings for 
 * ease of output to pdb files
 * @author Aaron Germuth
 *
 */
public class Connection {
	/**
	 * Connection tag for pdb file
	 */
	private static FixedLengthString title = new FixedLengthString("CONECT", 6);
	
	/**
	 * The atom number this Connection object is for
	 */
	private FixedLengthString thisAtomNumber = new FixedLengthString(5);
	
	//all atoms that this atom is connect to---------------------------
	private FixedLengthString firstBondedAtom = new FixedLengthString(5);
	private FixedLengthString secondBondedAtom = new FixedLengthString(5);
	private FixedLengthString thirdBondedAtom = new FixedLengthString(5);
	private FixedLengthString fourthBondedAtom = new FixedLengthString(5);
	
	/**
	 * Constructor for atoms only connected to another atom
	 * @param me
	 * @param first
	 */
	public Connection(int me, int first){
		this.thisAtomNumber.setString(me + "");
		this.firstBondedAtom.setString(first + "");
	}
	
	/**
	 * Constructor ofr atoms connected to two others
	 * for example hydrogen
	 * @param me
	 * @param first
	 * @param second
	 */
	public Connection(int me, int first, int second){
		this.thisAtomNumber.setString(me + "");
		this.firstBondedAtom.setString(first + "");
		this.secondBondedAtom.setString(second + "");
	}
	
	/**
	 * Constructor for atoms connected to three others.
	 * for example nitrogen (usually)
	 * @param me
	 * @param first
	 * @param second
	 * @param third
	 */
	public Connection(int me, int first, int second, int third){
		this.thisAtomNumber.setString(me + "");
		this.firstBondedAtom.setString(first + "");
		this.secondBondedAtom.setString(second + "");
		this.thirdBondedAtom.setString(third + "");
	}
	
	/**
	 * Constructor for atoms connected to four others.
	 * For example carbon
	 * @param me
	 * @param first
	 * @param second
	 * @param third
	 * @param fourth
	 */
	public Connection(int me, int first, int second, int third, int fourth){
		this.thisAtomNumber.setString(me + "");
		this.firstBondedAtom.setString(first + "");
		this.secondBondedAtom.setString(second + "");
		this.thirdBondedAtom.setString(third + "");
		this.fourthBondedAtom.setString(fourth + "");
	}
	
	/**
	 * Returns this connection object in a string. The string corresponds to 
	 * pdb format
	 * @return
	 */
	public String printConnection(){
		String answer = "";
		answer += Connection.title.print();
		answer += this.thisAtomNumber.print();
		answer += this.firstBondedAtom.print();
		answer += this.secondBondedAtom.print();
		answer += this.thirdBondedAtom.print();
		answer += this.fourthBondedAtom.print();
		return answer;
	}
	
	
	
}
