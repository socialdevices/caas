#include <jni/jni.h>
#include "wcrl_configurator_server_smodels_EngineInterface.h"
#include "kumbang_configurator_server_smodels_EngineInterface.h"
#include <stdio.h>

#include <iostream>
#include <fstream>

#include "stable.h"
#include "api.h"
#include "atomrule.h"

#define NMODELS 10

Stable *models[NMODELS];

jmethodID inconsistent;
jmethodID consistent;
jmethodID printMethodID;
jclass stringcls;

typedef enum { ePositive, eNegative, eUnknown } atomState;

/*
 * Class:     wcrl_configurator_server_smodels_EngineInterface
 * Method:    initModel
 * Signature: (Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_wcrl_configurator_server_smodels_EngineInterface_initModel
  (JNIEnv *env, jobject, jstring path, jobjectArray pos, jobjectArray neg)
{
	int index = NMODELS;
	Stable *stable = NULL;

	for (int j = 0; j < NMODELS; j++)
		if (models[j] == NULL)
		{
			index = j;
			models[j] = new Stable();
			stable = models[j];
			break;
		}

	//	No empty place in the models
	if (index == NMODELS)
		return -1;

	const char *strPath = env->GetStringUTFChars(path, 0);

	stable->api.remember();

	ifstream input(strPath);
	stable->read(input);

	env->ReleaseStringUTFChars(path, strPath);

	jsize size;
	jstring str;

	//	Get the positive elements in the compute statement.
	if (pos) {
		size = env->GetArrayLength(pos);
		for (jsize i = 0; i < size; i++) {
			str = (jstring) env->GetObjectArrayElement(pos, i);
			strPath = env->GetStringUTFChars(str, 0);
			Atom* a = stable->api.get_atom(strPath);

			if (!a) 
				continue;

			env->ReleaseStringUTFChars(str, strPath);
			stable->api.set_compute(a, true);
		}
	}

	if (neg) {
		size = env->GetArrayLength(neg);
		for (jsize i = 0; i < size; i++) {
			str = (jstring) env->GetObjectArrayElement(neg, i);
			strPath = env->GetStringUTFChars(str, 0);
			Atom* a = stable->api.get_atom(strPath);

			if (!a)
					continue;

			env->ReleaseStringUTFChars(str, strPath);
			stable->api.set_compute(a, false);
		}
	}

	return index;
}

/**
 * Class:     wcrl_configurator_server_smodels_EngineInterface
 * Method:    resetComputeStatement
 * Signature: (ILkumbang/configurator/server/smodels/WCRLConfigurationState;[Ljava/lang/String;[Ljava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_wcrl_configurator_server_smodels_EngineInterface_resetComputeStatement
  (JNIEnv *env, jobject, jint id, jobject ct, jobjectArray pos, jobjectArray neg) 
{
	if (id < 0 || id >= NMODELS || !models[id])
		return false;

	Stable *stable = models[id];
	Node *nd= stable->smodels.program.atoms.head();
	for(;nd;nd=nd->next){
		Atom *a1 =(nd->atom);
		if (a1->Bpos || a1->Bneg){
			stable->api.reset_compute(a1,true);
			stable->api.reset_compute(a1,false);
		}
	}

	const char *strPath;
	jsize size;
	jstring str;
	if (pos) {
		size = env->GetArrayLength(pos);
		for (jsize i = 0; i < size; i++) {
			str = (jstring) env->GetObjectArrayElement(pos, i);
			strPath = env->GetStringUTFChars(str, 0);
			Atom* a = stable->api.get_atom(strPath);

			if (!a)
				continue;

			env->ReleaseStringUTFChars(str, strPath);
			stable->api.set_compute(a, true);
		}
	}

	if (neg) {
		size = env->GetArrayLength(neg);
		for (jsize i = 0; i < size; i++) {
			str = (jstring) env->GetObjectArrayElement(neg, i);
			strPath = env->GetStringUTFChars(str, 0);
			Atom* a = stable->api.get_atom(strPath);

			if (!a)
					continue;

			env->ReleaseStringUTFChars(str, strPath);
			stable->api.set_compute(a, false);
		}
	}
	return true;
}

/*
 * Class:     wcrl_configurator_server_smodels_EngineInterface
 * Method:    releaseModel
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_wcrl_configurator_server_smodels_EngineInterface_releaseModel
  (JNIEnv *env, jobject, jint id)
{
	if (id < 0 || id >= NMODELS || !models[id])
		return false;

	delete models[id];
	models[id] = 0;

	return true;
}

/*
 * Class:     wcrl_configurator_server_smodels_EngineInterface
 * Method:    findConfiguration
 * Signature: (ILkumbang/configurator/server/smodels/WCRLConfigurationState;)Z
 */
