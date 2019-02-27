package fredboat.audio.queue.tbd

import fredboat.audio.player.GuildPlayer
import fredboat.audio.queue.AudioTrackContext
import fredboat.definitions.RepeatMode
import org.bson.types.ObjectId

class QueueManager(val guildPlayer: GuildPlayer) : IQueueHandler {

    private var handler: IQueueHandler = ShuffleableQueueHandler()
    var lastTrack: AudioTrackContext? = null
    var repeat = RepeatMode.OFF
    var roundRobin: Boolean
        get() = handler is RoundRobinQueueHandler
        set(value) {
            if (roundRobin == value) return

            val new = if (value) RoundRobinQueueHandler() else ShuffleableQueueHandler()
            new.addAll(handler.asList.sortChronologically())
            handler = new
        }

    private fun List<AudioTrackContext>.sortChronologically(): List<AudioTrackContext> {
        //TODO
        return this
    }

    fun onSkipped() { lastTrack = null }

    override fun peek() = guildPlayer.playingTrack ?: handler.peek()

    override fun take(): AudioTrackContext? {
        if (repeat == RepeatMode.SINGLE && lastTrack != null) return lastTrack
        if (repeat == RepeatMode.ALL)
        val taken = handler.take()
        if (taken != null) lastTrack = taken
        return taken
    }

    override val asList: List<AudioTrackContext> get() = handler.asList

    override val isEmpty: Boolean get() = handler.isEmpty


    /* Untouched delegations */
    override val durationMillis get() = handler.durationMillis
    override val size get() = handler.size
    override val streamsCount get() = handler.streamsCount
    override var shuffle: Boolean
        get() = handler.shuffle
        set(value) { handler.shuffle = value }

    override fun reshuffle() = handler.reshuffle()
    override fun addAll(tracks: Collection<AudioTrackContext>) = handler.addAll(tracks)
    override fun clear() = handler.clear()
    override fun removeAllById(trackIds: Collection<ObjectId>) = handler.removeAllById(trackIds)
    override fun getTracksInRange(startIndex: Int, endIndex: Int) = handler.getTracksInRange(startIndex, endIndex)
    override fun isUserTrackOwner(userId: Long, trackIds: Collection<ObjectId>) = handler.isUserTrackOwner(userId, trackIds)
}
