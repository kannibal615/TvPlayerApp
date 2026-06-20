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
import com.smartvision.svplayer.data.local.entity.CategoryEntity
import com.smartvision.svplayer.data.local.entity.EpisodeEntity
import com.smartvision.svplayer.data.local.entity.FavoriteEntity
import com.smartvision.svplayer.data.local.entity.LiveStreamEntity
import com.smartvision.svplayer.data.local.entity.MovieEntity
import com.smartvision.svplayer.data.local.entity.PlaybackProgressEntity
import com.smartvision.svplayer.data.local.entity.ProfileEntity
import com.smartvision.svplayer.data.local.entity.SeriesEntity
import com.smartvision.svplayer.data.local.entity.SyncStateEntity

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
    ],
    version = 2,
    exportSchema = true,
)
abstract class SVDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun mediaDao(): MediaDao
    abstract fun profileDao(): ProfileDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun progressDao(): ProgressDao
    abstract fun syncStateDao(): SyncStateDao

    companion object {
        fun build(context: Context): SVDatabase =
            Room.databaseBuilder(context, SVDatabase::class.java, "svplayer.db")
                .addMigrations(Migration1To2)
                .build()

        private val Migration1To2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE playback_progress ADD COLUMN title TEXT")
                db.execSQL("ALTER TABLE playback_progress ADD COLUMN subtitle TEXT")
                db.execSQL("ALTER TABLE playback_progress ADD COLUMN imageUrl TEXT")
                db.execSQL("ALTER TABLE playback_progress ADD COLUMN parentContentId TEXT")
            }
        }
    }
}
