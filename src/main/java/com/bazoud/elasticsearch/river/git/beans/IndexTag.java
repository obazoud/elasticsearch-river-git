package com.bazoud.elasticsearch.river.git.beans;

import lombok.Builder;
import lombok.Data;

/**
 * @author Olivier Bazoud
 */
@Data
@Builder
@SuppressWarnings("PMD.UnusedPrivateField")
public class IndexTag implements Id {
    private String id;
    private String tag;
    private String ref;
    private String sha1;
}
