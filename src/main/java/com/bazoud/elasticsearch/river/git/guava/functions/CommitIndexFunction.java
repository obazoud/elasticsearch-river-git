package com.bazoud.elasticsearch.river.git.guava.functions;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;

import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;

import com.bazoud.elasticsearch.river.git.beans.Commit;
import com.bazoud.elasticsearch.river.git.beans.Context;
import com.bazoud.elasticsearch.river.git.beans.Identity;
import com.bazoud.elasticsearch.river.git.beans.Parent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Ordering;

import static org.elasticsearch.client.Requests.indexRequest;

/**
 * @author Olivier Bazoud
 */
public class CommitIndexFunction implements Function<Context, Context> {
    private static ESLogger logger = Loggers.getLogger(CommitIndexFunction.class);

    @Override
    public Context apply(final Context context) {
        try {
            final RevWalk walk = new RevWalk(context.getRepository());

            walk.markStart(
                FluentIterable
                    .from(context.getRefs())
                    .transform(new Function<Ref, RevCommit>() {
                        @Override
                        public RevCommit apply(Ref ref) {
                            try {
                                return walk.parseCommit(ref.getObjectId());
                            } catch (Throwable e) {
                                logger.error(this.getClass().getName(), e);
                                return null;
                            }
                        }
                    })
                    .toList()
            );

            final BulkRequestBuilder bulk = context.getClient().prepareBulk();
            FluentIterable
                .from(walk)
                .transform(new Function<RevCommit, RevCommit>() {
                    @Override
                    public RevCommit apply(RevCommit commit) {
                        try {
                            bulk.add(indexRequest(context.getIndexName())
                                .type("commit")
                                .id(commit.getId().name())
                                .source(toJson(toCommit(context, walk, commit))));
                            return commit;
                        } catch (Throwable e) {
                            logger.error(this.getClass().getName(), e);
                            return null;
                        }
                    }
                })
                .toList();

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
        } catch(Throwable e) {
            logger.error(this.getClass().getName(), e);
        }

        return context;
    }

    private static Commit toCommit(Context context, RevWalk walk, RevCommit revCommit) throws Exception {
        return Commit.commit()
            .id(revCommit.getId().name())
            .project(context.getName())
            .author(
                Identity.identity()
                    .name(revCommit.getAuthorIdent().getName())
                    .emailAddress(revCommit.getAuthorIdent().getEmailAddress())
                    .when(revCommit.getAuthorIdent().getWhen())
                    .timeZone(revCommit.getAuthorIdent().getTimeZone())
                    .timeZoneOffset(revCommit.getAuthorIdent().getTimeZoneOffset())
                    .build()
            )
            .committer(
                Identity.identity()
                    .name(revCommit.getCommitterIdent().getName())
                    .emailAddress(revCommit.getCommitterIdent().getEmailAddress())
                    .when(revCommit.getCommitterIdent().getWhen())
                    .timeZone(revCommit.getCommitterIdent().getTimeZone())
                    .timeZoneOffset(revCommit.getCommitterIdent().getTimeZoneOffset())
                    .build()
            )
            .subject(revCommit.getShortMessage())
            .messsage(revCommit.getFullMessage())
            .encoding(revCommit.getEncoding().name())
            .issues(parseMessage(context, revCommit.getFullMessage()))
            .parents(
                FluentIterable
                    .from(Arrays.asList(revCommit.getParents()))
                    .transform(new Function<RevCommit, Parent>() {
                        @Override
                        public Parent apply(RevCommit commit) {
                            try {
                                return Parent.parent()
                                    .id(commit.getId().getName())
                                    .build();
                            } catch (Throwable e) {
                                logger.error(this.getClass().getName(), e);
                                return null;
                            }
                        }
                    })
                    .toList()
            )
            .diff(parseDiff(context, walk, revCommit))
            .build();
    }

    private static String parseDiff(Context context, RevWalk walk, RevCommit revCommit) throws Exception {
        if (context.isIndexingDiff() && revCommit.getParentCount() > 0) {
            RevCommit parentCommit = walk.parseCommit(revCommit.getParent(0).getId());
            ByteArrayOutputStream diffOutputStream = new ByteArrayOutputStream();
            DiffFormatter diffFormatter = new DiffFormatter(diffOutputStream);
            diffFormatter.setRepository(context.getRepository());
            diffFormatter.setDiffComparator(RawTextComparator.DEFAULT);
            diffFormatter.setDetectRenames(true);
            diffFormatter.format(parentCommit.getTree(),revCommit.getTree());
            return new String(diffOutputStream.toByteArray());
        } else {
            return null;
        }
    }

    private static List<String> parseMessage(Context context, String message) {
        if (!context.getIssuePattern().isPresent()) {
            return Collections.emptyList();
        }

        Matcher matcher = context.getIssuePattern().get().matcher(message);
        List<String> issues = new ArrayList<String>();
        while(matcher.find()) {
            issues.add(matcher.group(1));
        }

        return Ordering.natural().sortedCopy(issues);
    }

    private static String toJson(Commit commit) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(commit);
        return json;
    }
}
