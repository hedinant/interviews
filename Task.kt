package interview

abstract class Task(
    val name: String
) {
    var status: TaskStatus = TaskStatus.NotStarted
    var schedule: Long = 0
    var dependsOn: Set<Task> = emptySet()

    abstract fun runTask(remoteHistory: RemoteHistoryService)

    protected fun finish() {
        status = TaskStatus.Finished
        TaskManager.instance.taskFinished(this)
    }

    protected fun reschedule(timestamp: Long) {
        schedule = timestamp
        TaskManager.instance.taskFinished(this)
        TaskManager.instance.skeduleTask(this)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Task) return false

        return schedule == other.schedule && name == other.name
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + status.hashCode()
        result = 31 * result + (schedule xor (schedule shr 32)).toInt()
        return result
    }
}