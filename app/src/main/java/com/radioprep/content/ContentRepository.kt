package com.radioprep.content

import android.content.Context

class ContentRepository(context: Context, packDir: String = "content/asoc-in") {
    private val assets = context.applicationContext.assets
    val pack: ContentPack by lazy {
        ContentPack.load { path -> assets.open("$packDir/$path").bufferedReader().use { it.readText() } }
    }
}
