package fredboat.audio.queue.tbd

import fredboat.audio.queue.AudioTrackContext
import org.bson.types.ObjectId

interface IQueueHandler {

    /**
     * @return an immutable list of all tracks in the queue in the playback order
     */
    val asList: List<AudioTrackContext>

    /**
     * @return true if there are no tracks in the queue
     */
    val isEmpty: Boolean get() = asList.isEmpty()

    /**
     * @return duration of all tracks
     */
    val durationMillis get() = asList.fold(0L) { acc, v -> acc + v.effectiveDuration }

    /**
     * @return amount of tracks in the queue
     */
    val size: Int get() = asList.size

    /**
     * @return amount of live streams
     */
    val streamsCount get() = asList.count { it.track.info.isStream }

    var shuffle: Boolean

    /**
     * @return the track that a call to provideAudioTrack() would return
     */
    fun peek() = asList.firstOrNull()

    /**
     * Removes and returns the next track to be played, or returns null if empty.
     */
    fun take(): AudioTrackContext?
    /**
     * Reshuffle the current queue. Does nothing if shuffle is disabled.
     */
    fun reshuffle()

    /**
     * @param track add a track to the queue
     */
    fun add(track: AudioTrackContext) = addAll(listOf(track))

    /**
     * @param tracks add several tracks to the queue
     */
    fun addAll(tracks: Collection<AudioTrackContext>)

    /**
     * empty the queue
     */
    fun clear()

    /**
     * @param trackIds tracks to be removed from the queue
     */
    fun removeAllById(trackIds: Collection<ObjectId>)

    /**
     * @param track track to be removed
     */
    fun remove(track: AudioTrackContext) = removeAll(listOf(track))

    /**
     * @param tracks tracks to be removed from the queue
     */
    fun removeAll(tracks: Collection<AudioTrackContext>) = removeAllById(tracks.map { it.trackId })

    /**
     * Returns all songs from one index till another in a non-bitching way.
     * That means we will look from the inclusive lower one of the provided two indices to the exclusive higher one.
     * If an index is lower 0 the range will start at 0, and if an index is over the max size of the track list
     * the range will end at the max size of the track list
     *
     * @param startIndex inclusive starting index
     * @param endIndex   exclusive ending index
     * @return the tracks in the given range
     */
    fun getTracksInRange(startIndex: Int, endIndex: Int): List<AudioTrackContext>

    /**
     * @return false if any of the provided tracks was added by user that is not the provided userId
     */
    fun isUserTrackOwner(userId: Long, trackIds: Collection<ObjectId>): Boolean

}