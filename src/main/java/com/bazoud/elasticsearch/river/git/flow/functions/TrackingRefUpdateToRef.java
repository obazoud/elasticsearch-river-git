package com.bazoud.elasticsearch.river.git.flow.functions;

import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.TrackingRefUpdate;

import com.bazoud.elasticsearch.river.git.guava.MyFunction;

/**
 * @author Olivier Bazoud
 */
public class TrackingRefUpdateToRef extends MyFunction<TrackingRefUpdate, Ref> {
    private Repository repository;

    public TrackingRefUpdateToRef(Repository repository) {
        this.repository = repository;
    }

    @Override
    public Ref doApply(TrackingRefUpdate input) throws Throwable {
        return repository.getRef(input.getLocalName());
    }
}
