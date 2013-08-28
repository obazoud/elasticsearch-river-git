package com.bazoud.elasticsearch.river.git.es;

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;

/**
 * @author Olivier Bazoud
 */
public final class Bulk {
    private static ESLogger logger = Loggers.getLogger(Bulk.class);

    private Bulk() {
    }

    public static void execute(BulkRequestBuilder bulk) {
        logger.info("Executing bulk {} actions", bulk.numberOfActions());
        if (bulk.numberOfActions() > 0) {
            BulkResponse response = bulk.execute().actionGet();
            logger.info("Bulk actions tooks {} ms", response.getTookInMillis());
            if (response.hasFailures()) {
                logger.warn("failed to execute bulk: {}", response.buildFailureMessage());
            }
        } else {
            logger.info("Sorry nothing to do");
        }
    }


}
