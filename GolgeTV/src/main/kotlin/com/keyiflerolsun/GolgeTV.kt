package com.keyiflerolsun

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.lagradost.cloudstream3.HomePageResponse
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.mainPageOf
import com.lagradost.cloudstream3.newHomePageResponse
import com.lagradost.cloudstream3.newLiveSearchResponse
import com.lagradost.cloudstream3.newLiveStreamLoadResponse
import com.lagradost.cloudstream3.utils.AppUtils
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.Qualities

//TODO: iframe & m3u
class GolgeTV : MainAPI() {
    override var name = "GolgeTV"
    override val hasMainPage = true
    override var lang = "tr"
    override val hasQuickSearch = true
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries, TvType.Live)
    override var sequentialMainPage = false
    override val mainPage = mainPageOf(
        "https://panel2.cloudgolge.shop/appMainGetData.php" to "ULUSAL",
        "https://panel2.cloudgolge.shop/appMainGetData.php" to "SPOR",
        "https://panel2.cloudgolge.shop/appMainGetData.php" to "HABER",
        "https://panel2.cloudgolge.shop/appMainGetData.php" to "BELGESEL",
        "https://panel2.cloudgolge.shop/appMainGetData.php" to "SİNEMA",
        "https://panel2.cloudgolge.shop/appMainGetData.php" to "ÇOCUK",
        "https://panel2.cloudgolge.shop/appMainGetData.php" to "MÜZİK",
        "https://panel2.cloudgolge.shop/appMainGetData.php" to "WORLD",
        // "https://panel2.cloudgolge.shop/appMainGetData.php" to "PANELLER",
        // "https://panel2.cloudgolge.shop/appMainGetData.php" to "FİLMLER",
        // "https://panel2.cloudgolge.shop/appMainGetData.php" to "DİZİLER",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val home = app.post(
            request.data,
            headers = mapOf(
                "x-requested-with" to "com.golge.golgetv2",
                "user-agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:80.0) Gecko/20100101 Firefox/79.0"
            ),
            data = mapOf(
                "ormoxRoks" to "D8C42BC6CD20C00E85659003F62B1F4A7A882DCB",
                "ormxArmegedEryxc" to "",
                "uuid" to "9717237692aa18b2",
                "asize" to "oZT%2BZxn8zjH8LOwj%2FUwiYA%3D%3D", // TODO: don't forget to url encode
                "serverurl" to "https://raw.githubusercontent.com/sevdaliyim/sevdaliyim/refs/heads/main/ssl2.key",
                "glg1Key" to "1FbcLGctAooQU7L6LQ2YaDtpNHNryPGMde7wUd47Jc53lOikXegk4LKREvfKqZYk"
            )
        )

        val contents = mutableListOf<SearchResponse>()
        AppUtils.tryParseJson<Resp>(home.text)!!.ormoxChnlx
            .filter { it.kategori == request.name }
            .forEach {
                val toDict = jacksonObjectMapper().writeValueAsString(it)
                contents.add(newLiveSearchResponse(it.isim, toDict, TvType.Live) {
                    this.posterUrl = it.resim
                })
            }
        return newHomePageResponse(request.name, contents)
    }

    override suspend fun load(url: String): LoadResponse? {
        val content = AppUtils.tryParseJson<OrmoxChnlx>(url) ?: return null
        return newLiveStreamLoadResponse(content.isim, url, url) {
            this.posterUrl = content.resim
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val content = AppUtils.tryParseJson<OrmoxChnlx>(data) ?: return false
        if (content.player == "exo") {
            var headers = mapOf(
                content.h1Key to content.h1Val,
                content.h2Key to content.h2Val,
                content.h3Key to content.h3Val,
                content.h4Key to content.h4Val,
                content.h5Key to content.h5Val
            )
            headers = headers.filterKeys { it != "0" }
            callback.invoke(
                ExtractorLink(
                    source = this.name,
                    name = content.isim,
                    url = content.link,
                    referer = headers["Referer"] ?: "",
                    quality = Qualities.Unknown.value,
                    headers = headers,
                    type = ExtractorLinkType.M3U8
                )
            )
            return true
        } else {
            return false
        }
    }
}

