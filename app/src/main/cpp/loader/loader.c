#include "angdroid.h"

#include <jni.h>
#include <android/log.h>
#include <pthread.h>
#include <dlfcn.h>
#include <stdio.h>
#include <unistd.h>

#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, "Angband", __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG  , "Angband", __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO   , "Angband", __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN   , "Angband", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR  , "Angband", __VA_ARGS__) 

/* FIXME __android_log_write or __android_log_print ??? */
#define LOG(msg) (__android_log_write(ANDROID_LOG_DEBUG, "Angband", (msg)));

void( * angdroid_gameStart ) (JNIEnv*, jobject, jint, jobjectArray) = NULL;
jint( * angdroid_gameQueryRedraw ) (JNIEnv*, jobject, jint, jint, jint, jint) = NULL;
jint( * angdroid_gameQueryInt ) (JNIEnv*, jobject, jint, jobjectArray) = NULL;
jstring( * angdroid_gameQueryString ) (JNIEnv*, jobject, jint, jobjectArray) = NULL;

static JavaVM *jvm;
static void* handle = NULL;
pthread_mutex_t muQuery = PTHREAD_MUTEX_INITIALIZER;
pthread_mutex_t muGame = PTHREAD_MUTEX_INITIALIZER;

static jclass NativeWrapperClass;
static jobject NativeWrapperObj;
static jmethodID NativeWrapper_onGameExit;

void gameStart
(JNIEnv *env1, jobject obj1, jstring pluginPath, jint argc, jobjectArray argv)
{
	//LOGD("loader.startGame.syncwait");

	// begin synchronize

	int ct = 0;
	while (pthread_mutex_trylock(&muGame)!=0) {
		sleep(100);
		if(ct++>5) {
			LOGE("failed to acquire game thread lock, bailing");
			return;
		}
	}

	pthread_mutex_lock (&muQuery);

	//LOGD("loader.startGame.initializing");

	/* Init exit game callback */
	NativeWrapperObj = obj1;
	NativeWrapperClass = (*env1)->GetObjectClass(env1, NativeWrapperObj);
	NativeWrapper_onGameExit = (*env1)->GetMethodID(env1, NativeWrapperClass, "onGameExit", "()V");

	// load game plugin lib
	const char *copy_pluginPath = (*env1)->GetStringUTFChars(env1, pluginPath, 0);
	//LOGD("loader.dlopen %s", copy_pluginPath);
	handle = dlopen(copy_pluginPath,RTLD_LOCAL | RTLD_LAZY);  
	if (!handle) {
		LOGE("dlopen failed on %s", copy_pluginPath);
		return;
	}	
	(*env1)->ReleaseStringUTFChars(env1, pluginPath, copy_pluginPath);

	// find entry point
	angdroid_gameStart = dlsym(handle, "angdroid_gameStart");   
	if (!angdroid_gameStart) {
		LOGE("dlsym failed on gameStart");
		return;
	}	

	// end synchronize
	pthread_mutex_unlock (&muQuery);

	// start the game
	angdroid_gameStart(env1, obj1, argc, argv);   

	// begin synchronize
	pthread_mutex_lock (&muQuery);

	LOGD("loader.game_thread is finished");
	dlclose(handle);           	 // unload angband lib

	// clear pointers
	handle = NULL;
	angdroid_gameQueryInt = NULL;
	angdroid_gameQueryString = NULL;

	// signal game has exited
	(*env1)->CallVoidMethod(env1, NativeWrapperObj, NativeWrapper_onGameExit);

	//LOGD("loader.unlock mutexes");
	// end synchronize
	pthread_mutex_unlock (&muQuery);
	pthread_mutex_unlock (&muGame);

	//LOGD("loader.return");
}

JNIEXPORT void JNICALL Java_org_rephial_angband_NativeWrapper_gameStart
(JNIEnv *env1, jobject obj1, jstring pluginPath, jint argc, jobjectArray argv)
{
	gameStart(env1,obj1,pluginPath,argc,argv);
}

JNIEXPORT jstring JNICALL Java_org_rephial_angband_NativeWrapper_gameQueryString
  (JNIEnv *env1, jobject obj1, jint argc, jobjectArray argv)
{
	return (jstring)0; // null indicates error
}

jint gameQueryInt
(JNIEnv *env1, jobject obj1, jint argc, jobjectArray argv)
{
	jint result = -1; // -1 indicates error

	// begin synchronize
	pthread_mutex_lock (&muQuery);

	if (handle) {
		if (!angdroid_gameQueryInt)
		  	// find entry point
		  	angdroid_gameQueryInt = dlsym(handle, "angdroid_gameQueryInt");   

		if (angdroid_gameQueryInt)
			result = angdroid_gameQueryInt(env1, obj1, argc, argv);
		else
			LOGE("dlsym failed on angdroid_gameQueryInt");
	}
	else {
		LOGE("dlopen failed -- angdroid_gameQueryInt");
	}

	// end synchronize
	pthread_mutex_unlock (&muQuery);

	return result;
}

JNIEXPORT jint JNICALL Java_org_rephial_angband_NativeWrapper_gameQueryInt
(JNIEnv *env1, jobject obj1, jint argc, jobjectArray argv)
{
	return gameQueryInt(env1,obj1,argc,argv);
}
