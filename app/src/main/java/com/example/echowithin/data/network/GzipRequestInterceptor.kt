package com.example.echowithin.data.network

import okhttp3.Interceptor
import okhttp3.RequestBody
import okhttp3.Response
import okio.Buffer
import okio.BufferedSink
import okio.GzipSink

/**
 * Compresses request bodies with gzip (sets `Content-Encoding: gzip`) so the
 * sync pipeline sends only the changed note payloads, compressed. The backend
 * (Flask/Werkzeug) transparently decompresses gzip request bodies.
 *
 * Skips bodies that are null, already encoded, or multipart (file uploads),
 * which must remain uncompressed.
 */
class GzipRequestInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val body = original.body ?: return chain.proceed(original)
        if (original.header("Content-Encoding") != null) return chain.proceed(original)
        if (body.contentType()?.toString()?.startsWith("multipart/") == true) return chain.proceed(original)

        val compressed = object : RequestBody() {
            override fun contentType() = body.contentType()
            override fun contentLength(): Long = -1L
            override fun writeTo(sink: BufferedSink) {
                val gzipSink = GzipSink(sink)
                val buffer = Buffer()
                body.writeTo(buffer)
                gzipSink.use { it.write(buffer, buffer.size) }
            }
        }
        val request = original.newBuilder()
            .header("Content-Encoding", "gzip")
            .method(original.method, compressed)
            .build()
        return chain.proceed(request)
    }
}