JNIEXPORT jboolean JNICALL Java_wcrl_configurator_server_smodels_EngineInterface_findConfiguration
  (JNIEnv *env, jobject, jint id, jobject state)
{
	jclass cls = env->GetObjectClass(state);
	inconsistent = env->GetMethodID(cls, "setInconsistent", "()V");
    consistent = env->GetMethodID(cls, "setConsistentState", "(Z[Ljava/lang/String;)V");

    if (id < 0 || id >= NMODELS || !models[id])
        return false;

    Stable *stable = models[id];

    ////////////////////////////
    //      Compute the stable model
    stable->smodels.revert();

    if (stable->smodels.model() == 0) {
        //      Set the consistent state to false
        env->CallVoidMethod(state, inconsistent); // removed "false" argument from ()V signatured call!!
        return false;
    }

    jstring buf;
    stringcls = env->FindClass("java/lang/String");

    jsize modelSize = 0;
	int nodeCount = 0;


    Node *n;
    for (n = stable->smodels.program.atoms.head(); n; n = n->next) {
		nodeCount++; 
        if (n->atom->name && n->atom->Bpos)
            modelSize++;
	}

    int pos = 0;
    jobjectArray fullModel = env->NewObjectArray(modelSize, stringcls, NULL);
    for (n = stable->smodels.program.atoms.head (); n; n = n->next) {
        if (n->atom->name && n->atom->Bpos) {
            buf = env->NewStringUTF(n->atom->name);
            env->SetObjectArrayElement(fullModel, pos, buf);
            pos++;
        }
    }

	env->CallVoidMethod(state, consistent, true, fullModel);

	return true;
}


/*
 * Class:     wcrl_configurator_server_smodels_EngineInterface
 * Method:    getConfigurationState
 * Signature: (ILkumbang/configurator/server/smodels/WCRLConfigurationState;)Z
 */
JNIEXPORT jboolean JNICALL Java_wcrl_configurator_server_smodels_EngineInterface_getConfigurationState
  (JNIEnv *env, jobject, jint id, jobject state)
{
	jclass cls = env->GetObjectClass(state);

	inconsistent = env->GetMethodID(cls, "setInconsistent", "()V");
	consistent = env->GetMethodID(cls, "setConsistentState", "(Z[Ljava/lang/String;)V");

	if (id < 0 || id >= NMODELS || !models[id])
		return false;

	Stable *stable = models[id];

	if (stable->smodels.model() == 0) {
		//	Set the consistent state to false
		env->CallVoidMethod(state, inconsistent ); //removed "false" argument from a ()V signature call!!
		return false;
	}

	jstring buf, ends;
	stringcls = env->FindClass("java/lang/String");
	jsize modelSize = 0;
	Node *n;

	stable->smodels.revert();

	while( stable->smodels.model() ) {
		for (n = stable->smodels.program.atoms.head(); n; n = n->next)
			if (n->atom->name && n->atom->Bpos)
				modelSize++;
		modelSize++; // for "%end of the model%"
	}

	int pos = 0;
	jobjectArray fullModel = env->NewObjectArray(modelSize, stringcls, NULL);

	stable->smodels.revert();

	while( stable->smodels.model() ) 
	{
		for (n = stable->smodels.program.atoms.head (); n; n = n->next)
		{
			if (n->atom->name && n->atom->Bpos)
			{

				buf = env->NewStringUTF(n->atom->name);
				env->SetObjectArrayElement(fullModel, pos, buf);
				pos++;
			}
		}
		ends = env->NewStringUTF("%end of the model%");
		env->SetObjectArrayElement(fullModel, pos, ends);
		pos++;
	}

	env->CallVoidMethod(state, consistent, true, fullModel);

	return true;
}

