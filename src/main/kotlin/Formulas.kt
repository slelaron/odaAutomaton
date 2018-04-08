/**
 * Created by nikita on 30.03.18.
 */

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
            "int ${it.first}: ${it.second.first}..${it.second.second}\n"
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
