package venturewave.one.gridgames

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform