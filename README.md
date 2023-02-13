# AC

**Read the [wiki](https://github.com/manuel-freire/ac2/wiki) for a step-by-step example of an analysis, and documentation on how to perform the most common tasks in AC2.**

## Introduction

AC is a source code plagiarism detection tool.
It aids instructors and graders to detect plagiarism within a group of assignments
written in languages such as C, C++, Java, PHP, XML, Python, ECMAScript, Pascal or VHDL 
(plaintext works too, but is less precise).
AC incorporates multiple similarity detection algorithms found in the scientific
literature, and allows their results to be visualized graphically.

Manuel Freire, the main author, is currently teaching Computer Science at the [Universidad Complutense de Madrid](http://informatica.ucm.es/). AC is still being used in other Spanish universities (UAM, URJC) and abroad (UTorino), 
although this list is by no means either complete or exhaustive. By moving to Github, the project expects to gain contributors and ensure future development.

There are other plagiarism detection tools, such as [JPlag](https://github.com/jplag/jplag), [Moss](https://theory.stanford.edu/~aiken/moss/), or [Plaggie](https://www.cs.hut.fi/Software/Plaggie/). AC is, in this context
* **local**, and does not require sending data to remote servers (not the case of Moss)
* **robust**, using [Normalized Compression Distance](https://en.wikipedia.org/wiki/Normalized_compression_distance) as its main measure-of-similarity, but with the possibility of integrating other, additional measures to gain better pictures of what is going on (JPlag, Moss and Plaggie have hard-coded analyses, mostly based on sub-string matching after tokenization).
* heavy on **information visualization**. AC will not provide "percentage of copy"; instead, it will create graphical representations of the degree of similarity between student submissions within a group, so instructors can build their own explanations regarding what really happened (see [here](http://doi.acm.org/10.1145/1385569.1385644) and [here](http://dx.doi.org/10.1109/VAST.2010.5652834) for papers on AC's visualizations).

## Installing and running the program

You will need a Java Runtime Environment installed (JRE 9 or above for versions 2.2+, JRE 8 for prior versions).
You can download the latest [Java JRE from Oracle's website](https://java.com/en/download/),
although OpenJDK will work just as well.

Once you have Java installed, simply download the [latest release](https://github.com/manuel-freire/ac2/releases/) 
(look for the latest one that has a `.jar` file available), and either double-click it 
(assuming the JRE is correctly installed), or use the command-line to execute it via `java -jar name-of-jar`

## Building the source

If you want to build the binaries yourself or change the code, you will also need
to download [Apache Maven](https://maven.apache.org/download.cgi).

To build everything, 
* run `mvn install`. 
* the executable jar-file will be stored in `ac-ui/target/ac-version-githash.jar` 
(note that `version` is set in `pom.xml`, and `githash` is calculated by checking the commit in `git`). 
There are other, non-default entry-points (for instance, without UI or skipping the assignment selection phase).

Pull requests and issues are *very* welcome.

## Docker

You can also run this with docker. You will need an X-server to use as the display.

`docker build -t ac2 .`
`docker run -it --rm -e DISPLAY=<x-server-host>:0.0 -v <local_path>:<container_path> ac2`

### Adding support for programming languages

AC2 uses [Antlr4](https://github.com/antlr/antlr4) grammars to generate lexers (= tokenizers) and parsers for languages. This makes adding support for new languages a breeze: you only need to plug in a good Antlr4 grammar.

Currently, AC supports
* Java (up to Java 17),
adapted from https://github.com/antlr/grammars-v4/blob/master/java, BSD license.
* C and C++ (up to C++ 14),
adapted from https://github.com/antlr/grammars-v4/blob/master/cpp, MIT license.
* Python (2 and 3)
adapted from https://github.com/antlr/grammars-v4/blob/master/python/python, MIT license.
* Pascal
adapted from https://github.com/antlr/grammars-v4/blob/master/pascal, BSD license.
* JS
adapted from https://github.com/antlr/grammars-v4/blob/master/javascript/ecmascript, MIT license.
* PHP
adapted from https://github.com/antlr/grammars-v4/blob/master/php/,  MIT license.
* VHDL
adapted from https://github.com/antlr/grammars-v4/blob/master/vhdl/, GNU v3 or later license.
* XML
adapted from https://github.com/antlr/grammars-v4/blob/master/xml/, BSD license.

To add support for tokenizing more programming languages, place the grammar file (`.g4`) into
the [grammars](https://github.com/manuel-freire/ac2/blob/ac-lexers/src/main/antlr4/es/ucm/fdi/ac/lexers), and update the [AntlrTokenizerFactory](https://github.com/manuel-freire/ac2/blob/ac-lexers/src/main/java/es/ucm/fdi/ac/parser/AntlrTokenizerFactory.java#L41) so that files with your chosen extensions are parsed using the corresponding parsers and lexers.

## License and code-structure

The code is split into 4 modules:
 * `ac-lexers`: contains source-code lexers and parsers. The lexers and parsers are generated by [Antlr4](https://github.com/antlr/antlr4) from their `.g4` grammars.
 * `ac-core`: contains the main similarity-detection engine. Depends on the `ac-lexers` to compare token-streams instead of raw text. Use of token-streams greatly reduces comparison noise due to extraneous comments, or differences in whitespace or identifier names.
 * `clover` is used as a graph-layout library. Uses [JGraphT](https://github.com/jgrapht/jgrapht) and [JGraph](https://github.com/jgraph/legacy-jgraph5) for graph representation and rendering.
Note that JGraph is now known as [GraphMX](https://www.jgraph.com/); clover relies on an old version.
 * `ac-ui` provides the user interface, and relies on all other AC modules. It is entirely possible to build a command-line
tool to run comparisons without any interface.

All modules of AC are licensed under the [GPLv3](https://www.gnu.org/licenses/gpl-3.0.en.html).

The direct dependencies of each module have their own licences - check the output of `mvn dependency:tree` (after running `mvn install` on the project) for a full list of transitive dependencies.

## History

AC was born in the [Escuela Politécnica Superior](http://www.uam.es/ss/Satellite/EscuelaPolitecnica/es/home.htm) 
of the [Universidad Autónoma de Madrid](http://www.uam.es/ss/Satellite/es/home/) to deter and detect source-code plagiarism in programming assignments.

Notable versions:
  * 1.0: a collection of csh scripts, written by an unnamed teacher at EPS/UAM
  * 1.1 (March 2006): Manuel Freire rewrites it in bash, using a single script,
  and using graphviz for graph visualization
  * 1.2: Manuel Cebrián and Juan del Rosal join the team, and the code is rewritten
  in Java, with clover used for graph visualization instead of graphviz. Widespread
  use in UAM. Old website (http://tangow.ii.uam.es/ac).
  * 1.8 (Oct 2010): Successfully used to uncover gene mutation paths in that year's [VAST
  analytics contest](http://dx.doi.org/10.1109/VAST.2010.5652834) (although a custom test was added).
  * 2.0 (Oct 2016): Switched to Maven, Antlr4, moved to github
