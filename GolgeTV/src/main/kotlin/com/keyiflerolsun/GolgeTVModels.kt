package com.keyiflerolsun

data class Resp(
    val icerikler: List<Any?>,
    val ormoxChnlx: List<OrmoxChnlx>,
    val menuPaylas: String,
    val menuInstagram: String,
    val menuTelegram: String,
    val onlineTime: String,
    val onlineDurum: String,
)

data class OrmoxChnlx(
    val id: String,
    val isim: String,
    val resim: String,
    val link: String,
    val kategori: String,
    val player: String,
    val tip: String,
    val userAgent: String,
    val h1Key: String,
    val h2Key: String,
    val h3Key: String,
    val h4Key: String,
    val h1Val: String,
    val h2Val: String,
    val h3Val: String,
    val h4Val: String,
    val h5Key: String,
    val h5Val: String,
)
