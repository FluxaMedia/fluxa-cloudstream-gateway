package app.fluxa.gateway

import android.content.Context
import com.googlecode.d2j.dex.Dex2jar
import com.lagradost.cloudstream3.APIHolder
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.plugins.Plugin
import kotlinx.serialization.json.Json
import java.io.File
import java.net.URLClassLoader
import java.util.zip.ZipFile

class PluginHost(private val cacheDir: File) {
    private val json = Json { ignoreUnknownKeys = true }
    private val loaded = HashMap<String, BasePlugin>()

    init {
        cacheDir.mkdirs()
    }

    val providers: List<MainAPI>
        get() = APIHolder.allProviders.toList()

    fun provider(name: String): MainAPI? = providers.firstOrNull { it.name == name }

    fun install(cs3: File): BasePlugin {
        val zip = ZipFile(cs3)
        val manifest = zip.use {
            val entry = it.getEntry("manifest.json") ?: error("manifest.json missing in ${cs3.name}")
            json.decodeFromString<BasePlugin.Manifest>(it.getInputStream(entry).bufferedReader().readText())
        }
        val className = manifest.pluginClassName ?: error("pluginClassName missing in ${cs3.name}")
        loaded.remove(className)?.beforeUnload()

        val jar = File(cacheDir, cs3.nameWithoutExtension + "-" + cs3.lastModified() + ".jar")
        if (!jar.exists()) {
            val dex = File(cacheDir, cs3.nameWithoutExtension + ".dex")
            ZipFile(cs3).use {
                val entry = it.getEntry("classes.dex") ?: error("classes.dex missing in ${cs3.name}")
                it.getInputStream(entry).use { input -> dex.outputStream().use { out -> input.copyTo(out) } }
            }
            Dex2jar.from(dex).dontSanitizeNames(true).to(jar.toPath())
            dex.delete()
        }

        val loader = URLClassLoader(arrayOf(jar.toURI().toURL()), PluginHost::class.java.classLoader)
        val instance = loader.loadClass(className).getDeclaredConstructor().newInstance() as BasePlugin
        instance.filename = cs3.absolutePath
        when (instance) {
            is Plugin -> instance.load(Context())
            else -> instance.load()
        }
        loaded[className] = instance
        return instance
    }
}
