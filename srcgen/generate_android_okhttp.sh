CLASSPATH=${ANDROID_BUILD_TOP}/out/host/common/obj/JAVA_LIBRARIES/currysrc_intermediates/javalib.jar:${ANDROID_BUILD_TOP}/out/host/common/obj/JAVA_LIBRARIES/android_okhttp_srcgen_intermediates/javalib.jar

(cd ${ANDROID_BUILD_TOP}; make android_okhttp_srcgen && java -cp $CLASSPATH com.android.okhttp.srcgen.OkHttpTransform && java -cp $CLASSPATH com.android.okhttp.srcgen.OkioTransform )
