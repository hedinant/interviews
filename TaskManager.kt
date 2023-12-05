package interview

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.util.Collections
import java.util.LinkedList

// GENERAL  comment Add more comprehensive error handling, especially in the asynchronous code. Handle exceptions that may occur during the execution of coroutines.
// Consider the use of appropriate synchronization mechanisms (e.g., locks) when dealing with shared data structures like the queue, running, and blocked sets.
class TaskManager(
    private val remoteHistory: RemoteHistoryService
) {
    // use companion object in the bottom of the class
    // whay lateinit here, it kind of simulation of Singlton, use Object instead
    companion object {
        lateinit var instance: TaskManager
    }

    //
    private val queue: LinkedList<Task> = LinkedList()
    private val running: HashSet<Task> = hashSetOf()
    private val blocked: HashSet<Task> = hashSetOf()
    private val taskScope = MainScope()

    init {
        instance = this
        launchChecker()
    }

    fun runTask(task: Task) {
        if (Collections.disjoint(task.dependsOn, running)) {
            running.add(task)
            // for data class we can use copy
            task.status = TaskStatus.Working
            taskScope.launch { task.runTask(remoteHistory) }
            // if we use suspend fonction we can have multiple choices how to hadndle exception, and launch
            uploadHistory(
                TaskHistoryRecord(
                    // we can make a constants this magic words "Android" etc and move to companion object
                    platform = "android",
                    record = "task " + task.name + " " + task.schedule + " " +
                            " started " + System.currentTimeMillis()
                )
            )
        } else {
            blocked.add(task)
            // for data class we can use copy
            task.status = TaskStatus.Blocked
            uploadHistory(
                // same here
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
                // same here
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
        for (i in 0 until queue.size) {
            if (task.schedule < queue[i].schedule) {
                queue.add(i, task)
                return
            }
        }
    }

    fun isRunning(name: String): Boolean {
        return running.any { task -> task.name === name }
    }

    private fun launchChecker() {
        taskScope.launch() {
            while (true) {
                while (queue[0].schedule <= System.currentTimeMillis()) {
                    runTask(queue.removeFirst())
                }
            }
        }
    }

// we can use suspend Corotine with Result class
    private fun uploadHistory(record: TaskHistoryRecord) {
        val response = remoteHistory.upload(record)
        if (!response.isSuccessful) Log.e("TaskManager", "Failed to upload history record")
    }
}
