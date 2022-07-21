package ubc.pavlab.rdp.util;

import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import java.util.concurrent.CompletableFuture;

public class FutureUtils {

    public static <T> CompletableFuture<T> toCompletableFuture( ListenableFuture<T> listenableFuture ) {
        CompletableFuture<T> cf = new CompletableFuture<T>();
        listenableFuture.addCallback( new ListenableFutureCallback<T>() {

            @Override
            public void onSuccess( T result ) {
                cf.complete( result );
            }

            @Override
            public void onFailure( Throwable ex ) {
                cf.completeExceptionally( ex );
            }
        } );
        return cf;
    }
}
