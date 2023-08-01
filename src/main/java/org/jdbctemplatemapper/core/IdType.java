package org.jdbctemplatemapper.core;

public enum IdType {   
    AUTO_INCREMENT,   // ids which are auto incremented by the database 
    MANUAL; // The identifier has to be manually set. User for all ids which are not database auto incremented.
}
