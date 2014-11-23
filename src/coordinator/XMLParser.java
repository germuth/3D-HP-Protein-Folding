package coordinator;
//Possible sources of errors in this class:
//C++ lets you use an integer as a condition in if statements and loops
// make sure you translated them to their equivalent java boolean counterparts
import java.util.Stack;



public class XMLParser {
	private String str;
	private String error_msg;
	private int pos;
	
	//Wrapper for Recursive Parse
	public boolean parse(String str, XMLNode node, String[] error_msg){
		this.pos = 0;
		this.str = str;
		Stack<String> tag_stack = new Stack<String>();
		return parse(node, tag_stack, error_msg);
	}
	
	//Recursive Parse, do not call this directly call the other parse
	public boolean parse(XMLNode parent, Stack<String> tag_stack,String[] error_msg){
		Token token = new Token();
		boolean child_added = false;
		while(true){
			//If the current token couldn't be found return false
			if(!getToken(token, error_msg))
				return false;
			switch(token.type){
			case TAG:
				tag_stack.push(token.data);
				//TODO make sure this is suppose to be == 1 possible error causer
				if(parent.name.length() >= 1){
					XMLNode child = new XMLNode(token.data);
					if(!parse(child, tag_stack, error_msg))
						return false;
					parent.children.add(child);
					child_added = true;
				}
				else{
					parent.name = token.data;
				}
				break;
			case VALUE:
				if(parent.name.length() <= 0){
					error_msg[0] = "lonely value found";
					return false;
				}
				parent.value = token.data;
				return true;
			case ENDTAG:
				if(tag_stack.size() <= 0 || !tag_stack.peek().equals(token.data)){
					if(tag_stack.peek().equals(token.data)){
						System.out.println(tag_stack.peek());
					}
					error_msg[0] = "mismatching tags found";
					return false;
				}
				tag_stack.pop();
				if(!child_added)
					return true;
				else
					child_added = false;
				break;
			case ENDXML:
				if(tag_stack.size() >= 1){
					error_msg[0] = "missing end tag(s)";
					return false;
				}
				return true;
			//Invalid Token	
			default:
				System.out.println("XMLParser.Parse: invalid token");
				return false;
			}
		}
	}
	
	//Gets the token at the current position
	public boolean getToken(Token token, String[] error_msg){
		token.data = "";
		//End of XML
		if(pos >= str.length()){
			token.type = Type.ENDXML;
			return true;
		}
		if(str.charAt(pos) == '<'){
			pos++;
			//End tag
			if(str.charAt(pos) == '/'){
				pos++;
				token.type = Type.ENDTAG;
				while(str.charAt(pos) != '>' && pos < str.length()){
					token.data += str.charAt(pos);
					pos++;
				}
				if(pos == str.length()){
					error_msg[0] = "malformed end tag";
					return false;
				}
				else
					pos++;
			}
			else{
				//Start tag
				token.type = Type.TAG;
				while(str.charAt(pos) != '>' && pos < str.length()){
					token.data += str.charAt(pos);
					pos++;
				}
				if(pos == str.length()){
					error_msg[0] = "malformed start tag";
					return false;
				}
				else
					pos++;
			}
		}
		else{
			//Value
			token.type = Type.VALUE;
			while(str.charAt(pos) != '<' && pos < str.length()){
				token.data +=str.charAt(pos);
				pos++;
			}
		}
		return true;
	}
	

	//Token Class
	//Type: Specifies what piece of XML code this is
	//Data: The data in this token
	private class Token {
		private Type type;
		private String data;
	}

	private enum Type{
		VALUE, TAG, ENDTAG, ERROR, ENDXML
	}

}
