3D-HP-Protein-Folding
=====================

A Distributed, Concurrent, 3D Protein Folding Application

Worked on by Travis Reinheimer, Yishan Hu, Chen Yao, Lee Foster, and Aaron Germuth.

<b>Protein Folding:</b>

Proteins are large polymers located within the body, with a large array of functions (from muscle to digesting food).
Proteins function is decided in part by it's primary structure (the amino acids which make it up) but also it's 3D
folded shape it conforms to within the body. Therefore, determining the shape of a protein is just as important as
determining the sequence.

<b>HP Model:</b>

The HP model is a simple abstraction used to simplify protein folding. Instead of the (normal) 20 amino acids found
in nature, there are only two (h and p). H stands for hydrophobic (scared of water) and p stands for hydrophilic or
polar (attracted to water). In large protein complexs, a driving force towards the resulting shape is the arrangement
of whether each amino acid is hydrophobic or hydrophilic. Usually, hydrophobic cores are formed on the inside, with
a hydrophilic 'shell' around the core. We can take a know sequence, translate it to HP, and then fold it much
easier to get an approximation at the resulting structure. In real protein folding, there are many other
factors which contribute to shape, such as Hydrogen bonds (alpha-helix / beta-sheet), di-sulphide bonds, and ionic
interactions. 

<b>Genetic Algorithm:</b>

Even in the HP model the amount of possible shapes a protein could take is massive. The problem has been proved to be
NP - Complete. Therefore, writing a simple program to find all possibilities, and return the best one would take
an unreasonable amount of time. We use a genetic algorithm to approximate at the best solution. Genetic Algorithms
start with a random population and apply genetic operations (crossover, mutation, reproduction) to create new populations.
Then a Fitness Function is ran to determine who is the best of the population, and the process in repeated, often
thousands of times. The fitness function in our case is as follows:

We Iterate through our chain of h's and p's, and every time we find an h, we search all positions around the h and
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
application for displaying all kinds of molecular structures. It can display everything from nucleic acids, quaternary proteins, to ligands. 

<b>PDB File:</b>

There are protein databases on the internet, such as <i>RCSB.org</i>, which supply thousands of experimentally deterimined
protein structures. These structure are held in many protein formats, one of then being .pdb files. These files contain
a list of coordinates for every atom in the molecule, and which atom it is bonded to. They can also contain much more 
complicated information. Jmol can open .pdb files and display them in 3 dimensions.

<b>Our Application:</b>

Our application takes in input in three forms: (1) a .pdb file, (2) user inputted sequence, or (3) random. This inputs
multiple amino acids into our application. These amino acids are converted to the HP model and folded using an genetic 
algorithm. The resulting 3D HP protein is then displayed. The user then has the option to have the folded HP model
displayed at an atomic level using Jmol. Since this structure is generated using the 3d HP model, it may not be 
a correct molecule (atoms may overlay, bonds may not have exact lengths or angles found in nature). But this is the purpose.
We can visually show the results of the HP model. In the case of input in a pdb file, you also have the option to display
the experimentally determined structure. This allows you to open two windows and visually compare the two structures. The 
main benefit of this, is it supplies an easy way to visually test the accuracy of the HP model. 

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
