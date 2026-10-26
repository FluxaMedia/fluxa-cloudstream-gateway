package app.fluxa.gateway

import com.lagradost.cloudstream3.AnimeLoadResponse
import com.lagradost.cloudstream3.Episode
import com.lagradost.cloudstream3.ErrorLoadingException
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.MovieLoadResponse
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.TvSeriesLoadResponse
import com.lagradost.cloudstream3.utils.ExtractorLink
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class ProviderDto(val name: String, val mainUrl: String, val lang: String, val types: List<String>)

@Serializable
data class SearchItemDto(
    val name: String,
    val url: String,
    val provider: String,
    val type: String?,
    val poster: String?,
    val posterHeaders: Map<String, String>,
)

@Serializable
data class SearchPageDto(val items: List<SearchItemDto>, val hasNext: Boolean)

@Serializable
data class EpisodeDto(
    val data: String,
    val name: String?,
    val season: Int?,
    val episode: Int?,
    val poster: String?,
    val dub: String?,
)

@Serializable
data class LoadDto(
    val name: String,
    val url: String,
    val provider: String,
    val type: String?,
    val poster: String?,
    val year: Int?,
    val plot: String?,
    val tags: List<String>,
    val movieData: String?,
    val episodes: List<EpisodeDto>,
)

@Serializable
data class LinksRequest(val provider: String, val data: String, val isCasting: Boolean = false)

@Serializable
data class LinkDto(
    val source: String,
    val name: String,
    val url: String,
    val referer: String,
    val quality: Int,
    val type: String,
    val headers: Map<String, String>,
)

@Serializable
data class SubtitleDto(val lang: String, val url: String)

@Serializable
data class LinksDto(val links: List<LinkDto>, val subtitles: List<SubtitleDto>)

@Serializable
data class InstallRequest(val path: String)

fun Episode.toDto(dub: String?) = EpisodeDto(data, name, season, episode, posterUrl, dub)

fun SearchResponse.toDto() = SearchItemDto(name, url, apiName, type?.name, posterUrl, posterHeaders ?: emptyMap())

fun LoadResponse.toDto() = LoadDto(
    name = name,
    url = url,
    provider = apiName,
    type = type?.name,
    poster = posterUrl,
    year = year,
    plot = plot,
    tags = tags ?: emptyList(),
    movieData = (this as? MovieLoadResponse)?.dataUrl,
    episodes = when (this) {
        is TvSeriesLoadResponse -> episodes.map { it.toDto(null) }
        is AnimeLoadResponse -> episodes.flatMap { (dub, list) -> list.map { it.toDto(dub.name) } }
        else -> emptyList()
    },
)

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8787
    val dataDir = File(System.getenv("GATEWAY_DATA") ?: "data")
    val host = PluginHost(File(dataDir, "cache"))
    File(dataDir, "plugins").listFiles { f -> f.extension == "cs3" }?.forEach {
        runCatching { host.install(it) }.onFailure { e -> System.err.println("plugin ${it.name} failed: $e") }
    }

    embeddedServer(Netty, port = port) {
        install(ContentNegotiation) { json() }
        routing {
            get("/health") { call.respondText("ok") }

            get("/providers") {
                call.respond(host.providers.map {
                    ProviderDto(it.name, it.mainUrl, it.lang, it.supportedTypes.map { t -> t.name })
                })
            }

            post("/plugins") {
                val file = File(call.receive<InstallRequest>().path)
                runCatching { host.install(file) }
                    .onSuccess { call.respond(HttpStatusCode.Created) }
                    .onFailure { call.respond(HttpStatusCode.UnprocessableEntity, it.toString()) }
            }

            get("/search") {
                val api = host.provider(call.request.queryParameters["provider"].orEmpty())
                val query = call.request.queryParameters["q"].orEmpty()
                if (api == null) return@get call.respond(HttpStatusCode.NotFound)
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val res = api.search(query, page)
                call.respond(SearchPageDto(res?.items.orEmpty().map { it.toDto() }, res?.hasNext ?: false))
            }

            get("/load") {
                val api = host.provider(call.request.queryParameters["provider"].orEmpty())
                val url = call.request.queryParameters["url"].orEmpty()
                if (api == null) return@get call.respond(HttpStatusCode.NotFound)
                val res = api.load(url) ?: throw ErrorLoadingException("empty response")
                call.respond(res.toDto())
            }

            post("/links") {
                val req = call.receive<LinksRequest>()
                val api = host.provider(req.provider) ?: return@post call.respond(HttpStatusCode.NotFound)
                val links = mutableListOf<LinkDto>()
                val subs = mutableListOf<SubtitleDto>()
                api.loadLinks(
                    req.data,
                    req.isCasting,
                    { s: SubtitleFile -> subs += SubtitleDto(s.lang, s.url) },
                    { l: ExtractorLink ->
                        links += LinkDto(l.source, l.name, l.url, l.referer, l.quality, l.type.name, l.headers)
                    },
                )
                call.respond(LinksDto(links, subs))
            }
        }
    }.start(wait = true)
}
