#TODO: Move partial results and output to a separate dotfolder:
# OUTDIR=.analysis

IVY_CACHE=~/.ivy2/cache
FAKE_IVY_FOLDER=.ivy
SBT_MEMORY=2000
SBT_COMMAND=sbt -batch -ivy $(FAKE_IVY_FOLDER) -mem $(SBT_MEMORY) -Dsbt.log.noformat=true
CLOC_BY_FILE=cloc --csv --by-file-by-lang --list-file

all: sources.csv

$(FAKE_IVY_FOLDER):
	mkdir $(FAKE_IVY_FOLDER)
	ln -s $(IVY_CACHE) $(FAKE_IVY_FOLDER)/cache

# Generate Semanticdb
SEMANTICDB-1-PLUGIN="~/.sbt/1.0/plugins/semanticdb-config-1.0-v3.scala"
SEMANTICDB-0_13-PLUGIN="~/.sbt/0.13/plugins/semanticdb-config-0.13-v3.scala"
semanticdb-success: $(FAKE_IVY_FOLDER) $(SEMANTICDB-1-PLUGIN) $(SEMANTICDB-0_13-PLUGIN)
	$(SBT_COMMAND) semanticdb ; if [ $$? -eq 0 ] ; then echo "success!" > semanticdb-success ; fi

semanticdb-packages: semanticdb-success
	$(SBT_COMMAND) "package" "test:package" ; \
  if [ $$? -eq 0 ] ; \
		then find `pwd` -wholename "*target/scala-*/*_2.*-*.jar" > semanticdb-packages ; \
  fi
