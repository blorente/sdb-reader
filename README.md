# SDB exploration project

Running:

1. Install stuff with `setup.sh`
2. Generate semanticdb for the desired project:
    `make -C path/to/project -f relative/path/to/this/Makefile semanticdb-packages`
3. Run the project with sbt (you can add the quoted line as an intellij task):
    `sbt "run --jarsfile ../path/to/project/semanticdb-packages"`