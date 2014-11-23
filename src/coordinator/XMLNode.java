package coordinator;
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
	
	//Turns the xml into a string
	public String toString(){
		String str;
		str = "<";
		str += name;
		str += ">";
		//C++ Code, no idea what it does left it commented
		//if (value.length()) str += value;
		for(int i = 0; i<children.size(); i++){
			str += children.get(i).toString();
		}
		str += "</";
		str+= name;
		str += ">";
		return str;
	}
}
