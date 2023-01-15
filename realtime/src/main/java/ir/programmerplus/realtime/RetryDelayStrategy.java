package ir.programmerplus.realtime;


public enum RetryDelayStrategy {
    CONSTANT_DELAY,
    RETRY_COUNT,
    CONSTANT_DELAY_TIMES_RETRY_COUNT,
    CONSTANT_DELAY_RAISED_TO_RETRY_COUNT
}