JNIDIRS = org_argeo_api_uuid_libuuid

.PHONY: clean all

all: 
	$(foreach dir, $(JNIDIRS), $(MAKE) -C $(dir);)
	
clean:
	$(foreach dir, $(JNIDIRS), $(MAKE) -C $(dir) clean;)

