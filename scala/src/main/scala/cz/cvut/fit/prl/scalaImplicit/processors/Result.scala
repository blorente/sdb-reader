package cz.cvut.fit.prl.scalaImplicit.processors

case class Result(properties: Set[(String, String)]) {
  def ++(other: Result): Result =
    this.copy(properties = properties ++ other.properties)
}

object Result {
  def apply(name: String, value: String): Result =
    new Result(Set(name -> value))
}