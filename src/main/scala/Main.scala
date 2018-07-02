import better.files._
import org.langmeta.internal.semanticdb.{vfs => v}

import scala.collection.parallel.ParSeq
import scala.meta._
import scala.meta.internal.semanticdb3.{SymbolInformation, TextDocument}

class SymbolRepository(projectDocuments: Traversable[TextDocument],
                       dependencyDocuments: Traversable[TextDocument]) {
  private def symbols(
      docs: Traversable[TextDocument]): Map[String, SymbolInformation] =
    (for {
      document <- docs.par
      symbolDeclaration <- document.symbols
    } yield symbolDeclaration.symbol -> symbolDeclaration).seq.toMap

  val projectSymbols = symbols(projectDocuments)
  val dependencySymbols = symbols(dependencyDocuments)
  def symbol(id: String): SymbolInformation =
    if (projectSymbols.contains(id)) projectSymbols(id)
    else dependencySymbols(id)
}

case class ProcessResult(properties: Set[(String, String)]) {
  def ++(other: ProcessResult): ProcessResult =
    this.copy(properties = properties ++ other.properties)
}

object ProcessResult {
  def apply(name: String, value: String): ProcessResult =
    new ProcessResult(Set(name -> value))
}

trait Processor
    extends ((Traversable[TextDocument], SymbolRepository) => ProcessResult) {}
/*
object ImplicitParams extends Processor {
  override def apply(docs: Traversable[TextDocument],
                     repository: SymbolRepository): ProcessResult = {

    val syntheticsWithParams = (for {
      doc <- docs.par
      synthetic <- doc.synthetics
      synthDocument <- synthetic.text
    } yield synthDocument).filter(
      _.occurrences.exists(
        occ =>
          occ.symbol != "_star_." && repository
            .symbol(occ.symbol)
            .kind
            .isParameter))

    val relevantSynthetic = (for {
      doc <- docs.par
      synthetic <- doc.synthetics
      synthDocument <- synthetic.text
    } yield synthDocument).toSeq(6)

    val relevantSyntheticText = relevantSynthetic.text.parse[Term].get

    ProcessResult("callsites_with_implicit_params",
                  syntheticsWithParams.size.toString)
  }
}
*/

object Main {
  def loadJars(jarsFiles: Traversable[String]) =
    for {
      jar <- jarsFiles if !jar.startsWith("/tmp/")
      classpath = Classpath(jar)
      database = v.Database.load(classpath)
      withSchema = database.toSchema
      doc <- withSchema.documents
    } yield doc

  def main(args: Array[String]) = {
    val projectJars = File("./projects/test-project/semanticdb-packages")
    val dependencyJars = File("./projects/test-project/dependencies-packages")

    val projectDocs = loadJars(projectJars.lines)
    val dependencyDocs = loadJars(dependencyJars.lines)

    val symbols = new SymbolRepository(projectDocs, dependencyDocs)

    val registeredProcessors: ParSeq[Processor] = Seq(
      ImplicitDeclarations
    ).par
    val results =
      registeredProcessors.map(_(projectDocs, symbols)).reduce(_ ++ _)
    println(results)
  }
}
