package com.smartvision.svplayer.data.parental

import androidx.room.withTransaction
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.smartvision.svplayer.data.local.SVDatabase
import com.smartvision.svplayer.data.local.dao.ParentalFilterDao
import com.smartvision.svplayer.domain.parental.ParentalCatalogRepository
import com.smartvision.svplayer.domain.parental.ParentalFilterCounts
import com.smartvision.svplayer.domain.parental.ParentalHiddenContentType
import com.smartvision.svplayer.domain.parental.ParentalHiddenFolder
import com.smartvision.svplayer.domain.parental.ParentalHiddenItem
import com.smartvision.svplayer.domain.parental.ParentalKeywordPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.security.MessageDigest

class RoomParentalCatalogRepository(
    private val database: SVDatabase,
) : ParentalCatalogRepository {
    private val dao: ParentalFilterDao = database.parentalFilterDao()
    private val refreshMutex = Mutex()

    override suspend fun counts(profileId: String, keywords: List<String>): ParentalFilterCounts = withContext(Dispatchers.IO) {
        val normalized = ParentalKeywordPolicy.normalizedForMatching(keywords)
        if (normalized.isEmpty()) return@withContext ParentalFilterCounts()
        ensureSnapshot(profileId, normalized)
        coroutineScope {
            val folderCount = async { dao.countSnapshotFolders(profileId) }
            val itemCount = async { dao.countSnapshotItems(profileId) }
            ParentalFilterCounts(folders = folderCount.await(), items = itemCount.await())
        }
    }

    override suspend fun folders(
        profileId: String,
        keywords: List<String>,
        offset: Int,
        limit: Int,
    ): List<ParentalHiddenFolder> = withContext(Dispatchers.IO) {
        val normalized = ParentalKeywordPolicy.normalizedForMatching(keywords)
        if (normalized.isEmpty()) return@withContext emptyList()
        ensureSnapshot(profileId, normalized)
        dao.loadSnapshotFolders(profileId, offset.coerceAtLeast(0), limit.coerceIn(1, 100)).map { row ->
            ParentalHiddenFolder(
                stableKey = "${row.section}:${row.folderId}",
                section = row.section,
                folderId = row.folderId,
                name = row.folderName,
                hiddenCount = row.hiddenCount,
            )
        }
    }

    override suspend fun items(
        profileId: String,
        keywords: List<String>,
        folder: ParentalHiddenFolder,
        offset: Int,
        limit: Int,
    ): List<ParentalHiddenItem> = withContext(Dispatchers.IO) {
        val normalized = ParentalKeywordPolicy.normalizedForMatching(keywords)
        if (normalized.isEmpty()) return@withContext emptyList()
        ensureSnapshot(profileId, normalized)
        dao.loadSnapshotItems(
            profileId = profileId,
            section = folder.section,
            folderId = folder.folderId,
            offset = offset.coerceAtLeast(0),
            limit = limit.coerceIn(1, 100),
        ).map { row ->
            val type = when (row.contentType) {
                "channel" -> ParentalHiddenContentType.Channel
                "movie" -> ParentalHiddenContentType.Movie
                "series" -> ParentalHiddenContentType.Series
                else -> ParentalHiddenContentType.Episode
            }
            ParentalHiddenItem(
                stableKey = "${row.contentType}:${row.contentId}",
                type = type,
                contentId = row.contentId,
                title = row.title,
                folderName = row.folderName,
                imageUrl = row.imageUrl,
                secondaryLabel = row.secondaryLabel,
                duration = row.duration,
            )
        }
    }

    override suspend fun itemCount(
        profileId: String,
        keywords: List<String>,
        folder: ParentalHiddenFolder,
    ): Int = withContext(Dispatchers.IO) {
        val normalized = ParentalKeywordPolicy.normalizedForMatching(keywords)
        if (normalized.isEmpty()) return@withContext 0
        ensureSnapshot(profileId, normalized)
        dao.countSnapshotItems(profileId, folder.section, folder.folderId)
    }

    override suspend fun hiddenStableKeys(profileId: String, keywords: List<String>): Set<String> = withContext(Dispatchers.IO) {
        val normalized = ParentalKeywordPolicy.normalizedForMatching(keywords)
        if (normalized.isEmpty()) return@withContext emptySet()
        ensureSnapshot(profileId, normalized)
        dao.loadSnapshotStableKeys(profileId).toSet()
    }

    override suspend fun deleteProfileSnapshot(profileId: String) = withContext(Dispatchers.IO) {
        database.withTransaction {
            dao.deleteItemsByProfile(profileId)
            dao.deleteSnapshotByProfile(profileId)
        }
    }

    private suspend fun ensureSnapshot(profileId: String, normalizedKeywords: List<String>) {
        val fingerprint = normalizedKeywords.fingerprint()
        val catalogLastSync = database.syncStateDao().get(profileId)?.lastSync ?: 0L
        val current = dao.getSnapshot(profileId)
        if (current?.keywordFingerprint == fingerprint && current.catalogLastSync == catalogLastSync) return

        refreshMutex.withLock {
            val latest = dao.getSnapshot(profileId)
            if (latest?.keywordFingerprint == fingerprint && latest.catalogLastSync == catalogLastSync) return@withLock
            val statement = ParentalFilterQueryBuilder.materialize(profileId, normalizedKeywords)
            database.withTransaction {
                dao.deleteItemsByProfile(profileId)
                database.openHelper.writableDatabase.execSQL(statement.sql, statement.args)
                database.openHelper.writableDatabase.execSQL(
                    "INSERT OR REPLACE INTO parental_filter_snapshots " +
                        "(profileId, keywordFingerprint, catalogLastSync, generatedAt) VALUES (?, ?, ?, ?)",
                    arrayOf(profileId, fingerprint, catalogLastSync, System.currentTimeMillis()),
                )
            }
        }
    }
}

