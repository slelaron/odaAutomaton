import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.PrintWriter
import java.nio.file.Files
import java.nio.file.Paths
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

interface CNFBool

class Var(val name: String): CNFBool {
    init {
        list.add(name)
    }
    override fun toString() = name
    companion object {
        val list = mutableSetOf<String>()
        fun get() = list.map { "bool $it\n" }.joinToString("")
        fun clear() = list.clear()
    }
}
object One: CNFBool {
    override fun toString() = "true"
}

object Zero: CNFBool {
    override fun toString() = "false"
}

class Negate(val of: CNFBool): CNFBool {
    override fun toString() = "!($of)"
}

class Or(vararg args: CNFBool): CNFBool {
    val array: Array<out CNFBool>
    init {
        array = args
    }
    override fun toString() = array.map {"($it)"}.joinToString("|")
}

class And(vararg args: CNFBool): CNFBool {
    val array: Array<out CNFBool>
    init {
        array = args
    }
    override fun toString() = array.map {"($it)"}.joinToString("&")
}

class Impl(val first: CNFBool, val second: CNFBool): CNFBool {
    override fun toString() = "($first)->($second)"
}

class Equiv(val first: CNFBool, val second: CNFBool): CNFBool {
    override fun toString() = "($first)<=>($second)"
}

interface CNFNumber

class Integral(val name: String, left: Int = 0, right: Int = 2000000000): CNFNumber {
    init {
        list.add(Pair(name, Pair(left, right)))
    }
    override fun toString() = name
    companion object {
        val list = mutableSetOf<Pair<String, Pair<Int, Int>>>()
        fun get() = list.map {
            "int ${it.component1()}: ${it.component2().component1()}..${it.component2().component2()}\n"
        }.joinToString("")

        fun clear() = list.clear()
    }
}

class Constant(val number: Int): CNFNumber {
    override fun toString() = number.toString()
}

class Sum(vararg args: CNFNumber): CNFNumber {
    val array: Array<out CNFNumber>
    init {
        array = args
    }
    override fun toString() = array.map {"($it)"}.joinToString("+")
}

class Equal(val first: CNFNumber, val second: CNFNumber): CNFBool {
    override fun toString() = "($first) = ($second)"
}

fun getInputAlphabet(examples: List<Example>) = setOf(*examples.map(Example::component1).joinToString("").toList().toTypedArray()).toList()
fun getOutputAlphabet(examples: List<Example>) = setOf(*examples.map(Example::component2).joinToString("").toList().toTypedArray()).toList()

