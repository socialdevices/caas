/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni/jni.h>
/* Header for class kumbang_configurator_server_smodels_EngineInterface */

#ifndef _Included_kumbang_configurator_server_smodels_EngineInterface
#define _Included_kumbang_configurator_server_smodels_EngineInterface
#ifdef __cplusplus
extern "C" {
#endif
#undef kumbang_configurator_server_smodels_EngineInterface_ID_RESET
#define kumbang_configurator_server_smodels_EngineInterface_ID_RESET -1L
/*
 * Class:     kumbang_configurator_server_smodels_EngineInterface
 * Method:    initModel
 * Signature: (Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_kumbang_configurator_server_smodels_EngineInterface_initModel
  (JNIEnv *, jobject, jstring, jobjectArray, jobjectArray);

/*
 * Class:     kumbang_configurator_server_smodels_EngineInterface
 * Method:    resetComputeStatement
 * Signature: (I[Ljava/lang/String;[Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_kumbang_configurator_server_smodels_EngineInterface_resetComputeStatement
  (JNIEnv *, jobject, jint, jobjectArray, jobjectArray);

/*
 * Class:     kumbang_configurator_server_smodels_EngineInterface
 * Method:    releaseModel
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_kumbang_configurator_server_smodels_EngineInterface_releaseModel
  (JNIEnv *, jobject, jint);

/*
 * Class:     kumbang_configurator_server_smodels_EngineInterface
 * Method:    getConfigurationState
 * Signature: (ILkumbang/core/smodels/ConfigurationState;)Z
 */
JNIEXPORT jboolean JNICALL Java_kumbang_configurator_server_smodels_EngineInterface_getConfigurationState
  (JNIEnv *, jobject, jint, jobject);

#ifdef __cplusplus
}
#endif
#endif