/**
 * Created by nikita on 09.04.18.
 */

fun collectFormulas(amount: Int,
                    alphabet: List<Pair<Char, Char>>,
                    inputAlphabet: List<Char>,
                    outputAlphabet: List<Char>,
                    examples: List<Example>,
                    flags: Flags): Triple<Array<Map<Pair<Char, Char>, Array<Var>>>, Array<Var>, String> {
    val transactions = Array(amount) { i ->
        mapOf(*alphabet.map {
            it to Array(amount) { j ->
                Var("delta_${i}_${it.first}_${it.second}_$j")
            }
        }.toTypedArray())
    }

    val isFinal = Array(amount, {i -> Var("isFinal_$i")})

    val predicates = if (flags.addBFSbasedPredicates) addBFSbasedPredicates(amount, alphabet, transactions)
                        else mutableListOf()

    return Triple(transactions, isFinal, if (!flags.solveQBF) {
        val oda = generate(examples, alphabet, inputAlphabet, outputAlphabet, amount, transactions, isFinal, flags)
        Var.get() + Integral.get() + oda.joinToString("\n") + "\n" + predicates.joinToString("\n")
    } else {
        val every1 = mutableListOf<Var>()
        val exist2 = mutableListOf<Var>()
        val every2 = mutableListOf<Var>()

        val oda = generate(examples, alphabet, inputAlphabet, outputAlphabet, amount, transactions, isFinal, flags, every1, exist2, every2)

        Var.list.removeAll(every1)
        Var.list.removeAll(exist2)
        Var.list.removeAll(every2)

        val formula = And(*oda.toTypedArray(), *predicates.toTypedArray())
        val ex1 = "exists(${Var.getQBF()})"
        val ev1 = "forall(${every1.joinToString(", ") { it.ref }})"
        val ex2 = "exists(${exist2.joinToString(", ") { it.ref }})"
        val ev2 = "forall(${every2.joinToString(", ") { it.ref }})"
        val last = CNFBool.nextVal - 1
        "#QCIR-G14 $last\n$ex1\n$ev1\n$ex2\n$ev2\noutput($last)\n${formula.toQBFstring()}"
    }).apply {
        Var.clear()
        Integral.clear()
        Zero.produced = false
    }
}