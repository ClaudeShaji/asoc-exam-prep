package com.asocexamprep.content

import android.content.Context
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

class ContentRepository(context: Context, private val packDir: String = "content/asoc-in") {
    private val assets = context.applicationContext.assets
    val pack: ContentPack by lazy {
        ContentPack.load { path -> assets.open("$packDir/$path").bufferedReader().use { it.readText() } }
    }

    private val images = object : LruCache<String, ImageBitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: ImageBitmap) = value.width * value.height * 4
    }

    /** A figure referenced from a note, relative to the notes folder (e.g. "img/a-ac-niar-2021-1.png"). */
    fun noteImage(path: String): ImageBitmap? = images.get(path) ?: runCatching {
        assets.open("$packDir/notes/$path").use { BitmapFactory.decodeStream(it) }?.asImageBitmap()
    }.getOrNull()?.also { images.put(path, it) }
}
