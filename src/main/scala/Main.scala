import better.files._
import org.langmeta.internal.semanticdb.{vfs => v}

import scala.meta._

case class Config(projectJars: File = File("/dev/null"))
object Main {
  def main(args: Array[String]) = {
    val cliConfig = Cli.CliParser.parse(args, Config()).get
    def loadJars(jarsFiles: Traversable[String]) = for {
      jar <- jarsFiles
      classpath = Classpath(jar)
      database = v.Database.load(classpath)
      withSchema = database.toSchema
      doc <- withSchema.documents
    } yield doc

    val projectDocs = loadJars(cliConfig.projectJars.lines)
    val dependencyDocs = loadJars(File("./dependencies-packages").lines)
    // dbs.head.synthetics.head.text.get.occurrences.map(_.symbol).map(firstDoc(_))

    //implicit val ctx: DocumentContext = new DocumentContext(dbs)
    //val callSites = dbs.flatMap(doc => doc.occurrences.map(SchemaFactories.createCallSite(_, OccurrencePayload())))
    //val declarations = dbs.flatMap(_.symbols.map(SchemaFactories.createDeclaration))
//    val firstDoc = (for {
//      symbol <- dbs.head.symbols
//    } yield symbol.symbol -> symbol).toMap
    println(projectDocs)
  }
}
