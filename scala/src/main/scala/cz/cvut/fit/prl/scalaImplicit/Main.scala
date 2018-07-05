package cz.cvut.fit.prl.scalaImplicit

import better.files._
import cz.cvut.fit.prl.scalaImplicit.processors._
import org.langmeta.internal.semanticdb.{vfs => v}

import scala.collection.parallel.ParSeq
import scala.meta._



object Main {
  def loadJars(jarsFiles: Traversable[String]) =
    for {
      jar <- jarsFiles if !jar.startsWith("/tmp/")
      classpath = Classpath(jar)
      database = v.Database.load(classpath)
      withSchema = database.toSchema
      doc <- withSchema.documents
    } yield doc

  def main(args: Array[String]): Unit = {
    val output = File("./projects/test-project/results.csv")

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

    CSV.write(output, results)
  }
}

/*
object ImplicitParams extends cz.cvut.fit.prl.scalaImplicit.processors.Processor {
  override def apply(docs: Traversable[TextDocument],
                     repository: SymbolRepository): cz.cvut.fit.prl.scalaImplicit.processors.ProcessResult = {

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

    cz.cvut.fit.prl.scalaImplicit.processors.ProcessResult("callsites_with_implicit_params",
                  syntheticsWithParams.size.toString)
  }
}
*/
