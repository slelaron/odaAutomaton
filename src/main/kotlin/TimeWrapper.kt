/**
 * Created by nikita on 04.04.18.
 */

inline fun <R> wrapWithTimer(name: String, func: () -> R): R {
    val startTime = System.currentTimeMillis()
    val result = func()
    val stopTime = System.currentTimeMillis()
    System.err.println("$name: ${(stopTime - startTime).toDouble() / 1000} sec")
    return result
}
