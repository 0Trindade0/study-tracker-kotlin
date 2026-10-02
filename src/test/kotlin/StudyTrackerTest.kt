package studytracker

import java.nio.file.Files
import java.util.Comparator

fun main() {
    val directory = Files.createTempDirectory("study-tracker-test-")
    var checks = 0
    fun verify(value: Boolean) { check(value) { "Failed check ${checks + 1}" }; checks++ }
    fun rejects(action: () -> Unit) {
        var rejected = false
        try { action() } catch (_: Exception) { rejected = true }
        verify(rejected)
    }
    try {
        val file = directory.resolve("nested/tasks.tsv")
        val service = StudyService(TaskRepository(file))
        verify(service.tasks().isEmpty())
        verify(service.add("Revisão de grafos", 50).id == 1)
        verify(service.add("Kotlin | collections", 30).id == 2)
        verify(StudyService(TaskRepository(file)).tasks()[0].title == "Revisão de grafos")
        verify(service.complete(1).done)
        verify(service.complete(1).done) // completion is idempotent
        verify(service.tasks().size == 2)
        rejects { service.complete(999) }
        rejects { service.add(" ", 20) }
        rejects { service.add("Invalid", -1) }
        rejects { service.add("Invalid", 1441) }
        rejects { service.add("line\nbreak", 10) }
        val prefix = listOf("--file", file.toString())
        verify(execute(prefix + "stats").contains("Completed planned time: 50 min"))
        verify(execute(prefix + "list").contains("[x] #1"))
        rejects { execute(prefix + listOf("add", "Bad", "NaN")) }
        rejects { execute(prefix + listOf("list", "unexpected")) }
        Files.writeString(file, "corrupted data")
        rejects { service.add("Must not overwrite", 20) }
        verify(Files.readString(file) == "corrupted data")
        Files.writeString(file, "study-tracker-v1\n1\tQQ==\t10\topen\n1\tQg==\t10\tdone\n")
        rejects { service.tasks() }
        println("$checks Kotlin checks passed")
    } finally {
        Files.walk(directory).use { paths -> paths.sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) } }
    }
}
