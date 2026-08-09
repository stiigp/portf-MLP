package com.panucci.mlp.dto;

public record TrainingEventOptions(
    Integer progressSampleInterval,
    Integer outputSampleInterval,
    Integer weightsSampleInterval,
    Long progressMinMillis,
    Long weightsMinMillis
) {
    private static final int DEFAULT_PROGRESS_SAMPLE_INTERVAL = 10;
    private static final int DEFAULT_OUTPUT_SAMPLE_INTERVAL = 100;
    private static final int DEFAULT_WEIGHTS_SAMPLE_INTERVAL = 50;
    private static final long DEFAULT_PROGRESS_MIN_MILLIS = 100L;
    private static final long DEFAULT_WEIGHTS_MIN_MILLIS = 250L;

    public static TrainingEventOptions defaults() {
        return new TrainingEventOptions(
            DEFAULT_PROGRESS_SAMPLE_INTERVAL,
            DEFAULT_OUTPUT_SAMPLE_INTERVAL,
            DEFAULT_WEIGHTS_SAMPLE_INTERVAL,
            DEFAULT_PROGRESS_MIN_MILLIS,
            DEFAULT_WEIGHTS_MIN_MILLIS
        );
    }

    public static TrainingEventOptions normalize(TrainingEventOptions options) {
        if (options == null) {
            return defaults();
        }

        return new TrainingEventOptions(
            positiveOrDefault(options.progressSampleInterval(), DEFAULT_PROGRESS_SAMPLE_INTERVAL),
            positiveOrDefault(options.outputSampleInterval(), DEFAULT_OUTPUT_SAMPLE_INTERVAL),
            positiveOrDefault(options.weightsSampleInterval(), DEFAULT_WEIGHTS_SAMPLE_INTERVAL),
            positiveOrDefault(options.progressMinMillis(), DEFAULT_PROGRESS_MIN_MILLIS),
            positiveOrDefault(options.weightsMinMillis(), DEFAULT_WEIGHTS_MIN_MILLIS)
        );
    }

    private static int positiveOrDefault(Integer value, int defaultValue) {
        return value == null || value < 1 ? defaultValue : value;
    }

    private static long positiveOrDefault(Long value, long defaultValue) {
        return value == null || value < 1 ? defaultValue : value;
    }
}
