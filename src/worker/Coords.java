package worker;
/**
 * 
 * Coords.java
 *
 * This class holds coordinates of a node in the form of an x, y and z integer
 *
 * @author Aaron Germuth
 * @date 2013-03-16
 */
public class Coords {
	private int x; 
	private int y;
	private int z;
	
	public Coords(){
		this.x = 0;
		this.y = 0;
		this.z = 0;
	}
	
	public Coords(int x, int y, int z){
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public String toString(){
		return "Coords: x = " + this.x + ", y = " + this.y + ", z = " + this.z;
	}
	
	public void print(){
		System.out.println("x: " + this.x);
		System.out.println("y: " + this.y);
		System.out.println("z: " + this.z);
	}
	
	public boolean equals(Coords another){
		if(this.x == another.x 
				&& this.y == another.y
				&& this.z == another.z){
			return true;
		}
		return false;
	}
	
	/**
	 * Compare method, not implemented as i'm not sure we will need it
	 * TODO look into this
	 * 
	 * @param another
	 * @return not sure
	 */
	public boolean compare(Coords another){
		System.err.println("compare methods of compare method was called, it has not yet been implemented");
		return true;
	}

	public int getX() {
		return x;
	}

	public void setX(int x) {
		this.x = x;
	}

	public int getY() {
		return y;
	}

	public void setY(int y) {
		this.y = y;
	}

	public int getZ() {
		return z;
	}

	public void setZ(int z) {
		this.z = z;
	}
	
	
}
