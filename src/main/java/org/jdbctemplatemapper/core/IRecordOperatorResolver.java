package org.jdbctemplatemapper.core;

/**
 * An implementation of this interface will return the value
 * used to populate created_by, updated_by fields by JdbcMapper
 * 
 * @author ajoseph
 *
 */
public interface IRecordOperatorResolver {
	public Object getRecordOperator();
}
