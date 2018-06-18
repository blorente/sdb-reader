package cz.cvut.fit.prl.scalaimplicit.application

import scala.meta._
import better.files._
import org.langmeta.internal.semanticdb.{vfs => v}

import scala.meta.internal.semanticdb3.SymbolInformation

case class Config(projectJars: File = File("/dev/null"),
                  outdir: File = File("/dev/null"))
object Main {
  def main(args: Array[String]) = {
    val cliConfig = Cli.CliParser.parse(args, Config()).get
    val jars = cliConfig.projectJars.lines
    val dbs = for {
      jar <- jars
      classpath = Classpath(jar)
      database = v.Database.load(classpath)
      withSchema = database.toSchema
      doc <- withSchema.documents
    } yield doc

    // dbs.head.synthetics.head.text.get.occurrences.map(_.symbol).map(firstDoc(_))

    //implicit val ctx: DocumentContext = new DocumentContext(dbs)
    //val callSites = dbs.flatMap(doc => doc.occurrences.map(SchemaFactories.createCallSite(_, OccurrencePayload())))
    //val declarations = dbs.flatMap(_.symbols.map(SchemaFactories.createDeclaration))
    val firstDoc = (for {
      symbol <- dbs.head.symbols
    } yield symbol.symbol -> symbol).toMap
    println(dbs)
  }
}
