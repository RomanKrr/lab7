public class ThreadStatistics {

    private String threadName;
    private int filesFound;
    private long activeTimeMs;

    public ThreadStatistics(String threadName) {
        this.threadName = threadName;
        this.filesFound = 0;
        this.activeTimeMs = 0;
    }

    public String getThreadName() {
        return threadName;
    }

    public int getFilesFound() {
        return filesFound;
    }

    public long getActiveTimeMs() {
        return activeTimeMs;
    }

    public void addFilesFound(int count) {
        filesFound += count;
    }

    public void addActiveTime(long time) {
        activeTimeMs += time;
    }

    public double getTimePercentage(long totalTimeMs) {
        if (totalTimeMs == 0) {
            return 0;
        }

        return (double) activeTimeMs / totalTimeMs * 100;
    }
}