package com.smartvision.svplayer.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.smartvision.svplayer.data.local.dao.CategoryDao
import com.smartvision.svplayer.data.local.dao.FavoriteDao
import com.smartvision.svplayer.data.local.dao.MediaCenterDao
import com.smartvision.svplayer.data.local.dao.MediaDao
import com.smartvision.svplayer.data.local.dao.ProfileDao
import com.smartvision.svplayer.data.local.dao.ProgressDao
import com.smartvision.svplayer.data.local.dao.SyncStateDao
import com.smartvision.svplayer.data.local.dao.YoutubeDao
import com.smartvision.svplayer.data.local.entity.CategoryEntity
import com.smartvision.svplayer.data.local.entity.EpisodeEntity
import com.smartvision.svplayer.data.local.entity.FavoriteEntity
import com.smartvision.svplayer.data.local.entity.HomeTrendingPreviewCacheEntity
import com.smartvision.svplayer.data.local.entity.LiveStreamEntity
import com.smartvision.svplayer.data.local.entity.MediaFileEntity
import com.smartvision.svplayer.data.local.entity.MediaFolderEntity
import com.smartvision.svplayer.data.local.entity.MovieEntity
import com.smartvision.svplayer.data.local.entity.PlaybackProgressEntity
import com.smartvision.svplayer.data.local.entity.ProfileEntity
import com.smartvision.svplayer.data.local.entity.RecordingJobEntity
import com.smartvision.svplayer.data.local.entity.SeriesEntity
import com.smartvision.svplayer.data.local.entity.SyncStateEntity
import com.smartvision.svplayer.data.local.entity.TrendingMediaEntity
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
        TrendingMediaEntity::class,
        HomeTrendingPreviewCacheEntity::class,
        YoutubeSearchEntity::class,
        YoutubeVideoHistoryEntity::class,
        YoutubeSelectionEntity::class,
        YoutubeBehaviorEventEntity::class,
        MediaFolderEntity::class,
        MediaFileEntity::class,
        RecordingJobEntity::class,
    ],
    version = 10,
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
    abstract fun mediaCenterDao(): MediaCenterDao

    companion object {
        fun build(context: Context): SVDatabase =
            Room.databaseBuilder(context, SVDatabase::class.java, "svplayer.db")
                .addMigrations(
                    Migration1To2,
                    Migration2To3,
                    Migration3To4,
                    Migration4To5,
                    Migration5To6,
                    Migration6To7,
                    Migration7To8,
                    Migration8To9,
                    Migration9To10,
                )
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

        private val Migration4To5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE youtube_behavior_events ADD COLUMN contentTitle TEXT")
                db.execSQL("ALTER TABLE youtube_behavior_events ADD COLUMN categoryLabel TEXT")
                db.execSQL("ALTER TABLE youtube_behavior_events ADD COLUMN sourceScreen TEXT")
                db.execSQL("ALTER TABLE youtube_behavior_events ADD COLUMN durationSeconds INTEGER")
                db.execSQL("ALTER TABLE youtube_behavior_events ADD COLUMN engagementScore INTEGER")
                db.execSQL("ALTER TABLE youtube_behavior_events ADD COLUMN context TEXT")
            }
        }

        private val Migration5To6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE live_streams ADD COLUMN directStreamUrl TEXT")
                db.execSQL("ALTER TABLE live_streams ADD COLUMN source TEXT NOT NULL DEFAULT 'xtream'")
            }
        }

        private val Migration6To7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_live_streams_categoryId ON live_streams(categoryId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_live_streams_categoryId_number_name ON live_streams(categoryId, number, name)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_live_streams_source ON live_streams(source)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_movies_categoryId ON movies(categoryId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_movies_categoryId_number_title ON movies(categoryId, number, title)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_series_categoryId ON series(categoryId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_series_categoryId_number_title ON series(categoryId, number, title)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_episodes_seriesId ON episodes(seriesId)")
            }
        }

        private val Migration7To8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS trending_media (" +
                        "contentType TEXT NOT NULL, " +
                        "contentId INTEGER NOT NULL, " +
                        "sampleContentId INTEGER, " +
                        "sampleExtension TEXT, " +
                        "rating REAL NOT NULL, " +
                        "updatedAt INTEGER NOT NULL, " +
                        "PRIMARY KEY(contentType, contentId)" +
                        ")",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_trending_media_contentType_rating ON trending_media(contentType, rating)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_trending_media_contentType_updatedAt ON trending_media(contentType, updatedAt)")
            }
        }

        private val Migration8To9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS home_trending_preview_cache (" +
                        "contentType TEXT NOT NULL, " +
                        "contentId INTEGER NOT NULL, " +
                        "posterUrl TEXT, " +
                        "backdropUrl TEXT, " +
                        "durationLabel TEXT, " +
                        "durationMs INTEGER, " +
                        "previewKind TEXT NOT NULL, " +
                        "previewContentId INTEGER, " +
                        "previewExtension TEXT, " +
                        "previewStartPositionMs INTEGER NOT NULL, " +
                        "sampleLabel TEXT, " +
                        "backdropState TEXT NOT NULL, " +
                        "previewState TEXT NOT NULL, " +
                        "preparedAt INTEGER NOT NULL, " +
                        "lastSync INTEGER NOT NULL, " +
                        "PRIMARY KEY(contentType, contentId)" +
                        ")",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_home_trending_preview_cache_contentType_preparedAt ON home_trending_preview_cache(contentType, preparedAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_home_trending_preview_cache_lastSync ON home_trending_preview_cache(lastSync)")
            }
        }

        private val Migration9To10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS media_folders (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "name TEXT NOT NULL, " +
                        "relativePath TEXT NOT NULL, " +
                        "parentId INTEGER, " +
                        "createdAt INTEGER NOT NULL, " +
                        "updatedAt INTEGER NOT NULL" +
                        ")",
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_media_folders_relativePath ON media_folders(relativePath)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_media_folders_parentId ON media_folders(parentId)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS media_files (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "folderId INTEGER, " +
                        "displayName TEXT NOT NULL, " +
                        "relativePath TEXT NOT NULL, " +
                        "mimeType TEXT, " +
                        "mediaType TEXT NOT NULL, " +
                        "source TEXT NOT NULL, " +
                        "sizeBytes INTEGER NOT NULL, " +
                        "durationMs INTEGER, " +
                        "createdAt INTEGER NOT NULL, " +
                        "updatedAt INTEGER NOT NULL, " +
                        "lastIndexedAt INTEGER NOT NULL, " +
                        "deletedAt INTEGER" +
                        ")",
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_media_files_relativePath ON media_files(relativePath)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_media_files_folderId ON media_files(folderId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_media_files_mediaType ON media_files(mediaType)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_media_files_source ON media_files(source)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_media_files_deletedAt ON media_files(deletedAt)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS recording_jobs (" +
                        "id TEXT NOT NULL PRIMARY KEY, " +
                        "mediaFileId INTEGER, " +
                        "status TEXT NOT NULL, " +
                        "sourceType TEXT NOT NULL, " +
                        "title TEXT NOT NULL, " +
                        "outputRelativePath TEXT, " +
                        "startedAt INTEGER, " +
                        "endedAt INTEGER, " +
                        "updatedAt INTEGER NOT NULL, " +
                        "errorMessage TEXT" +
                        ")",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_recording_jobs_status ON recording_jobs(status)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_recording_jobs_mediaFileId ON recording_jobs(mediaFileId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_recording_jobs_updatedAt ON recording_jobs(updatedAt)")
            }
        }
    }
}
