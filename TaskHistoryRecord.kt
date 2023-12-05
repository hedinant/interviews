package interview

// возможно, нужен data class?
class TaskHistoryRecord(
    // зачем здесь var? оно должно изменяться извне?
    var platform: String,
    var record: String
)