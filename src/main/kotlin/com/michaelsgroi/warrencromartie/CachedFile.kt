package com.michaelsgroi.warrencromartie

import java.io.File
import java.time.Duration
import java.time.Instant

class CachedFile {
    companion object {
        fun loadFromCache(filename: String, expiration: Duration, loader: () -> String): List<String> {
            if (!filename.fileExists()) {
                println("filename=$filename does not exist, retrieving from baseball-reference.com ...")
                filename.writeFile(loader())
            } else {
                if (filename.fileExpired(expiration)) {
                    println("filename=$filename exists but is expired, retrieving from baseball-reference.com ...")
                    filename.writeFile(loader())
                }
            }
            return filename.readFile()
        }
    }
}

fun String.readFile(): List<String> {
    return File(this).useLines { it.toList() }
}

fun String.fileExists(): Boolean {
    return File(this).exists()
}

fun String.fileExpired(duration: Duration): Boolean {
    require(fileExists()) { "File $this does not exist" }
    val file = File(this)
    val ageMs = Instant.now().toEpochMilli() - file.lastModified()
    return ageMs > duration.toMillis()
}

fun String.dirExists(): Boolean {
    val file = File(this)
    val exists = file.exists()
    require(!exists || file.isDirectory) { "File $this is not a directory" }
    return exists
}

fun String.createDirectoryIfNotExists() {
    if (!this.dirExists()) {
        File(this).mkdirs()
    }
}

fun String.writeFile(contents: String) {
    File(this).writeBytes(contents.encodeToByteArray())
}