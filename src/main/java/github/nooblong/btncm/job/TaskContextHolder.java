package github.nooblong.btncm.job;

public class TaskContextHolder {

    private static final ThreadLocal<TaskContext>
            LOCAL = new ThreadLocal<>();

    public static void set(TaskContext ctx) {
        LOCAL.set(ctx);
    }

    public static TaskContext get() {
        return LOCAL.get();
    }

    public static void clear() {
        LOCAL.remove();
    }

}
