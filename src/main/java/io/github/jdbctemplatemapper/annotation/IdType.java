package io.github.jdbctemplatemapper.annotation;

/**
 * The type of the @id annotation
 */
public enum IdType {
    AUTO_INCREMENT, // for ids which are auto incremented by the database
    MANUAL; // The identifier has to be manually set. User for all ids which are NOT
            // database auto incremented.
}
