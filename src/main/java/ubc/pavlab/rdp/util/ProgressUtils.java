package ubc.pavlab.rdp.util;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.time.StopWatch;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@CommonsLog
public class ProgressUtils {

    /**
     * Emit progress on a progress callback in a safe manner.
     * <p>
     * Runtime exceptions are caught and turned into errors.
     *
     * @param progressCallback  progress callback, ignored if null
     * @param processedElements number of processed elements, which must be positive
     * @param totalElements     total number of elements to be processed, which must be greater than one
     * @param elapsedTime       elapsed time since the processing started
     */
    public static void emitProgress( ProgressCallback progressCallback, long processedElements, long totalElements, Duration elapsedTime ) {
        if ( processedElements < 0 ) {
            throw new IllegalArgumentException( "The processedElements parameter must be positive." );
        }
        if ( totalElements < 1 ) {
            throw new IllegalArgumentException( "The totalElements parameter must greater than one." );
        }
        if ( processedElements > totalElements ) {
            throw new IllegalArgumentException( ( "The processedElements parameter must be strictly smaller or equal to the totalElements parameter." ) );
        }
        if ( progressCallback != null ) {
            StopWatch timer = StopWatch.createStarted();
            try {
                progressCallback.onProgress( processedElements, totalElements, elapsedTime );
            } catch ( RuntimeException e ) {
                log.error( "Progress failed", e );
            } finally {
                timer.stop();
                if ( timer.getTime( TimeUnit.MILLISECONDS ) > 10 ) {
                    log.warn( String.format( "Progress callback is too slow and might impede processing (took %d ms).", timer.getTime( TimeUnit.MILLISECONDS ) ) );
                }
            }
        }
    }

    public static void emitProgress( ProgressCallback progressCallback, long processedElements, long totalElements, long elapsedTimeMillis ) {
        emitProgress( progressCallback, processedElements, totalElements, Duration.ofMillis( elapsedTimeMillis ) );
    }
}
