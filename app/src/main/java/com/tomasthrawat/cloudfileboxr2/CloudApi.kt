package com.tomasthrawat.cloudfileboxr2

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class CloudObject(val key: String, val size: Long, val updated: String?)

class CloudApi {
    private val baseUrl = BuildConfig.API_BASE_URL.trimEnd('/')
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private fun api(path: String): Request.Builder =
        Request.Builder().url(baseUrl + path).header("X-App-Key", BuildConfig.APP_KEY)

    fun list(): List<CloudObject> {
        api("/api/list").get().build().let { request ->
            client.newCall(request).execute().use { response ->
                check(response.isSuccessful) { "List failed: HTTP " + response.code }
                val arr = JSONArray(response.body.string())
                return buildList {
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        add(CloudObject(o.getString("key"), o.getLong("size"), o.optString("updated").ifBlank { null }))
                    }
                }
            }
        }
    }

    private fun sign(operation: String, key: String, contentType: String): String {
        val k = URLEncoder.encode(key, Charsets.UTF_8.name())
        val t = URLEncoder.encode(contentType, Charsets.UTF_8.name())
        val request = api("/api/sign?op=" + operation + "&key=" + k + "&contentType=" + t).get().build()
        client.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "Signing failed: HTTP " + response.code }
            return JSONObject(response.body.string()).getString("url")
        }
    }

    fun upload(file: File, key: String, contentType: String): Long {
        val url = sign("put", key, contentType)
        val response = client.newCall(
            Request.Builder().url(url).put(file.asRequestBody(contentType.toMediaType())).build()
        ).execute()
        response.use { check(it.isSuccessful) { "Upload failed: HTTP " + it.code } }
        return file.length()
    }

    fun download(key: String, target: File): Long {
        val url = sign("get", key, "application/octet-stream")
        client.newCall(Request.Builder().url(url).get().build()).execute().use { response ->
            check(response.isSuccessful) { "Download failed: HTTP " + response.code }
            target.outputStream().use { out -> response.body.byteStream().use { input -> input.copyTo(out, 1024 * 1024) } }
        }
        return target.length()
    }

    fun delete(key: String) {
        val url = sign("delete", key, "application/octet-stream")
        client.newCall(Request.Builder().url(url).delete().build()).execute().use {
            check(it.isSuccessful) { "Delete failed: HTTP " + it.code }
        }
    }
}
