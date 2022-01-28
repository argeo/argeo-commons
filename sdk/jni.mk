TARGET_EXEC := libJava_$(NATIVE_PACKAGE).so

BASE_DIR := $(shell realpath .)
SRC_DIRS := . 

LDFLAGS=-shared -fPIC -Wl,-soname,$(TARGET_EXEC).$(MAJOR).$(MINOR)

%:
	@echo Making '$@' $(if $^,from '$^')
	@echo 'MAJOR=$(MAJOR)'
	@echo 'MINOR=$(MINOR)'
	@echo 'BASE_DIR=$(BASE_DIR)'
	@echo 'SRC_DIRS=$(BASE_DIR)'
	@echo 'BUILD_DIR=$(BUILD_DIR)'
	@echo 'MAKEFILE_DIR=$(MAKEFILE_DIR)'
	

#
# Generic Argeo
#
#BUILD_DIR := $(SDK_BUILD_BASE)/$(NATIVE_PACKAGE)
BUILD_DIR := ./build
#META_INF_DIR := ./../META-INF
ARCH := $(shell uname -p)

# Every folder in ./src will need to be passed to GCC so that it can find header files
INC_DIRS := $(shell find $(SRC_DIRS) -type d) /usr/lib/jvm/java/include /usr/lib/jvm/java/include/linux

.PHONY: clean all ide
all: $(BUILD_DIR)/$(TARGET_EXEC)

# Find all the C and C++ files we want to compile
# Note the single quotes around the * expressions. Make will incorrectly expand these otherwise.
SRCS := $(shell find $(SRC_DIRS) -name '*.cpp' -or -name '*.c' -or -name '*.s')

# String substitution for every C/C++ file.
# As an example, hello.cpp turns into ./build/hello.cpp.o
OBJS := $(SRCS:%=$(BUILD_DIR)/%.o)

# String substitution (suffix version without %).
# As an example, ./build/hello.cpp.o turns into ./build/hello.cpp.d
DEPS := $(OBJS:.o=.d)

# Add a prefix to INC_DIRS. So moduleA would become -ImoduleA. GCC understands this -I flag
INC_FLAGS := $(addprefix -I,$(INC_DIRS))

# The -MMD and -MP flags together generate Makefiles for us!
# These files will have .d instead of .o as the output.
CPPFLAGS := $(INC_FLAGS) -MMD -MP

# The final build step.
$(BUILD_DIR)/$(TARGET_EXEC): $(OBJS)
	$(CC) $(OBJS) -o $@ $(LDFLAGS)

# Build step for C source
$(BUILD_DIR)/%.c.o: %.c
	mkdir -p $(dir $@)
	$(CC) $(CPPFLAGS) $(CFLAGS) -c $< -o $@

# Build step for C++ source
$(BUILD_DIR)/%.cpp.o: %.cpp
	mkdir -p $(dir $@)
	$(CXX) $(CPPFLAGS) $(CXXFLAGS) -c $< -o $@

clean:
	rm -r $(BUILD_DIR)

# Include the .d makefiles. The - at the front suppresses the errors of missing
# Makefiles. Initially, all the .d files will be missing, and we don't want those
# errors to show up.
-include $(DEPS)

# MAKEFILE_DIR := $(dir $(firstword $(MAKEFILE_LIST)))
