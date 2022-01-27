MAKEDIRS = org.argeo.api.uuid

.PHONY: clean all
all:
	$(foreach dir, $(MAKEDIRS), $(MAKE) -C $(dir);)
	
clean:
	$(foreach dir, $(MAKEDIRS), $(MAKE) -C $(dir) clean;)

include sdk.mk 