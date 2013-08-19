package com.bazoud.elasticsearch.river.git.jgit;

import org.eclipse.jgit.lib.ProgressMonitor;
import org.elasticsearch.common.logging.ESLogger;

/**
 * @author Olivier Bazoud
 */
public class LoggingProgressMonitor implements ProgressMonitor {
    private ESLogger logger;

    public LoggingProgressMonitor(ESLogger logger) {
        this.logger = logger;
    }

    @Override
    public void start(int totalTasks) {
    }

    @Override
    public void beginTask(String title, int totalWork) {
        logger.info(title);
    }

    @Override
    public void update(int completed) {
    }

    @Override
    public void endTask() {
    }

    @Override
    public boolean isCancelled() {
        return false;
    }
}
