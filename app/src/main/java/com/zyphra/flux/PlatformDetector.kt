package com.zyphra.flux

/**
 * Supported platforms with display metadata.
 * colorHex is used to tint the platform badge.
 */
enum class Platform(
    val displayName: String,
    val emoji: String,
    val colorHex: String
) {
    YOUTUBE("YouTube",    "▶",  "#FF0000"),
    INSTAGRAM("Instagram","◈",  "#C13584"),
    FACEBOOK("Facebook",  "f",  "#1877F2"),
    TIKTOK("TikTok",      "♪",  "#010101"),
    TWITTER_X("X",        "✕",  "#14171A"),
    THREADS("Threads",    "@",  "#101010"),
    BILIBILI("哔哩哔哩",   "⊛", "#00B5C5"),
    UNKNOWN("未知平台",    "?",  "#555555")
}

object PlatformDetector {

    fun detect(url: String): Platform {
        val u = url.lowercase().trim()
        return when {
            u.contains("youtube.com") || u.contains("youtu.be")          -> Platform.YOUTUBE
            u.contains("instagram.com")                                   -> Platform.INSTAGRAM
            u.contains("facebook.com") || u.contains("fb.watch")
                    || u.contains("fb.com")                               -> Platform.FACEBOOK
            u.contains("tiktok.com") || u.contains("vm.tiktok.com")      -> Platform.TIKTOK
            u.contains("threads.net")                                     -> Platform.THREADS
            u.contains("twitter.com") || u.contains("x.com")             -> Platform.TWITTER_X
            u.contains("bilibili.com") || u.contains("b23.tv")           -> Platform.BILIBILI
            else                                                          -> Platform.UNKNOWN
        }
    }

    fun isValidUrl(url: String): Boolean =
        url.trim().run { startsWith("http://") || startsWith("https://") }
}
