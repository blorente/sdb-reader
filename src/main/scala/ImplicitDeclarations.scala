import scala.meta.internal.semanticdb3.SymbolInformation.{Kind, Property}
import scala.meta.internal.semanticdb3.SymbolInformation.Property.IMPLICIT
import scala.meta.internal.semanticdb3.{SymbolInformation, TextDocument}

object ImplicitDeclarations extends Processor {
  def isImplicit(info: SymbolInformation): Boolean =
    (info.properties & IMPLICIT.value) > 0

  private def gatherImplicitConversions(allImplicitDeclarations: Traversable[SymbolInformation], symbols: SymbolRepository) = {
    def isImplicitConversion(declaration: SymbolInformation): Boolean = declaration.kind match {
      case Kind.CLASS => true
      case Kind.OBJECT => false
      case Kind.METHOD =>
        val parameterSymbols = declaration.tpe.get.methodType.get.parameters.map(_.symbols.map(param => symbols.symbol(param)))
        val nonImplicitParameterLists = parameterSymbols.filterNot(paramList => isImplicit(paramList.head))
        nonImplicitParameterLists.size == 1 && nonImplicitParameterLists.head.size == 1
      case _ => false
    }
    allImplicitDeclarations.filter(isImplicitConversion)
  }

  override def apply(docs: Traversable[TextDocument],
                     symbols: SymbolRepository): ProcessResult = {
    val allImplicitDeclarations = gatherImplicitSymbols(docs, symbols)

    val implicitConversions = gatherImplicitConversions(allImplicitDeclarations, symbols)

    ProcessResult(Set(
      "implicit_declarations" -> allImplicitDeclarations.size.toString,
      "implicit_conversions" -> implicitConversions.size.toString
    ))
  }

  private def gatherImplicitSymbols(docs: Traversable[TextDocument], symbols: SymbolRepository) = {
    for {
      doc <- docs
      definition <- doc.occurrences if
      definition.role.isDefinition &&
        isImplicit(symbols.symbol(definition.symbol))
    } yield symbols.symbol(definition.symbol)
  }
}
