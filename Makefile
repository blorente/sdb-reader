#TODO: Move partial results and output to a separate dotfolder:
# OUTDIR=.analysis

SEMANTICDB_VERSION=3.7.4

IVY_CACHE=~/.ivy2/cache
FAKE_IVY_FOLDER=.ivy
SBT_MEMORY=2000
SBT_COMMAND=sbt -batch -ivy $(FAKE_IVY_FOLDER) -mem $(SBT_MEMORY) -Dsbt.log.noformat=true
CLOC_BY_FILE=cloc --csv --by-file-by-lang --list-file

COURSIER=~/Software/coursier
METACP_CACHE_DIR=./dependencies
METACP_COMMAND=~/Software/coursier launch org.scalameta::metacp:$(SEMANTICDB_VERSION) -r sonatype:snapshots -- --cache-dir $(METACP_CACHE_DIR) --par --include-scala-library-synthetics

all:
	echo "No all for now"

# Setup

$(FAKE_IVY_FOLDER):
	mkdir $(FAKE_IVY_FOLDER)
	ln -s $(IVY_CACHE) $(FAKE_IVY_FOLDER)/cache

#####################################################################################
# This is an inelegant solution, I'd rather use the setup.sh script for now					#
# SDB_1_PLUGIN_SOURCE="~/work/scala/sdb-reader/semanticdb-config-1.0-v3.scala"		  #
# SDB_1_PLUGIN_TARGET="~/.sbt/1.0/plugins/semanticdb-config-1.0-v3.scala"					  #
# $(SDB_1_PLUGIN_TARGET):																													  #
# 	cp $(SDB_1_PLUGIN_SOURCE) $(SDB_1_PLUGIN_TARGET)															  #
# 																																								  #
# SDB_0_13_PLUGIN_SOURCE="~/work/scala/sdb-reader/semanticdb-config-0.13-v3.scala"  #
# SDB_0_13_PLUGIN_TARGET="~/.sbt/0.13/plugins/semanticdb-config-0.13-v3.scala"		  #
# $(SDB_0_13_PLUGIN_TARGET):																											  #
# 	cp $(SDB_0_13_PLUGIN_SOURCE) $(SDB_0_13_PLUGIN_TARGET)												  #
#####################################################################################

# Generate Semanticdb
semanticdb-success: $(FAKE_IVY_FOLDER) # $(SDB_1_PLUGIN_TARGET) $(SDB_0_13_PLUGIN_TARGET)
	$(SBT_COMMAND) semanticdb ; if [ $$? -eq 0 ] ; then echo "success!" > semanticdb-success ; fi

semanticdb-packages: semanticdb-success
	$(SBT_COMMAND) "package" "test:package" ; \
  if [ $$? -eq 0 ] ; \
		then find `pwd` -wholename "*target/scala-*/*_2.*-*.jar" > semanticdb-packages ; \
  fi

dependencies-success: dependencies.dat
	mkdir ./dependencies ; \
	for dep in $$(cat dependencies.dat | uniq) ; do \
    $(METACP_COMMAND) $$dep ; \
	done ; \
	if [ $$? -eq 0 ] ; \
		echo "success" > dependencies-success ; \
	fi

dependencies-packages: dependencies-success
	find ./dependencies -maxdepth 1 -mindepth 1 > dependencies-packages

dependencies.dat:
	$(SBT_COMMAND) "show test:fullClasspath" \
	| sed -n -E 's/Attributed\(([^)]*)\)[,]?/\n\1\n/gp' \
	| grep "^/" > dependencies.dat ;



