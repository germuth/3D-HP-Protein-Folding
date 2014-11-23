package coordinator;
/**
 * 
 * ParseParams.java
 *
 * This class parses parameters sent from java to C++
 * The parameters include all the options in the GUI
 * I think the java gui sends a long xml string to C++ through the socket, then parse params reads the long string and 
 * actually turns it into real stuff. 
 * 
 * They also use a cool xml class to do all this stuff
 * 
 * for example
 * <charges>phphphphhhhhppphphph</charges>,<numberOfMutations>1234123</numberOfMutations>, etc ..
 *
 * @author Aaron Germuth, Lee Foster
 * @date 2013-03-16
 */
public class ParseParams {
	
	//TODO make sure should be static
	public static boolean parse(String xml_str, String[] error_msg){
		XMLParser parser = new XMLParser();
		XMLNode xml_obj = new XMLNode();
		XMLNode child = new XMLNode();
		XMLNode grandchild = new XMLNode();
		
		System.out.println("Parsing XML...");
		//Parse XML and put it into an object
		if(!(parser.parse(xml_str, xml_obj, error_msg))){
			error_msg[0] = "error parsing xml: " + error_msg;
			return false;
		}
		
		//Make sure that the root parameter is named correctly
		if(!xml_obj.name.equals("parameters")){
			error_msg[0] = "error: invalid root parameter: ";
			error_msg[0] += xml_obj.name;
			return false;
		}
		//Deal with each of the parameters
		System.out.println("Setting parameters...");
		for(int i = 0; i<xml_obj.children.size(); i++){
			child = xml_obj.children.get(i);
			if(child.name.equals("mutate"))
				ParametersFromGUI.setMutateProbability(Integer.parseInt(child.value));
			else if(child.name.equals("ss-intro"))
				ParametersFromGUI.setSSIntroProbability(Integer.parseInt(child.value));
			else if(child.name.equals("ss-swap"))
				ParametersFromGUI.setSSSwapProbability(Integer.parseInt(child.value));
			else if(child.name.equals("pop-size"))
				ParametersFromGUI.setPopulationSize(Integer.parseInt(child.value));
			else if(child.name.equals("num-pops"))
				ParametersFromGUI.setNumPopulations(Integer.parseInt(child.value));
			else if(child.name.equals("ga-iterations"))
				ParametersFromGUI.setGeneticAlgorithmIterations(Integer.parseInt(child.value));
			else if(child.name.equals("min-ss-length"))
				ParametersFromGUI.setMinSSLength(Integer.parseInt(child.value));
			else if(child.name.equals("max-ss-length"))
				ParametersFromGUI.setMaxSSLength(Integer.parseInt(child.value));
			else if(child.name.equals("max-ss-pops"))
				ParametersFromGUI.setMaxSSPops(Integer.parseInt(child.value));
			else if(child.name.equals("user-ss-only")){
				if(child.value.equals("true"))
					ParametersFromGUI.setUserSSOnly(true);
				else ParametersFromGUI.setUserSSOnly(false);
			}
			else if(child.name.equals("charges"))
				ParametersFromGUI.setCharges(child.value);
			else if(child.name.equals("ss-list")){
				for(int j = 0; j<child.children.size();j++){
					grandchild = child.children.get(j);
					if(!grandchild.name.equals("ss")){
						error_msg[0] = "error: invalid parameter in ss-list: ";
						error_msg[0] += grandchild.name;
						return false;
					}
					ParametersFromGUI.insert(Integer.parseInt(grandchild.value));
				}
			}
			else {
				error_msg[0] = "error: invalid parameter: ";
				error_msg[0] += child.name;
				return false;
			}

		}
		
		if(!ParametersFromGUI.checkParameters(error_msg)){
			error_msg[0] = "error: " + error_msg[0];
			return false;
		}
		return true;
	}
	
}
