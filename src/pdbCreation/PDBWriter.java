package pdbCreation;

import java.io.FileWriter;
import java.io.IOException;
/**
 * PDB Writer
 * 
 * This class is a simple overlay of FileWriter, which includes a print method. The print method emulates 
 * the functionality of print, except each String is printed on a new line.
 * @author Aaron Germuth
 *
 */
public class PDBWriter extends FileWriter{
	/**
	 * Used to grab the character for line separator, regardless of OS
	 */
	private static final String NEW_LINE = System.getProperty( "line.separator");
	/**
	 * Constructor
	 * @param fileName, the name of the file we are writing to
	 * @throws IOException
	 */
	public PDBWriter(String fileName) throws IOException {
		super(fileName);
	}
	
	/**
	 * Prints the specified string to the file. The string is on it's own line.
	 * @param str, string to be printed to file
	 * @throws IOException
	 */
	public void print(String str) throws IOException{
		this.write(str);
		this.write(PDBWriter.NEW_LINE);
	}
}