/*
 * Class:     kumbang_configurator_server_smodels_EngineInterface
 * Method:    initModel
 * Signature: (Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/String;)Z
 */
JNIEXPORT jint JNICALL Java_kumbang_configurator_server_smodels_EngineInterface_initModel
  (JNIEnv *env, jobject, jstring path, jobjectArray pos, jobjectArray neg)
{

	int index = NMODELS;
	Stable *stable = NULL;

	for (int j = 0; j < NMODELS; j++)
		if (models[j] == NULL)
		{
			index = j;
			models[j] = new Stable();
			stable = models[j];
			break;
		}

	//	No empty place in the models
	if (index == NMODELS)
		return -1;

	const char *strPath = env->GetStringUTFChars(path, 0);

	stable->api.remember();

	ifstream input(strPath);
	stable->read(input);

	env->ReleaseStringUTFChars(path, strPath);

	jsize size;
	jstring str;

	//	Get the positive elements in the compute statement.
	if (pos) {
		size = env->GetArrayLength(pos);
		for (jsize i = 0; i < size; i++) {
			str = (jstring) env->GetObjectArrayElement(pos, i);
			strPath = env->GetStringUTFChars(str, 0);
			Atom* a = stable->api.get_atom(strPath);

			if (!a)
				continue;

			env->ReleaseStringUTFChars(str, strPath);
			stable->api.set_compute(a, true);
		}
	}

	if (neg) {
		size = env->GetArrayLength(neg);
		for (jsize i = 0; i < size; i++) {
			str = (jstring) env->GetObjectArrayElement(neg, i);
			strPath = env->GetStringUTFChars(str, 0);
			Atom* a = stable->api.get_atom(strPath);

			if (!a)
					continue;

			env->ReleaseStringUTFChars(str, strPath);
			stable->api.set_compute(a, false);
		}
	}

	return index;
}

/**
 * Class:     kumbang_configurator_server_smodels_EngineInterface
 * Method:    resetComputeStatement
 * Signature: (Ljava/lang/String;Z)Z
 */
JNIEXPORT void JNICALL Java_kumbang_configurator_server_smodels_EngineInterface_resetComputeStatement
  (JNIEnv *env, jobject, jint id, jobjectArray pos, jobjectArray neg) 
{
	if (id < 0 || id >= NMODELS || !models[id])
		return; // removed false return value --mylikang

	Stable *stable = models[id];
	Node *nd= stable->smodels.program.atoms.head ();
	for(;nd;nd=nd->next){
		Atom *a1 =(nd->atom);
		if (a1->Bpos || a1->Bneg){
			stable->api.reset_compute(a1,true);
			stable->api.reset_compute(a1,false);
		}
		//a->hasvalue = true;
	}
	const char *strPath;
	jsize size;
	jstring str;
	if (pos) {
		size = env->GetArrayLength(pos);
		for (jsize i = 0; i < size; i++) {
			str = (jstring) env->GetObjectArrayElement(pos, i);
			strPath = env->GetStringUTFChars(str, 0);
			Atom* a = stable->api.get_atom(strPath);

			if (!a)
				continue;

			env->ReleaseStringUTFChars(str, strPath);
			//stable->api.reset_compute(a,true);
			//stable->api.reset_compute(a,false);
			stable->api.set_compute(a, true);
		}
	}

	if (neg) {
		size = env->GetArrayLength(neg);
		for (jsize i = 0; i < size; i++) {
			str = (jstring) env->GetObjectArrayElement(neg, i);
			strPath = env->GetStringUTFChars(str, 0);
			Atom* a = stable->api.get_atom(strPath);

			if (!a)
					continue;

			env->ReleaseStringUTFChars(str, strPath);
			//stable->api.reset_compute(a,true);
			//stable->api.reset_compute(a,false);
			stable->api.set_compute(a, false);
		}
	}
	//removed return value: true --mylikang
}

/*
 * Class:     kumbang_configurator_server_smodels_EngineInterface
 * Method:    releaseModel
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_kumbang_configurator_server_smodels_EngineInterface_releaseModel
  (JNIEnv *env, jobject, jint id)
{
	if (id < 0 || id >= NMODELS || !models[id])
		return false;

	delete models[id];
	models[id] = 0;
	return true;
}

/*
 * Class:     kumbang_configurator_server_smodels_EngineInterface
 * Method:    getConfigurationState
 * Signature: (ILkumbang/configurator/server/smodels/ConfigurationState;)Z
 */
