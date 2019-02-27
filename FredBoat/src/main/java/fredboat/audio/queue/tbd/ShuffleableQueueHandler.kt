package fredboat.audio.queue.tbd

import fredboat.audio.queue.AudioTrackContext
import org.bson.types.ObjectId
import java.util.concurrent.ConcurrentLinkedDeque

class ShuffleableQueueHandler : IQueueHandler {

    private val queue: ConcurrentLinkedDeque<AudioTrackContext> = ConcurrentLinkedDeque()
    private val cachedList: MutableList<AudioTrackContext>? = mutableListOf()

    private var _shuffle = false
    override var shuffle: Boolean
        get() = _shuffle
        set(value) {
            _shuffle = value
            reshuffle()
        }

    override val asList: List<AudioTrackContext>
        get() {
            if (!shuffle) {
                return queue.toList()
            }
            
            if (!cachedList.isEmpty()) {
                return cachedList
            }
            
            val newList = ArrayList<AudioTrackContext>(queue.toList())
            newList.sort()

            val size = newList.size
            for ((i, atc) in newList.withIndex()) {
                val rand = ((i / (size + 1.0) + 1.0 / (size + 1.0)) * Integer.MAX_VALUE).toInt()
                atc.rand = if (atc.isPriority) Integer.MIN_VALUE else rand
            }
            cachedList.addAll(newList)
            
            return newList
        }

    override fun reshuffle() {
        cachedList.clear()
    }

    override fun take(): AudioTrackContext? {
        try {
            val atc = asList.firstOrNull()
            if (atc != null)
                remove(atc)

            return atc
        } finally {
            reshuffle()
        }
    }

    override fun addAll(tracks: Collection<AudioTrackContext>) {
        if (tracks.any { it.isPriority })
            tracks.reversed().forEach { queue.addFirst(it) }
        else
            queue.addAll(tracks)
    }

    override fun clear() = queue.clear()

    override fun removeAllById(trackIds: Collection<ObjectId>) {
        queue.removeIf { trackIds.contains(it.trackId) }
    }

    override fun getTracksInRange(startIndex: Int, endIndex: Int): List<AudioTrackContext> {
        return queue.filterIndexed { i, _ -> i in startIndex..endIndex }
    }

    override fun isUserTrackOwner(userId: Long, trackIds: Collection<ObjectId>): Boolean {
        return queue.filter { it.userId == userId }.all { trackIds.contains(it.trackId) }
    }
}