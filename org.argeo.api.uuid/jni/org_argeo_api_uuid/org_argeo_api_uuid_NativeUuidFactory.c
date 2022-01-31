#include <jni.h>
#include <uuid.h>
#include "org_argeo_api_uuid_NativeUuidFactory.h"

/*
 * Class:     org_argeo_api_uuid_NativeUuidFactory
 * Method:    timeUUID
 * Signature: ()Ljava/util/UUID;
 */
JNIEXPORT jobject JNICALL Java_org_argeo_api_uuid_NativeUuidFactory_timeUUID(
		JNIEnv *env, jobject) {
	uuid_t out;
	jlong msb = 0;
	jlong lsb = 0;

	uuid_generate_time(out);

	for (int i = 0; i < 8; i++)
		msb = (msb << 8) | (out[i] & 0xff);
	for (int i = 8; i < 16; i++)
		lsb = (lsb << 8) | (out[i] & 0xff);

	jclass uuidClass = (*env)->FindClass(env, "java/util/UUID");
	jmethodID uuidConstructor = (*env)->GetMethodID(env, uuidClass, "<init>",
			"(JJ)V");

	jobject jUUID = (*env)->AllocObject(env, uuidClass);
	(*env)->CallVoidMethod(env, jUUID, uuidConstructor, msb, lsb);

	return jUUID;
}

/*
 * Class:     org_argeo_api_uuid_NativeUuidFactory
 * Method:    nameUUIDv5
 * Signature: (Ljava/util/UUID;[B)Ljava/util/UUID;
 */
JNIEXPORT jobject JNICALL Java_org_argeo_api_uuid_NativeUuidFactory_nameUUIDv5(
		JNIEnv *env, jobject, jobject, jbyteArray name) {
	size_t length = (*env)->GetArrayLength(env, name);
	jbyte *bytes = (*env)->GetByteArrayElements(env, name, 0);
	return NULL;
}

/*
 * Class:     org_argeo_api_uuid_NativeUuidFactory
 * Method:    nameUUIDv3
 * Signature: (Ljava/util/UUID;[B)Ljava/util/UUID;
 */
JNIEXPORT jobject JNICALL Java_org_argeo_api_uuid_NativeUuidFactory_nameUUIDv3(
		JNIEnv *env, jobject, jobject, jbyteArray) {
	return NULL;
}

/*
 * Class:     org_argeo_api_uuid_NativeUuidFactory
 * Method:    randomUUIDStrong
 * Signature: ()Ljava/util/UUID;
 */
JNIEXPORT jobject JNICALL Java_org_argeo_api_uuid_NativeUuidFactory_randomUUIDStrong(
		JNIEnv *env, jobject) {
	uuid_t out;

	uuid_generate_random(out);
	return NULL;
}

//	void fromBytes(JNIEnv *env, jobject jUUID, jmethodID uuidConstructor, uuid_t out){
//		jlong msb = 0;
//		jlong lsb = 0;
//
//		for (int i = 0; i < 8; i++)
//			msb = (msb << 8) | (out[i] & 0xff);
//		for (int i = 8; i < 16; i++)
//			lsb = (lsb << 8) | (out[i] & 0xff);
//
//		(*env)->CallVoidMethod(env, jUUID, uuidConstructor, msb, lsb);
//
//	}

//	void getMostSignificantBits(uuid_t out) {
//		jlong msb = 0;
//		for (int i = 0; i < 8; i++)
//			msb = (msb << 8) | (out[i] & 0xff);
//		return msb;
//	}
//
//	jlong getLeastSignificantBits(uuid_t out) {
//		jlong lsb = 0;
//		for (int i = 8; i < 16; i++)
//			lsb = (lsb << 8) | (out[i] & 0xff);
//		return lsb;
//	}
//
//	jobject fromBits(JNIEnv *env, jlong msb, jlong lsb) {
//		jclass uuidClass = (*env)->FindClass(env, "java/util/UUID");
//		jmethodID uuidConstructor = (*env)->GetMethodID(env, uuidClass,
//				"<init>", "(JJ)V");
//
//		jobject jUUID = (*env)->AllocObject(env, uuidClass);
//		(*env)->CallVoidMethod(env, jUUID, uuidConstructor, msb, lsb);
//		return jUUID;
//	}

//	uuid_t toBytes(JNIEnv *env, jobject jUUID) {
//		uuid_t result;
//
//		jclass uuidClass = (*env)->FindClass(env, "java/util/UUID");
//		jmethodID getMostSignificantBits = (*env)->GetMethodID(env, uuidClass,
//				"getMostSignificantBits", "J");
//		jmethodID getLeastSignificantBits = (*env)->GetMethodID(env, uuidClass,
//				"getLeastSignificantBits", "J");
//
//		jlong msb = (*env)->CallLongMethod(env, jUUID, getMostSignificantBits);
//		jlong lsb = (*env)->CallLongMethod(env, jUUID, getMostSignificantBits);
//
//		for (int i = 0; i < 8; i++)
//			result[i] = (unsigned char) ((msb >> ((7 - i) * 8)) & 0xff);
//		for (int i = 8; i < 16; i++)
//			result[i] = (unsigned char) ((lsb >> ((15 - i) * 8)) & 0xff);
//		return result;
//	}
