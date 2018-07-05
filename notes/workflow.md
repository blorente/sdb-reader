
# Table of Contents

1.  [Workflow for this project](#org0c92aad)
    1.  [Makefile: Generating semanticdb files](#orgfde789c)
        1.  [Before using the Makefile](#orgf095030)
        2.  [Using the Makefile](#org873df8b)
        3.  [Generate Semanticdb files from a project.](#org6ba24ad)
        4.  [Generate semanticdb files for the dependencies of a project](#org37cb278)
        5.  [To do everything in one go:](#org7ecb1e9)
    2.  [Scala: Processing the semanticdb files](#org1d542a7)
        1.  [Base Semanticdb Concepts](#org276c472)
        2.  [Doing an analysis](#org0fadf96)
        3.  [A utility: class SymbolRepository](#orgeca7afa)
        4.  [The flow of the program](#org433d20b)
        5.  [Extracting new information](#org9d84f0c)
        6.  [Semanticdb Gotchas](#orga3e1df0)
    3.  [R to generate graphs](#org0fa8645)


<a id="org0c92aad"></a>

# Workflow for this project


<a id="orgfde789c"></a>

## Makefile: Generating semanticdb files

This project uses GNU Make to automate the extraction of information from projects.
In particular, we use it to compile each project and generate the semanticdb files for it.
This is reflected in the `semanticdb-packages` target in the Makefile.
There are also additional targets to generate semanticdb files for dependencies.


<a id="orgf095030"></a>

### Before using the Makefile

Make sure to change the variables at the top of the file to reflect the setup desired.
In addition to that, install the SBT plugins in `sbt-plugins` by executing `./sbt-plugins/setup.sh`.


<a id="org873df8b"></a>

### Using the Makefile

We can use the Makefile to generate the semanticdb for a single project.
For that, we execute the makefile inside the directory of the project:

    make -C path/to/project -f ../relative/path/from/project/to/the/makefile <target>

For instance, with the following directory structure:

    /var/lib/scala
    ├── Makefile
    ├── projects
    │   ├── ensime-server
    │   ├── ...
    │   └── metascala
    └── ...

We would execute:

    cd /var/lib/scala
    make -C projects/ensime-server -f ../../Makefile <target>


<a id="org6ba24ad"></a>

### Generate Semanticdb files from a project.

The main target to generate semanticdb files is `semanticdb-packages`.
It will leave a file with the same name in the root directory of a project, 
with a list of JAR files containing the semanticdb files of the projects.  

The generation process is the following

1.  semanticdb-success

    First, an intermediate target tries to compile the project and generate semanticdb files out of it.
    It calls the `semanticdb` task in SBT and it reads the exit code of the execution, 
    storing it in a file if it was successful.

2.  semanticdb-packages

    It is easier to read semanticdb files from JAR files, so we call SBT `package` and `test:package` to generate
    those jars. For each subproject, this will leave two JAR files in the `target` directory.
    If that is successful, it will find all the jars with the appropriate naming scheme and list them
    in the file `semanticdb-packages`.
    For instance, for an old version of our workshop, this would be the resulting `semanticdb-packages`:
    
        /home/borja/work/scala/sdb-reader/projects/workshop/core/target/scala-2.11/coreutils_2.11-0.1-SNAPSHOT-tests.jar
        /home/borja/work/scala/sdb-reader/projects/workshop/core/target/scala-2.11/coreutils_2.11-0.1-SNAPSHOT.jar
        /home/borja/work/scala/sdb-reader/projects/workshop/cs-counter/target/scala-2.11/callsitecounter_2.11-0.1-SNAPSHOT-tests.jar
        /home/borja/work/scala/sdb-reader/projects/workshop/cs-counter/target/scala-2.11/callsitecounter_2.11-0.1-SNAPSHOT.jar
        /home/borja/work/scala/sdb-reader/projects/workshop/queries/target/scala-2.11/queries_2.11-0.1-SNAPSHOT-tests.jar
        /home/borja/work/scala/sdb-reader/projects/workshop/queries/target/scala-2.11/queries_2.11-0.1-SNAPSHOT.jar
        /home/borja/work/scala/sdb-reader/projects/workshop/target/scala-2.11/root_2.11-0.1-SNAPSHOT.jar
        /home/borja/work/scala/sdb-reader/projects/workshop/target/scala-2.11/root_2.11-0.1-SNAPSHOT-tests.jar
        /home/borja/work/scala/sdb-reader/projects/workshop/extractor/target/scala-2.11/implicitextractor_2.11-0.1-SNAPSHOT-tests.jar
        /home/borja/work/scala/sdb-reader/projects/workshop/extractor/target/scala-2.11/implicitextractor_2.11-0.1-SNAPSHOT.jar


<a id="org37cb278"></a>

### Generate semanticdb files for the dependencies of a project

Semanticdb v3/4 does not capture all the information about symbols in a project.
In particular, SDB does not provide Symbol Information for synthetics.  
This means that there are cases where a synthetic references a symbol whose information we don't have.
To circunvent that, we use `metacp` to extract Symbol Information from the dependencies of a project,
to be later loaded into our analyses. Metacp works on class files, so we only have access to symbol definitions.
However, this ought to be enough.

To generate this we can use the target `dependencies-packages`, which uses the following subtargets:

1.  dependencies.dat

    The exact same process we used for `classpath.dat` in previous versions of the extractor, it leaves a file
    `dependencies.dat` with all the dependencies a project lists. It also adds two additional dependencies,
    `rt.jar` (Java runtime) and `sbt-launch.jar` (contains Scala runtime), which are not listed by SBT.

2.  dependencies-packages

    For each package listed in `dependencies.dat`, it uses `parallel` to execute `metacp` and generate 
    semanticdb files. To avoid extra work, all projects use the same `metacp` cache dir, so each dependencie
    is only processed once.
    
    Then, it lists the absolute paths to the results in a file called `dependencies-packages`.


<a id="org7ecb1e9"></a>

### To do everything in one go:

In practice, it's usually better to just use the `all` target in the makefile, wich will take care of everything:

    make -C path/to/project -f ../../Makefile all


<a id="org1d542a7"></a>

## Scala: Processing the semanticdb files


<a id="org276c472"></a>

### Base Semanticdb Concepts

(Based on [Semanticdb Spec](https://github.com/scalameta/scalameta/blob/master/semanticdb/semanticdb3/semanticdb3.md))

1.  TextDocument

    Corresponds to information about a single Scala file in a project. 
    Therefore, a project's Semanticdb will consist of a series of TextDocuments.
    
    A TextDocument can have a list of Synthetics, a list of SymbolInformation, and another list of SymbolOccurrences.
    Note that TextDocuments generated with `metacp` (such as dependencies) do not have SymbolOccurrences or Synthetics.

2.  Symbol

    In v3, a `Symbol` is a unique fully qualified name, used to reference a singular instance of SymbolInformation
    that describes it.
    We can think of Semanticdb as a `Map[Symbol, SymbolInformation].
         ~Symbols` are plain Scala ~String~s.

3.  SymbolInformation

    Contains all the information relevant to the declaration of a symbol. Among other things, it has it's Type,
    Kind (e.g. `METHOD`, `CLASS`) and Properties (e.g. `IMPLICIT`).

4.  SymbolOccurrence

    An appearance of a symbol in the source of a file.
    It contains location information, as well as a unique identifier String (e.g. `scala.Int#`).
    This identifier can be used as an index in our `SymbolRepository` (more later).
    It also contains the role of the symbol. This can be either DECLARATION or REFERENCE.


<a id="org0fadf96"></a>

### Doing an analysis

There is a small Scala project to read and process the semanticdb files.
When executed on a project, it loads the jars generated with the Makefile
and outputs a CSV. The CSV will have one header row and one data row.
For example, an output might be:

    implicit_declarations,implicit_conversions
    10, 4


<a id="orgeca7afa"></a>

### A utility: class SymbolRepository

The whole point of upgrading to v3 was to be able to lookup symbols by name in a unique way.
The SymbolRepository is aimed towards that, implementing the following interface:

    def symbol(fqn: String): SymbolInformation = ...

That is, one can lookup the information of any global symbol with just it's fully qualified name.
It looks in the semanticdb files of the project and its dependencies for the desired name.

NOTE that [local symbols](https://github.com/scalameta/scalameta/blob/master/semanticdb/semanticdb3/semanticdb3.md#symbol) are not supported yet.


<a id="org433d20b"></a>

### The flow of the program

-   Load the project's `TextDocuments` (semanticdb files) and dependencies with `loadJars`

    val projectJars = File("./projects/test-project/semanticdb-packages")
    val dependencyJars = File("./projects/test-project/dependencies-packages")
    
    val projectDocs = loadJars(projectJars.lines)
    val dependencyDocs = loadJars(dependencyJars.lines)

-   Generate a `SymbolRepository` to gather all the ~SymbolInformation~s

    val symbols = new SymbolRepository(projectDocs, dependencyDocs)

-   List the desired `Processors`. A `Processor` is an object implementing the function 
    `(Traversable[TextDocument], SymbolRepository) => Result`.
    Each one of these `Processor` s will generate a `Result`.
    A `Result` represents one or more columns of the target CSV.

    val registeredProcessors: ParSeq[Processor] = Seq(
      ImplicitDeclarations
    ).par

-   Apply all the `Processor~s in parallel, and merge the ~Result~s.
      A ~Result` is just a `Set` of `(String, String)`, so they can be merged in parallel.

    val results =
       registeredProcessors.map(_(projectDocs, symbols)).reduce(_ ++ _)

-   Write the results to a CSV file.
    The results are sorted lexicographically, so the order of the columns is consistent accross executions.

    CSV.write(output, results)

    object CSV {
      def write(target: File, result: Result): Unit = {
        val properties = result.properties.toSeq.sortBy(_._1).unzip
        val headers = properties._1.mkString(",")
        val values = properties._2.mkString(",")
        target.append(s"$headers\n$values")
      }
    }


<a id="org9d84f0c"></a>

### Extracting new information

To extract new information from a project, the only necessary thing is to create an implementation of `Processor`,
and adding it to `registeredProcessors` in Main.

For instance, this would count how many implicits are defined in a project:

    object ImplicitDeclarations extends Processor {
      def isImplicit(info: SymbolInformation): Boolean =
        (info.properties & IMPLICIT.value) > 0
    
      override def apply(docs: Traversable[TextDocument],
                         symbols: SymbolRepository): Result = {
        val implicitDeclarations = for {
          doc <- docs
          definition <- doc.occurrences if
          definition.role.isDefinition &&
            isImplicit(symbols.symbol(definition.symbol))
        } yield symbols.symbol(definition.symbol)
    
        Result("implicit_declarations", implicitDeclarations.size.toString)
      }
    }


<a id="orga3e1df0"></a>

### Semanticdb Gotchas

Semanticdb has a few gotchas that we hadn't predicted:

1.  Implicit classes:

    -   Implicit classes generate 2 IMPLICIT SymbolInformations: One for the class, one for the constructor.
    -   The latter is synthetic, and therefore won't have a DEFINITION Occurrence in the source code.
    -   It might have REFERENCE Occurrences though, if the implicit class is used in the file. This is why
        it appears in SymbolInformation.
    -   To treat this, if we only care about declarations -> We have to start from Occurrences.

2.  Type information:

    Types in v3 are fairly complete, but uncomfortable to access.
    Each SymbolInformation has a field Type. Each Type has a Tag to identify which kind of type it is,
    [as explained in the schema](https://github.com/scalameta/scalameta/blob/v3.7.4/semanticdb/semanticdb3/semanticdb3.md#type).
    
    There has been some work to fix this on v4.0, but it's not stable yet.

3.  Parameters:

    Parameters in v3 are global symbols.
    This means that if we want to analyze all the parameters from a method,
    we can look into its Type, which will contain the appropriate Symbol names for each of the parameters.     


<a id="org0fa8645"></a>

## TODO R to generate graphs

It should be simple to merge the CSV files in each project and form a graph with R.

