@Component
public class TaskManager {
    @PersistenceContext
    private EntityManager entityManager;
    private static TaskManager instance;
    private LinkedList<Task> queue = new LinkedList<>();
    private HashSet<Task> running = new HashSet<>();
    private HashSet<Task> blocked = new HashSet<>();
    private Thread checker = new Thread(() -> {
        while (true) {
            checkRun();
        }
    });

    public TaskManager() {
        instance = this;
        checker.run();
    }

    public static TaskManager getInstance() {
        return instance;
    }

    @Transactional
    public void runTask(Task task) {
        if (Collections.disjoint(task.getDependsOn(), running)) {
            running.add(task);
            task.setStatus(TaskStatus.Working);
            new Thread(() -> task.runTask(entityManager)).run();
            entityManager.persist(new TaskHistoryRecord("task " + task.getName() + " " + task.getSchedule() + " " +
                    " started " + System.currentTimeMillis()));
        } else {
            blocked.add(task);
            task.setStatus(TaskStatus.Blocked);
            entityManager.persist(new TaskHistoryRecord("task " + task.getName() + " " + task.getSchedule() + " " +
                    " blocked " + System.currentTimeMillis()));
        }
    }

    @Transactional
    public void taskFinished(Task task) {
        running.remove(task);
        entityManager.persist(new TaskHistoryRecord("task " + task.getName() + " " + task.getSchedule() + " " +
                " finished " + System.currentTimeMillis()));

        blocked.stream().filter(task1 -> task1.getDependsOn().contains(task)).forEach(task1 -> {
            if (Collections.disjoint(task1.getDependsOn(), running)) {
                blocked.remove(task1);
                runTask(task1);
            }
        });
    }

    public void skeduleTask(Task task) {
        for (int i = 0; i < queue.size(); i++)
            if (task.getSchedule() < queue.get(i).getSchedule()) {
                queue.add(i, task);
                return;
            }
    }

    public boolean isRunning(String name){
        return running.stream().anyMatch(task -> task.getName() == name);
    }

    private void checkRun() {
        while (queue.get(0).getSchedule() <= System.currentTimeMillis()) {
            runTask(queue.removeFirst());
        }
    }
}
