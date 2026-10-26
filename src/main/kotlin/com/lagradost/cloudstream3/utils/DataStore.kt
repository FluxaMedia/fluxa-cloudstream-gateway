package com.lagradost.cloudstream3.utils

import android.content.Context
import android.content.SharedPreferences

object DataStore {
    fun getSharedPrefs(context: Context): SharedPreferences = context.getSharedPreferences("rebuild_preference", 0)

    fun getDefaultSharedPrefs(context: Context): SharedPreferences = context.getSharedPreferences("default_preference", 0)

    fun getFolderName(folder: String, path: String): String = "${folder}/${path}"
}
