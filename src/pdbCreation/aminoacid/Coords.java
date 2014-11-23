package pdbCreation.aminoacid;

import java.text.DecimalFormat;

/**
 * 
 * Coords.java
 *
 * This class holds coordinates of a node in the form of an x, y and z doubles. Doubles are 
 * restricted to 4 decimal places and currently 2 normal places.
 *
 * @author Aaron Germuth
 * @date 2013-03-16
 */
public class Coords {
	private double x; 
	private double y;
	private double z;
	
	public Coords(){
		this.x = 0;
		this.y = 0;
		this.z = 0;
	}
	
	public Coords(double x, double y, double z){
		this.setX(x);
		this.setY(y);
		this.setZ(z);
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

	public double getX() {
		return x;
	}

	public void setX(double x) {
		x = Double.parseDouble(new DecimalFormat("#.###").format(x));
		this.x = x;
	}

	public double getY() {
		return y;
	}

	public void setY(double y) {
		y = Double.parseDouble(new DecimalFormat("#.###").format(y));
		this.y = y;
	}

	public double getZ() {
		return z;
	}

	public void setZ(double z) {
		z = Double.parseDouble(new DecimalFormat("#.###").format(z));
		this.z = z;
	}
}
