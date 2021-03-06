package utils

import java.util.*
import kotlin.properties.Delegates

/**
 * Utils used in the project code.
 *
 * Created by igushs on 8/30/2015.
 */

inline fun <T> Iterable<T>.withEach(action: T.() -> Unit) = forEach(action)

fun <T : Any> Iterable<T>.maxBy(comparator: Comparator<in T>): T? {
    var max = firstOrNull()
    for (t in drop(1))
        if (comparator.compare(t, max) >= 0)
            max = t
    return max
}

@Suppress("unused")
fun <T : Any> Iterable<T>.minBy(comparator: Comparator<in T>): T? = maxBy(comparator.reversed())

fun Double.between(a: Double, b: Double) = this <= Math.max(a, b) && this >= Math.min(a, b)

inline fun <T> timed(f: () -> T): Pair<T, Long> {
    val time = System.currentTimeMillis()
    val result = f()
    return Pair(result, System.currentTimeMillis() - time)
}

fun <T> Comparator<in T>.min(t1: T, t2: T) = when (compare(t1, t2)) {
    1 -> t2
    else -> t1
}

fun <T> Comparator<in T>.max(t1: T, t2: T) = reversed().min(t1, t2)

fun <T: Any> later() = Delegates.notNull<T>()

fun <T> T.after(action: (T) -> Unit): T {
    action(this)
    return this
}