fun generate(examples: List<Example>,
             alphabet: List<Pair<Char, Char>>,
             inputAlphabet: List<Char>,
             outputAlphabet: List<Char>,
             amount: Int): String {

    val transactions = Array(amount, {i ->
        mapOf(*alphabet.map {
            it to Array(amount, {j ->
                Var("delta_${i}_${it.component1()}_${it.component2()}_$j")
            })
        }.toTypedArray())
    })

    val isFinal = Array(amount, {i -> Var("isFinal_$i")})

    val projection = Array(amount, {i ->
        mapOf(*inputAlphabet.map {
            it to Array(amount, {j ->
                Var("deltaN_${i}_${it}_$j")
            })
        }.toTypedArray())
    })

    val formulas = mutableListOf<CNFBool>()

    // Initial

    for (map in transactions) {
        for ((key, list) in map) {
            for (i in 0 until list.size) {
                for (j in i + 1 until list.size) {
                    formulas.add(Or(Negate(list[i]), Negate(list[j])))
                }
            }
        }
    }

    // AcceptExamples

    val ts = Array(examples.size, {i ->
        Array(examples[i].length + 1, {l ->
            Array(amount, {
                Var("t_${i}_${l}_$it")
            })
        })
    })

    for (i in 0 until examples.size) {
        formulas.add(ts[i][0][0])
    }

    for (i in 0 until examples.size) {
        for (l in 0 until examples[i].length) {
            val a: Pair<Char, Char> = examples[i][l]
            for (k in 0 until amount) {
                val dis = Array(amount, {j ->
                    val b: CNFBool = transactions[k][a]?.let { it[j] } ?: Zero
                    And(b, ts[i][l + 1][j])
                })
                formulas.add(Impl(ts[i][l][k], Or(*dis)))
            }
        }
    }

    for (i in 0 until examples.size) {
        for (l in 0 until examples[i].length) {
            for (k in 0 until amount) {
                formulas.add(Impl(ts[i][examples[i].length][k], isFinal[k]))
            }
        }
    }

    // Projection

    for (i in 0 until amount) {
        for (j in 0 until amount) {
            for ((a, b) in alphabet) {
                val delta = transactions[i][Pair(a, b)]?.let { it[j] } ?: Zero
                val deltaN = projection[i][a]?.let { it[j] } ?: Zero
                formulas.add(Impl(delta, deltaN))
            }
        }
    }

    for (i in 0 until amount) {
        for (j in 0 until amount) {
            for (a in inputAlphabet) {
                val dis = Array(outputAlphabet.size, {e ->
                    transactions[i][Pair(a, outputAlphabet[e])]?.let { it[j] } ?: Zero
                })
                val deltaN = projection[i][a]?.let { it[j] } ?: Zero
                formulas.add(Impl(deltaN, Or(*dis)))
            }
        }
    }

    // Unambiguous

    val rs = Array(amount, {Var("r_$it")})

    formulas.add(rs[0])
    for (i in 1 until amount) {
        val dis = Array(amount, {j ->
            Array(inputAlphabet.size, {e ->
                projection[i][inputAlphabet[e]]?.let { it[j] } ?: Zero
            })
        }).flatMap { it.asIterable() }.toTypedArray()
        formulas.add(Equiv(rs[i], Or(*dis)))
    }

    val taus = Array(amount, {i -> Array(i + 1, {Var("tau_${i}_$it")})})

    for (i in 0 until amount) {
        for (a in inputAlphabet) {
            for (j in 0 until amount) {
                for (k in 0..j) {
                    for (b in outputAlphabet) {
                        for (c in outputAlphabet) {
                            if (b == c || (j == k && b > c)) {
                                continue
                            }
                            val deltaN1 = transactions[i][Pair(a, b)]?.let { it[j] } ?: Zero
                            val deltaN2 = transactions[i][Pair(a, c)]?.let { it[k] } ?: Zero
                            formulas.add(Impl(And(rs[i], deltaN1, deltaN2), taus[j][k]))
                        }
                    }
                }
            }
        }
    }

    for (i in 0 until amount) {
        for (j in 0..i) {
            for (q in 0 until amount) {
                for (w in 0..q) {
                    for (a in inputAlphabet) {
                        val deltaN1 = projection[i][a]?.let { it[q] } ?: Zero
                        val deltaN2 = projection[j][a]?.let { it[w] } ?: Zero
                        formulas.add(Impl(And(taus[i][j], deltaN1, deltaN2), taus[q][w]))
                    }
                }
            }
        }
    }

    for (i in 0 until amount) {
        for (j in 0..i) {
            formulas.add(Negate(And(taus[i][j], isFinal[i], isFinal[j])))
        }
    }

    // Total

    val sizes = Array(amount + 1, {Math.pow(inputAlphabet.size.toDouble(), it.toDouble()).toInt()})

    val cs = Array(amount + 1, {l ->
        Array(amount, {q ->
            Integral("c_${l}_$q", right = sizes[l])
        })
    })

    val ands = Array(amount, { l ->
        Array(amount, { q ->
            Array(amount, { j ->
                mapOf(*inputAlphabet.map {
                    it to Integral("and_${l}_${q}_${j}_$it", right = sizes[l])
                }.toTypedArray())
            })
        })
    })

    for (q in 0 until amount) {
        for (l in 0 until amount) {
            for (j in 0 until amount) {
                for (a in inputAlphabet) {
                    val deltaN = projection[j][a]?.let { it[q] } ?: Zero
                    val and = ands[l][q][j][a] ?: Constant(0)
                    formulas.add(Impl(deltaN, Equal(and, cs[l][j])))
                    formulas.add(Impl(Negate(deltaN), Equal(and, Constant(0))))
                }
            }
            formulas.add(Equal(cs[l + 1][q], Sum(*ands[l][q].flatMap {
                it.map { it.component2() }.asIterable()
            }.toTypedArray())))
        }
    }

    val results = Array(amount + 1, {l ->
        Array(amount, {
            Integral("result_${l}_$it", right = sizes[l])
        })
    })

    for (l in 0..amount) {
        for (q in 0 until amount) {
            formulas.add(Impl(isFinal[q], Equal(results[l][q], cs[l][q])))
            formulas.add(Impl(Negate(isFinal[q]), Equal(results[l][q], Constant(0))))
        }
        formulas.add(Equal(Constant(sizes[l]), Sum(*results[l])))
    }

    formulas.add(Equal(cs[0][0], Constant(1)))
    for (i in 1 until amount) {
        formulas.add(Equal(cs[0][i], Constant(0)))
    }

    val result = (Var.get() + Integral.get() + formulas.joinToString("\n"))/*.toLowerCase().replace("_", "v")*/
    Var.clear()
    Integral.clear()

    return result
}

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

