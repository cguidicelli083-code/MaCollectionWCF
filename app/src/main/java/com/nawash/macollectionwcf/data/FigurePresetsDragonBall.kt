package com.nawash.macollectionwcf.data

/**
 * Catalogue vérifié des figurines WCF Dragon Ball (Banpresto). Source principale :
 * dragonballfigures.com/thread/7/banpresto-world-collectable-figure-visual (guide visuel
 * communautaire détaillé, consulté le 2026-07-19) — la page Dragon Ball Wiki dédiée
 * (dragonball.fandom.com/wiki/Dragon_Ball_World_Collectable_Figure) ne donne que des totaux
 * globaux en prose, sans liste de personnages par volume, donc insuffisante pour coder des
 * fiches en dur (jamais de personnage deviné). Aucune fiche fabriquée : la série "Dragon Ball Z
 * Kami to Kami" a été volontairement omise car la source elle-même indique "no full character
 * listings" pour ces volumes.
 *
 * 2026-07-19 — chasse aux dates de sortie manquantes : deux dates supplémentaires confirmées via
 * abitof.jennystroom.nl/2020/04/19/dragon-ball-wcf-banpresto (blog spécialisé avec chronologie
 * détaillée) — DBZ Series 02 (2009, "?" absent contrairement aux séries 4/6 du même blog, donc
 * gardé) et Prince Vegeta (août 2016). Les autres séries "de base" (DB 01-05, DBZ 01/03/04/05/06,
 * Kai 01-07) restent sans date confirmée : aucune source trouvée ne documente précisément leur
 * sortie individuelle (gamme ancienne, 2009-2012, peu documentée en ligne) — laissées `null`
 * plutôt que devinées. Taille standard WCF (~7cm) confirmée et appliquée par défaut à toutes les
 * fiches sauf la gamme "Mega" (échelle plus grande, valeur exacte non confirmée pour Dragon Ball).
 */
private data class DbWcfBox(val series: String, val year: Int?, val characters: List<String>, val heightCm: Double? = 7.0)

