package interview

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.util.Collections
import java.util.LinkedList

class TaskManager(
    private val remoteHistory: RemoteHistoryService // inject
) {
    companion object { // just use object TaskManager or DI
        lateinit var instance: TaskManager
    }

    private val queue: LinkedList<Task> = LinkedList()
    private val running: HashSet<Task> = hashSetOf()
    private val blocked: HashSet<Task> = hashSetOf()
    private val taskScope = MainScope() // check if the scope is correct i.e. background

    init {
        instance = this
        launchChecker()
    }

    fun runTask(task: Task) { // private?
        if (Collections.disjoint(task.dependsOn, running)) {
            running.add(task)
            task.status = TaskStatus.Working
            taskScope.launch { task.runTask(remoteHistory) }
            uploadHistory(
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
                TaskHistoryRecord(
                    platform = "android",
                    record = "task " + task.name + " " + task.schedule + " " +
                            " blocked " + System.currentTimeMillis()
                )
            )
        }
    }

    fun taskFinished(task: Task) { // onTaskFinished hide from public
        running.remove(task)
        uploadHistory(
            TaskHistoryRecord(
                platform = "android",
                record = "task " + task.name + " " + task.schedule + " " +
                        " finished " + System.currentTimeMillis()
            )
        )

        blocked.filter { task1 -> task1.dependsOn.contains(task) }.forEach { task1 -> // blockedTask, dependantTask
            // remove task from dependsOn
            if (Collections.disjoint(task1.dependsOn, running)) { // consider removing because it duplicates in runTask
                blocked.remove(task1)
                runTask(task1)
            }
        }
    }

    fun skeduleTask(task: Task) { // typo schedule
        for (i in 0 until queue.size) { // use iterator, insert without index
            if (task.schedule < queue[i].schedule) { // to slow on LinkedList
                queue.add(i, task)
                return
            }
        }
    }

    fun isRunning(name: String): Boolean {
        return running.any { task -> task.name === name } // consider using redundant collection if there's need to optimize
    }

    private fun launchChecker() {
        taskScope.launch() {
            while (true) {
                while (queue[0].schedule <= System.currentTimeMillis()) { // re-check comparison or rename schedule field
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
