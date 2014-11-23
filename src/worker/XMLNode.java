package worker;
/**
 * Class that represents a variable and its value found from the XML String
 * Holds a link to all of its children XMLNodes.
 * 
 * @author Lee Foster
 */
import java.util.ArrayList;


public class XMLNode {
	public String name;
	public String value;
	public ArrayList<XMLNode> children;
	
	public XMLNode(){
		this.name = "";
		this.value = "";
		children = new ArrayList<XMLNode>();
	}
	
	public XMLNode(String name){
		this.name = name;
		this.value = "";
		children = new ArrayList<XMLNode>();
	}
	
	public String toString(){
		String str;
		str = "<";
		str += name;
		str += ">";
		for(int i = 0; i<children.size(); i++){
			str += children.get(i).toString();
		}
		str += "</";
		str+= name;
		str += ">";
		return str;
	}
}
