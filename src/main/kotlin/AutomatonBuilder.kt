import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.PrintWriter
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

/**
 * Created by nikita on 30.03.18.
 */

const val resourcesDir = "resources/"
const val resultDir = "result/"
const val dataDir = "data/"
const val tmpDir = "tmp/"

fun printProcessOutput(stream: InputStream) {
    val result = ByteArrayOutputStream()
    val buffer = ByteArray(1024)
    var length = stream.read(buffer)
    while (length != -1) {
        result.write(buffer, 0, length)
        length = stream.read(buffer)
    }
    System.err.println(result.toString("UTF-8"))
}

fun isSatisfiable(file: File): Scanner? {
    val scanner = Scanner(file)
    scanner.useDelimiter("\n")
    (1..6).forEach { scanner.nextLine() }
    if (scanner.hasNext("""%  solving CNF \(satisfy\) \.\.\.""")) {
        scanner.nextLine()
    }
    return when (scanner.hasNext("""=====UNSATISFIABLE=====""")) {
        true -> {
            scanner.close()
            null
        }
        else -> scanner
    }
}

fun decode(scanner: Scanner, amount: Int, alphabet: List<Pair<Char, Char>>) =
        Pair(Array(amount) {i ->
            mapOf(*alphabet.map {
                it to Array(amount) {j ->
                    scanner.nextLine() == "delta_${i}_${it.first}_${it.second}_$j = true"
                }
            }.toTypedArray())
        }, Array(amount) {
            scanner.nextLine() == "isFinal_$it = true"
        })

fun find(examples: List<Example>,
         alphabet: List<Pair<Char, Char>>,
         inputAlphabet: List<Char>,
         outputAlphabet: List<Char>,
         flags: Flags): Pair<Array<Map<Pair<Char, Char>, Array<Boolean>>>, Array<Boolean>> {
    var amount = 1
    while (true) {
        var result: Pair<Array<Map<Pair<Char, Char>, Array<Boolean>>>, Array<Boolean>>? = null

        System.err.println("Iteration $amount")

        wrapWithTimer("$amount iteration") {
            val curInput = "input${if (flags.saveAllBEE2BEEP) "$amount" else ""}"
            val curOutput = "output${if (flags.saveAllBEE) "$amount" else ""}"

            val string = generate(examples, alphabet, inputAlphabet, outputAlphabet, amount)
            PrintWriter(Paths.get(curInput).toFile()).use {
                it.write(string)
            }

            val beepp2bee = ProcessBuilder("java", "-jar", resourcesDir + "beepp2bee/jar/beepp2bee.jar", curInput, curOutput).start()
            beepp2bee.waitFor()
            beepp2bee.errorStream.use(::printProcessOutput)
            beepp2bee.destroy()

            val bumble = ProcessBuilder("./BumbleBEE", curOutput).redirectOutput(Paths.get("bumbleOutput").toFile()).start()
            bumble.waitFor()
            bumble.errorStream.use(::printProcessOutput)

            val scanner = isSatisfiable(Paths.get("bumbleOutput").toFile())
            if (scanner != null) {
                result = scanner.use { decode(it, amount, alphabet) }.apply {
                    bumble.destroy()
                    Files.deleteIfExists(Paths.get("input"))
                    Files.deleteIfExists(Paths.get("bumbleOutput"))
                    Files.deleteIfExists(Paths.get("output"))
                }
            }

            bumble.destroy()
        }

        result?.let { return it }

        amount++
    }
}