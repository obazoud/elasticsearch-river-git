package com.bazoud.elasticsearch.river.git.flow.functions;

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;

import com.bazoud.elasticsearch.river.git.beans.Id;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.google.common.base.Throwables;

import static org.elasticsearch.client.Requests.indexRequest;

/**
 * @author Olivier Bazoud
 */
public class ObjectToJsonFunction implements Function<Id, Object> {
    private static ESLogger logger = Loggers.getLogger(ObjectToJsonFunction.class);
    private BulkRequestBuilder bulk;
    private String riverName;
    private String type;

    public ObjectToJsonFunction(BulkRequestBuilder bulk, String riverName, String type) {
        this.bulk = bulk;
        this.riverName = riverName;
        this.type = type;
    }

    @Override
    public Object apply(Id input) {
        try {
            bulk.add(indexRequest(riverName)
                .type(type)
                .id(input.getId())
                .source(toJson(input)));
            return input;
        } catch (Throwable e) {
            logger.error(this.getClass().getName(), e);
            Throwables.propagate(e);
            return input;
        }    }


    public String toJson(Object object) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(object);
    }

}
