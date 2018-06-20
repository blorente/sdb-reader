import better.files._
import org.langmeta.internal.semanticdb.{vfs => v}

import scala.meta._
import scala.meta.internal.semanticdb3.{SymbolInformation, TextDocument}

case class Config(projectJars: File = File("/dev/null"))

class SymbolRepository(documents: Traversable[TextDocument]) {
  val symbols: Map[String, SymbolInformation] =
    (for {
      document <- documents.par
      symbolDeclaration <- document.symbols
    } yield symbolDeclaration.symbol -> symbolDeclaration).seq.toMap
}

object Main {
  def loadJars(jarsFiles: Traversable[String]) =
    for {
      jar <- jarsFiles
      classpath = Classpath(jar)
      database = v.Database.load(classpath)
      withSchema = database.toSchema
      doc <- withSchema.documents
    } yield doc

  def main(args: Array[String]) = {
    val projectJars = File("./projects/workshop/semanticdb-packages")
    val dependencyJars = File("./projects/workshop/dependencies-packages")

    val projectDocs = loadJars(projectJars.lines)
    val dependencyDocs = loadJars(dependencyJars.lines)

    val projectSymbols = new SymbolRepository(projectDocs)
    val dependencySymbols = new SymbolRepository(dependencyDocs)
    println(projectDocs)
  }
}
