import java.io.PrintWriter
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Created by nikita on 30.03.18.
 */

fun makeGraphVis(listOfEdges: List<Edge<Pair<Char, Char>>>,
                 finals: Array<Boolean>,
                 states: Int,
                 flags: Flags) {
    PrintWriter(Paths.get(resultDir + "graph.dot").toFile()).use {
        it.write("digraph G {\n")
        it.write("vertex_0 [label=\"1\" root=true ${if (finals[0]) "color=\"red\"" else ""}]\n")
        for (id in 1 until states) {
            it.write("vertex_$id [label=\"${id + 1}\" ${if (finals[id]) "color=\"red\"" else ""}]\n")
        }
        for ((start, sym, end) in listOfEdges) {
            it.write("vertex_$start -> vertex_$end [label=\"${sym.first}|${sym.second}\"]\n")
        }
        it.write("}\n")
    }

    val psBuilder = ProcessBuilder("dot", "-Tps", resultDir + "graph.dot", "-o", resultDir + "graph.ps").start()
    psBuilder.waitFor()
    printProcessOutput(psBuilder.errorStream)
    psBuilder.destroy()

    if (!flags.saveDotFile) {
        Files.deleteIfExists(Paths.get(resultDir + "graph.dot"))
    }
}