private val dragonBallSeries = listOf(
    // --- Dragon Ball (original) ---
    DbWcfBox("Dragon Ball Series 01", null, listOf("Kid Goku", "Bulma", "Bear Bandit", "Master Roshi", "Turtle", "Goku (Girl Disguise)", "Oolong's Robot Transformation", "Oolong")),
    DbWcfBox("Dragon Ball Series 02", null, listOf("Pterodactyl", "Oolong's Bull Transformation", "Ox King", "Chi Chi", "Super Roshi", "Goku (Roshi Training Outfit)", "Yamcha", "Puar")),
    DbWcfBox("Dragon Ball Series 03", null, listOf("Krillin", "Fat Lady", "Boss Rabbit", "Emperor Pilaf", "Shuu", "Mai", "Pilaf Machine", "Goku (Blue Outfit w/Power Pole)")),
    DbWcfBox("Dragon Ball Series 04", null, listOf("World Tournament Announcer", "Goku (Turtle School Tournament Outfit)", "Jackie Chun", "Bacterian", "Ranfan", "Giran", "Nam", "Oozaru Goku")),
    DbWcfBox("Dragon Ball Series 05", null, listOf("Goku (cold weather clothing)", "Suno", "Major Metallitron", "Murasaki", "Android 8", "Buyon", "Commander Red", "Staff Officer Black")),
    // --- Dragon Ball Z ---
    DbWcfBox("Dragon Ball Z Series 01", null, listOf("Goku", "Piccolo", "Kid Gohan", "Raditz", "Yamcha", "Vegeta", "Nappa", "Saibaman")),
    DbWcfBox("Dragon Ball Z Series 02", 2009, listOf("Goku (Kaioken)", "King Kai", "Bubbles", "Gohan (Pre-Namek)", "Krillin (Pre-Namek)", "Bulma (Pre-Namek Spacesuit)", "Chi Chi", "Ox King")),
    DbWcfBox("Dragon Ball Z Series 03", null, listOf("Frieza First Form", "Zarbon", "Dodoria", "Recoome", "Guldo", "Captain Ginyu", "Jeice", "Burter")),
    DbWcfBox("Dragon Ball Z Series 04", null, listOf("Nail", "Guru", "Dende", "Ginyu Frog & Female Frog", "Frieza Third Form", "Frieza Final Form", "Super Saiyan Goku", "Bardock")),
    DbWcfBox("Dragon Ball Z Series 05", null, listOf("Tien", "Chiaotzu", "Future Trunks", "Mecha Frieza", "King Cold", "Goku (Yardrat Outfit)", "Android 19", "Android 20")),
    DbWcfBox("Dragon Ball Z Series 06", null, listOf("Baby Trunks", "Super Saiyan Future Trunks", "Super Saiyan Vegeta", "Android 16", "Android 17", "Android 18", "Imperfect Cell", "Angel Goku")),
    // --- Dragon Ball Kai ---
    DbWcfBox("Dragon Ball Kai Series 01", null, listOf("Goku", "Piccolo", "Kid Gohan", "Raditz", "Yamcha", "Vegeta", "Nappa", "Saibaman")),
    DbWcfBox("Dragon Ball Kai Series 02", null, listOf("Goku (Kaioken)", "King Kai", "Bubbles", "Gohan (Pre-Namek)", "Krillin (Pre-Namek)", "Bulma (Pre-Namek Spacesuit)", "Chi Chi", "Ox King")),
    DbWcfBox("Dragon Ball Kai Series 03", null, listOf("Frieza First Form", "Zarbon", "Dodoria", "Recoome", "Guldo", "Captain Ginyu", "Jeice", "Burter")),
    DbWcfBox("Dragon Ball Kai Series 04", null, listOf("Nail", "Dende", "Ginyu Frog & Female Frog", "Frieza Third Form", "Frieza Final Form", "Super Saiyan Goku", "Bardock")),
    DbWcfBox("Dragon Ball Kai Series 05", null, listOf("Chi Chi (Wedding Dress)", "Goku (Wedding Tuxedo)", "Gohan", "Farmer w/Gun", "Tapion", "King Yemma", "Pikkon", "Gogeta")),
    DbWcfBox("Dragon Ball Kai Series 06", null, listOf("Appule", "Zarbon Transformed", "Cui", "Turles", "Cooler Final Form", "Frieza Second Form", "Frieza Third Form", "Captain Ginyu (Goku Body Switch)")),
    DbWcfBox("Dragon Ball Kai Series 07", null, listOf("Oozaru Vegeta", "Broly", "Legendary Super Saiyan Broly", "Kami", "Mr. Popo", "Shenron", "Porunga", "Hildegarn")),
    // --- Battle of Gods ---
    DbWcfBox("Dragon Ball Battle of Gods Vol.01", 2014, listOf("Super Saiyan God Goku", "Super Saiyan 3 Goku", "Beerus", "Whis", "Vegeta", "Kid Pilaf", "Kid Shuu", "Kid Mai")),
    DbWcfBox("Dragon Ball Battle of Gods Vol.02", 2014, listOf("Goku", "Chi Chi", "Super Saiyan Vegeta", "Bulma", "Gohan", "Videl", "Trunks", "Kid Mai (embarrassed version)")),
    DbWcfBox("Dragon Ball Battle of Gods Vol.03", 2014, listOf("Goten", "Oracle Fish", "Master Roshi", "Mr. Satan", "Krillin", "Android 18 & Marron", "Piccolo", "Beerus")),
    DbWcfBox("Dragon Ball Battle of Gods Vol.04", 2014, listOf("Super Saiyan God Goku (posed to fight)", "Fat Majin Buu", "Tien", "Chiaotzu", "Great Saiyaman", "Yamcha and Puar", "Oolong", "Ox King")),
    // --- Episode of Boo ---
    DbWcfBox("Dragon Ball Episode of Boo Vol.01", 2015, listOf("Son Gohan", "Videl", "Mr. Satan", "Super Saiyan Goku", "Majin Vegeta", "Dabura")),
    DbWcfBox("Dragon Ball Episode of Boo Vol.02", 2015, listOf("Vegetto", "Thin Boo", "Super Saiyan Gotenks + Ghost", "Super Saiyan Son Gohan", "Kibitoshin", "Elder Kai")),
    // --- VS Buu ---
    DbWcfBox("Dragon Ball VS Buu Vol.01", 2014, listOf("Majin Buu", "Super Saiyan 3 Goku", "Super Buu", "Super Saiyan 3 Gotenks", "Buutenks", "Gohan")),
    // --- Dragon Ball Volume 0 ---
    DbWcfBox("Dragon Ball Volume 0 Vol.01", 2015, listOf("Bardock", "Gine", "Kid Goku", "Kid Vegeta", "Kid Bulma", "Jaco")),
    // --- Super Saiyans ---
    DbWcfBox("Dragon Ball Super Saiyans Vol.01", 2014, listOf("Super Saiyan Vegeta", "Super Saiyan Goku", "Super Saiyan Future Trunks", "Super Saiyan Gohan", "Super Saiyan Kid Goten", "Super Saiyan Kid Trunks")),
    // --- Mega WCF (échelle plus grande que le WCF standard 7cm, ex. confirmée One Piece ~13cm —
    // taille exacte non confirmée pour la ligne Dragon Ball, laissée à null plutôt que devinée) ---
    DbWcfBox("Dragon Ball Mega WCF", 2014, listOf("Shenron", "Tori-Bot"), heightCm = null),
    DbWcfBox("Dragon Ball Mega WCF", 2014, listOf("Legendary Super Saiyan Broly"), heightCm = null),
    DbWcfBox("Dragon Ball Mega WCF", null, listOf("Super Shenron", "Time Machine", "Frieza Space Ship"), heightCm = null),
    // --- Resurrection of F ---
    DbWcfBox("Dragon Ball Resurrection of F Vol.01", 2015, listOf("Super Saiyan Blue Goku", "Super Saiyan Blue Vegeta", "Gohan (track suit)", "Piccolo", "Krillin", "Master Roshi")),
    DbWcfBox("Dragon Ball Resurrection of F Vol.02", 2015, listOf("Frieza (First Form)", "Sorbet", "Tagoma", "Shisami", "Frieza (Fourth Form)", "Golden Frieza")),
    DbWcfBox("Dragon Ball Resurrection of F Vol.03", 2015, listOf("Goku", "Vegeta", "Tien", "Jaco", "Bulma", "Mechanical Frieza (Cocoon)")),
    // --- Frieza Special Series ---
    DbWcfBox("Dragon Ball Frieza Special Series Vol.01", null, listOf("Dodoria", "Frieza (first stage)", "Zarbon", "Raspberry", "Frieza (second stage)", "Frieza (third stage)")),
    DbWcfBox("Dragon Ball Frieza Special Series Vol.02", null, listOf("Cooler", "Frieza (full power)", "Golden Frieza", "Mecha Frieza", "King Cold", "Blueberry")),
    // --- Z Warriors Series ---
    DbWcfBox("Dragon Ball Super Z Warriors Series Vol.01", null, listOf("Super Saiyan Blue Goku (ki rising)", "Super Saiyan Blue Vegeta (ki rising)", "Super Saiyan Gohan (track suit)", "Piccolo (martial stance)", "Beerus", "Whis")),
    // --- Dragon Ball Super Series ---
    DbWcfBox("Dragon Ball Super Series Vol.01", null, listOf("Goku", "Vegeta", "Champa", "Vados", "Supreme Kai", "Kibito")),
    DbWcfBox("Dragon Ball Super Series Vol.02", null, listOf("God Ki Goku (aerial combat)", "King Kai", "Hercule Satan", "Majin Buu (fat)", "Korin", "Yajirobe")),
    DbWcfBox("Dragon Ball Super Series Vol.03", null, listOf("Super Saiyan Blue Goku", "Super Saiyan Blue Vegeta", "Super Saiyan Trunks", "Super Saiyan Goten", "Baba", "Grandpa Gohan")),
    DbWcfBox("Dragon Ball Super Series Vol.04", null, listOf("Super Saiyan God Goku (ki rising)", "Super Saiyan Vegeta", "Monaka", "Frost", "Cabba", "Hit")),
    DbWcfBox("Dragon Ball Super Series Vol.05", null, listOf("Super Saiyan God Goku", "Vegeta w/pacifier", "Clone Vegeta", "Jaco", "Zen-Oh's attendant", "Zen-Oh")),
    DbWcfBox("Dragon Ball Super Series Vol.06", null, listOf("Future Trunks", "Future Mai", "Black", "Zamasu", "Future Gohan", "Future Kid Trunks")),
    DbWcfBox("Dragon Ball Super Series Vol.07", null, listOf("Super Saiyan Rose Black", "Super Saiyan God Zamasu (fused)", "Super Saiyan God Vegeto", "Super Saiyan Future Trunks (Godslayer)", "Gowasu", "High Priest")),
    // --- Battle of Saiyans Series ---
    DbWcfBox("Dragon Ball Battle of Saiyans Series Vol.01", null, listOf("Goku", "Vegeta", "Bardock", "Frieza (first stage)", "Super Saiyan Goku", "Super Saiyan Gohan")),
    DbWcfBox("Dragon Ball Battle of Saiyans Series Vol.02", null, listOf("Legendary Super Saiyan Broly", "Super Saiyan Goku", "Super Saiyan Future Trunks", "Perfect Cell", "Demon King Piccolo", "Kid Goku")),
    DbWcfBox("Dragon Ball Battle of Saiyans Series Vol.03", null, listOf("Super Saiyan Goku", "Frieza (final stage)", "Super Saiyan 3 Goku", "Kid Buu", "Super Saiyan Blue Goku", "Golden Frieza")),
    // --- Prince Vegeta Series ---
    DbWcfBox("Dragon Ball Prince Vegeta Series Vol.01", 2016, listOf("Vegeta (octopus)", "Vegeta (kneeling)", "Vegeta (orange apron)", "Vegeta (pink apron)", "Vegeta (gasping)", "Vegeta (cleaning)")),
    // --- Memorial Parade ---
    DbWcfBox("Dragon Ball Memorial Parade Dr. Slump", null, listOf("Arale Norimaki (orange gi)", "Gatchan", "Senbei Norimaki", "Midori Yamabuki & King Nikochan & Servant", "Suppaman")),
    DbWcfBox("Dragon Ball Memorial Parade Vol.01", 2014, listOf("Tori Bot & Nimbus (purple gi)", "Goku", "Krillin", "Gohan (four star)", "Piccolo", "Vegeta")),
    // --- Anime 30th Anniversary Series ---
    DbWcfBox("Dragon Ball Anime 30th Anniversary Series Vol.01", null, listOf("Kid Goku & Nimbus", "Bulma", "Master Roshi", "Krillin", "Yamcha & Puar", "General Tao")),
    DbWcfBox("Dragon Ball Anime 30th Anniversary Series Vol.02", null, listOf("Goku", "Piccolo", "Vegeta (Saiyan saga)", "Raditz", "Frieza (final form)", "Captain Ginyu")),
    DbWcfBox("Dragon Ball Anime 30th Anniversary Series Vol.03", null, listOf("Super Saiyan Goku", "Super Saiyan Gohan (Cell games)", "Android 17", "Android 18", "Perfect Cell", "Future Trunks")),
    DbWcfBox("Dragon Ball Anime 30th Anniversary Series Vol.04", null, listOf("Super Saiyan 3 Goku", "Super Saiyan 3 Goku", "Majin Buu (fat)", "Hercule Satan", "Super Saiyan Vegeto", "Majin Buu (kid)")),
    DbWcfBox("Dragon Ball Anime 30th Anniversary Series Vol.05", null, listOf("God Ki Goku", "Super Saiyan Vegeta", "Beerus", "Whis", "Jaco", "Golden Frieza")),
    DbWcfBox("Dragon Ball Anime 30th Anniversary Series Vol.06", null, listOf("Super Saiyan Blue Goku", "Super Saiyan Blue Vegeta", "Champa", "Vados", "Uub", "Goku (blue gi)")),
    // --- Boss Frieza Series ---
    DbWcfBox("Dragon Ball Boss Frieza Series (Risou no Joushi Freezer)", null, listOf("Frieza (palms outward)", "Mecha Frieza", "Frieza (clapping)", "Frieza (pointing)", "Frieza (fists)", "Frieza (100% power)")),
    // --- Childhood Arc (2026-07-20, source shandorashop.com — fiches produit avec date/taille
    // précises par volume, coffret jamais référencé avant sur dragonballfigures.com) ---
    DbWcfBox("Dragon Ball Childhood Arc Vol.1", 2026, listOf("Son Goku", "Bulma", "Master Roshi", "Sea Turtle", "Pteranodon"), heightCm = 8.0),
    DbWcfBox("Dragon Ball Childhood Arc Vol.2", 2026, listOf("Son Goku", "Bulma", "Oolong", "Village Chief", "Robot (Oolong Transformation)"), heightCm = 8.0),
    DbWcfBox("Dragon Ball Childhood Arc Vol.3", 2026, listOf("Son Goku", "Yamcha A", "Puar", "Bulma (Oolong Transformation)", "Yamcha B"), heightCm = 8.0),
    DbWcfBox("Dragon Ball Childhood Arc Vol.4", 2026, listOf("Son Goku", "Bulma", "Ox King", "Chi-Chi", "Master Roshi"), heightCm = 7.0)
)

val figurePresetsDragonBall: List<FigurePreset> = dragonBallSeries.flatMap { box ->
    box.characters.map { char ->
        FigurePreset(licence = Licence.DRAGON_BALL, character = char, name = char, series = box.series, year = box.year, heightCm = box.heightCm)
    }
}
