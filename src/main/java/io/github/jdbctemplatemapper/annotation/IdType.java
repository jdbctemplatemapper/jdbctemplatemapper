package io.github.jdbctemplatemapper.annotation;

/**
 * The type of the @id annotation.
 */
public enum IdType {
  AUTO_INCREMENT, // for ids which are auto incremented by the database
  MANUAL; // Default. The identifier has to be manually set. Use for all ids which are NOT 
          // database auto incremented.
}
