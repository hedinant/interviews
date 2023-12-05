package interview

abstract class Task(
    val name: String
) {
    // должны ли быть эти переменные публичными для изменения?
    var status: TaskStatus = TaskStatus.NotStarted
    var schedule: Long = 0
    // это не должно быть mutableSet для приватного поля и set для публичного?
    var dependsOn: Set<Task> = emptySet()

    abstract fun runTask(remoteHistory: RemoteHistoryService)

    // кто должен быть ответственным за вызов finish? разве не TaskManager?
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
        // а если other == null?
        if (this === other) return true
        if (other !is Task) return false

        // а проверка status и dependsOn?
        return schedule == other.schedule && name == other.name
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + status.hashCode()
        // точно нужно использовать shr и xor?
        result = 31 * result + (schedule xor (schedule shr 32)).toInt()
        return result
    }
}