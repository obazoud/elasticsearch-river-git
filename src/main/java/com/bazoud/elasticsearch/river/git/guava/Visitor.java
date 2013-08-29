package com.bazoud.elasticsearch.river.git.guava;

/**
 * @author Olivier Bazoud
 */
public interface Visitor<T> {
    void before();

    void visit(T input) throws Exception;

    void after();
}
