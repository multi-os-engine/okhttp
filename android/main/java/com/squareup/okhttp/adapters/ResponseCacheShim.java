package com.squareup.okhttp.adapters;

import com.squareup.okhttp.OkResponseCache;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseSource;

import java.io.IOException;
import java.net.CacheRequest;
import java.net.CacheResponse;
import java.net.ResponseCache;
import java.net.URI;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;

/**
 * A wrapper for {@link com.squareup.okhttp.HttpResponseCache} that is both {@link OkResponseCache}
 * and a {@link ResponseCache}.
 *
 * <p>This class exists to enable support for {@link android.net.http.HttpResponseCache} after
 * OkHttp stopped supporting {@link ResponseCache}. It acts as both a {@link ResponseCache},
 * for code that uses {@link java.net.ResponseCache#getDefault()} or
 * {@link ResponseCache#setDefault(ResponseCache)} (e.g. {@link com.squareup.okhttp.HttpHandler} and
 * {@link android.net.http.HttpResponseCache}), and as an {@link OkResponseCache} so that OkHttp can
 * use it natively, with all the extra stats support promised by
 * {@link android.net.http.HttpResponseCache}.
 */
public class ResponseCacheShim extends ResponseCache implements OkResponseCache {

    private final com.squareup.okhttp.HttpResponseCache delegate;

    public ResponseCacheShim(com.squareup.okhttp.HttpResponseCache delegate) {
        this.delegate = delegate;
    }

    public com.squareup.okhttp.HttpResponseCache getHttpResponseCache() {
        return delegate;
    }

    @Override
    public Response get(Request request) throws IOException {
        return delegate.get(request);
    }

    @Override
    public CacheRequest put(Response response) throws IOException {
        return delegate.put(response);
    }

    @Override
    public boolean maybeRemove(Request request) throws IOException {
        return delegate.maybeRemove(request);
    }

    @Override
    public void update(Response cached, Response network) throws IOException {
        delegate.update(cached, network);
    }

    @Override
    public void trackConditionalCacheHit() {
        delegate.trackConditionalCacheHit();
    }

    @Override
    public void trackResponse(ResponseSource source) {
        delegate.trackResponse(source);
    }

    @Override
    public CacheResponse get(URI uri, String requestMethod,
            Map<String, List<String>> requestHeaders) throws IOException {
        Request request = JavaApiConverter.createOkRequest(uri, requestMethod, requestHeaders);
        Response response = get(request);
        if (response == null) {
            return null;
        }
        return JavaApiConverter.createJavaCacheResponse(response);
    }

    @Override
    public CacheRequest put(URI uri, URLConnection urlConnection) throws IOException {
        if (!isCacheableConnection(urlConnection)) {
            return null;
        }
        return put(JavaApiConverter.createOkResponse(uri, urlConnection));
    }

    private static boolean isCacheableConnection(URLConnection httpConnection) {
        return (httpConnection instanceof com.squareup.okhttp.internal.http.HttpURLConnectionImpl)
            || (httpConnection instanceof com.squareup.okhttp.internal.http.HttpsURLConnectionImpl);
    }

}
