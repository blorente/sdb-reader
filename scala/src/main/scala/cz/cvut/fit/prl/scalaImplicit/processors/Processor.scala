package cz.cvut.fit.prl.scalaImplicit.processors

import cz.cvut.fit.prl.scalaImplicit.SymbolRepository

import scala.meta.internal.semanticdb3.TextDocument

trait Processor
    extends ((Traversable[TextDocument], SymbolRepository) => Result) {}
