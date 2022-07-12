package ubc.pavlab.rdp.util;

import java.time.Duration;

/**
 * Generic interface for monitoring progress of some process.
 */
@FunctionalInterface
public interface ProgressCallback {

    /**
     * Emitted when a certain number of elements have been processed.
     *
     * @param processedElements number of processed elements
     * @param totalElements     total number of elements
     * @param elapsedTime       elapsed time since the processing started
     * @see ProgressUtils#emitProgress(ProgressCallback, long, long, Duration)
     */
    void onProgress( long processedElements, long totalElements, Duration elapsedTime );
}
