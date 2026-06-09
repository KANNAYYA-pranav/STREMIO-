package com.example.model

data class MediaItem(
    val id: String,
    val title: String,
    val description: String,
    val type: String, // "movie" or "series"
    val imageUrl: String,
    val backdropUrl: String,
    val duration: String,
    val rating: String,
    val year: Int,
    val genre: String,
    val matchScore: Int = (90..99).random(),
    val isBillboard: Boolean = false,
    val videoUrl: String = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4" // Stable public test streaming MP4
) {
    val isMovie: Boolean get() = type == "movie"
}

object MovieCatalog {
    val items = listOf(
        MediaItem(
            id = "b1",
            title = "RED ALLIANCE",
            description = "In a dystopian cyberpunk tomorrow, an elite rogue strike force navigates high-stakes corporate espionage and street warfare in subterranean Neo-Tokyo. Driven by loyalty, betrayal, and dark synthetic augmentations.",
            type = "movie",
            imageUrl = "https://images.unsplash.com/photo-1542751371-adc38448a05e?auto=format&fit=crop&q=80&w=400",
            backdropUrl = "https://images.unsplash.com/photo-1578632767115-351597cf2477?auto=format&fit=crop&q=80&w=1200",
            duration = "2h 14m",
            rating = "TV-MA",
            year = 2026,
            genre = "Cyberpunk Action",
            isBillboard = true
        ),
        MediaItem(
            id = "m1",
            title = "The Midnight Cosmos",
            description = "A deep-space exploratory mission uncovers an anomalous artificial structure orbiting a supermassive black hole. As system power fails, the crew makes contact with an ancient cosmic entity.",
            type = "movie",
            imageUrl = "https://images.unsplash.com/photo-1462331940025-496dfbfc7564?auto=format&fit=crop&q=80&w=400",
            backdropUrl = "https://images.unsplash.com/photo-1419242902214-272b3f66ee7a?auto=format&fit=crop&q=80&w=1200",
            duration = "1h 56m",
            rating = "PG-13",
            year = 2025,
            genre = "Sci-Fi Thriller"
        ),
        MediaItem(
            id = "s1",
            title = "Shadow Samurai",
            description = "In feudal Japan, an exiled Ronin masters the lethal art of the phantom shadow style to seek vengeance on the corrupted Shogun warlords who slaughtered his entire clan.",
            type = "series",
            imageUrl = "https://images.unsplash.com/photo-1534447677768-be436bb09401?auto=format&fit=crop&q=80&w=400",
            backdropUrl = "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?auto=format&fit=crop&q=80&w=1200",
            duration = "4 Seasons",
            rating = "TV-MA",
            year = 2026,
            genre = "Action Anime"
        ),
        MediaItem(
            id = "m2",
            title = "Formula 1: Apex Speed",
            description = "Go behind the pit lane gates to experience the high-octane pressure, intense relationships, and perilous rivalries of championship racing over another explosive motorsport season.",
            type = "movie",
            imageUrl = "https://images.unsplash.com/photo-1568605117036-5fe5e7bab0b7?auto=format&fit=crop&q=80&w=400",
            backdropUrl = "https://images.unsplash.com/photo-1552519507-da3b142c6e3d?auto=format&fit=crop&q=80&w=1200",
            duration = "1h 48m",
            rating = "PG-13",
            year = 2025,
            genre = "Documentary Sport"
        ),
        MediaItem(
            id = "s2",
            title = "Chronicles of Etheria",
            description = "Two estranged royal brothers from warring magical realms must unite to seal a dark dimensional tear that threatens to consume all natural elements of their mystic planet.",
            type = "series",
            imageUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?auto=format&fit=crop&q=80&w=400",
            backdropUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?auto=format&fit=crop&q=80&w=1200",
            duration = "2 Seasons",
            rating = "TV-14",
            year = 2024,
            genre = "Fantasy Drama"
        ),
        MediaItem(
            id = "m3",
            title = "The Neon Heist",
            description = "A colorful team of international hackers plots a near-impossible biometric vault infiltration inside Macau's most luxurious casino, during the massive festive Lunar New Year countdown.",
            type = "movie",
            imageUrl = "https://images.unsplash.com/photo-1511512578047-dfb367046420?auto=format&fit=crop&q=80&w=400",
            backdropUrl = "https://images.unsplash.com/photo-1514306191717-452ec28c7814?auto=format&fit=crop&q=80&w=1200",
            duration = "2h 02m",
            rating = "R",
            year = 2025,
            genre = "Suspense Crime"
        ),
        MediaItem(
            id = "s3",
            title = "Love, Code & Cyberbots",
            description = "This anthology explore the strange intersections of human relationships, digital simulation, consciousness, and near-future technologies where robotic artificial intelligence seeks intimacy.",
            type = "series",
            imageUrl = "https://images.unsplash.com/photo-1485827404703-89b55fcc595e?auto=format&fit=crop&q=80&w=400",
            backdropUrl = "https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?auto=format&fit=crop&q=80&w=1200",
            duration = "3 Volumes",
            rating = "TV-MA",
            year = 2026,
            genre = "Sci-Fi Anthology"
        ),
        MediaItem(
            id = "m4",
            title = "Chef's Passion: Kaiseki",
            description = "Travel deep into the tranquil bamboo forests of Kyoto to understand the lifelong philosophy of Master Chef Kenji Mori as he crafts a seasonally sublime, twelve-course sensory kaiseki feast.",
            type = "movie",
            imageUrl = "https://images.unsplash.com/photo-1555396273-367ea4eb4db5?auto=format&fit=crop&q=80&w=400",
            backdropUrl = "https://images.unsplash.com/photo-1504674900247-0877df9cc836?auto=format&fit=crop&q=80&w=1200",
            duration = "1h 35m",
            rating = "G",
            year = 2024,
            genre = "Culinary Arts"
        ),
        MediaItem(
            id = "s4",
            title = "Stranger Sparks",
            description = "When a secondary electromagnetic generator explodes beneath an abandoned midwestern amusement park, local teens discover high-voltage supernatural anomalies and a shadow dimension.",
            type = "series",
            imageUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?auto=format&fit=crop&q=80&w=400",
            backdropUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?auto=format&fit=crop&q=80&w=1200",
            duration = "5 Seasons",
            rating = "TV-14",
            year = 2025,
            genre = "Sci-Fi Horror"
        ),
        MediaItem(
            id = "m5",
            title = "Deep Abyss",
            description = "A deep-sea engineering crew working on an underwater geothermal drill is trapped six miles down when seismic activity triggers an unexpected rupture in the absolute dark trench.",
            type = "movie",
            imageUrl = "https://images.unsplash.com/photo-1682687220063-4742bd7fd538?auto=format&fit=crop&q=80&w=400",
            backdropUrl = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?auto=format&fit=crop&q=80&w=1200",
            duration = "1h 51m",
            rating = "PG-13",
            year = 2025,
            genre = "Survival Thriller"
        ),
        MediaItem(
            id = "s5",
            title = "Demon Slayer: Tokyo Arc",
            description = "Tasked with investigating unexplained serial midnight disappearances inside Tokyo's prestigious imperial palace, Tanjuro must confront a high-ranking upper-rank demon of terrifying speed.",
            type = "series",
            imageUrl = "https://images.unsplash.com/photo-1578632767115-351597cf2477?auto=format&fit=crop&q=80&w=400",
            backdropUrl = "https://images.unsplash.com/photo-1534447677768-be436bb09401?auto=format&fit=crop&q=80&w=1200",
            duration = "2 Seasons",
            rating = "TV-MA",
            year = 2026,
            genre = "Action Anime"
        )
    )

    fun getByCategory(category: String): List<MediaItem> {
        return when (category.lowercase()) {
            "trending" -> items.shuffled()
            "movies" -> items.filter { it.type == "movie" }
            "series" -> items.filter { it.type == "series" }
            "cyberpunk" -> items.filter { it.genre.contains("Cyberpunk") || it.description.contains("cyberpunk", ignoreCase = true) }
            "scifi" -> items.filter { it.genre.contains("sci-fi", ignoreCase = true) || it.genre.contains("cosmos", ignoreCase = true) }
            "anime" -> items.filter { it.genre.contains("anime", ignoreCase = true) }
            else -> items
        }
    }
}
