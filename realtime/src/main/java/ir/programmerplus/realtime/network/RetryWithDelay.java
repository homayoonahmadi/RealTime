package ir.programmerplus.realtime.network;

import org.reactivestreams.Publisher;

import java.text.MessageFormat;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import io.reactivex.Flowable;
import io.reactivex.functions.Function;
import ir.programmerplus.realtime.utils.LogUtils;

import static java.util.Objects.requireNonNull;

public class RetryWithDelay implements Function<Flowable<? extends Throwable>, Publisher<Object>> {

    private static final String TAG = RetryWithDelay.class.getSimpleName();

    private int retryCount;
    private final String host;

    private final int maxRetries;
    private final long mexDelaySeconds;
    private final long retryDelaySeconds;
    private final RetryDelayStrategy retryDelayStrategy;

    private RetryWithDelay(String host, int maxRetries, long mexDelaySeconds, long retryDelaySeconds, RetryDelayStrategy retryDelayStrategy) {
        this.host = host == null ? "" : host;
        this.maxRetries = maxRetries;
        this.mexDelaySeconds = mexDelaySeconds;
        this.retryDelaySeconds = retryDelaySeconds;
        this.retryDelayStrategy = retryDelayStrategy;
    }

    public static RetryWithDelayBuilder builder() {
        return new RetryWithDelayBuilder();
    }

    @Override
    public Publisher<Object> apply(Flowable<? extends Throwable> attempts) {
        return attempts
                .concatMap((Function<Throwable, Flowable<?>>) throwable -> {
                    if (++retryCount <= maxRetries) {
                        // When this Observable calls onNext, the original
                        // Observable will be retried (i.e. resubscribed).
                        long delaySeconds = delaySeconds();

                        LogUtils.d(TAG, MessageFormat.format("RealTime: Retrying {0}... attempt #{1} in {2} second(s).", host, retryCount, delaySeconds));
                        return Flowable.timer(delaySeconds, TimeUnit.SECONDS);
                    }

                    // Max retries hit. Just pass the error along.
                    LogUtils.w(TAG, MessageFormat.format("RealTime {0}: Exhausted all retries: {1}.", host, maxRetries));
                    return Flowable.error(throwable);
                });
    }

    private long delaySeconds() {
        requireNonNull(retryDelayStrategy, "RetryDelayStrategy must not be null.");

        return switch (retryDelayStrategy) {
            case CONSTANT_DELAY -> retryDelaySeconds;
            case RETRY_COUNT -> retryCount;
            case CONSTANT_DELAY_TIMES_RETRY_COUNT -> Math.min(retryDelaySeconds * retryCount, mexDelaySeconds);
            case CONSTANT_DELAY_RAISED_TO_RETRY_COUNT -> (long) Math.pow(retryDelaySeconds, retryCount);
        };
    }

    public static class RetryWithDelayBuilder {
        private String host;
        private int maxRetries;
        private long mexDelaySeconds;
        private long retryDelaySeconds;
        private RetryDelayStrategy retryDelayStrategy;

        RetryWithDelayBuilder() {
        }

        public RetryWithDelayBuilder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public RetryWithDelayBuilder mexDelaySeconds(long mexDelaySeconds) {
            this.mexDelaySeconds = mexDelaySeconds;
            return this;
        }

        public RetryWithDelayBuilder retryDelaySeconds(long retryDelaySeconds) {
            this.retryDelaySeconds = retryDelaySeconds;
            return this;
        }

        public RetryWithDelayBuilder retryDelayStrategy(RetryDelayStrategy retryDelayStrategy) {
            this.retryDelayStrategy = retryDelayStrategy;
            return this;
        }

        public RetryWithDelayBuilder host(String host) {
            this.host = host;
            return this;
        }

        public RetryWithDelay build() {
            return new RetryWithDelay(host, maxRetries, mexDelaySeconds, retryDelaySeconds, retryDelayStrategy);
        }

        @NonNull
        public String toString() {
            return "RetryWithDelay.RetryWithDelayBuilder(maxRetries=" + this.maxRetries +
                    ", mexDelaySeconds=" + this.mexDelaySeconds + ", retryDelaySeconds=" +
                    this.retryDelaySeconds + ", retryDelayStrategy=" + this.retryDelayStrategy + ")";
        }
    }
}