package worker;
/**
 * 
 * SSInfo.java
 *
 * This class stores info about secondary structures
 *
 * @author Aaron Germuth
 * @date 2013-03-16
 */
public class SSInfo {
	private int position;
	private int length;
	private boolean on;
	
	public SSInfo(int position, int length, boolean on){
		this.position = position;
		this.length = length;
		this.on = on;
	}

	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public boolean isOn() {
		return on;
	}

	public void setOn(boolean on) {
		this.on = on;
	}
	
	
}
