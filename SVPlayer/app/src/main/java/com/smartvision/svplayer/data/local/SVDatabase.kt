package com.smartvision.svplayer.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.smartvision.svplayer.data.local.dao.CategoryDao
import com.smartvision.svplayer.data.local.dao.FavoriteDao
import com.smartvision.svplayer.data.local.dao.MediaDao
import com.smartvision.svplayer.data.local.dao.ProfileDao
import com.smartvision.svplayer.data.local.dao.ProgressDao
import com.smartvision.svplayer.data.local.dao.SyncStateDao
import com.smartvision.svplayer.data.local.dao.YoutubeDao
import com.smartvision.svplayer.data.local.entity.CategoryEntity
import com.smartvision.svplayer.data.local.entity.EpisodeEntity
import com.smartvision.svplayer.data.local.entity.FavoriteEntity
import com.smartvision.svplayer.data.local.entity.LiveStreamEntity
import com.smartvision.svplayer.data.local.entity.MovieEntity
import com.smartvision.svplayer.data.local.entity.PlaybackProgressEntity
import com.smartvision.svplayer.data.local.entity.ProfileEntity
import com.smartvision.svplayer.data.local.entity.SeriesEntity
import com.smartvision.svplayer.data.local.entity.SyncStateEntity
import com.smartvision.svplayer.data.local.entity.YoutubeBehaviorEventEntity
import com.smartvision.svplayer.data.local.entity.YoutubeSearchEntity
import com.smartvision.svplayer.data.local.entity.YoutubeSelectionEntity
import com.smartvision.svplayer.data.local.entity.YoutubeVideoHistoryEntity

@Database(
    entities = [
        ProfileEntity::class,
        CategoryEntity::class,
        LiveStreamEntity::class,
        MovieEntity::class,
        SeriesEntity::class,
        EpisodeEntity::class,
        FavoriteEntity::class,
        PlaybackProgressEntity::class,
        SyncStateEntity::class,
        YoutubeSearchEntity::class,
        YoutubeVideoHistoryEntity::class,
        YoutubeSelectionEntity::class,
        YoutubeBehaviorEventEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
abstract class SVDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun mediaDao(): MediaDao
    abstract fun profileDao(): ProfileDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun progressDao(): ProgressDao
    abstract fun syncStateDao(): SyncStateDao
    abstract fun youtubeDao(): YoutubeDao

    companion object {
        fun build(context: Context): SVDatabase =
            Room.databaseBuilder(context, SVDatabase::class.java, "svplayer.db")
                .addMigrations(Migration1To2, Migration2To3, Migration3To4)
                .build()

        private val Migration1To2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE playback_progress ADD COLUMN title TEXT")
                db.execSQL("ALTER TABLE playback_progress ADD COLUMN subtitle TEXT")
                db.execSQL("ALTER TABLE playback_progress ADD COLUMN imageUrl TEXT")
                db.execSQL("ALTER TABLE playback_progress ADD COLUMN parentContentId TEXT")
            }
        }

        private val Migration2To3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS youtube_searches (" +
                        "query TEXT NOT NULL PRIMARY KEY, " +
                        "updatedAt INTEGER NOT NULL" +
                        ")",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS youtube_video_history (" +
                        "videoId TEXT NOT NULL PRIMARY KEY, " +
                        "title TEXT NOT NULL, " +
                        "channelTitle TEXT NOT NULL, " +
                        "thumbnailUrl TEXT, " +
                        "publishedAt TEXT, " +
                        "updatedAt INTEGER NOT NULL" +
                        ")",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS youtube_selection (" +
                        "id TEXT NOT NULL PRIMARY KEY, " +
                        "videoId TEXT, " +
                        "updatedAt INTEGER NOT NULL" +
                        ")",
                )
            }
        }

        private val Migration3To4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE youtube_video_history ADD COLUMN channelId TEXT")
                db.execSQL("ALTER TABLE youtube_video_history ADD COLUMN viewCount INTEGER")
                db.execSQL("ALTER TABLE youtube_video_history ADD COLUMN durationIso TEXT")
                db.execSQL("ALTER TABLE youtube_video_history ADD COLUMN durationSeconds INTEGER")
                db.execSQL("ALTER TABLE youtube_video_history ADD COLUMN categoryId TEXT")
                db.execSQL("ALTER TABLE youtube_video_history ADD COLUMN tags TEXT")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS youtube_behavior_events (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "eventType TEXT NOT NULL, " +
                        "videoIdHash TEXT, " +
                        "channelId TEXT, " +
                        "categoryId TEXT, " +
                        "tags TEXT, " +
                        "createdAt INTEGER NOT NULL, " +
                        "syncedAt INTEGER" +
                        ")",
                )
            }
        }
    }
}