private fun List<String>.fingerprint(): String {
    val digest = MessageDigest.getInstance("SHA-256")
        .digest(joinToString("\u0000").toByteArray(Charsets.UTF_8))
    return digest.joinToString("") { byte -> "%02x".format(byte) }
}

internal object ParentalFilterQueryBuilder {
    internal data class SqlStatement(val sql: String, val args: Array<Any>)

    fun materialize(profileId: String, keywords: List<String>): SqlStatement {
        require(keywords.isNotEmpty())
        val statement = statement(
            profileId = profileId,
            keywords = keywords,
            select = "INSERT OR REPLACE INTO parental_hidden_items " +
                "(profileId, section, folderId, folderName, contentType, contentId, title, imageUrl, secondaryLabel, duration) " +
                "SELECT ?, section, folderId, folderName, contentType, contentId, title, imageUrl, secondaryLabel, duration FROM hidden",
            trailingArgs = arrayOf(profileId),
        )
        return SqlStatement(statement.first, statement.second)
    }

    fun folderCount(profileId: String, keywords: List<String>): SupportSQLiteQuery =
        query(profileId, keywords, "SELECT COUNT(*) FROM (SELECT section, folderId FROM hidden GROUP BY section, folderId)")

    fun itemCount(profileId: String, keywords: List<String>): SupportSQLiteQuery =
        query(profileId, keywords, "SELECT COUNT(*) FROM hidden")

    fun folders(profileId: String, keywords: List<String>, offset: Int, limit: Int): SupportSQLiteQuery =
        query(
            profileId,
            keywords,
            "SELECT section, folderId, folderName, COUNT(*) AS hiddenCount FROM hidden " +
                "GROUP BY section, folderId, folderName ORDER BY section, folderName LIMIT ? OFFSET ?",
            limit.coerceIn(1, 100),
            offset.coerceAtLeast(0),
        )

    fun items(profileId: String, keywords: List<String>, offset: Int, limit: Int): SupportSQLiteQuery =
        query(
            profileId,
            keywords,
            "SELECT contentType, contentId, title, folderName, imageUrl, secondaryLabel, duration FROM hidden " +
                "ORDER BY section, folderName, title, contentType, contentId LIMIT ? OFFSET ?",
            limit.coerceIn(1, 100),
            offset.coerceAtLeast(0),
        )

    private fun query(
        profileId: String,
        keywords: List<String>,
        select: String,
        vararg trailingArgs: Any,
    ): SupportSQLiteQuery {
        val statement = statement(profileId, keywords, select, trailingArgs)
        return SimpleSQLiteQuery(statement.first, statement.second)
    }

