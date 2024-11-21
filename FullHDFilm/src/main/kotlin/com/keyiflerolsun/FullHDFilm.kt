// ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.

package com.keyiflerolsun

import android.util.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

class FullHDFilm : MainAPI() {
    override var mainUrl              = "https://fullhdfilm.pro"
    override var name                 = "FullHDFilm"
    override val hasMainPage          = true
    override var lang                 = "tr"
    override val hasQuickSearch       = false
    override val hasChromecastSupport = true
    override val hasDownloadSupport   = true
    override val supportedTypes       = setOf(TvType.Movie)

    override val mainPage = mainPageOf(
        "${mainUrl}/aile-filmleri/page"				    to "Aile",
        "${mainUrl}/aksiyon-filmleri-izle-hd1/page"	    to "Aksiyon",
        "${mainUrl}/animasyon-filmleri-izlesene/page"	to "Animasyon",
        "${mainUrl}/anime-izle/page"					to "Anime",
        "${mainUrl}/belgesel/page"					    to "Belgesel",
        "${mainUrl}/bilim-kurgu-filmleri/page"		    to "Bilim-Kurgu",
        "${mainUrl}/biyografi-filmleri/page"			to "Biyografi",
        "${mainUrl}/dram-filmleri/page"				    to "Dram",
        "${mainUrl}/fantastik-filmler-izle/page"		to "Fantastik",
        "${mainUrl}/gerilim-filmleri-izle-hd"		    to "Gerilim",
        "${mainUrl}/gizem-filmleri/page"				to "Gizem",
        "${mainUrl}/hint-filmleri/page"				    to "Hint",
        "${mainUrl}/komedi-filmleri/page"				to "Komedi",
        "${mainUrl}/korku-filmleri-izle/page"			to "Korku",
        "${mainUrl}/macera-filmleri-izle-hd/page"		to "Macera",
        "${mainUrl}/muzik-filmleri/page"				to "Müzik",
        "${mainUrl}/netflix-filmleri-izle/page"		    to "Netflix",
        "${mainUrl}/romantik-filmler/page"			    to "Romantik",
        "${mainUrl}/savas-filmleri-izle-hd/page"		to "Savaş",
        "${mainUrl}/spor-filmleri/page"				    to "Spor",
        "${mainUrl}/suc-filmleri-izle/page"			    to "Suç",
        "${mainUrl}/tarih-filmleri/page"				to "Tarih",
        "${mainUrl}/vahsi-bati-filmleri/page"			to "Vahşi Batı",
        "${mainUrl}/yabanci-dizi-izle/page"			    to "Yabancı Dizi",
        "${mainUrl}/yabanci-film-izle/page"			    to "Yabancı Filmler",
        "${mainUrl}/yerli-film-izle/page"				to "Yerli Film",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}/${page}/").document
        val home     = document.select("div.movie_box").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title     = this.selectFirst("h2")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-src"))

        return newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("${mainUrl}/arama/?s=${query}").document

        return document.select("div.movie_box").mapNotNull { it.toMainPageResult() }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title           = document.selectFirst("h1 span")?.text()?.trim() ?: return null
        val poster          = fixUrlNull(document.selectFirst("[property='og:image']")?.attr("content"))
        val description     = document.selectFirst("div[itemprop='description']")?.text()?.trim()
        val year            = document.selectFirst("span[itemprop='dateCreated'] a")?.text()?.trim()?.toIntOrNull()
        val tags            = document.select("div.detail ul.bottom li:nth-child(5) span a").map { it.text() }
        val rating          = document.selectFirst("ul.right li:nth-child(2) span")?.text()?.trim()?.toRatingInt()
        val duration        = document.selectFirst("span[itemprop='duration']")?.text()?.split(" ")?.first()?.trim()?.toIntOrNull()
        val actors          = document.select("sc[itemprop='actor'] span").map { Actor(it.text()) }
        val trailer         = fixUrlNull(document.selectFirst("[property='og:video']")?.attr("content"))

        return newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.posterUrl       = poster
            this.plot            = description
            this.year            = year
            this.tags            = tags
            this.rating          = rating
            this.duration        = duration
            addActors(actors)
            addTrailer(trailer)
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        Log.d("FHDF", "data » ${data}")
        val document = app.get(data).document

        val iframeSkici = IframeKodlayici()

        val partNumbers  = document.select("li.psec").map { it.attr("id") }
        val partNames    = document.select("li.psec a").map { it.text()?.trim() }
        val pdataMatches = Regex("""pdata\[\'(.*?)'\] = \'(.*?)\';""").findAll(document.html())
        val pdataList    = pdataMatches.map { it.destructured }.toList()

        partNumbers.forEachIndexed { index, partNumber ->
            val partName = partNames.getOrNull(index)
            val pdata    = pdataList.getOrNull(index)
            
            val key   = pdata?.component1()
            val value = pdata?.component2()

            if (partName!!.lowercase().contains("fragman") || partNumber!!.lowercase().contains("fragman")) return@forEachIndexed

            // Log.d("FHDF", "Part Number: $partNumber") // ! fragman0
            Log.d("FHDF", "Part Name: $partName")     // ! Fragman
            // Log.d("FHDF", "Key: $key")                // ! prt_fragman0
            // Log.d("FHDF", "Value: $value")            // ! Şifreli veri

            val iframeData = iframeSkici.iframeCoz(value!!)
            val iframeLink = app.get(iframeData, referer="${mainUrl}/").url.toString()
            Log.d("FHDF", "iframeLink » ${iframeLink}")

            //  loadExtractor(iframeLink, "${mainUrl}/", subtitleCallback, callback)

            loadExtractor(iframeLink, subtitleCallback) { extractor ->
                callback.invoke (
                    ExtractorLink (
                        source  = "${extractor.name} - ${partName}",
                        name    = "${extractor.name} - ${partName}",
                        url     = extractor.url,
                        referer = "${mainUrl}/",
                        quality = Qualities.Unknown.value,
                        type    = INFER_TYPE
                    )
                )
            }
        }

        return true
    }
}