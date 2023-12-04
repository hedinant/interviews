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
    companion object {
        lateinit var instance: TaskManager
    }

    private val queue: LinkedList<Task> = LinkedList()
    private val running: HashSet<Task> = hashSetOf()
    private val blocked: HashSet<Task> = hashSetOf()
    // почему мэйн поток?
    private val taskScope = MainScope()

    init {
        // зачем мы в ините меняем instance А если будет создано несколько экземпляров
        // легче проинициализировать при старте приложения один раз
        instance = this
        launchChecker()
    }

    fun runTask(task: Task) {
        // проверить сравнение тасок
        if (Collections.disjoint(task.dependsOn, running)) {
            running.add(task)
            task.status = TaskStatus.Working
            taskScope.launch { task.runTask(remoteHistory) }
            uploadHistory(
                //вынести строки в константы
                TaskHistoryRecord(
                    platform = "android",
                    record = "task " + task.name + " " + task.schedule + " " +
                            " started " + System.currentTimeMillis()
                )
            )
        } else {
            blocked.add(task)
            task.status = TaskStatus.Blocked
            uploadHistory(
                //вынести строки в константы
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
            //вынести строки в константы
            TaskHistoryRecord(
                platform = "android",
                record = "task " + task.name + " " + task.schedule + " " +
                        " finished " + System.currentTimeMillis()
            )
        )

        blocked.filter { task1 -> task1.dependsOn.contains(task) }.forEach { task1 ->
            if (Collections.disjoint(task1.dependsOn, running)) {
                blocked.remove(task1)
                runTask(task1)
            }
        }
    }

    fun skeduleTask(task: Task) {
        //зачем? Зачем вообще тогда использовать шаблон очереди?
        // просто добавлять в конец
        for (i in 0 until queue.size) {
            if (task.schedule < queue[i].schedule) {
                queue.add(i, task)
                return
            }
        }
    }

    fun isRunning(name: String): Boolean {
        // у тасок могут совпадать имена?
        return running.any { task -> task.name === name }
    }

    private fun launchChecker() {
        taskScope.launch() {
            while (true) {
                // заблочить всю очередь таской на будущее
                // отпралять таску в конец очереди если такое случилось
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
