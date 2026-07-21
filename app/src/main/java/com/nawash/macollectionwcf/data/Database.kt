package com.nawash.macollectionwcf.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionDao {
    @Query("SELECT * FROM collection_items")
    fun observeAll(): Flow<List<CollectionItem>>

    @Insert suspend fun insert(item: CollectionItem): Long
    @Insert suspend fun insertAll(items: List<CollectionItem>)
    @Update suspend fun update(item: CollectionItem)
    @Delete suspend fun delete(item: CollectionItem)
}

@Dao
interface PriceHistoryDao {
    @Insert suspend fun insert(entry: PriceHistory)
    @Insert suspend fun insertAll(entries: List<PriceHistory>)

    @Query("SELECT * FROM price_history WHERE itemId = :itemId ORDER BY timestamp ASC")
    suspend fun forItem(itemId: Long): List<PriceHistory>

    @Query("SELECT * FROM price_history WHERE itemId = :itemId ORDER BY timestamp DESC LIMIT 1")
    suspend fun latest(itemId: Long): PriceHistory?

    @Query("SELECT * FROM price_history")
    suspend fun getAll(): List<PriceHistory>
}

@Dao
interface ItemPhotoDao {
    @Query("SELECT * FROM item_photos WHERE itemId = :itemId ORDER BY position ASC, id ASC")
    fun observeForItem(itemId: Long): Flow<List<ItemPhoto>>

    @Query("SELECT * FROM item_photos")
    suspend fun getAll(): List<ItemPhoto>

    @Insert suspend fun insert(photo: ItemPhoto): Long
    @Insert suspend fun insertAll(photos: List<ItemPhoto>)
    @Delete suspend fun delete(photo: ItemPhoto)

    @Query("DELETE FROM item_photos WHERE itemId = :itemId")
    suspend fun deleteForItem(itemId: Long)
}

@Dao
interface CustomFigurePresetDao {
    @Query("SELECT * FROM custom_figure_presets ORDER BY name ASC")
    fun observeAll(): Flow<List<CustomFigurePreset>>

    @Insert suspend fun insert(preset: CustomFigurePreset): Long
    @Insert suspend fun insertAll(presets: List<CustomFigurePreset>)
    @Update suspend fun update(preset: CustomFigurePreset)
    @Delete suspend fun delete(preset: CustomFigurePreset)
}

@Dao
interface PresetPhotoOverrideDao {
    @Query("SELECT * FROM preset_photo_overrides")
    fun observeAll(): Flow<List<PresetPhotoOverride>>

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun upsert(override: PresetPhotoOverride)

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertAll(overrides: List<PresetPhotoOverride>)

    @Query("DELETE FROM preset_photo_overrides WHERE presetName = :name")
    suspend fun delete(name: String)
}

@Dao
interface FigureCatalogDao {
    @Query("SELECT * FROM figure_catalog ORDER BY licence ASC, series ASC, numero ASC")
    fun observeAll(): Flow<List<FigureCatalogEntry>>

    @Query("SELECT DISTINCT licence FROM figure_catalog")
    suspend fun distinctLicences(): List<Licence>

    @Insert suspend fun insertAll(entries: List<FigureCatalogEntry>)

    @Query("UPDATE figure_catalog SET imagePath = :imagePath, priceCents = :priceCents, photoChecked = 1 WHERE id = :id AND photoLocked = 0")
    suspend fun markEnriched(id: Long, imagePath: String?, priceCents: Int?)

    /** Fiches déjà enrichies avec une photo en cache, hors photos de référence verrouillées (voir `AppViewModel.init`, purge ponctuelle après le passage à une requête personnage+vague plus précise). */
    @Query("SELECT * FROM figure_catalog WHERE photoChecked = 1 AND imagePath IS NOT NULL AND photoLocked = 0")
    suspend fun entriesWithCachedPhoto(): List<FigureCatalogEntry>

    /** Remet à zéro l'enrichissement (photo/côte) hors photos de référence verrouillées, pour forcer un nouveau passage avec une requête plus précise. */
    @Query("UPDATE figure_catalog SET imagePath = NULL, priceCents = NULL, photoChecked = 0 WHERE photoLocked = 0")
    suspend fun resetAllEnrichment()

    /** Fiche correspondant à un code (ex. "OP-427") — voir `CatalogReferencePhotos`. */
    @Query("SELECT * FROM figure_catalog WHERE numero = :numero LIMIT 1")
    suspend fun byNumero(numero: String): FigureCatalogEntry?

    /** Applique une photo de référence fournie manuellement (voir `CatalogReferencePhotos`) : verrouillée, plus jamais écrasée par l'enrichissement auto. */
    @Query("UPDATE figure_catalog SET imagePath = :imagePath, photoChecked = 1, photoLocked = 1 WHERE id = :id")
    suspend fun setLockedPhoto(id: Long, imagePath: String)

