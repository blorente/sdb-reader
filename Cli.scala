package cz.cvut.fit.prl.scalaimplicit.application

import better.files.File

/**
  * Module to parse command line arguments using scopt.
  * Each `opt` represents an argument. In this case, both are `required()`.
  * We also make sure that the files passed in --changed-files exists
  * (and therefore their parent folders too).
  */
object Cli {
  val CliParser = new scopt.OptionParser[Config]("scopt") {
    head("implicitExtractor", "0.2")

    opt[String]("jarsfile")
      .required()
      .validate(x =>
        if (File(x).exists) success
        else failure("Project jars file not found"))
      .action((x, c) => c.copy(projectJars = File(x)))

    opt[String]("output")
      .required()
      .validate(x => {
        val d = File(x)
        if (d.exists && d.isDirectory) success
        else failure("Output directory not specified")
      })
      .action((x, c) => c.copy(outdir = File(x)))
  }
}
