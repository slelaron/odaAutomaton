import java.util.*

/**
 * Created by nikita on 30.03.18.
 */

data class Edge<out E>(val start: Int, val symbol: E, val end: Int)

fun checkAutomaton(listOfEdges: Array<Edge<Pair<Char, Char>>>,
                   finals: Array<Boolean>,
                   examples: List<Example>,
                   inputAlphabet: List<Char>,
                   states: Int): String {

    val automaton = Array(states, { mutableMapOf<Pair<Char, Char>, Int>() }).apply {
        listOfEdges.forEach {
            if (this[it.start].contains(it.symbol)) {
                return "Automaton has two ways to go by one symbol"
            }
            this[it.start].put(it.symbol, it.end)
        }
    }

    val projection = Array(states, {cur ->
        mutableMapOf<Char, MutableList<Int>>().apply {
            automaton[cur].forEach {
                getOrPut(it.key.first, { mutableListOf() }).add(it.value)
            }
        }
    })

    // Check AcceptExamples

    fun passExample(now: Int,
                    automaton: Array<MutableMap<Pair<Char, Char>, Int>>,
                    finals: Array<Boolean>,
                    pos: Int, example: Example): Boolean {
        return when (pos) {
            example.length -> finals[now]
            else -> automaton[now][example[pos]]?.let {
                passExample(it, automaton, finals, pos + 1, example)
            } ?: false
        }
    }

    if (examples.any { !passExample(0, automaton, finals, 0, it) }) {
        return "Automaton doesn't pass all examples"
    }

    // Check Unambiguous

    val set = mutableSetOf<Pair<Int, Int>>().apply {
        val reachable = Array(states) { false }.apply {
            val queue: Queue<Int> = LinkedList()
            queue.add(0)
            set(0, true)

            while (!queue.isEmpty()) {
                val a = queue.remove()
                for ((_, next) in automaton[a]) {
                    if (!get(next)) {
                        queue.add(next)
                        this[next] = true
                    }
                }
            }
        }

        val queue: Queue<Pair<Int, Int>> = LinkedList()

        for (a in 0 until states) {
            if (reachable[a]) {
                for ((_, list) in projection[a]) {
                    for (first in 1 until list.size) {
                        for (second in 0 until first) {
                            val pair = if (list[first] <= list[second]) {
                                Pair(list[first], list[second])
                            } else {
                                Pair(list[second], list[first])
                            }
                            add(pair)
                            queue.add(pair)
                        }
                    }
                }
            }
        }

        while (!queue.isEmpty()) {
            val (i, j) = queue.remove()
            for (symbol in inputAlphabet) {
                for (q in projection[i][symbol] ?: emptyList<Int>()) {
                    for (w in projection[j][symbol] ?: emptyList<Int>()) {
                        val pair = if (q <= w) {
                            Pair(q, w)
                        } else {
                            Pair(w, q)
                        }
                        if (!contains(pair)) {
                            add(pair)
                            queue.add(pair)
                        }
                    }
                }
            }
        }
    }

    if (set.any { finals[it.first] && finals[it.second] }) {
        return "Automaton is ambiguous"
    }

    // Check Total

    val dynamic = Array(states + 1) { Array(states, {0}) }.apply {
        this[0][0] = 1
        for (len in 1..states) {
            for (fromWhere in 0 until states) {
                for ((_, list) in projection[fromWhere]) {
                    for (toWhere in list) {
                        this[len][toWhere] += this[len - 1][fromWhere]
                    }
                }
            }
        }
    }

    val amount = Array(states + 1) {len ->
        (0 until states).filter { finals[it] }.map { dynamic[len][it] }.sum()
    }

    if (amount.mapIndexed { index, element ->
        element == Math.pow(inputAlphabet.size.toDouble(), index.toDouble()).toInt() }.any { !it }) {
        return "Automaton isn't total"
    }

    return "Automaton is correct"
}
