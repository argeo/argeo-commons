



#base-java-sources : $(shell find org.argeo.* -name '*.java') 

#base-java-classes : $(shell find org.argeo.* -name '*.class') 

# each dir depends on its package directories
#$(SDK_SRC_BASE)/% : $(shell  find */src -type f  | sed "s%/[^/]*$%%" | sort -u)

# non empty package dirs
#$(SDK_SRC_BASE)/*/src/% : $(shell  grep --no-filename ^import '%/*.java' | sed 's/import //g' | sed 's/static //g' | sed 's/\.[A-Za-z0-9_]*;//' | sed 's/\.[A-Z].*//'  | sort |  uniq)

# convert dir to package
#$(shell find %/src -mindepth 1 -type d -printf '%P\n' | sed "s/\//\./g")

# all packages
# grep --no-filename ^import src/org/argeo/api/uuid/*.java | sed "s/import //g" | sed "s/static //g" | sed "s/\.[A-Za-z0-9_]*;//" | sed "s/\.[A-Z].*//"  | sort |  uniq


