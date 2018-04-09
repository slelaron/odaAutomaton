/**
 * Created by nikita on 09.04.18.
 */

fun addBFSbasedPredicates(amount: Int,
                          alphabet: List<Pair<Char, Char>>,
                          transactions: Array<Map<Pair<Char, Char>, Array<Var>>>): MutableList<CNFBool> {
    val formulas = mutableListOf<CNFBool>()

    val tvs = Array(amount) { a -> Array(amount) { Var("tv_${a}_$it") } }

    for (i in 0 until amount) {
        for (j in i + 1 until amount) {
            formulas.add(Equiv(tvs[i][j], Or(*Array(alphabet.size) {
                transactions[i][alphabet[it]]?.get(j) ?: Zero
            })))
        }
    }

    val ps = Array(amount) { a -> Array(amount) { Var("p_${a}_$it") } }

    for (i in 0 until amount) {
        for (j in i + 1 until amount) {
            formulas.add(Equiv(ps[j][i], And(tvs[i][j], *(0 until i).map {
                Negate(tvs[it][j])
            }.toTypedArray())))
        }
    }

    for (i in 1 until amount) {
        formulas.add(Or(*(0 until i).map {
            ps[i][it]
        }.toTypedArray()))
    }

    for (k in 0 until amount - 1) {
        for (i in k + 1 until amount - 1) {
            for (j in i + 1 until amount - 1) {
                formulas.add(Impl(ps[j][i], Negate(ps[j + 1][k])))
            }
        }
    }

    val ms = Array(amount) {i ->
        mapOf(*alphabet.map {
            it to Array(amount) {k ->
                Var("m_${i}_${it.first}_${it.second}_$k")
            }
        }.toTypedArray())
    }

    for (i in 0 until amount) {
        for (j in i + 1 until amount) {
            for (c in 0 until alphabet.size) {
                val now = ms[i][alphabet[c]]?.get(j) ?: Zero
                val minTran = transactions[i][alphabet[c]]?.get(j) ?: Zero
                formulas.add(Equiv(now, And(minTran, *(0 until c).map {
                    Negate(transactions[i][alphabet[it]]?.get(j) ?: Zero)
                }.toTypedArray())))
            }
        }
    }

    for (i in 0 until amount - 1) {
        for (j in i + 1 until amount - 1) {
            for (k in 0 until alphabet.size) {
                for (n in k + 1 until alphabet.size) {
                    val first = ms[i][alphabet[n]]?.get(j) ?: Zero
                    val second = ms[i][alphabet[k]]?.get(j + 1) ?: Zero
                    formulas.add(Impl(And(ps[j][i], ps[j + 1][i], first), Negate(second)))
                }
            }
        }
    }

    return formulas
}