    private fun statement(
        profileId: String,
        keywords: List<String>,
        select: String,
        trailingArgs: Array<out Any>,
    ): Pair<String, Array<Any>> {
        require(keywords.isNotEmpty())
        val values = keywords.joinToString(",") { "(?)" }
        val sql = """
            WITH keyword(value) AS (VALUES $values),
            hidden AS (
                SELECT 'live' AS section,
                       COALESCE(l.categoryId, '__none__') AS folderId,
                       COALESCE(c.name, 'Live TV') AS folderName,
                       'channel' AS contentType,
                       CAST(l.streamId AS TEXT) AS contentId,
                       l.name AS title,
                       l.logoUrl AS imageUrl,
                       COALESCE(c.name, 'Live TV') AS secondaryLabel,
                       NULL AS duration
                FROM live_streams l
                LEFT JOIN categories c ON c.profileId = l.profileId AND c.id = l.categoryId AND c.type = 'live'
                WHERE l.profileId = ? AND EXISTS (
                    SELECT 1 FROM keyword k
                    WHERE INSTR(LOWER(COALESCE(l.name, '') || ' ' || COALESCE(c.name, '')), k.value) > 0
                )
                UNION ALL
                SELECT 'movies', COALESCE(m.categoryId, '__none__'), COALESCE(c.name, 'Movies'),
                       'movie', CAST(m.streamId AS TEXT), m.title, m.posterUrl,
                       TRIM(COALESCE(m.genre, '') || CASE WHEN COALESCE(m.year, '') = '' THEN '' ELSE ' · ' || m.year END),
                       m.duration
                FROM movies m
                LEFT JOIN categories c ON c.profileId = m.profileId AND c.id = m.categoryId AND c.type = 'movies'
                WHERE m.profileId = ? AND EXISTS (
                    SELECT 1 FROM keyword k
                    WHERE INSTR(LOWER(COALESCE(m.title, '') || ' ' || COALESCE(m.plot, '') || ' ' || COALESCE(m.genre, '') || ' ' || COALESCE(c.name, '')), k.value) > 0
                )
                UNION ALL
                SELECT 'series', COALESCE(s.categoryId, '__none__'), COALESCE(c.name, 'Series'),
                       'series', CAST(s.seriesId AS TEXT), s.title, s.posterUrl,
                       TRIM(COALESCE(s.genre, '') || CASE WHEN s.seasonsCount IS NULL THEN '' ELSE ' · ' || s.seasonsCount || ' seasons' END),
                       NULL
                FROM series s
                LEFT JOIN categories c ON c.profileId = s.profileId AND c.id = s.categoryId AND c.type = 'series'
                WHERE s.profileId = ? AND EXISTS (
                    SELECT 1 FROM keyword k
                    WHERE INSTR(LOWER(COALESCE(s.title, '') || ' ' || COALESCE(s.plot, '') || ' ' || COALESCE(s.genre, '') || ' ' || COALESCE(c.name, '')), k.value) > 0
                )
                UNION ALL
                SELECT 'series', COALESCE(s.categoryId, '__none__'), COALESCE(c.name, 'Series'),
                       'episode', CAST(e.episodeId AS TEXT), e.title, s.posterUrl,
                       printf('S%02dE%02d', e.seasonNumber, e.episodeNumber) || CASE WHEN COALESCE(s.title, '') = '' THEN '' ELSE ' · ' || s.title END,
                       e.duration
                FROM episodes e
                LEFT JOIN series s ON s.profileId = e.profileId AND s.seriesId = e.seriesId
                LEFT JOIN categories c ON c.profileId = s.profileId AND c.id = s.categoryId AND c.type = 'series'
                WHERE e.profileId = ? AND EXISTS (
                    SELECT 1 FROM keyword k
                    WHERE INSTR(LOWER(COALESCE(e.title, '') || ' ' || COALESCE(e.plot, '') || ' ' || COALESCE(s.title, '') || ' ' || COALESCE(s.plot, '') || ' ' || COALESCE(s.genre, '') || ' ' || COALESCE(c.name, '')), k.value) > 0
                )
            )
            $select
        """.trimIndent()
        val args = buildList<Any> {
            addAll(keywords)
            repeat(4) { add(profileId) }
            addAll(trailingArgs)
        }
        return sql to args.toTypedArray()
    }
}