JNIEXPORT jboolean JNICALL Java_kumbang_configurator_server_smodels_EngineInterface_getConfigurationState
  (JNIEnv *env, jobject, jint id, jobject state)
{
	jclass cls = env->GetObjectClass(state);

	inconsistent = env->GetMethodID(cls, "setInconsistent", "()V");
	consistent = env->GetMethodID(cls, "setConsistentState", "(Z[Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/String;)V");


	if (id < 0 || id >= NMODELS || !models[id])
		return false;

	Stable *stable = models[id];

	////////////////////////////
	//	Compute the stable model

	stable->smodels.revert();
	
	if (stable->smodels.model() == 0)
	{
		//	Set the consistent state to false
		env->CallVoidMethod(state, inconsistent);
		return false;
	}

	jstring buf;

	stringcls = env->FindClass("java/lang/String");

	jsize modelSize = 0;
	Node *n;
	for (n = stable->smodels.program.atoms.head (); n; n = n->next)
		if (n->atom->name && n->atom->Bpos)
			modelSize++;

	int pos = 0;
	jobjectArray fullModel = env->NewObjectArray(modelSize, stringcls, NULL);
	for (n = stable->smodels.program.atoms.head (); n; n = n->next)
	{
		if (n->atom->name && n->atom->Bpos)
		{
			buf = env->NewStringUTF(n->atom->name);
			env->SetObjectArrayElement(fullModel, pos, buf);
			pos++;
		}
	}

	/////////////////////////////////
	// Compute the well-founded model

	stable->smodels.revert();

	stable->smodels.setup ();
	if (stable->smodels.conflict ())
		return 0;

	jsize sizePos = 0, sizeNeg = 0;
	int nrNamedAtoms = 0;
	for (n = stable->smodels.program.atoms.head (); n; n = n->next)
		if (n->atom->name)
		{
			if (n->atom->Bpos)
				sizePos++;
			if (n->atom->Bneg)
				sizeNeg++;

			nrNamedAtoms++;
		}

	// jclass cls = env->FindClass("java/lang/String");
	jobjectArray wfPos = env->NewObjectArray(sizePos, stringcls, NULL);
	jobjectArray wfNeg = env->NewObjectArray(sizeNeg, stringcls, NULL);

	atomState *compute = new atomState[nrNamedAtoms];

	jsize iPos = 0, iNeg = 0;
	int i = 0; 
	for (n = stable->smodels.program.atoms.head (); n; n = n->next)
		if (n->atom->name)
		{
			buf = env->NewStringUTF(n->atom->name);
			if (n->atom->Bpos)
			{
				compute[i] = ePositive;
				env->SetObjectArrayElement(wfPos, iPos, buf);
				iPos++;
			} else if (n->atom->Bneg)
			{
				compute[i] = eNegative;
				env->SetObjectArrayElement(wfNeg, iNeg, buf);
				iNeg++;
			} else
			{
				compute[i] = eUnknown;
				stable->api.set_compute(n->atom, false);
				//cout << "-(" << n->atom->name << ") ";
			}

		/*	if (n->atom->computeTrue)
				compute[i] = ePositive;
			else if (n->atom->computeFalse)
				compute[i] = eNegative;
			else 
				compute[i] = eUnknown;	*/

			i++;
		}

	stable->smodels.revert();

	bool complete = (stable->smodels.model() != 0);

	for (n = stable->smodels.program.atoms.head (), i = 0; n; n = n->next)
		if (n->atom->name)
		{
			if (compute[i] == eUnknown)
			{
				stable->api.reset_compute(n->atom, false);
				//cout << "res(" << n->atom->name << ") ";
			}

			i++;
		}

	delete[] compute;

	env->CallVoidMethod(state, consistent, complete, wfPos, wfNeg, fullModel);

	return true;
}

// -Djava.security.policy=java.server.policy kumbang.configurator.server.services.KumbangConfigurationServer
// kumbang.util.test.SmodelsTester .\testmodels\weather_case.kbm




