package com.bazoud.elasticsearch.river.git.flow.visitors;

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;

import com.bazoud.elasticsearch.river.git.beans.Context;
import com.bazoud.elasticsearch.river.git.beans.Id;
import com.bazoud.elasticsearch.river.git.guava.Visitor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;

import static org.elasticsearch.client.Requests.indexRequest;

/**
 * @author Olivier Bazoud
 */
public class BulkVisitor<T extends Id> implements Visitor<T> {
    private static ESLogger logger = Loggers.getLogger(BulkVisitor.class);

    private BulkRequestBuilder bulk;
    private Context context;
    private String type;

    public BulkVisitor(Context context, String type) {
        this.context = context;
        this.type = type;
    }

    @Override
    public void before() {
        this.bulk = context.getClient().prepareBulk();
    }

    @Override
    public void visit(Id input) {
        try {
            bulk.add(indexRequest(context.getRiverName())
                .type(type)
                .id(input.getId())
                .source(toJson(input)));
        } catch (Throwable e) {
            logger.error(this.getClass().getName(), e);
            Throwables.propagate(e);
        }
    }

    @Override
    public void after() {
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

    public String toJson(Object object) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(object);
    }
}
