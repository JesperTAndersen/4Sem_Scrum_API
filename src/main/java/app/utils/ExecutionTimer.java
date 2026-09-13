package app.utils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class ExecutionTimer
{
    private static Long start;
    private static long lastSplit;

    private ExecutionTimer()
    {
    }

    public static void start()
    {
        start = System.currentTimeMillis();
        lastSplit = start;
    }

    public static void split(String label)
    {
        validateStartTime();
        long now = System.currentTimeMillis();
        String splitDuration = formatElapsed(lastSplit, now);
        String totalTime = formatElapsed(start, now);
        log.info("{}: {} (total: {})", label, splitDuration, totalTime);
        lastSplit = now;
    }

    public static void finish(String label)
    {
        validateStartTime();
        long now = System.currentTimeMillis();
        String totalTime = formatElapsed(start, now);
        log.info("{}: {}", label, totalTime);
        lastSplit = now;
    }

    private static String formatElapsed(long startTime, long endTime)
    {
        long elapsed = endTime - startTime;
        long minutes = (elapsed / 1000) / 60;
        long seconds = (elapsed / 1000) % 60;
        long hundredths = (elapsed % 1000) / 10;

        return String.format("%02d:%02d.%02d", minutes, seconds, hundredths);
    }

    private static void validateStartTime()
    {
        if (start == null)
        {
            throw new IllegalStateException("ExecutionTimer.start() must be called before split() or finish()");
        }
    }
}
