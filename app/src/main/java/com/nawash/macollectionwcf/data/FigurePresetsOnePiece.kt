package com.nawash.macollectionwcf.data

/**
 * Catalogue vérifié des figurines WCF One Piece (Banpresto). Source : page "One Piece World
 * Collectable Figure" du One Piece Wiki (onepiece.fandom.com/wiki/One_Piece_World_Collectable_Figure,
 * consultée via l'API MediaWiki le 2026-07-18), qui référence chaque volume officiel avec sa
 * date de sortie et la liste exacte des personnages inclus. Aucune fiche devinée/inventée.
 *
 * Cette première passe couvre la série numérotée principale "One Piece" (Vol.0 à Vol.35,
 * avril 2010 à juin 2015) — la ligne la plus emblématique de la gamme. Les nombreuses
 * sous-séries/éditions limitées (Halloween Special, Treasure Rally, History of..., Dressrosa,
 * Marineford, Zodiac, etc.) seront ajoutées dans des passes ultérieures.
 */
private data class WcfVolume(val series: String, val year: Int, val characters: List<String>, val heightCm: Double? = 7.0)

private val onePieceMainSeries = listOf(
    WcfVolume("One Piece Vol.1", 2010, listOf("Monkey D. Luffy", "Monkey D. Garp", "Shanks", "Whitebeard", "Portgas D. Ace", "Bentham", "Buggy", "Monkey D. Dragon")),
    WcfVolume("One Piece Vol.2", 2010, listOf("Monkey D. Luffy", "Tony Tony Chopper", "Nico Robin", "Franky", "Brook", "Cindry", "Hogback", "Horo Horo Ghosts")),
    WcfVolume("One Piece Vol.3", 2010, listOf("Nightmare Luffy", "Roronoa Zoro", "Sanji", "Nami", "Usopp", "Perona", "Kumashi", "Oars")),
    WcfVolume("One Piece Vol.4", 2010, listOf("Crocodile", "Jinbe", "Bartholomew Kuma", "Gecko Moria", "Donquixote Doflamingo", "Dracule Mihawk", "Boa Hancock", "Marshall D. Teach (Blackbeard)")),
    WcfVolume("One Piece Vol.5", 2010, listOf("Monkey D. Luffy", "Roronoa Zoro", "Eustass Kid", "Trafalgar Law", "Jewelry Bonney", "X Drake", "Urouge", "Bepo")),
    WcfVolume("One Piece Vol.6", 2010, listOf("Monkey D. Luffy", "Buggy", "Alvida", "Lord of the Coast", "Shanks", "Lucky Roux", "Benn Beckman", "Yasopp")),
    WcfVolume("One Piece Vol.7", 2010, listOf("Monkey D. Luffy", "Koby", "Helmeppo", "Roronoa Zoro", "Kuina", "Gaimon", "Tashigi", "Smoker")),
    WcfVolume("One Piece Vol.8", 2010, listOf("Monkey D. Luffy", "Duval", "Hatchan", "Camie", "Silvers Rayleigh", "Basil Hawkins", "Capone Bege", "Scratchmen Apoo")),
    WcfVolume("One Piece Vol.9", 2010, listOf("Monkey D. Luffy", "Usopp", "Kaya", "Going Merry", "Jango", "Kuro", "Brogy", "Dorry")),
    WcfVolume("One Piece Vol.10", 2011, listOf("Monkey D. Luffy", "Sanji", "Zeff", "Patty", "Sanji (Baratie)", "Don Krieg", "Gin", "Alvida")),
    WcfVolume("One Piece Vol.11", 2011, listOf("Monkey D. Luffy", "Mr. 2 (as Nami)", "Buggy", "Mr. 3", "Mr. 2 Bon Kurei", "Emporio Ivankov", "Magellan", "Hannyabal")),
    WcfVolume("One Piece Vol.12", 2011, listOf("Monkey D. Luffy", "Nami", "Nojiko", "Bell-mère", "Genzo", "Momoo", "Chew", "Arlong")),
    WcfVolume("One Piece Vol.0", 2011, listOf("Gol D. Roger", "Silvers Rayleigh", "Shanks", "Monkey D. Garp (w/ Ace)", "Sengoku", "Whitebeard", "Shiki", "Boa Hancock")),
    WcfVolume("One Piece Vol.13", 2011, listOf("Monkey D. Luffy", "Tony Tony Chopper", "Kureha", "Hiriluk", "Dalton", "Wapol", "Robson", "Hiking Bear")),
    WcfVolume("One Piece Vol.14", 2011, listOf("Portgas D. Ace", "Sengoku", "Akainu", "Aokiji", "Kizaru", "Little Oars Jr.", "Sentomaru", "Koby")),
    WcfVolume("One Piece Vol.15", 2011, listOf("Monkey D. Luffy", "Nefertari Vivi", "Karoo", "Nefertari Cobra", "Igaram", "Pell", "Chaka", "Nico Robin")),
    WcfVolume("One Piece Vol.16", 2011, listOf("Crocodile (Mr. 0)", "Nico Robin (Ms. All Sunday)", "Daz Bonez (Mr. 1)", "Zala (Ms. Double Finger)", "Bentham (Mr. 2)", "Lassoo", "Babe (Mr. 4)", "Drophy (Miss Merry Christmas)")),
    WcfVolume("One Piece Vol.17", 2011, listOf("Whitebeard", "Marco", "Jozu", "Vista", "Shanks (Red-Haired)", "Benn Beckman", "Yasopp", "Lucky Roux")),
    WcfVolume("One Piece Vol.18", 2011, listOf("Masira", "Mont Blanc Cricket", "Shoujou", "Flying Merry", "South Bird", "Monkey D. Luffy", "Bellamy", "Sarquiss")),
    WcfVolume("One Piece Vol.19", 2011, listOf("Monkey D. Luffy", "Gan Fall", "Pierre", "Enel", "Wyper", "Conis & Su", "Mont Blanc Noland", "Kalgara")),
    WcfVolume("One Piece Vol.20", 2011, listOf("Monkey D. Luffy", "Portgas D. Ace", "Sabo", "Makino", "Woop Slap", "Curly Dadan", "Bluejam", "Outlook III")),
    WcfVolume("One Piece Vol.21", 2011, listOf("Monkey D. Luffy", "Usopp", "Tony Tony Chopper", "Tonjit", "Foxy", "Porche", "Itomimizu/Chuchun", "Sexy Foxy")),
    WcfVolume("One Piece Vol.22", 2012, listOf("Monkey D. Luffy", "Boa Hancock", "Salome", "Boa Marigold", "Boa Sandersonia", "Nyon", "Marguerite", "Perfume Yuda")),
    WcfVolume("One Piece Vol.23", 2012, listOf("Monkey D. Luffy", "Franky", "Nico Robin", "Tony Tony Chopper", "Demalo Black", "Turco", "Cocoa", "Nora Gitsune")),
    WcfVolume("One Piece Vol.24", 2012, listOf("Blueno", "Fukurou", "Spandam", "Kaku", "Rob Lucci", "Jabra", "Kalifa", "Kumadori")),
    WcfVolume("One Piece Vol.25", 2012, listOf("Usopp", "Roronoa Zoro", "Sanji", "Nami", "Mounblutain", "Manjaro", "Drip", "Chocolat")),
    WcfVolume("One Piece Vol.26", 2012, listOf("Franky", "Mozu", "Kiwi", "Zambai", "Iceburg", "Paulie", "Peepley Lulu", "Tilestone")),
    WcfVolume("One Piece Vol.27", 2012, listOf("Roronoa Zoro", "Usopp", "Nami", "Nico Robin", "Franky", "Nefertari Vivi", "Shirahoshi", "Koala")),
    WcfVolume("One Piece Vol.28", 2012, listOf("Brook", "Perona", "Usopp", "Heracles", "Haredas", "Sanji", "Boa Hancock", "Tibany")),
    WcfVolume("One Piece Vol.29", 2012, listOf("Camie", "Flying Dutchman", "Shyarly", "Tony Tony Chopper", "Caribou", "Coribou", "Hammond", "Hyouzou")),
    WcfVolume("One Piece Vol.30", 2013, listOf("Rob Lucci (Leopard)", "Kaku (Giraffe)", "Jabra (Wolf)", "Spandine", "Nico Olvia", "Jaguar D. Saul", "Tom", "Oro Jackson")),
    WcfVolume("One Piece Vol.31", 2013, listOf("Shirahoshi", "Fukaboshi", "Ryuboshi", "Manboshi", "Neptune", "Minister of the Left", "Minister of the Right", "Gyro")),
    WcfVolume("One Piece Vol.32", 2013, listOf("Portgas D. Ace", "Jozu", "Thatch", "Haruta", "Namur", "Speed Jiru", "Fossa", "Curiel")),
    WcfVolume("One Piece Vol.33", 2013, listOf("Marco", "Vista", "Izou", "Kingdew", "Rakuyo", "Blamenco", "Blenheim", "Atmos")),
    WcfVolume("One Piece Vol.34", 2014, listOf("Monkey D. Luffy", "Roronoa Zoro", "Fisher Tiger", "Snapper Head", "Jinbe", "Arlong", "Otohime", "Hody Jones")),
    WcfVolume("One Piece Vol.35", 2015, listOf("Monkey D. Luffy", "Trafalgar Law", "Kin'emon", "Kouzuki Momonosuke", "Smoker", "Tashigi", "Vergo", "Caesar Clown"))
)

val figurePresetsOnePiece: List<FigurePreset> = onePieceMainSeries.flatMap { vol ->
    vol.characters.map { char ->
        FigurePreset(licence = Licence.ONE_PIECE, character = char, name = char, series = vol.series, year = vol.year, heightCm = vol.heightCm)
    }
}
