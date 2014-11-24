/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.squareup.okhttp.internal;

import javax.net.ssl.DefaultHostnameVerifier;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;

/**
 * Customizations for standalone okhttp (as opposed to bundled with the Android platform).
 */
public final class Bundling {
    private Bundling() {}

    public static HostnameVerifier getDefaultHostnameVerifier() {
        HostnameVerifier verifier = HttpsURLConnection.getDefaultHostnameVerifier();
        // Assume that the internal verifier is better than the default platform-default verifier.
        if (verifier instanceof DefaultHostnameVerifier) {
            return com.squareup.okhttp.internal.tls.OkHostnameVerifier.INSTANCE;
        } else {
            return verifier;
        }
    }
}