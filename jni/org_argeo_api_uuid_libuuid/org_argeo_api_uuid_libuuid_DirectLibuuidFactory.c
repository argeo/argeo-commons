#include <jni.h>
#include <uuid.h>
#include "org_argeo_api_uuid_libuuid_DirectLibuuidFactory.h"

JNIEXPORT void JNICALL Java_org_argeo_api_uuid_libuuid_DirectLibuuidFactory_timeUUID(
		JNIEnv *env, jobject uuidFactory, jobject uuidBuf) {
	uuid_generate_time((*env)->GetDirectBufferAddress(env, uuidBuf));
}
