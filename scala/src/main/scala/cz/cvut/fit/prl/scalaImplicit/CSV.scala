package cz.cvut.fit.prl.scalaImplicit

import better.files.File
import cz.cvut.fit.prl.scalaImplicit.processors.Result

object CSV {
  def write(target: File, result: Result): Unit = {
    val properties = result.properties.toSeq.sortBy(_._1).unzip
    val headers = properties._1.mkString(",")
    val values = properties._2.mkString(",")
    target.append(s"$headers\n$values")
  }
}
