import java.io.PrintWriter
import java.util.*

/**
 * Created by nikita on 15.03.18.
 */

data class Example(val input: String, val output: String) {
    init {
        assert(input.length == output.length)
    }
    val length: Int
        get() = input.length

    operator fun get(index: Int) = Pair(input[index], output[index])
}

fun getInputAlphabet(examples: List<Example>) = examples.joinToString("", transform = Example::component1).toSortedSet().toList()
fun getOutputAlphabet(examples: List<Example>) = examples.joinToString("", transform = Example::component2).toSortedSet().toList()

fun main(args: Array<String>) {
    val flags = Flags(args)
    val scanner = Scanner(flags.inputStream)
    val n = scanner.nextInt()
    val examples = Array(n) { Example(scanner.next(), scanner.next()) }.toList()
    val inputAlphabet = getInputAlphabet(examples)
    val outputAlphabet = getOutputAlphabet(examples)

    val alphabet = Array(inputAlphabet.size) { i ->
        Array(outputAlphabet.size) {
            Pair(inputAlphabet[i], outputAlphabet[it])
        }
    }.flatten()

    val (automaton, finals) = wrapWithTimer("Total time") {
        find(examples, alphabet, inputAlphabet, outputAlphabet, flags)
    }

    val listOfEdges = mutableListOf<Edge<Pair<Char, Char>>>()
    automaton.forEachIndexed { i, map ->
        map.forEach {
            it.component2().forEachIndexed { j, b ->
                if (b) {
                    listOfEdges.add(Edge(i, it.component1(), j))
                }
            }
        }
    }

    val listOfFinals = mutableListOf<Int>()
    finals.forEachIndexed { i, b ->
        if (b) {
            listOfFinals.add(i)
        }
    }

    PrintWriter(flags.outputStream).use { printer ->
        printer.print("${automaton.size} ")
        printer.println(listOfEdges.size)

        listOfEdges.forEach {
            printer.println("${it.start + 1} ${it.end + 1} (${it.symbol.first} ${it.symbol.second})")
        }

        printer.println(listOfFinals.size)
        listOfFinals.forEach {
            printer.print("${it + 1} ")
        }
        printer.println()
    }

    System.err.print(checkAutomaton(listOfEdges.toTypedArray(),
            Array(automaton.size) { false }.apply { listOfFinals.forEach { this[it] = true } },
            examples,
            inputAlphabet,
            automaton.size))

    makeGraphVis(listOfEdges,
            Array(automaton.size) { false }.apply { listOfFinals.forEach { this[it] = true } },
            automaton.size,
            flags)
}