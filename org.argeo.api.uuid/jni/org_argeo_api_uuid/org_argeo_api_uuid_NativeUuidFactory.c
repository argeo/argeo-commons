#include <jni.h>
#include <uuid.h>
#include "org_argeo_api_uuid_NativeUuidFactory.h"

/*
 * UTILITIES
 */

static inline jobject fromBytes(JNIEnv *env, uuid_t out) {
	jlong msb = 0;
	jlong lsb = 0;

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

static inline void toBytes(JNIEnv *env, jobject jUUID, uuid_t result) {

	jclass uuidClass = (*env)->FindClass(env, "java/util/UUID");
	jmethodID getMostSignificantBits = (*env)->GetMethodID(env, uuidClass,
			"getMostSignificantBits", "()J");
	jmethodID getLeastSignificantBits = (*env)->GetMethodID(env, uuidClass,
			"getLeastSignificantBits", "()J");

	jlong msb = (*env)->CallLongMethod(env, jUUID, getMostSignificantBits);
	jlong lsb = (*env)->CallLongMethod(env, jUUID, getLeastSignificantBits);

	for (int i = 0; i < 8; i++)
		result[i] = (unsigned char) ((msb >> ((7 - i) * 8)) & 0xff);
	for (int i = 8; i < 16; i++)
		result[i] = (unsigned char) ((lsb >> ((15 - i) * 8)) & 0xff);
}

/*
 * JNI IMPLEMENTATION
 */

/*
 * Class:     org_argeo_api_uuid_NativeUuidFactory
 * Method:    timeUUID
 * Signature: ()Ljava/util/UUID;
 */
JNIEXPORT jobject JNICALL Java_org_argeo_api_uuid_NativeUuidFactory_timeUUID(
		JNIEnv *env, jobject) {
	uuid_t out;

	uuid_generate_time(out);
	return fromBytes(env, out);
}

/*
 * Class:     org_argeo_api_uuid_NativeUuidFactory
 * Method:    nameUUIDv5
 * Signature: (Ljava/util/UUID;[B)Ljava/util/UUID;
 */
JNIEXPORT jobject JNICALL Java_org_argeo_api_uuid_NativeUuidFactory_nameUUIDv5(
		JNIEnv *env, jobject, jobject namespaceUuid, jbyteArray name) {
	uuid_t ns;
	uuid_t out;

	toBytes(env, namespaceUuid, ns);
	jsize length = (*env)->GetArrayLength(env, name);
	jbyte *bytes = (*env)->GetByteArrayElements(env, name, 0);

	uuid_generate_sha1(out, ns, bytes, length);
	return fromBytes(env, out);
}

/*
 * Class:     org_argeo_api_uuid_NativeUuidFactory
 * Method:    nameUUIDv3
 * Signature: (Ljava/util/UUID;[B)Ljava/util/UUID;
 */
JNIEXPORT jobject JNICALL Java_org_argeo_api_uuid_NativeUuidFactory_nameUUIDv3(
		JNIEnv *env, jobject, jobject namespaceUuid, jbyteArray name) {
	uuid_t ns;
	uuid_t out;

	toBytes(env, namespaceUuid, ns);
	jsize length = (*env)->GetArrayLength(env, name);
	jbyte *bytes = (*env)->GetByteArrayElements(env, name, 0);

	uuid_generate_md5(out, ns, bytes, length);
	return fromBytes(env, out);
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
	return fromBytes(env, out);
}
