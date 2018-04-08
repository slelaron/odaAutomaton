import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Paths

/**
 * Created by nikita on 05.04.18.
 */

class Flags(flags: Array<String>) {
    val saveAllBEE: Boolean
    val saveAllBEEPP: Boolean
    val saveDotFile: Boolean
    val fileToRead: File?
    val fileToWrite: File?

    init {
        val set = mutableSetOf<String>()
        val map = mutableMapOf<String, String>()
        var pos = 0
        while (pos < flags.size) {
            when (flags[pos]) {
                "-i", "-o" -> {
                    map[flags[pos]] = flags[pos + 1]
                    pos++
                }
                "-bee", "-beepp", "-dot" -> set.add(flags[pos])
                else -> throw Exception("Unknown flag")
            }

            pos++
        }
        saveAllBEE = set.contains("-bee")
        saveAllBEEPP = set.contains("-beepp")
        saveDotFile = set.contains("-dot")
        fileToRead = map["-i"]?.let { Paths.get(dataDir + it).toFile() }
        fileToWrite = map["-o"]?.let { Paths.get(resultDir + it).toFile() }
    }

    val inputStream: InputStream
        get() = fileToRead?.inputStream() ?: System.`in`

    val outputStream: OutputStream
        get() = fileToWrite?.outputStream() ?: System.`out`
}