fun isSatisfiable(stream: InputStream): Scanner? {
    val scanner = Scanner(stream)
    scanner.useDelimiter("\n")
    (1..6).forEach { scanner.nextLine() }
    if (scanner.hasNext("""%  solving CNF \(satisfy\) \.\.\.""")) {
        scanner.nextLine()
    }
    return when (scanner.hasNext("""=====UNSATISFIABLE=====""")) {
        true -> null
        else -> {
            scanner
        }
    }
}

fun decode(scanner: Scanner, amount: Int, alphabet: List<Pair<Char, Char>>) =
    Pair(Array(amount, {i ->
        mapOf(*alphabet.map {
            it to Array(amount, {j ->
                scanner.nextLine() == "delta_${i}_${it.component1()}_${it.component2()}_$j = true"
            })
        }.toTypedArray())
    }), Array(amount, {
        scanner.nextLine() == "isFinal_$it = true"
    }))

fun find(examples: List<Example>,
         alphabet: List<Pair<Char, Char>>,
         inputAlphabet: List<Char>,
         outputAlphabet: List<Char>): Pair<Array<Map<Pair<Char, Char>, Array<Boolean>>>, Array<Boolean>> {
    var amount = 1
    while (true) {
        System.err.println("Stage $amount")

        val string = generate(examples, alphabet, inputAlphabet, outputAlphabet, amount)
        val path = Paths.get("input")
        val writer = PrintWriter(path.toFile())
        writer.write(string)
        writer.flush()

        val beepp2bee = ProcessBuilder("java", "-jar", "beepp2bee/jar/beepp2bee.jar", "input", "output").start()
        beepp2bee.waitFor()
        printProcessOutput(beepp2bee.errorStream)
        beepp2bee.destroy()

        val bumble = ProcessBuilder("./BumbleBEE", "output").start()
        bumble.waitFor()
        printProcessOutput(bumble.errorStream)

        val scanner = isSatisfiable(bumble.inputStream)
        if (scanner != null) {
            bumble.destroy()
            Files.deleteIfExists(Paths.get("input"))
            Files.deleteIfExists(Paths.get("output"))
            return decode(scanner, amount, alphabet)
        }

        bumble.destroy()

        writer.close()
        amount++
    }
}

fun main(args: Array<String>) {
    val scanner = Scanner(System.`in`)
    val n = scanner.nextInt()
    val examples = Array(n, { Example(scanner.next(), scanner.next()) }).toList()
    val inputAlphabet = getInputAlphabet(examples)
    val outputAlphabet = getOutputAlphabet(examples)

    /*println(inputAlphabet.toString())
    println(outputAlphabet.toString())*/

    val alphabet = Array(inputAlphabet.size, {i ->
        Array(outputAlphabet.size, {
            Pair(inputAlphabet[i], outputAlphabet[it])
        })
    }).flatMap { it.asIterable() }

    val (automato, finals) = find(examples, alphabet, inputAlphabet, outputAlphabet)

    automato.forEachIndexed { i, map ->
        map.forEach {
            it.component2().forEachIndexed { j, b ->
                val (c1, c2) = it.component1()
                if (b) {
                    println("$i $j ($c1 $c2)")
                }
            }
        }
    }
    finals.forEachIndexed { i, b ->
        if (b) {
            print("$i ")
        }
    }
}