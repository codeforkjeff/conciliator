package com.codefork.refine.viaf;

import com.codefork.refine.SearchResult;
import com.codefork.refine.SearchTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * A service wrapper around an ExecutorService thread pool.
 */
@Service
public class VIAFThreadPool {

    private VIAF viaf;
    /**
     * NOTE: VIAF seems to have a limit of 6 simultaneous
     * requests. To be conservative, we default to 4 for
     * the entire app.
     */
    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    @Autowired
    public VIAFThreadPool(VIAF viaf) {
        this.viaf = viaf;
        // TODO: make thread pool size configurable
    }

    public Future<SearchResult> submit(SearchTask task) {
        return executor.submit(task);
    }

}
