package worker;
/**
 * This class represents a Coordinate - Node Pair. Used in foldModel to keep track of all nodes added
 * @author Aaron Germuth
 *
 */
public class CoordsNodePair {
	private Coords coords;
	private Node node;
	
	public CoordsNodePair(){
		this.coords = null;
		this.node = null;
	}
	
	public CoordsNodePair(Coords c, Node n){
		this.coords = c;
		this.node = n;
	}
	
	public boolean equals(CoordsNodePair another){
		if(this.coords.equals(another.coords)){
			return true;
		}
		return false;
	}
	
	public String toString(){
		return "CoordsNodePair. " + this.coords + " " + this.node;
	}

	public Coords getCoords() {
		return coords;
	}

	public void setCoords(Coords coords) {
		this.coords = coords;
	}

	public Node getNode() {
		return node;
	}

	public void setNode(Node node) {
		this.node = node;
	}
}
