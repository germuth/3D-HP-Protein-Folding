3D-HP-Protein-Folding
=====================
A Concurrent 3D Protein Folding Application

Worked on by Ben Hartshorn, Mike Peters, Travis Reinheimer, Yishan Hu, Chen Yao, Lee Foster, and Aaron Germuth.

<b>Overview:</b>

This application can be used to approximate the 3D structure of proteins. Folding proteins is an computationally intensive procedure, so we use many simplifications. The application is multi-threaded and may be ran from multiple different computers through a network. There are many improvements to be made.

<b>How to Run</b>

The entire project should first be imported into eclipse. Java3D should be installed on the machine. Use this tutorial to help you (https://www.cs.utexas.edu/~scottm/cs324e/handouts/setUpJava3dEclipse.htm). Make sure all of the jars in lib are actually imported into the build path. As it stands, there are 3 main threads. The graphical user interface (Gui.java), the worker thread which folds the protein (Worker.java), and a communication thread (Coordinator.java). All three of these threads must be running in order to fold a protein. First run the coordinator and you should see "Waiting for a new connection". A Worker can than be started on the same machine (a worker can be run from a seperate machine). The coordinator should respond immediately saying "Got a worker". Multiple workers can be loaded. At this point, you can run the Graphical User Interface from any computer and select Connection. "localhost" means the gui and coordinate are run on the same machine. Otherwise specific the server here such as "servername.com". Once the connection is valid, you can select "Generate" and begin to watch the protein fold. The worker threads will send back progress while running, and the interface will automatically display these. One 

<b>Protein Folding:</b>

Proteins are large polymers located within the body, with a large array of functions (from muscle to digesting food).
Protein function is decided in part by it's primary structure (the amino acids which make it up) but also it's 3D
folded shape it conforms to within the body. Therefore, determining the shape of a protein is an important step towards determining its function.

<b>HP Model:</b>

The HP model is a simple abstraction used to simplify protein folding. Instead of the 20 amino acids found
in nature, there are only two (h and p). H stands for hydrophobic (scared of water) and p stands for hydrophilic or
polar (attracted to water). In large protein complexes, a driving force towards the resulting shape is the arrangement
of whether each amino acid is hydrophobic or hydrophilic. Usually, hydrophobic cores are formed on the inside, with
a hydrophilic 'shell' around the core. We can take a known sequence, translate it to HP, and then fold it to get an approximation at the resulting structure. In real protein folding, there are many other
factors which contribute to shape, such as Hydrogen bonds (alpha-helix / beta-sheet), di-sulphide bonds, and ionic
interactions. 

<b>Other Assumptions:</b>

The position of each amino acid is restricted to integer coordinates. Each peptide bond forms an exact angle of either 0, 90, or 180 degrees. When replacing each H or P with the actual amino acid structure, the relative positions of each atom in the amino acid are kept constant. Many improvements could be made in this area. For example, having some sort of limit as to how close atoms can be, and allowing for the rotation about each atom of the amino acid.

<b>Genetic Algorithm:</b>

Even in the HP model, the amount of possible shapes a protein could take is massive. This problem has been proved to be
NP - Complete. Therefore, writing a simple program to find all possibilities, and return the best one would take
an unreasonable amount of time. We use a genetic algorithm to approximate at the best solution. Genetic Algorithms
start with a random population and apply genetic operations (crossover, mutation, reproduction) to create new populations.
Then a Fitness Function is ran to determine who is the best of the population, and the process in repeated, often
thousands of times. The fitness function in our case is as follows:

We iterate through our chain of h's and p's, and every time we find an h, we search all positions around the h and
look at what residue in there. 
<i>Note: that only residues which have been folded to be beside the current h are included, not ones which are right before or after in the sequence.</i>

H - H : +6 to protein health

	This is given a high score, as is it inidicates the formation of a hydrophobic core

H - P : +1 to protein health

	This is given a posivive score, as it indicates p's are forming a shell around the h's

H - nothing: - 2 to protein health

	This is given a negative score, as it indicates the h is bare and has no 'protection'

<b>Jmol:</b>

Our program also makes use of an open source, free, java application called Jmol. Jmol is meant to serve as a universal
application for displaying all kinds of molecular structures. 

<b>PDB File:</b>

There are protein databases on the internet, such as <i>RCSB.org</i>, which supply thousands of experimentally deterimined
protein structures. These structure are held in many protein formats, one of then being .pdb files. These files contain
a list of coordinates for every atom in the molecule, and which atom it is bonded to. They can also contain much more 
complicated information. Jmol can open .pdb files and display them in 3 dimensions.

<b>Our Application:</b>

Our application takes as input a sequence of amino acids. These amino acids are converted to the HP model and folded using an genetic algorithm. The resulting 3D HP protein is then displayed. The user then has the option to have the protein displayed at an atomic level using Jmol. Since this structure is generated using the 3d HP model, it may not be  a correct molecule (atoms may overlay, bonds may not have exact lengths or angles found in nature). But this is the purpose. We can visually show the results of the HP model. When a pdf file is used as input, you also have the option to display the actual structure in Jmol. This allows you to open two windows and visually compare the two structures. This is an easy way to visually test the accuracy of the HP model. 

<b>Further Directions:</b>

Now that we can visually compare the actual structure and the computer generated one, we can do a couple things to attempt
to improve the computer's guess.
<p>
(1) Tweak setting of genetic algorithm 
	- such as secondary structure length
	- mutation rate
</p>
<p>
(2) Tweak how amino acids are displayed based on HP model
</p>
<p>
(3) Provide a quantitative measure for comparing a computer generated and an experimentally deterimined fold. This way we can graph results
and see trends, rather than comparing structures one by one.
</p>
