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
import com.smartvision.svplayer.data.local.dao.KidsFilterDao
import com.smartvision.svplayer.data.local.dao.ProfileDao
import com.smartvision.svplayer.data.local.dao.ProgressDao
import com.smartvision.svplayer.data.local.dao.SyncStateDao
import com.smartvision.svplayer.data.local.dao.YoutubeDao
import com.smartvision.svplayer.data.local.entity.CategoryEntity
import com.smartvision.svplayer.data.local.entity.EpisodeEntity
import com.smartvision.svplayer.data.local.entity.FavoriteEntity
import com.smartvision.svplayer.data.local.entity.HomeTrendingPreviewCacheEntity
import com.smartvision.svplayer.data.local.entity.LiveStreamEntity
import com.smartvision.svplayer.data.local.entity.KidsCategoryDecisionEntity
import com.smartvision.svplayer.data.local.entity.KidsItemDecisionEntity
import com.smartvision.svplayer.data.local.entity.MediaFileEntity
import com.smartvision.svplayer.data.local.entity.MediaFolderEntity
import com.smartvision.svplayer.data.local.entity.MovieEntity
import com.smartvision.svplayer.data.local.entity.PlaybackProgressEntity
import com.smartvision.svplayer.data.local.entity.ProfileEntity
import com.smartvision.svplayer.data.local.entity.RecordingJobEntity
import com.smartvision.svplayer.data.local.entity.SeriesEntity
import com.smartvision.svplayer.data.local.entity.SyncStateEntity
import com.smartvision.svplayer.data.local.entity.TmdbContentMappingEntity
import com.smartvision.svplayer.data.local.entity.TmdbMovieMetadataEntity
import com.smartvision.svplayer.data.local.entity.TmdbSeriesMetadataEntity
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
        TmdbContentMappingEntity::class,
        TmdbMovieMetadataEntity::class,
        TmdbSeriesMetadataEntity::class,
        YoutubeSearchEntity::class,
        YoutubeVideoHistoryEntity::class,
        YoutubeSelectionEntity::class,
        YoutubeBehaviorEventEntity::class,
        MediaFolderEntity::class,
        MediaFileEntity::class,
        RecordingJobEntity::class,
        KidsCategoryDecisionEntity::class,
        KidsItemDecisionEntity::class,
    ],
    version = 16,
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
    abstract fun kidsFilterDao(): KidsFilterDao

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
                    Migration10To11,
                    Migration11To12,
                    Migration12To13(context),
                    Migration13To14(context),
                    Migration14To15,
                    Migration15To16,
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

        private val Migration10To11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS tmdb_content_mapping (" +
                        "contentType TEXT NOT NULL, " +
                        "contentId INTEGER NOT NULL, " +
                        "tmdbId INTEGER, " +
                        "mediaType TEXT NOT NULL, " +
                        "matchedTitle TEXT, " +
                        "originalTitle TEXT, " +
                        "matchedYear TEXT, " +
                        "confidence INTEGER NOT NULL, " +
                        "matchSource TEXT NOT NULL, " +
                        "language TEXT NOT NULL, " +
                        "adult INTEGER NOT NULL, " +
                        "lastError TEXT, " +
                        "createdAt INTEGER NOT NULL, " +
                        "updatedAt INTEGER NOT NULL, " +
                        "PRIMARY KEY(contentType, contentId)" +
                        ")",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tmdb_content_mapping_tmdbId ON tmdb_content_mapping(tmdbId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tmdb_content_mapping_contentType_confidence ON tmdb_content_mapping(contentType, confidence)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tmdb_content_mapping_updatedAt ON tmdb_content_mapping(updatedAt)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS tmdb_movie_metadata (" +
                        "tmdbId INTEGER NOT NULL, " +
                        "language TEXT NOT NULL, " +
                        "title TEXT NOT NULL, " +
                        "originalTitle TEXT, " +
                        "overview TEXT, " +
                        "posterPath TEXT, " +
                        "posterUrl TEXT, " +
                        "backdropPath TEXT, " +
                        "backdropUrl TEXT, " +
                        "logoPath TEXT, " +
                        "logoUrl TEXT, " +
                        "releaseDate TEXT, " +
                        "runtimeMinutes INTEGER, " +
                        "genres TEXT, " +
                        "voteAverage REAL, " +
                        "voteCount INTEGER, " +
                        "popularity REAL, " +
                        "cast TEXT, " +
                        "director TEXT, " +
                        "trailerKey TEXT, " +
                        "collectionName TEXT, " +
                        "certification TEXT, " +
                        "providersSummary TEXT, " +
                        "homepage TEXT, " +
                        "status TEXT, " +
                        "adult INTEGER NOT NULL, " +
                        "originCountry TEXT, " +
                        "originalLanguage TEXT, " +
                        "updatedAt INTEGER NOT NULL, " +
                        "PRIMARY KEY(tmdbId, language)" +
                        ")",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tmdb_movie_metadata_updatedAt ON tmdb_movie_metadata(updatedAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tmdb_movie_metadata_releaseDate ON tmdb_movie_metadata(releaseDate)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS tmdb_series_metadata (" +
                        "tmdbId INTEGER NOT NULL, " +
                        "language TEXT NOT NULL, " +
                        "name TEXT NOT NULL, " +
                        "originalName TEXT, " +
                        "overview TEXT, " +
                        "posterPath TEXT, " +
                        "posterUrl TEXT, " +
                        "backdropPath TEXT, " +
                        "backdropUrl TEXT, " +
                        "logoPath TEXT, " +
                        "logoUrl TEXT, " +
                        "firstAirDate TEXT, " +
                        "episodeRunTimeMinutes INTEGER, " +
                        "genres TEXT, " +
                        "voteAverage REAL, " +
                        "voteCount INTEGER, " +
                        "popularity REAL, " +
                        "cast TEXT, " +
                        "createdBy TEXT, " +
                        "trailerKey TEXT, " +
                        "certification TEXT, " +
                        "providersSummary TEXT, " +
                        "homepage TEXT, " +
                        "status TEXT, " +
                        "adult INTEGER NOT NULL, " +
                        "originCountry TEXT, " +
                        "originalLanguage TEXT, " +
                        "updatedAt INTEGER NOT NULL, " +
                        "PRIMARY KEY(tmdbId, language)" +
                        ")",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tmdb_series_metadata_updatedAt ON tmdb_series_metadata(updatedAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tmdb_series_metadata_firstAirDate ON tmdb_series_metadata(firstAirDate)")
            }
        }

        private val Migration11To12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE home_trending_preview_cache ADD COLUMN trailerKey TEXT")
                db.execSQL("ALTER TABLE tmdb_movie_metadata ADD COLUMN castJson TEXT")
                db.execSQL("ALTER TABLE tmdb_movie_metadata ADD COLUMN directorJson TEXT")
                db.execSQL("ALTER TABLE tmdb_movie_metadata ADD COLUMN videosJson TEXT")
                db.execSQL("ALTER TABLE tmdb_movie_metadata ADD COLUMN recommendationsJson TEXT")
                db.execSQL("ALTER TABLE tmdb_series_metadata ADD COLUMN castJson TEXT")
                db.execSQL("ALTER TABLE tmdb_series_metadata ADD COLUMN createdByJson TEXT")
                db.execSQL("ALTER TABLE tmdb_series_metadata ADD COLUMN videosJson TEXT")
                db.execSQL("ALTER TABLE tmdb_series_metadata ADD COLUMN recommendationsJson TEXT")
                db.execSQL(
                    "UPDATE home_trending_preview_cache " +
                        "SET previewKind = 'none', previewContentId = NULL, previewExtension = NULL, " +
                        "previewState = 'unavailable' " +
                        "WHERE previewKind IN ('movie', 'episode')",
                )
            }
        }

        private fun Migration12To13(context: Context) = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val profileId = context
                    .getSharedPreferences("xtream_accounts", Context.MODE_PRIVATE)
                    .getString("active_playlist_profile_id", null)
                    ?.takeIf { it.isNotBlank() }
                    ?: "default"
                val sqlProfileId = profileId.replace("'", "''")

                recreateCategories(db, sqlProfileId)
                recreateLiveStreams(db, sqlProfileId)
                recreateMovies(db, sqlProfileId)
                recreateSeries(db, sqlProfileId)
                recreateEpisodes(db, sqlProfileId)
                recreateFavorites(db, sqlProfileId)
                recreatePlaybackProgress(db, sqlProfileId)
                recreateSyncState(db, sqlProfileId)
                recreateTrendingMedia(db, sqlProfileId)
                recreateHomeTrendingPreviewCache(db, sqlProfileId)
                recreateTmdbContentMapping(db, sqlProfileId)
            }

            private fun recreateCategories(db: SupportSQLiteDatabase, profileId: String) {
                db.execSQL(
                    "CREATE TABLE categories_new (" +
                        "profileId TEXT NOT NULL, id TEXT NOT NULL, type TEXT NOT NULL, name TEXT NOT NULL, " +
                        "PRIMARY KEY(profileId, id, type))",
                )
                db.execSQL("INSERT INTO categories_new SELECT '$profileId', id, type, name FROM categories")
                db.execSQL("DROP TABLE categories")
                db.execSQL("ALTER TABLE categories_new RENAME TO categories")
            }

            private fun recreateLiveStreams(db: SupportSQLiteDatabase, profileId: String) {
                db.execSQL(
                    "CREATE TABLE live_streams_new (" +
                        "profileId TEXT NOT NULL, streamId INTEGER NOT NULL, number INTEGER NOT NULL, name TEXT NOT NULL, " +
                        "categoryId TEXT, logoUrl TEXT, epgChannelId TEXT, directStreamUrl TEXT, source TEXT NOT NULL, " +
                        "PRIMARY KEY(profileId, streamId))",
                )
                db.execSQL(
                    "INSERT INTO live_streams_new SELECT '$profileId', streamId, number, name, categoryId, logoUrl, epgChannelId, directStreamUrl, source FROM live_streams",
                )
                db.execSQL("DROP TABLE live_streams")
                db.execSQL("ALTER TABLE live_streams_new RENAME TO live_streams")
                db.execSQL("CREATE INDEX index_live_streams_profileId_categoryId ON live_streams(profileId, categoryId)")
                db.execSQL("CREATE INDEX index_live_streams_profileId_categoryId_number_name ON live_streams(profileId, categoryId, number, name)")
                db.execSQL("CREATE INDEX index_live_streams_profileId_source ON live_streams(profileId, source)")
            }

            private fun recreateMovies(db: SupportSQLiteDatabase, profileId: String) {
                db.execSQL(
                    "CREATE TABLE movies_new (" +
                        "profileId TEXT NOT NULL, streamId INTEGER NOT NULL, number INTEGER NOT NULL, title TEXT NOT NULL, " +
                        "categoryId TEXT, posterUrl TEXT, year TEXT, genre TEXT, rating TEXT, duration TEXT, plot TEXT, containerExtension TEXT NOT NULL, " +
                        "PRIMARY KEY(profileId, streamId))",
                )
                db.execSQL(
                    "INSERT INTO movies_new SELECT '$profileId', streamId, number, title, categoryId, posterUrl, year, genre, rating, duration, plot, containerExtension FROM movies",
                )
                db.execSQL("DROP TABLE movies")
                db.execSQL("ALTER TABLE movies_new RENAME TO movies")
                db.execSQL("CREATE INDEX index_movies_profileId_categoryId ON movies(profileId, categoryId)")
                db.execSQL("CREATE INDEX index_movies_profileId_categoryId_number_title ON movies(profileId, categoryId, number, title)")
            }

            private fun recreateSeries(db: SupportSQLiteDatabase, profileId: String) {
                db.execSQL(
                    "CREATE TABLE series_new (" +
                        "profileId TEXT NOT NULL, seriesId INTEGER NOT NULL, number INTEGER NOT NULL, title TEXT NOT NULL, " +
                        "categoryId TEXT, posterUrl TEXT, year TEXT, genre TEXT, rating TEXT, seasonsCount INTEGER, plot TEXT, " +
                        "PRIMARY KEY(profileId, seriesId))",
                )
                db.execSQL(
                    "INSERT INTO series_new SELECT '$profileId', seriesId, number, title, categoryId, posterUrl, year, genre, rating, seasonsCount, plot FROM series",
                )
                db.execSQL("DROP TABLE series")
                db.execSQL("ALTER TABLE series_new RENAME TO series")
                db.execSQL("CREATE INDEX index_series_profileId_categoryId ON series(profileId, categoryId)")
                db.execSQL("CREATE INDEX index_series_profileId_categoryId_number_title ON series(profileId, categoryId, number, title)")
            }

            private fun recreateEpisodes(db: SupportSQLiteDatabase, profileId: String) {
                db.execSQL(
                    "CREATE TABLE episodes_new (" +
                        "profileId TEXT NOT NULL, episodeId INTEGER NOT NULL, seriesId INTEGER NOT NULL, seasonNumber INTEGER NOT NULL, " +
                        "episodeNumber INTEGER NOT NULL, title TEXT NOT NULL, containerExtension TEXT NOT NULL, duration TEXT, plot TEXT, " +
                        "PRIMARY KEY(profileId, episodeId))",
                )
                db.execSQL(
                    "INSERT INTO episodes_new SELECT '$profileId', episodeId, seriesId, seasonNumber, episodeNumber, title, containerExtension, duration, plot FROM episodes",
                )
                db.execSQL("DROP TABLE episodes")
                db.execSQL("ALTER TABLE episodes_new RENAME TO episodes")
                db.execSQL("CREATE INDEX index_episodes_profileId_seriesId ON episodes(profileId, seriesId)")
            }

            private fun recreateFavorites(db: SupportSQLiteDatabase, profileId: String) {
                db.execSQL(
                    "CREATE TABLE favorites_new (" +
                        "profileId TEXT NOT NULL, contentType TEXT NOT NULL, contentId TEXT NOT NULL, createdAt INTEGER NOT NULL, " +
                        "PRIMARY KEY(profileId, contentType, contentId))",
                )
                db.execSQL("INSERT INTO favorites_new SELECT '$profileId', contentType, contentId, createdAt FROM favorites")
                db.execSQL("DROP TABLE favorites")
                db.execSQL("ALTER TABLE favorites_new RENAME TO favorites")
            }

            private fun recreatePlaybackProgress(db: SupportSQLiteDatabase, profileId: String) {
                db.execSQL(
                    "CREATE TABLE playback_progress_new (" +
                        "profileId TEXT NOT NULL, contentType TEXT NOT NULL, contentId TEXT NOT NULL, positionMs INTEGER NOT NULL, " +
                        "durationMs INTEGER NOT NULL, updatedAt INTEGER NOT NULL, title TEXT, subtitle TEXT, imageUrl TEXT, parentContentId TEXT, " +
                        "PRIMARY KEY(profileId, contentType, contentId))",
                )
                db.execSQL(
                    "INSERT INTO playback_progress_new SELECT '$profileId', contentType, contentId, positionMs, durationMs, updatedAt, title, subtitle, imageUrl, parentContentId FROM playback_progress",
                )
                db.execSQL("DROP TABLE playback_progress")
                db.execSQL("ALTER TABLE playback_progress_new RENAME TO playback_progress")
            }

            private fun recreateSyncState(db: SupportSQLiteDatabase, profileId: String) {
                db.execSQL(
                    "CREATE TABLE sync_state_new (" +
                        "profileId TEXT NOT NULL, id TEXT NOT NULL, lastSync INTEGER, status TEXT NOT NULL, message TEXT, " +
                        "PRIMARY KEY(profileId, id))",
                )
                db.execSQL("INSERT INTO sync_state_new SELECT '$profileId', id, lastSync, status, message FROM sync_state")
                db.execSQL("DROP TABLE sync_state")
                db.execSQL("ALTER TABLE sync_state_new RENAME TO sync_state")
            }

            private fun recreateTrendingMedia(db: SupportSQLiteDatabase, profileId: String) {
                db.execSQL(
                    "CREATE TABLE trending_media_new (" +
                        "profileId TEXT NOT NULL, contentType TEXT NOT NULL, contentId INTEGER NOT NULL, sampleContentId INTEGER, " +
                        "sampleExtension TEXT, rating REAL NOT NULL, updatedAt INTEGER NOT NULL, " +
                        "PRIMARY KEY(profileId, contentType, contentId))",
                )
                db.execSQL(
                    "INSERT INTO trending_media_new SELECT '$profileId', contentType, contentId, sampleContentId, sampleExtension, rating, updatedAt FROM trending_media",
                )
                db.execSQL("DROP TABLE trending_media")
                db.execSQL("ALTER TABLE trending_media_new RENAME TO trending_media")
                db.execSQL("CREATE INDEX index_trending_media_profileId_contentType_rating ON trending_media(profileId, contentType, rating)")
                db.execSQL("CREATE INDEX index_trending_media_profileId_contentType_updatedAt ON trending_media(profileId, contentType, updatedAt)")
            }

            private fun recreateHomeTrendingPreviewCache(db: SupportSQLiteDatabase, profileId: String) {
                db.execSQL(
                    "CREATE TABLE home_trending_preview_cache_new (" +
                        "profileId TEXT NOT NULL, contentType TEXT NOT NULL, contentId INTEGER NOT NULL, posterUrl TEXT, backdropUrl TEXT, " +
                        "durationLabel TEXT, durationMs INTEGER, previewKind TEXT NOT NULL, previewContentId INTEGER, previewExtension TEXT, " +
                        "trailerKey TEXT, previewStartPositionMs INTEGER NOT NULL, sampleLabel TEXT, backdropState TEXT NOT NULL, " +
                        "previewState TEXT NOT NULL, preparedAt INTEGER NOT NULL, lastSync INTEGER NOT NULL, " +
                        "PRIMARY KEY(profileId, contentType, contentId))",
                )
                db.execSQL(
                    "INSERT INTO home_trending_preview_cache_new SELECT '$profileId', contentType, contentId, posterUrl, backdropUrl, durationLabel, durationMs, previewKind, previewContentId, previewExtension, trailerKey, previewStartPositionMs, sampleLabel, backdropState, previewState, preparedAt, lastSync FROM home_trending_preview_cache",
                )
                db.execSQL("DROP TABLE home_trending_preview_cache")
                db.execSQL("ALTER TABLE home_trending_preview_cache_new RENAME TO home_trending_preview_cache")
                db.execSQL("CREATE INDEX index_home_trending_preview_cache_profileId_contentType_preparedAt ON home_trending_preview_cache(profileId, contentType, preparedAt)")
                db.execSQL("CREATE INDEX index_home_trending_preview_cache_profileId_lastSync ON home_trending_preview_cache(profileId, lastSync)")
            }

            private fun recreateTmdbContentMapping(db: SupportSQLiteDatabase, profileId: String) {
                db.execSQL(
                    "CREATE TABLE tmdb_content_mapping_new (" +
                        "profileId TEXT NOT NULL, contentType TEXT NOT NULL, contentId INTEGER NOT NULL, tmdbId INTEGER, mediaType TEXT NOT NULL, " +
                        "matchedTitle TEXT, originalTitle TEXT, matchedYear TEXT, confidence INTEGER NOT NULL, matchSource TEXT NOT NULL, " +
                        "language TEXT NOT NULL, adult INTEGER NOT NULL, lastError TEXT, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, " +
                        "PRIMARY KEY(profileId, contentType, contentId))",
                )
                db.execSQL(
                    "INSERT INTO tmdb_content_mapping_new SELECT '$profileId', contentType, contentId, tmdbId, mediaType, matchedTitle, originalTitle, matchedYear, confidence, matchSource, language, adult, lastError, createdAt, updatedAt FROM tmdb_content_mapping",
                )
                db.execSQL("DROP TABLE tmdb_content_mapping")
                db.execSQL("ALTER TABLE tmdb_content_mapping_new RENAME TO tmdb_content_mapping")
                db.execSQL("CREATE INDEX index_tmdb_content_mapping_tmdbId ON tmdb_content_mapping(tmdbId)")
                db.execSQL("CREATE INDEX index_tmdb_content_mapping_profileId_contentType_confidence ON tmdb_content_mapping(profileId, contentType, confidence)")
                db.execSQL("CREATE INDEX index_tmdb_content_mapping_profileId_updatedAt ON tmdb_content_mapping(profileId, updatedAt)")
            }
        }

        private fun Migration13To14(context: Context) = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val profileId = context
                    .getSharedPreferences("xtream_accounts", Context.MODE_PRIVATE)
                    .getString("active_playlist_profile_id", null)
                    ?.takeIf { it.isNotBlank() }
                    ?: "default"
                val safeProfileId = profileId.replace("'", "''")
                db.execSQL(
                    "CREATE TABLE youtube_searches_new (profileId TEXT NOT NULL, query TEXT NOT NULL, updatedAt INTEGER NOT NULL, PRIMARY KEY(profileId, query))",
                )
                db.execSQL("INSERT INTO youtube_searches_new SELECT '$safeProfileId', query, updatedAt FROM youtube_searches")
                db.execSQL("DROP TABLE youtube_searches")
                db.execSQL("ALTER TABLE youtube_searches_new RENAME TO youtube_searches")

                db.execSQL(
                    "CREATE TABLE youtube_video_history_new (profileId TEXT NOT NULL, videoId TEXT NOT NULL, title TEXT NOT NULL, channelTitle TEXT NOT NULL, thumbnailUrl TEXT, publishedAt TEXT, channelId TEXT, viewCount INTEGER, durationIso TEXT, durationSeconds INTEGER, categoryId TEXT, tags TEXT, updatedAt INTEGER NOT NULL, PRIMARY KEY(profileId, videoId))",
                )
                db.execSQL(
                    "INSERT INTO youtube_video_history_new SELECT '$safeProfileId', videoId, title, channelTitle, thumbnailUrl, publishedAt, channelId, viewCount, durationIso, durationSeconds, categoryId, tags, updatedAt FROM youtube_video_history",
                )
                db.execSQL("DROP TABLE youtube_video_history")
                db.execSQL("ALTER TABLE youtube_video_history_new RENAME TO youtube_video_history")

                db.execSQL(
                    "CREATE TABLE youtube_selection_new (profileId TEXT NOT NULL, id TEXT NOT NULL, videoId TEXT, updatedAt INTEGER NOT NULL, PRIMARY KEY(profileId, id))",
                )
                db.execSQL("INSERT INTO youtube_selection_new SELECT '$safeProfileId', id, videoId, updatedAt FROM youtube_selection")
                db.execSQL("DROP TABLE youtube_selection")
                db.execSQL("ALTER TABLE youtube_selection_new RENAME TO youtube_selection")
            }
        }

        private val Migration14To15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE movies ADD COLUMN addedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE series ADD COLUMN addedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_movies_profileId_addedAt ON movies(profileId, addedAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_series_profileId_addedAt ON series(profileId, addedAt)")
            }
        }

        private val Migration15To16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS kids_category_decisions (" +
                        "sourceKey TEXT NOT NULL, contentType TEXT NOT NULL, categoryId TEXT NOT NULL, " +
                        "normalizedName TEXT NOT NULL, decision TEXT NOT NULL, score INTEGER NOT NULL, " +
                        "source TEXT NOT NULL, reason TEXT NOT NULL, ruleVersion INTEGER NOT NULL, " +
                        "metadataFingerprint TEXT NOT NULL, updatedAt INTEGER NOT NULL, " +
                        "PRIMARY KEY(sourceKey, contentType, categoryId))",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_kids_category_decisions_sourceKey_contentType_decision " +
                        "ON kids_category_decisions(sourceKey, contentType, decision)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_kids_category_decisions_ruleVersion " +
                        "ON kids_category_decisions(ruleVersion)",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS kids_item_decisions (" +
                        "sourceKey TEXT NOT NULL, contentType TEXT NOT NULL, contentId TEXT NOT NULL, categoryId TEXT, " +
                        "allowed INTEGER NOT NULL, decision TEXT NOT NULL, score INTEGER NOT NULL, source TEXT NOT NULL, " +
                        "reason TEXT NOT NULL, inheritedCategoryId TEXT, ruleVersion INTEGER NOT NULL, " +
                        "metadataFingerprint TEXT NOT NULL, updatedAt INTEGER NOT NULL, " +
                        "PRIMARY KEY(sourceKey, contentType, contentId))",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_kids_item_decisions_sourceKey_contentType_categoryId " +
                        "ON kids_item_decisions(sourceKey, contentType, categoryId)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_kids_item_decisions_sourceKey_contentType_allowed " +
                        "ON kids_item_decisions(sourceKey, contentType, allowed)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_kids_item_decisions_ruleVersion " +
                        "ON kids_item_decisions(ruleVersion)",
                )
            }
        }
    }
}
