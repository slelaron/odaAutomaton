/**
 * Created by nikita on 09.04.18.
 */

fun collectFormulas(amount: Int,
                    alphabet: List<Pair<Char, Char>>,
                    inputAlphabet: List<Char>,
                    outputAlphabet: List<Char>,
                    examples: List<Example>,
                    flags: Flags): String {
    val transactions = Array(amount) {i ->
        mapOf(*alphabet.map {
            it to Array(amount) {j ->
                Var("delta_${i}_${it.first}_${it.second}_$j")
            }
        }.toTypedArray())
    }

    val oda = generate(examples, alphabet, inputAlphabet, outputAlphabet, amount, transactions)
    val predicates = if (flags.addBFSbasedPredicates) addBFSbasedPredicates(amount, alphabet, transactions)
                        else mutableListOf()

    return (Var.get() + Integral.get() + oda.joinToString("\n") + "\n" + predicates.joinToString("\n")).apply {
        Var.clear()
        Integral.clear()
    }
}