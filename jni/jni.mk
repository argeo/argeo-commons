TARGET_EXEC := libJava_$(NATIVE_PACKAGE).so

LDFLAGS = -shared -fPIC -Wl,-soname,$(TARGET_EXEC).$(MAJOR).$(MINOR) $(ADDITIONAL_LIBS)
CFLAGS = -O3 -fPIC

SRC_DIRS := . 

#
# Generic Argeo
#
BUILD_DIR := $(SDK_BUILD_BASE)/jni/$(NATIVE_PACKAGE)

# Every folder in ./src will need to be passed to GCC so that it can find header files
INC_DIRS := $(shell find $(SRC_DIRS) -type d) $(JAVA_HOME)/include $(JAVA_HOME)/include/linux $(ADDITIONAL_INCLUDES)


.PHONY: clean all ide
all: $(SDK_BUILD_BASE)/jni/$(TARGET_EXEC)

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
$(SDK_BUILD_BASE)/jni/$(TARGET_EXEC): $(OBJS)
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
	rm -r $(BUILD_DIR) $(SDK_BUILD_BASE)/$(TARGET_EXEC)

# Include the .d makefiles. The - at the front suppresses the errors of missing
# Makefiles. Initially, all the .d files will be missing, and we don't want those
# errors to show up.
-include $(DEPS)

# MAKEFILE_DIR := $(dir $(firstword $(MAKEFILE_LIST)))
