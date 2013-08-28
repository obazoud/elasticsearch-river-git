package com.bazoud.elasticsearch.river.git.beans;

import lombok.Builder;
import lombok.Data;

/**
 * @author Olivier Bazoud
 */
@Data
@Builder
@SuppressWarnings("PMD.UnusedPrivateField")
public class Parent {
    private String id;
}
