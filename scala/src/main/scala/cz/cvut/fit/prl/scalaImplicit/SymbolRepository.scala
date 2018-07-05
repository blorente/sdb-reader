package cz.cvut.fit.prl.scalaImplicit

import scala.meta.internal.semanticdb3.{SymbolInformation, TextDocument}

class SymbolRepository(projectDocuments: Traversable[TextDocument],
                       dependencyDocuments: Traversable[TextDocument]) {
  private def gatherSymbols(
      docs: Traversable[TextDocument]): Map[String, SymbolInformation] =
    (for {
      document <- docs.par
      symbolDeclaration <- document.symbols
    } yield symbolDeclaration.symbol -> symbolDeclaration).seq.toMap

  private val projectSymbols = gatherSymbols(projectDocuments)
  private val dependencySymbols = gatherSymbols(dependencyDocuments)
  def symbol(id: String): SymbolInformation =
    if (projectSymbols.contains(id)) projectSymbols(id)
    else dependencySymbols(id)
}
