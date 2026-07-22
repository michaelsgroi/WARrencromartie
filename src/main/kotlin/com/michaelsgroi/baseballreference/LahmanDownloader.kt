package com.michaelsgroi.baseballreference

import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.time.Duration

object LahmanDownloader {
    private const val SHARED_NAME = "y1prhc795jk8zvmelfd3jq7tl389y6cd"
    private const val FOLDER_URL = "https://sabr.app.box.com/s/$SHARED_NAME"
    private const val CACHE_DIR = "data/lahman"
    private val EXPIRATION = Duration.ofDays(30)

    private val FILES = setOf(
        "Batting.csv",
        "Pitching.csv",
        "Fielding.csv",
        "HallOfFame.csv",
        "AwardsPlayers.csv",
        "AllstarFull.csv",
    )

    fun download() {
        File(CACHE_DIR).mkdirs()
        val client = OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(30))
            .readTimeout(Duration.ofMinutes(5))
            .followRedirects(true)
            .build()

        val needed = FILES.filter { filename ->
            val dest = "$CACHE_DIR/$filename"
            !File(dest).exists() || dest.fileExpired(EXPIRATION)
        }
        if (needed.isEmpty()) {
            println("all Lahman files are up to date")
            return
        }

        println("fetching Lahman file listing from Box...")
        val (cookies, requestToken, fileIds) = fetchFolderIndex(client)

        for (filename in needed) {
            val fileId = fileIds[filename]
                ?: throw IOException("$filename not found in Box folder listing")
            val dest = "$CACHE_DIR/$filename"
            println("downloading $filename ...")
            downloadFile(client, cookies, requestToken, fileId, dest)
            println("wrote $dest")
        }
    }

    private data class FolderIndex(val cookies: String, val requestToken: String, val fileIds: Map<String, String>)

    // Returns cookies, requestToken, and map of filename -> fileId
    private fun fetchFolderIndex(client: OkHttpClient): FolderIndex {
        val fileIds = mutableMapOf<String, String>()
        var cookies = ""
        var requestToken = ""

        for (page in 1..2) {
            val reqBuilder = Request.Builder().url("$FOLDER_URL?page=$page").get()
            if (cookies.isNotEmpty()) reqBuilder.header("Cookie", cookies)
            val resp = client.newCall(reqBuilder.build()).execute()
            resp.use {
                if (!it.isSuccessful) throw IOException("Failed to fetch Box folder page $page: ${it.code}")
                if (page == 1) {
                    cookies = it.headers("Set-Cookie")
                        .filter { c -> c.startsWith("z=") || c.startsWith("box_visitor_id=") || c.startsWith("bv=") || c.startsWith("cn=") || c.startsWith("site_preference=") }
                        .map { c -> c.substringBefore(";") }
                        .joinToString("; ")
                        .ifEmpty { throw IOException("No session cookies returned from Box") }
                }
                val html = it.body.string()
                if (page == 1) {
                    requestToken = Regex(""""requestToken"\s*:\s*"([a-f0-9]+)"""").find(html)?.groupValues?.get(1)
                        ?: throw IOException("requestToken not found in Box folder page")
                }
                val entryRegex = Regex(""""typedID":"f_(\d+)","type":"file","id":\d+[^}]*?"name":"([^"]+)"""")
                for (match in entryRegex.findAll(html)) {
                    fileIds[match.groupValues[2]] = match.groupValues[1]
                }
            }
        }

        return FolderIndex(cookies, requestToken, fileIds)
    }

    private fun downloadFile(client: OkHttpClient, cookies: String, requestToken: String, fileId: String, dest: String) {
        val url = "https://sabr.app.box.com/index.php" +
            "?rm=box_download_shared_file" +
            "&shared_name=$SHARED_NAME" +
            "&file_id=f_$fileId"
        val body = FormBody.Builder().add("request_token", requestToken).build()
        val resp = client.newCall(
            Request.Builder()
                .url(url)
                .post(body)
                .header("Cookie", cookies)
                .header("Referer", FOLDER_URL)
                .build()
        ).execute()
        resp.use {
            if (!it.isSuccessful) throw IOException("Failed to download $fileId: HTTP ${it.code}")
            val contentType = it.header("Content-Type") ?: ""
            val bytes = it.body.bytes()
            if (contentType.contains("text/html", ignoreCase = true) ||
                bytes.take(15).toByteArray().toString(Charsets.UTF_8).contains("<!DOCTYPE", ignoreCase = true)
            ) {
                throw IOException("Box returned HTML instead of CSV for $fileId")
            }
            File(dest).writeBytes(bytes)
        }
    }
}
