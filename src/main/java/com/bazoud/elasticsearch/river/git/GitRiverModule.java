package com.bazoud.elasticsearch.river.git;

import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.river.River;

import com.bazoud.elasticsearch.river.git.GitRiver;

/**
 * @author Olivier Bazoud
 */
public class GitRiverModule  extends AbstractModule {
    @Override
    protected void configure() {
        bind(River.class).to(GitRiver.class).asEagerSingleton();
    }}
