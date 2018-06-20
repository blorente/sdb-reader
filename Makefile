#TODO: Move partial results and output to a separate dotfolder:
# OUTDIR=.analysis

IVY_CACHE=~/.ivy2/cache
FAKE_IVY_FOLDER=.ivy
SBT_MEMORY=2000
SBT_COMMAND=sbt -batch -ivy $(FAKE_IVY_FOLDER) -mem $(SBT_MEMORY) -Dsbt.log.noformat=true
CLOC_BY_FILE=cloc --csv --by-file-by-lang --list-file

all: sources.csv

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
