public abstract class Task {
    private String name;
    private TaskStatus status;
    private long schedule;
    private Set<Task> dependsOn;

    public abstract void runTask(EntityManager entityManager);

    protected void finish(){
        status = TaskStatus.Finished;
        TaskManager.getInstance().taskFinished(this);
    }

    protected void reschedule(long timestamp){
        schedule = timestamp;
        TaskManager.getInstance().taskFinished(this);
        TaskManager.getInstance().skeduleTask(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Task)) return false;

        Task task = (Task) o;

        return schedule == task.schedule && name.equals(task.name);
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + status.hashCode();
        result = 31 * result + (int) (schedule ^ (schedule >>> 32));
        return result;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public long getSchedule() {
        return schedule;
    }

    public void setSchedule(long schedule) {
        this.schedule = schedule;
    }

    public Set<Task> getDependsOn() {
        return dependsOn;
    }

    public void setDependsOn(Set<Task> dependsOn) {
        this.dependsOn = dependsOn;
    }
}
