/**
 * Created by nikita on 30.03.18.
 */

fun generate(examples: List<Example>,
             alphabet: List<Pair<Char, Char>>,
             inputAlphabet: List<Char>,
             outputAlphabet: List<Char>,
             amount: Int,
             transactions: Array<Map<Pair<Char, Char>, Array<Var>>>,
             isFinal: Array<Var>,
             flags: Flags,
             vararg args: MutableList<Var>): MutableList<CNFBool> {

    val projection = Array(amount, {i ->
        mapOf(*inputAlphabet.map {
            it to Array(amount) {j ->
                Var("deltaN_${i}_${it}_$j")
            }
        }.toTypedArray())
    })

    val formulas = mutableListOf<CNFBool>()

    // Initial

    fun generateInitial() {
        for (map in transactions) {
            for ((_, list) in map) {
                for (i in 0 until list.size) {
                    for (j in i + 1 until list.size) {
                        formulas.add(Or(Negate(list[i]), Negate(list[j])))
                        //System.err.println("Nothing")
                    }
                }
            }
        }
    }

    // AcceptExamples

    fun generateAcceptExamples() {
        val ts = Array(examples.size) { i ->
            Array(examples[i].length + 1) { l ->
                Array(amount) {
                    Var("t_${i}_${l}_$it")
                }
            }
        }

        for (i in 0 until examples.size) {
            formulas.add(ts[i][0][0])
        }

        for (i in 0 until examples.size) {
            for (l in 0 until examples[i].length) {
                val a = examples[i][l]
                for (k in 0 until amount) {
                    val dis = Array(amount) { j ->
                        val b = transactions[k][a]?.get(j) ?: Zero
                        And(b, ts[i][l + 1][j])
                    }
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
    }

    // Projection

    fun generateProjection() {
        for (i in 0 until amount) {
            for (j in 0 until amount) {
                for ((a, b) in alphabet) {
                    val delta = transactions[i][Pair(a, b)]?.get(j) ?: Zero
                    val deltaN = projection[i][a]?.get(j) ?: Zero
                    formulas.add(Impl(delta, deltaN))
                }
            }
        }

        for (i in 0 until amount) {
            for (j in 0 until amount) {
                for (a in inputAlphabet) {
                    val dis = Array(outputAlphabet.size) { e ->
                        transactions[i][Pair(a, outputAlphabet[e])]?.get(j) ?: Zero
                    }
                    val deltaN = projection[i][a]?.get(j) ?: Zero
                    formulas.add(Impl(deltaN, Or(*dis)))
                }
            }
        }
    }

    // Unambiguous

    fun generateUnambiguous() {
        val rs = Array(amount) { Var("r_$it") }

        formulas.add(rs[0])
        for (i in 1 until amount) {
            val dis = Array(amount) { j ->
                Array(inputAlphabet.size) { e ->
                    And(rs[j], projection[j][inputAlphabet[e]]?.get(i) ?: Zero)
                }
            }.flatten().toTypedArray()
            formulas.add(Impl(Or(*dis), rs[i]))
        }

        val taus = Array(amount) { i -> Array(i + 1) { Var("tau_${i}_$it") } }

        for (i in 0 until amount) {
            for (a in inputAlphabet) {
                for (j in 0 until amount) {
                    for (k in 0..j) {
                        for (b in outputAlphabet) {
                            for (c in outputAlphabet) {
                                if (b == c || (j == k && b > c)) {
                                    continue
                                }
                                val deltaN1 = transactions[i][Pair(a, b)]?.get(j) ?: Zero
                                val deltaN2 = transactions[i][Pair(a, c)]?.get(k) ?: Zero
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
                            val fDeltaN1 = projection[i][a]?.get(q) ?: Zero
                            val fDeltaN2 = projection[j][a]?.get(w) ?: Zero
                            val sDeltaN1 = projection[i][a]?.get(w) ?: Zero
                            val sDeltaN2 = projection[j][a]?.get(q) ?: Zero
                            formulas.add(Impl(Or(And(taus[i][j], fDeltaN1, fDeltaN2), And(taus[i][j], sDeltaN1, sDeltaN2)), taus[q][w]))
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
    }

    // Total

    fun generateTotal() {
        val sizes = Array(amount + 1) { Math.pow(inputAlphabet.size.toDouble(), it.toDouble()).toInt() }

        val cs = Array(amount + 1, { l ->
            Array(amount, { q ->
                Integral("c_${l}_$q", right = sizes[l])
            })
        })

        val ands = Array(amount) { l ->
            Array(amount) { q ->
                Array(amount) { j ->
                    mapOf(*inputAlphabet.map {
                        it to Integral("and_${l}_${q}_${j}_$it", right = sizes[l])
                    }.toTypedArray())
                }
            }
        }

        for (q in 0 until amount) {
            for (l in 0 until amount) {
                for (j in 0 until amount) {
                    for (a in inputAlphabet) {
                        val deltaN = projection[j][a]?.get(q) ?: Zero
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

        val results = Array(amount + 1) { l ->
            Array(amount) {
                Integral("result_${l}_$it", right = sizes[l])
            }
        }

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
    }

    // Natural unambiguous, total

    fun generateNaturalUnambiguousTotal() {
        fun getBinaryFirst(x: Int): List<Char> {
            val cur = String.format("%16s", Integer.toBinaryString(x)).replace(' ', '0')
            return (cur.dropLast(1).dropWhile { it == '0' } + cur.last()).toList()
        }

        val binInp = getBinaryFirst(inputAlphabet.size - 1)
        val binOut = getBinaryFirst(outputAlphabet.size - 1)
        val binStates = getBinaryFirst(amount - 1)

        fun getBinary(x: Int, length: Int) =
                String.format("%16s", Integer.toBinaryString(x)).replace(' ', '0').takeLast(length).toList()

        fun template(x: List<Var>, y: List<Char>) =
                (x zip y).foldRight<Pair<Var, Char>, CNFBool>(Zero) { (a, b), prev ->
                    when (b) {
                        '1'  -> And(a, prev)
                        else -> Or(a, /*And(Negate(a), */prev/*)*/)
                    }
                }

        fun get(count: Int, bin: List<Char>, prefix: String, storage: MutableList<List<Var>>) =
                Or(*Array(count) { i ->
                    val vars = Array(bin.size) { Var("${prefix}_${i}_$it") }.toList()
                    storage.add(vars)
                    template(vars, bin)
                })

        val length = amount * amount

        val inpMap = mapOf(*inputAlphabet.mapIndexed { index, c -> Pair(c, index) }.toTypedArray())
        val outMap = mapOf(*outputAlphabet.mapIndexed { index, c -> Pair(c, index) }.toTypedArray())

        fun equals(x: List<Var>, y: List<Char>) =
                And(*(x zip y).map { (a, b) ->
                    when (b) {
                        '1' -> a
                        else -> Negate(a)
                    }
                }.toTypedArray())

        val symbolsInp = mutableListOf<List<Var>>()
        val upperBoundInp = get(length, binInp, "x", symbolsInp)
        val existStates = mutableListOf<List<Var>>()
        val upperBoundExistStates = get(length + 1, binStates, "f", existStates)
        val existOutput = mutableListOf<List<Var>>()
        val upperBoundExistOutput = get(length, binOut, "y", existOutput)
        val everyStates = mutableListOf<List<Var>>()
        val upperBoundEveryStates = get(length + 1, binStates, "g", everyStates)
        val everyOutput = mutableListOf<List<Var>>()
        val upperBoundEveryOutput = get(length, binOut, "z", everyOutput)

        val theLength = Array(length + 1) { Var("len_$it") }
        val correctLength = And(Negate(theLength[0]), theLength[length], *(0 until length).map { Impl(theLength[it], theLength[it + 1]) }.toTypedArray())

        fun addAccepter(from: MutableList<List<Var>>, symbolsOut: MutableList<List<Var>>): CNFBool {
            val list = mutableListOf<CNFBool>()
            for (k in 0 until length) {
                list.add(Or(theLength[k], *transactions.mapIndexed { i, map ->
                    map.map { (c, array) ->
                        val (a, b) = c
                        array.mapIndexed { j, p ->
                            And(p, equals(from[k], getBinary(i, binStates.size)),
                                    equals(symbolsInp[k], getBinary(inpMap[a] ?: 0, binInp.size)),
                                    equals(symbolsOut[k], getBinary(outMap[b] ?: 0, binOut.size)),
                                    equals(from[k + 1], getBinary(j, binStates.size)))
                        }
                    }.flatten()
                }.flatten().toTypedArray()))
            }
            for (i in 0 until length) {
                list.add(Or(theLength[i], Negate(theLength[i + 1]), *isFinal.mapIndexed { index, p ->
                    And(p, equals(from[i + 1], getBinary(index, binStates.size)))
                }.toTypedArray()))
            }
            list.add(equals(from[0], getBinary(0, binStates.size)))
            return And(*list.toTypedArray())
        }

        val acceptExist = addAccepter(existStates, existOutput)
        val acceptEvery = addAccepter(everyStates, everyOutput)

        val equalFormula = And(*(existStates zip everyStates).drop(1).mapIndexed { index, (a, b) ->
            (a zip b).map { (c, d) ->
                Or(theLength[index], Equiv(c, d))
            }
        }.flatten().toTypedArray(), *(existOutput zip everyOutput).mapIndexed { index, (a, b) ->
            (a zip b).map { (c, d) ->
                Or(theLength[index], Equiv(c, d))
            }
        }.flatten().toTypedArray())

        val lastFormula = Or(
                Negate(correctLength),
                upperBoundInp,
                And(
                        Negate(upperBoundExistStates),
                        Negate(upperBoundExistOutput),
                        acceptExist,
                        Or(
                                upperBoundEveryStates,
                                upperBoundEveryOutput,
                                Negate(acceptEvery),
                                equalFormula
                        )
                )
        )

        formulas.add(lastFormula)
        formulas.add(isFinal[0])

        args[0].addAll(symbolsInp.flatten())
        args[0].addAll(theLength)
        args[1].addAll(existStates.flatten())
        args[1].addAll(existOutput.flatten())
        args[2].addAll(everyStates.flatten())
        args[2].addAll(everyOutput.flatten())
    }

    generateInitial()
    generateAcceptExamples()
    generateProjection()

    if (flags.solveQBF) {
        generateNaturalUnambiguousTotal()
    } else {
        generateUnambiguous()
        generateTotal()
    }

    return formulas
}
