package fredboat.audio.queue.tbd

import fredboat.audio.queue.AudioTrackContext
import org.bson.types.ObjectId
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

class RoundRobinQueueHandler : IQueueHandler {

    private val queues: ConcurrentMap<Long, ShuffleableQueueHandler> = ConcurrentHashMap()
    private var cachedList: MutableList<AudioTrackContext>? = mutableListOf()
    private var usersPlayedThisRound = mutableSetOf<Long>()

    override val asList: List<AudioTrackContext>
        get() = cachedList ?: buildNewList()

    private fun buildNewList(): List<AudioTrackContext> {
        val userQueues = queues.map { it.value.asList.toMutableList() }

        val newQueue = mutableListOf<AudioTrackContext>()
        for (pendingUser in getPendingUsers()) {
            val personalQueue = userQueues.find { it.firstOrNull()?.userId == pendingUser } ?: continue
            newQueue.add(personalQueue.removeAt(0))
        }

        val maxSize: Int = userQueues.maxBy { it.size }?.size ?: 0
        for (i in 0..maxSize) {
            for (userQueue in userQueues) {
                userQueue.getOrNull(i)?.let { newQueue.add(it) }
            }
        }

        cachedList = newQueue

        return newQueue
    }

    override var shuffle: Boolean = queues.any { it.value.shuffle }

    // TODO remove from real queue
    override fun take() = asList.firstOrNull()?.also {
        usersPlayedThisRound.add(it.userId)
        if (isRoundCompleted()) usersPlayedThisRound.clear()
    }

    override fun reshuffle() = queues.values.forEach { it.reshuffle() }

    override fun addAll(tracks: Collection<AudioTrackContext>) = tracks.forEach {
        queues.getOrPut(it.userId) { ShuffleableQueueHandler() }
                .add(it)
    }

    override fun clear() = queues.clear()

    override fun removeAllById(trackIds: Collection<ObjectId>) = queues.values.forEach { it.removeAllById(trackIds) }

    override fun getTracksInRange(startIndex: Int, endIndex: Int): List<AudioTrackContext> = queues.values.flatMap {
        it.getTracksInRange(startIndex, endIndex)
    }

    override fun isUserTrackOwner(userId: Long, trackIds: Collection<ObjectId>): Boolean = queues[userId]?.isUserTrackOwner(userId, trackIds)?: false

    private fun getPendingUsers(): Set<Long> {
        val missing = queues.keys
        missing.removeAll(usersPlayedThisRound)
        missing.filter {
            val queue = queues[it] ?: return@filter false
            !queue.isEmpty
        }
        return missing
    }

    private fun isRoundCompleted() = getPendingUsers().isEmpty()
}