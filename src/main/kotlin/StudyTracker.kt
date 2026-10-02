package studytracker

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.AtomicMoveNotSupportedException
import java.util.Base64
import kotlin.system.exitProcess

data class StudyTask(val id: Int, val title: String, val minutes: Int, val done: Boolean = false) {
    init {
        require(id > 0) { "Task ID must be positive." }
        require(title.isNotBlank() && title.length <= 200) { "Title must contain 1 to 200 characters." }
        require(title.none { it.isISOControl() }) { "Title cannot contain control characters." }
        require(minutes in 1..1440) { "Duration must be between 1 and 1440 minutes." }
    }
}

class TaskRepository(private val file: Path) {
    fun load(): List<StudyTask> {
        if (!Files.exists(file)) return emptyList()
        val lines = Files.readAllLines(file, Charsets.UTF_8)
        require(lines.firstOrNull() == "study-tracker-v1") { "Invalid storage header; original file preserved." }
        val tasks = lines.drop(1).mapIndexed { index, line ->
            try {
                val fields = line.split('\t')
                require(fields.size == 4) { "Expected four fields." }
                val title = String(Base64.getDecoder().decode(fields[1]), Charsets.UTF_8)
                val state = when (fields[3]) { "open" -> false; "done" -> true; else -> error("Invalid status.") }
                StudyTask(fields[0].toInt(), title, fields[2].toInt(), state)
            } catch (e: Exception) { throw IllegalArgumentException("Invalid storage row ${index + 2}: ${e.message}", e) }
        }
        require(tasks.map { it.id }.distinct().size == tasks.size) { "Duplicate task IDs in storage." }
        return tasks
    }

    fun save(tasks: List<StudyTask>) {
        require(tasks.map { it.id }.distinct().size == tasks.size) { "Duplicate task IDs." }
        val target = file.toAbsolutePath()
        Files.createDirectories(target.parent)
        val lines = listOf("study-tracker-v1") + tasks.map {
            val title = Base64.getEncoder().encodeToString(it.title.toByteArray(Charsets.UTF_8))
            "${it.id}\t$title\t${it.minutes}\t${if (it.done) "done" else "open"}"
        }
        val temporary = Files.createTempFile(target.parent, ".study-", ".tmp")
        try {
            Files.write(temporary, lines, Charsets.UTF_8)
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING)
            }
        } finally { Files.deleteIfExists(temporary) }
    }
}

class StudyService(private val repository: TaskRepository) {
    fun tasks() = repository.load()
    fun add(title: String, minutes: Int): StudyTask {
        val tasks = repository.load()
        val next = Math.addExact(tasks.maxOfOrNull { it.id } ?: 0, 1)
        val task = StudyTask(next, title.trim(), minutes)
        repository.save(tasks + task)
        return task
    }
    fun complete(id: Int): StudyTask {
        val tasks = repository.load()
        val task = tasks.find { it.id == id } ?: throw IllegalArgumentException("Task $id does not exist.")
        val completed = task.copy(done = true)
        repository.save(tasks.map { if (it.id == id) completed else it })
        return completed
    }
}

fun execute(arguments: List<String>): String {
    val args = arguments.toMutableList()
    val file = if (args.firstOrNull() == "--file") {
        require(args.size >= 3) { "Expected --file PATH COMMAND." }
        args.removeAt(0)
        Path.of(args.removeAt(0))
    } else Path.of("data", "tasks.tsv")
    val service = StudyService(TaskRepository(file))
    return when (args.firstOrNull()) {
        "add" -> {
            require(args.size == 3) { "Usage: add TITLE MINUTES" }
            val minutes = args[2].toIntOrNull() ?: throw IllegalArgumentException("Minutes must be an integer.")
            val task = service.add(args[1], minutes)
            "Added #${task.id}: ${task.title} (${task.minutes} min)"
        }
        "done" -> {
            require(args.size == 2) { "Usage: done ID" }
            val id = args[1].toIntOrNull() ?: throw IllegalArgumentException("ID must be an integer.")
            "Completed #${service.complete(id).id}"
        }
        "list" -> {
            require(args.size == 1) { "Usage: list" }
            service.tasks().joinToString("\n") { "${if (it.done) "[x]" else "[ ]"} #${it.id} ${it.title} | ${it.minutes} min" }
                .ifEmpty { "No study tasks yet." }
        }
        "stats" -> {
            require(args.size == 1) { "Usage: stats" }
            val tasks = service.tasks()
            val done = tasks.filter { it.done }
            "Tasks: ${done.size}/${tasks.size} completed\nPlanned: ${tasks.sumOf { it.minutes.toLong() }} min\nCompleted planned time: ${done.sumOf { it.minutes.toLong() }} min"
        }
        null, "--help", "help" -> "Study Tracker\nUsage: [--file PATH] add TITLE MINUTES | list | done ID | stats\nLocal, single-user storage. No timer; minutes are planned duration."
        else -> throw IllegalArgumentException("Unknown command. Use --help.")
    }
}

fun main(args: Array<String>) {
    try { println(execute(args.toList())) }
    catch (e: Exception) { System.err.println("Error: ${e.message}"); exitProcess(2) }
}
