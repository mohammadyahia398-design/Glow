package com.glow.music.data.model

import android.net.Uri
import androidx.compose.runtime.Immutable

/**
 * Glow — Luminous Minimalism
 *
 * Core music playlist model.
 *
 * Represents user-created and application-managed playlists.
 *
 * This model contains playlist metadata and track snapshots only.
 * Playback state belongs to the playback layer.
 *
 * Notes:
 * - [tracks] preserves the playlist's current track order and may contain
 *   the same track more than once.
 * - [trackIds] provides track references in playlist order. It is computed
 *   once per instance on first access and cached, which is safe because
 *   instances are immutable.
 * - [createdAt] defaults to the current time. Persistence and mapping code
 *   should pass timestamps explicitly rather than rely on the defaults.
 * - Instances are immutable; use [copy] to produce updated versions, and
 *   pass a new [updatedAt] value when the playlist content changes.
 */
@Immutable
data class GlowPlaylist(
    val id: String,
    val name: String,
    val description: String? = null,
    val tracks: List<GlowTrack> = emptyList(),
    val artworkUri: Uri? = null,
    val isSystemPlaylist: Boolean = false,
    val isPinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt
) {
    init {
        require(id.isNotBlank()) { "Playlist ID must not be blank." }
        require(createdAt >= 0L) { "Playlist creation timestamp cannot be negative." }
        require(updatedAt >= createdAt) { "Playlist update timestamp cannot precede creation." }
    }

    val displayName: String
        get() = name.ifBlank { UNTITLED_PLAYLIST }

    val hasDescription: Boolean
        get() = !description.isNullOrBlank()

    val trackCount: Int
        get() = tracks.size

    /**
     * Track IDs in playlist order. Declared in the class body, so it is not
     * part of equals/hashCode/copy; computed lazily and cached per instance.
     */
    val trackIds: List<String> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        tracks.map { it.id }
    }

    val hasTracks: Boolean
        get() = tracks.isNotEmpty()

    val hasArtwork: Boolean
        get() = artworkUri != null

    val isEditable: Boolean
        get() = !isSystemPlaylist

    private companion object {
        const val UNTITLED_PLAYLIST = "Untitled Playlist"
    }
}
