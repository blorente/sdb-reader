# Variables that you might want to change

# Version of semanticdb to use. Note that this must coincide with the one in the SBT plugins
SEMANTICDB_VERSION=3.7.4
# Absolute path to the directory where the ivy dependencies will be downloaded and stored
IVY_CACHE=/home/borja/.ivy2/cache
# Amount of memory to allocate to each SBT command
SBT_MEMORY=2000
# Directory where COURSIER is installed (we need it for metacp, see below)
COURSIER=/home/borja/Software/coursier
# Absolute path to the directory where the semanticdb files of dependencies will be stored
METACP_CACHE_DIR=/home/borja/work/scala/.dependencies
# Location of additional jar files with dependencies not caught by SBT
RT_JAR_LOCATION=/usr/lib/jvm/java-8-oracle/jre/lib/rt.jar
SBT_LAUNCH_JAR_LOCATION=/usr/share/sbt/bin/sbt-launch.jar

# End of variables that you might want to change

# Name of the fake ivy folder used to trick sbt into executing in parallel
FAKE_IVY_FOLDER=.ivy
# Command to execute the tools with all the flags already in place
SBT_COMMAND=sbt -batch -ivy $(FAKE_IVY_FOLDER) -mem $(SBT_MEMORY) -Dsbt.log.noformat=true
METACP_COMMAND=$(COURSIER) launch org.scalameta::metacp:$(SEMANTICDB_VERSION) -r sonatype:snapshots -- --cache-dir $(METACP_CACHE_DIR) --par

all: semanticdb-packages dependencies-packages

clean:
	rm semanticdb-packages
	rm semanticdb-success
	rm dependencies-packages
	rm dependencies.dat

# Setup

$(FAKE_IVY_FOLDER):
	mkdir $(FAKE_IVY_FOLDER)
	ln -s $(IVY_CACHE) $(FAKE_IVY_FOLDER)/cache

$(METACP_CACHE_DIR):
	mkdir -p $(METACP_CACHE_DIR)
	$(METACP_COMMAND) $(RT_JAR_LOCATION)

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

# Generate Semanticdb for the project
semanticdb-packages: semanticdb-success
	$(SBT_COMMAND) "package" "test:package" ; \
  if [ $$? -eq 0 ] ; \
		then find `pwd` -wholename "*target/scala-*/*_2.*-*.jar" > semanticdb-packages ; \
  fi

semanticdb-success: $(FAKE_IVY_FOLDER) # $(SDB_1_PLUGIN_TARGET) $(SDB_0_13_PLUGIN_TARGET)
	$(SBT_COMMAND) semanticdb ; if [ $$? -eq 0 ] ; then echo "success!" > semanticdb-success ; fi

# Generate Semanticdb for the dependencies
dependencies-packages: dependencies.dat $(METACP_CACHE_DIR)
	echo "$(METACP_CACHE_DIR)/scala-library-synthetics.jar" > dependencies-packages ; \
	sort dependencies.dat | uniq | parallel $(METACP_COMMAND) {} >> dependencies-packages \;

dependencies.dat: $(FAKE_IVY_FOLDER)
	$(SBT_COMMAND) "show test:fullClasspath" \
	| sed -n -E 's/Attributed\(([^)]*)\)[,]?/\n\1\n/gp' \
	| grep "^/" > dependencies.dat ; \
	echo $(RT_JAR_LOCATION) >> dependencies.dat ; \
	echo $(SBT_LAUNCH_JAR_LOCATION) >> dependencies.dat ;

