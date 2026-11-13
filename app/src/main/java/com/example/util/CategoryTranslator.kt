package com.example.util

object CategoryTranslator {

    private val exactMap = mapOf(
        "all" to "الكل",
        "favorites" to "المفضلة",
        "uncategorized" to "غير مصنف",
        "general" to "عام",
        "live tv" to "بث مباشر",
        "movies" to "أفلام",
        "series" to "مسلسلات",
        "tv series" to "مسلسلات تلفزيونية",
        "tv shows" to "برامج ومسلسلات",
        "arabic" to "عربي",
        "arabic channels" to "قنوات عربية",
        "arabic movies" to "أفلام عربية",
        "arabic series" to "مسلسلات عربية",
        "turkish series" to "مسلسلات تركية",
        "turkish movies" to "أفلام تركية",
        "asian series" to "مسلسلات آسيوية",
        "asian movies" to "أفلام آسيوية",
        "korean series" to "مسلسلات كورية",
        "indian movies" to "أفلام هندية",
        "indian series" to "مسلسلات هندية",
        "english movies" to "أفلام أجنبية",
        "english series" to "مسلسلات أجنبية",
        "foreign movies" to "أفلام أجنبية",
        "foreign series" to "مسلسلات أجنبية",
        "kids" to "أطفال",
        "kids & family" to "أطفال وعائلة",
        "kids movies" to "أفلام أطفال",
        "kids series" to "مسلسلات أطفال",
        "animation" to "رسوم متحركة",
        "anime" to "أنمي",
        "anime series" to "مسلسلات أنمي",
        "anime movies" to "أفلام أنمي",
        "sports" to "رياضة",
        "sport" to "رياضة",
        "news" to "أخبار",
        "documentary" to "وثائقي",
        "documentaries" to "وثائقيات",
        "islamic" to "إسلامية",
        "religion" to "دينية",
        "music" to "موسيقى",
        "entertainment" to "ترفيه",
        "comedy" to "كوميديا",
        "action" to "أكشن",
        "drama" to "دراما",
        "horror" to "رعب",
        "thriller" to "إثارة وتشويق",
        "romance" to "رومانسية",
        "sci-fi" to "خيال علمي",
        "fantasy" to "فانتازيا",
        "crime" to "جريمة",
        "mystery" to "غموض",
        "history" to "تاريخي",
        "war" to "حرب",
        "western" to "غربي",
        "adventure" to "مغامرات",
        "family" to "عائلي",
        "ramadan" to "رمضان",
        "ramadan series" to "مسلسلات رمضان",
        "netflix" to "نتفليكس",
        "netflix movies" to "أفلام نتفليكس",
        "netflix series" to "مسلسلات نتفليكس",
        "amazon prime" to "أمازون برايم",
        "amazon prime series" to "مسلسلات أمازون برايم",
        "amazon prime movies" to "أفلام أمازون برايم",
        "disney+" to "ديزني بلس",
        "disney plus" to "ديزني بلس",
        "shahid" to "شاهد",
        "shahid vip" to "شاهد VIP",
        "osn" to "OSN"
    )

    private val tokenReplacements = listOf(
        "\\bMulti-Sub\\b" to "متعدد الترجمة",
        "\\bArabic Audio\\b" to "مدبلج للعربية",
        "\\bArabic Sub\\b" to "مترجم للعربية",
        "\\bArabic\\b" to "عربي",
        "\\bTurkish\\b" to "تركي",
        "\\bAsian\\b" to "آسيوي",
        "\\bKorean\\b" to "كوري",
        "\\bIndian\\b" to "هندي",
        "\\bEnglish\\b" to "إنجليزي",
        "\\bFrench\\b" to "فرنسي",
        "\\bSpanish\\b" to "إسباني",
        "\\bGerman\\b" to "ألماني",
        "\\bItalian\\b" to "إيطالي",
        "\\bAmazon Prime\\b" to "أمازون برايم",
        "\\bNetflix\\b" to "نتفليكس",
        "\\bDisney\\+\\b" to "ديزني بلس",
        "\\bApple TV\\b" to "آبل تي في",
        "\\bHBO\\b" to "HBO",
        "\\bSeries\\b" to "مسلسلات",
        "\\bMovies\\b" to "أفلام",
        "\\bMovie\\b" to "فيلم",
        "\\bChannels\\b" to "قنوات",
        "\\bChannel\\b" to "قناة",
        "\\bSports\\b" to "رياضة",
        "\\bSport\\b" to "رياضة",
        "\\bNews\\b" to "أخبار",
        "\\bKids\\b" to "أطفال",
        "\\bAnime\\b" to "أنمي",
        "\\bDocumentary\\b" to "وثائقي",
        "\\bAction\\b" to "أكشن",
        "\\bComedy\\b" to "كوميديا",
        "\\bDrama\\b" to "دراما",
        "\\bHorror\\b" to "رعب",
        "\\bThriller\\b" to "إثارة",
        "\\bRomance\\b" to "رومانسية",
        "\\bFamily\\b" to "عائلي",
        "\\bAdventure\\b" to "مغامرات",
        "\\bCrime\\b" to "جريمة",
        "\\bSci-Fi\\b" to "خيال علمي",
        "\\bFantasy\\b" to "فانتازيا",
        "\\bAnimation\\b" to "رسوم متحركة",
        "\\bMusic\\b" to "موسيقى",
        "\\bIslamic\\b" to "إسلامية",
        "\\bEntertainment\\b" to "ترفيه",
        "\\bGeneral\\b" to "عام",
        "\\bLive\\b" to "مباشر"
    )

    fun translate(rawCategory: String, isAr: Boolean): String {
        if (!isAr) return rawCategory
        val trimmed = rawCategory.trim()
        if (trimmed.isEmpty()) return rawCategory

        // If already contains Arabic characters, keep provider's native Arabic title
        if (trimmed.any { it in '\u0600'..'\u06FF' }) return trimmed

        // Check exact match (case-insensitive)
        exactMap[trimmed.lowercase()]?.let { return it }

        // Apply smart phrase/keyword replacement while preserving tags/brands
        var result = trimmed
        for ((pattern, arabic) in tokenReplacements) {
            result = result.replace(Regex(pattern, RegexOption.IGNORE_CASE), arabic)
        }
        return result
    }
}
