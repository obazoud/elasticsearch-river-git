package com.bazoud.elasticsearch.river.git.guava;

/**
 * @author Olivier Bazoud
 */
public final class Visitors {
    private Visitors() {
    }

    public static <T> void visit(Iterable<T> iterable, Visitor<T> visitor) throws Exception {
        visitor.before();
        for (T current : iterable) {
            visitor.visit(current);
        }
        visitor.after();

    }
}
