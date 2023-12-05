package interview

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.util.Collections
import java.util.LinkedList

class TaskManager(
    private val remoteHistory: RemoteHistoryService
) {
    // companion object должен находиться в конце класса согласно kotlin code style
    companion object {
        // должен быть private set, чтобы нельзя было изменить instance извне класса
        // этот instance - singleton? важно, чтобы он создавался один раз извне (с помощью DI или как-то иначе)
        lateinit var instance: TaskManager
    }

    private val queue: LinkedList<Task> = LinkedList()
    // важно, чтобы был именно HashSet? может заменить на mutableSet?
    private val running: HashSet<Task> = hashSetOf()
    private val blocked: HashSet<Task> = hashSetOf()
    // почему MainScope? точно должен выполняться в главном потоке и в одном потоке? не нужны никакие SupervisorJob и т.п.?
    private val taskScope = MainScope()

    init {
        instance = this
        launchChecker()
    }

    fun runTask(task: Task) {
        if (Collections.disjoint(task.dependsOn, running)) {
            running.add(task)
            task.status = TaskStatus.Working
            taskScope.launch { task.runTask(remoteHistory) }
            uploadHistory(
                TaskHistoryRecord(
                    platform = "android",
                    // может стоит использовать string interpolation?
                    record = "task " + task.name + " " + task.schedule + " " +
                            " started " + System.currentTimeMillis()
                )
            )
        } else {
            blocked.add(task)
            task.status = TaskStatus.Blocked
            uploadHistory(
                TaskHistoryRecord(
                    platform = "android",
                    record = "task " + task.name + " " + task.schedule + " " +
                            " blocked " + System.currentTimeMillis()
                )
            )
        }
    }

    fun taskFinished(task: Task) {
        running.remove(task)
        uploadHistory(
            TaskHistoryRecord(
                platform = "android",
                record = "task " + task.name + " " + task.schedule + " " +
                        " finished " + System.currentTimeMillis()
            )
        )

        // вместо task1 возможно стоит использовать it для лаконичности в filter
        blocked.filter { task1 -> task1.dependsOn.contains(task) }.forEach { task1 ->
            if (Collections.disjoint(task1.dependsOn, running)) {
                blocked.remove(task1)
                runTask(task1)
            }
        }
    }

    fun skeduleTask(task: Task) {
        // в случае с LinkedList лучше использовать iterator, а не обращаться к элементам по индексу
        for (i in 0 until queue.size) {
            if (task.schedule < queue[i].schedule) {
                queue.add(i, task)
                return
            }
        }
    }

    fun isRunning(name: String): Boolean {
        // для лаконичности стоит использовать it вместо task
        // зачем сравнение по === вместо ==?
        return running.any { task -> task.name === name }
    }

    private fun launchChecker() {
        // зачем () тут?
        taskScope.launch() {
            while (true) {
                // не нужен никакой delay?
                // а если queue пустая окажется?
                while (queue[0].schedule <= System.currentTimeMillis()) {
                    runTask(queue.removeFirst())
                }
            }
        }
    }

    private fun uploadHistory(record: TaskHistoryRecord) {
        val response = remoteHistory.upload(record)
        if (!response.isSuccessful) Log.e("TaskManager", "Failed to upload history record")
    }
}
