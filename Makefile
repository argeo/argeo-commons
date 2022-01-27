MAKEDIRS = org.argeo.api.uuid

BUILD_BASE=generated

.PHONY: clean all
all:
	$(foreach dir, $(MAKEDIRS), $(MAKE) -C $(dir);)
	
clean:
	$(foreach dir, $(MAKEDIRS), $(MAKE) -C $(dir) clean;)

