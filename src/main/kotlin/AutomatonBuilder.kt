import java.io.*
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
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

fun isSatisfiableQBF(file: File): Scanner? {
    val scanner = Scanner(file)
    //throw Exception()
    scanner.nextLine()
    return when (scanner.next() == "r") {
        true -> {
            scanner.close()
            null
        }
        else -> scanner
    }
}

fun decode(scanner: Scanner, amount: Int, alphabet: List<Pair<Char, Char>>) =
        Pair(Array(amount) { i ->
            mapOf(*alphabet.map {
                it to Array(amount) { j ->
                    scanner.nextLine() == "delta_${i}_${it.first}_${it.second}_$j = true"
                }
            }.toTypedArray())
        }, Array(amount) {
            scanner.nextLine() == "isFinal_$it = true"
        })

fun decodeQBF(scanner: Scanner,
              amount: Int,
              alphabet: List<Pair<Char, Char>>,
              trans: Map<String, Triple<Int, Pair<Char, Char>, Int>>,
              finals: Map<String, Int>): Pair<Array<Map<Pair<Char, Char>, Array<Boolean>>>, Array<Boolean>> {
    val transactions = Array(amount) {
        mutableMapOf(*alphabet.map {
            it to Array(amount) { false }
        }.toTypedArray())
    }
    val isFinal = Array(amount) { false }

    while (!scanner.hasNext("""0""")) {
        val cur = scanner.next()
        val (number, value) = if (cur.startsWith('-')) Pair(cur.substring(1), false)
        else Pair(cur, true)
        trans[number]?.let { (i, a, j) -> transactions[i][a]?.set(j, value) }
        finals[number]?.let { isFinal[it] = value }
    }

    return Pair(transactions.map { it.toMap() }.toTypedArray(), isFinal)
}


fun find(examples: List<Example>,
         alphabet: List<Pair<Char, Char>>,
         inputAlphabet: List<Char>,
         outputAlphabet: List<Char>,
         flags: Flags): Pair<Array<Map<Pair<Char, Char>, Array<Boolean>>>, Array<Boolean>> {

    if (Files.exists(Paths.get(resultDir))) {
        Files.walkFileTree(Paths.get(resultDir), object: SimpleFileVisitor<Path>() {
            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                Files.delete(file)
                return FileVisitResult.CONTINUE
            }
            override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
                Files.delete(dir)
                return FileVisitResult.CONTINUE
            }
        })
    }
    Files.createDirectory(Paths.get(resultDir))

    var amount = 1
    while (true) {
        var result: Pair<Array<Map<Pair<Char, Char>, Array<Boolean>>>, Array<Boolean>>? = null

        System.err.println("Iteration $amount")

        wrapWithTimer("$amount iteration") {
            val curInput = "$resultDir${if (flags.solveQBF) "qbf" else "beepp"}${if (flags.saveAllBEEPP) "$amount" else ""}"
            val curOutput = "${resultDir}bee${if (flags.saveAllBEE) "$amount" else ""}"

            val (transactions, isFinal, string) = collectFormulas(amount, alphabet, inputAlphabet, outputAlphabet, examples, flags)

            PrintWriter(Paths.get(curInput).toFile()).use {
                it.write(string)
            }

            val processToDestroy = if (!flags.solveQBF) {
                val beepp2bee = ProcessBuilder("java", "-jar", resourcesDir + "beepp2bee/jar/beepp2bee.jar", curInput, curOutput).start()
                beepp2bee.waitFor()
                beepp2bee.errorStream.use(::printProcessOutput)
                beepp2bee.destroy()

                val bumble = ProcessBuilder("./BumbleBEE", curOutput).redirectOutput(Paths.get("processOutput").toFile()).start()
                bumble.waitFor()
                bumble.errorStream.use(::printProcessOutput)

                bumble
            } else {
                val quabs = ProcessBuilder("./quabs-bin/quabs", "--preprocessing", "0", "--partial-assignment", curInput).redirectOutput(Paths.get("processOutput").toFile()).start()
                quabs.waitFor()
                quabs.errorStream.use(::printProcessOutput)

                quabs
            }

            val scanner = (if (!flags.solveQBF) ::isSatisfiable else ::isSatisfiableQBF)(Paths.get("processOutput").toFile())
            if (scanner != null) {
                result = scanner.use {
                    if (!flags.solveQBF) {
                        decode(it, amount, alphabet)
                    } else {
                        val trans = mapOf(*transactions.mapIndexed { i, map ->
                            map.map { (k, v) ->
                                v.mapIndexed { j, p ->
                                    p.ref to Triple(i, k, j)
                                }
                            }.flatten()
                        }.flatten().toTypedArray())

                        val finals = mapOf(*isFinal.mapIndexed { index, p -> p.ref to index }.toTypedArray())

                        decodeQBF(it, amount, alphabet, trans, finals)
                    }
                }.apply {
                    processToDestroy.destroy()
                    Files.deleteIfExists(Paths.get("processOutput"))
                    if (!flags.saveAllBEEPP) {
                        Files.deleteIfExists(Paths.get(curInput))
                    }
                    if (!flags.saveAllBEE) {
                        Files.deleteIfExists(Paths.get(curOutput))
                    }
                }
            }

            processToDestroy.destroy()
        }

        result?.let { return it }

        amount++
    }
}