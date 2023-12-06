package interview

abstract class Task(
    val name: String
) {
    var status: TaskStatus = TaskStatus.NotStarted
    var schedule: Long = 0 // clear up, rename to scheduledTimeStamp
    var dependsOn: Set<Task> = emptySet()

    abstract fun runTask(remoteHistory: RemoteHistoryService)

    protected fun finish() {
        status = TaskStatus.Finished
        TaskManager.instance.taskFinished(this) // use callback or return result
    }

    protected fun reschedule(timestamp: Long) {
        schedule = timestamp
        TaskManager.instance.taskFinished(this) // use callback or return result
        TaskManager.instance.skeduleTask(this) // use callback or return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Task) return false

        return schedule == other.schedule && name == other.name
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + status.hashCode() // no contract with equals()
        result = 31 * result + (schedule xor (schedule shr 32)).toInt()
        return result
    }
}