    /** Rattrape [FigureCatalogEntry.releaseYear]/[FigureCatalogEntry.heightCm] sur une fiche déjà en base (voir `FigureCatalogSeeder.backfillMissingData`, chasse aux dates/tailles menée après le peuplement initial). */
    @Query("UPDATE figure_catalog SET releaseYear = :releaseYear, heightCm = :heightCm WHERE id = :id")
    suspend fun backfill(id: Long, releaseYear: Int?, heightCm: Double?)

    @Query("SELECT * FROM figure_catalog WHERE licence = :licence")
    suspend fun byLicence(licence: Licence): List<FigureCatalogEntry>

    /** Réaligne la référence de tri après correction d'un coffret (voir `FigureCatalogSeeder.resyncNumbering`). */
    @Query("UPDATE figure_catalog SET numero = :numero WHERE id = :id")
    suspend fun updateNumero(id: Long, numero: String)

    /** Prix de référence (voir `CatalogReferencePrices`) — n'écrase jamais une côte déjà connue (eBay ou autre). */
    @Query("UPDATE figure_catalog SET priceCents = :priceCents WHERE id = :id AND priceCents IS NULL")
    suspend fun setReferencePriceIfMissing(id: Long, priceCents: Int)

    @Delete suspend fun deleteAll(entries: List<FigureCatalogEntry>)
}

@Dao
interface WcfNewsDao {
    @Query("SELECT * FROM wcf_news ORDER BY id DESC")
    fun observeAll(): Flow<List<WcfNewsEntry>>

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entries: List<WcfNewsEntry>)

    @Query("DELETE FROM wcf_news")
    suspend fun clear()
}

/** Ajoute uniquement la table `figure_catalog` (Phase 5) — aucune table existante touchée, aucune perte de données. */
private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `figure_catalog` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `licence` TEXT NOT NULL,
                `series` TEXT NOT NULL,
                `numero` TEXT NOT NULL,
                `character` TEXT NOT NULL,
                `releaseYear` INTEGER,
                `imagePath` TEXT,
                `priceCents` INTEGER,
                `photoChecked` INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
    }
}

/** Ajoute [CollectionItem.numero] et [FigureCatalogEntry.photoLocked] — aucune table recréée, aucune perte de données. */
private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `collection_items` ADD COLUMN `numero` TEXT")
        db.execSQL("ALTER TABLE `figure_catalog` ADD COLUMN `photoLocked` INTEGER NOT NULL DEFAULT 0")
    }
}

/** Ajoute [FigureCatalogEntry.heightCm] — aucune table recréée, aucune perte de données. */
private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `figure_catalog` ADD COLUMN `heightCm` REAL")
    }
}

/** Ajoute la table `wcf_news` (onglet Actu) — aucune table existante touchée, aucune perte de données. */
private val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `wcf_news` (
                `id` TEXT PRIMARY KEY NOT NULL,
                `series` TEXT NOT NULL,
                `characters` TEXT NOT NULL,
                `releaseDateRaw` TEXT NOT NULL,
                `priceRaw` TEXT NOT NULL,
                `imageUrl` TEXT NOT NULL,
                `itemUrl` TEXT NOT NULL,
                `scrapedAt` TEXT NOT NULL
            )
            """.trimIndent()
        )
    }
}

/** Ajoute les traductions FR des actus WCF (seriesFr/charactersFr/releaseDateFr/priceFr) — aucune table recréée, aucune perte de données. */
private val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `wcf_news` ADD COLUMN `seriesFr` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `wcf_news` ADD COLUMN `charactersFr` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `wcf_news` ADD COLUMN `releaseDateFr` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `wcf_news` ADD COLUMN `priceFr` TEXT NOT NULL DEFAULT ''")
    }
}

@Database(
    entities = [CollectionItem::class, PriceHistory::class, ItemPhoto::class, CustomFigurePreset::class, PresetPhotoOverride::class, FigureCatalogEntry::class, WcfNewsEntry::class],
    version = 6,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun collectionDao(): CollectionDao
    abstract fun priceHistoryDao(): PriceHistoryDao
    abstract fun itemPhotoDao(): ItemPhotoDao
    abstract fun customFigurePresetDao(): CustomFigurePresetDao
    abstract fun presetPhotoOverrideDao(): PresetPhotoOverrideDao
    abstract fun figureCatalogDao(): FigureCatalogDao
    abstract fun wcfNewsDao(): WcfNewsDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "macollectionwcf.db"
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                .fallbackToDestructiveMigration()
                .build().also { instance = it }
        }
    }
}
