package com.bazoud.elasticsearch.river.git.beans;

import java.io.File;
import java.util.Collection;
import java.util.regex.Pattern;

import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.elasticsearch.client.Client;

import com.google.common.base.Optional;
import lombok.Builder;
import lombok.Data;

/**
 * @author Olivier Bazoud
 */
@Data
@Builder
public class Context {
    private String name;
    private String uri;
    private File projectPath;
    private Repository repository;
    private String workingDir;
    private Collection<Ref> refs;
    private Client client;
    private String indexName;
    private Optional<Pattern> issuePattern;
    private boolean indexingDiff;
}
