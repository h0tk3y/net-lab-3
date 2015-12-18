import java.util.*

/**
 * Created by igushs on 12/17/15.
 */

public class Board() {

    public var version = 0L

    val messages = TreeMap<Long, MutableSet<VersionedMessage>>()
    val missedVersions = Collections.synchronizedSet(HashSet<Long>())

    /**
     * @return True if the message is a new one, false if it has already been saved earlier.
     */
    public fun saveMessage(message: VersionedMessage): Boolean {
        version = message.version.coerceAtLeast(version)

        if (message.version in missedVersions)
            missedVersions.remove(message.version)

        val lastVersion = (messages.lastEntry()?.key ?: 0L)
        if (message.version > lastVersion + 1) {
            for (i in lastVersion + 1..message.version - 1)
                missedVersions.add(i)
        }

        val result = messages.getOrPut(message.version) { HashSet() }.add(message)
        return result
    }

    public fun nextVersion(): Long {
        return version + 1
    }
}