/**
 * Created by nikita on 30.03.18.
 */

fun generate(examples: List<Example>,
             alphabet: List<Pair<Char, Char>>,
             inputAlphabet: List<Char>,
             outputAlphabet: List<Char>,
             amount: Int): String {

    val transactions = Array(amount) {i ->
        mapOf(*alphabet.map {
            it to Array(amount) {j ->
                Var("delta_${i}_${it.first}_${it.second}_$j")
            }
        }.toTypedArray())
    }

    val isFinal = Array(amount, {i -> Var("isFinal_$i")})

    val projection = Array(amount, {i ->
        mapOf(*inputAlphabet.map {
            it to Array(amount) {j ->
                Var("deltaN_${i}_${it}_$j")
            }
        }.toTypedArray())
    })

    val formulas = mutableListOf<CNFBool>()

    // Initial

    for (map in transactions) {
        for ((_, list) in map) {
            for (i in 0 until list.size) {
                for (j in i + 1 until list.size) {
                    formulas.add(Or(Negate(list[i]), Negate(list[j])))
                }
            }
        }
    }

    // AcceptExamples

    val ts = Array(examples.size) {i ->
        Array(examples[i].length + 1) {l ->
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
            val a: Pair<Char, Char> = examples[i][l]
            for (k in 0 until amount) {
                val dis = Array(amount) {j ->
                    val b: CNFBool = transactions[k][a]?.get(j) ?: Zero
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

    // Projection

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
                val dis = Array(outputAlphabet.size) {e ->
                    transactions[i][Pair(a, outputAlphabet[e])]?.get(j) ?: Zero
                }
                val deltaN = projection[i][a]?.get(j) ?: Zero
                formulas.add(Impl(deltaN, Or(*dis)))
            }
        }
    }

    // Unambiguous

    val rs = Array(amount) { Var("r_$it") }

    formulas.add(rs[0])
    for (i in 1 until amount) {
        val dis = Array(amount) {j ->
            Array(inputAlphabet.size) {e ->
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

    // Total

    val sizes = Array(amount + 1) { Math.pow(inputAlphabet.size.toDouble(), it.toDouble()).toInt() }

    val cs = Array(amount + 1, {l ->
        Array(amount, {q ->
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

    return (Var.get() + Integral.get() + formulas.joinToString("\n")).apply {
        Var.clear()
        Integral.clear()
    }
}