/**
 * Created by nikita on 30.03.18.
 */

interface CNFBool {
    fun toQBFinternal(builder: StringBuilder)
    fun toQBFstring(): String {
        val builder = StringBuilder()
        toQBFinternal(builder)
        return "$builder"
    }
    val ref: String
    companion object {
        var nextVal = 1
            private set

        val next: Int
            get() = nextVal++
    }
}

class Var(val name: String): CNFBool {
    override val ref: String
        get() = "$current"

    override fun toQBFinternal(builder: StringBuilder) {}

    private val current = CNFBool.next

    init {
        list.add(this)
    }
    override fun toString() = name
    companion object {
        val list = mutableSetOf<Var>()
        fun get() = list.joinToString("") { "bool ${it.name}\n" }
        fun getQBF() = list.joinToString(", ") { it.ref }
        fun clear() = list.clear()
    }
}

object Zero: CNFBool {
    override val ref: String
        get() = "$current"

    override fun toQBFinternal(builder: StringBuilder) {
        if (!produced) {
            builder.append("$current = or()\n")
            produced = true
        }
    }

    val current: Int = CNFBool.next
    var produced = false

    override fun toString() = "false"
}

class Negate(val of: CNFBool): CNFBool {
    override val ref: String
        get() = (of as? Negate)?.of?.ref ?: "-${of.ref}"

    override fun toQBFinternal(builder: StringBuilder) {
        of.toQBFinternal(builder)
    }

    override fun toString() = "!($of)"
}

abstract class AndOr(vararg args: CNFBool): CNFBool {
    val array: Array<out CNFBool> = args

    abstract val symbol: String
    abstract val symbolQBF: String

    final override fun toString() = array.map {"($it)"}.joinToString(symbol)
    final override fun toQBFinternal(builder: StringBuilder) {
        array.forEach { it.toQBFinternal(builder) }
        builder.append("$ref = $symbolQBF(${array.joinToString(", ") { it.ref }})\n")
    }

    final override val ref: String
        get() = "$current"

    private val current = CNFBool.next
}

class Or(vararg args: CNFBool): AndOr(*args) {
    override val symbol: String
        get() = "|"
    override val symbolQBF: String
        get() = "or"
}

class And(vararg args: CNFBool): AndOr(*args) {
    override val symbol: String
        get() = "&"
    override val symbolQBF: String
        get() = "and"
}

class Impl(val first: CNFBool, val second: CNFBool): CNFBool {
    override fun toQBFinternal(builder: StringBuilder) {
        formula.toQBFinternal(builder)
    }

    override val ref: String
        get() = formula.ref

    private val formula = Or(Negate(first), And(first, second))

    override fun toString() = "($first)->($second)"
}

class Equiv(val first: CNFBool, val second: CNFBool): CNFBool {
    override fun toQBFinternal(builder: StringBuilder) {
        formula.toQBFinternal(builder)
    }

    private val formula = Or(And(Negate(first), Negate(second)), And(first, second))

    override val ref: String
        get() = formula.ref

    override fun toString() = "($first)<=>($second)"
}

abstract class ExistEvery(private val literal: CNFBool, vararg args: Var): CNFBool {
    val array: Array<out CNFBool>
    init {
        array = args
        array.forEach { Var.list.remove(it) }
    }

    abstract val symbol: String

    private val current = CNFBool.next

    override fun toString(): String {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    final override fun toQBFinternal(builder: StringBuilder) {
        array.forEach { it.toQBFinternal(builder) }
        literal.toQBFinternal(builder)
        builder.append("$current = $symbol(${array.joinToString(", ") { it.ref }}; ${literal.ref})\n")
    }

    final override val ref: String
        get() = "$current"
}

class Exist(literal: CNFBool, vararg args: Var): ExistEvery(literal, *args) {
    override val symbol: String
        get() = "exists"
}

class Every(literal: CNFBool, vararg args: Var): ExistEvery(literal, *args) {
    override val symbol: String
        get() = "forall"
}

interface CNFNumber

class Integral(val name: String, left: Int = 0, right: Int = 2000000000): CNFNumber {
    init {
        list.add(Pair(name, Pair(left, right)))
    }
    override fun toString() = name
    companion object {
        val list = mutableSetOf<Pair<String, Pair<Int, Int>>>()
        fun get() = list.joinToString("") {
            "int ${it.first}: ${it.second.first}..${it.second.second}\n"
        }

        fun clear() = list.clear()
    }
}

class Constant(val number: Int): CNFNumber {
    override fun toString() = number.toString()
}

class Sum(vararg args: CNFNumber): CNFNumber {
    val array: Array<out CNFNumber> = args
    override fun toString() = array.joinToString("+") {"($it)"}
}

class Equal(val first: CNFNumber, val second: CNFNumber): CNFBool {
    override fun toQBFinternal(builder: StringBuilder) {}

    override val ref: String
        get() = "0"

    override fun toString() = "($first) = ($second)"
}
