JNIDIRS = org_argeo_api_uuid

.PHONY: clean all jni

all: jni

jni:
	$(foreach dir, $(JNIDIRS), $(MAKE) -C $(dir);)
	
clean:
	$(foreach dir, $(JNIDIRS), $(MAKE) -C $(dir) clean;)

#ide: $(META_INF_DIR)/$(ARCH)/$(TARGET_EXEC)
	
#$(META_INF_DIR)/$(ARCH)/$(TARGET_EXEC): $(BUILD_DIR)/$(TARGET_EXEC)
#	mkdir -p $(dir $@)
#	cp $(BUILD_DIR)/$(TARGET_EXEC) $(dir $@)


%:
	@echo Making '$@' $(if $^,from '$^')
	@echo 'OBJ=$(OBJ)'
	@echo 'DEP=$(DEP)'
