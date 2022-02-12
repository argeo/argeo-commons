include ../sdk.mk

JNIDIRS = org_argeo_api_uuid_libuuid

.PHONY: clean all

all: 
	$(foreach dir, $(JNIDIRS), $(MAKE) -C $(dir);)
	
clean:
	rm -r $(BUILD_DIR) $(SDK_BUILD_BASE)/jni



