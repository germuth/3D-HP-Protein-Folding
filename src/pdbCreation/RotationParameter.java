package pdbCreation;

/**
 * RotationParameter
 * 
 * This class holds an axis, and the degree to which we want to rotate on that axis.
 * @author Aaron Germuth
 *
 */
public class RotationParameter {
	/**
	 * Character designating the axis, 'X', 'Y', and 'Z'
	 */
	private char axis;
	/**
	 * Holds the degree of rotation in radians
	 * Can be Negative
	 */
	private double radians;
	
	public static final char X_AXIS = 'X';
	public static final char Y_AXIS = 'Y';
	public static final char Z_AXIS = 'Z';
	
	public RotationParameter(char axis, double radians){
		this.axis = axis;
		this.radians = radians;
	}

	public char getAxis() {
		return axis;
	}

	public void setAxis(char axis) {
		this.axis = axis;
	}

	public double getRadians() {
		return radians;
	}

	public void setRadians(double radians) {
		this.radians = radians;
	}
}
