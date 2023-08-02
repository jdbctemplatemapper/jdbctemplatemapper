package io.github.jdbctemplatemapper.core;

/**
 * An implementation of this interface will return the value
 * used to populate created by, updated by fields when using JdbcTemplateMapper
 * 
 * @author ajoseph
 *
 */
public interface IRecordOperatorResolver {
	/**
	 * The implementation should return the name/id ... which is used to populate
	 * the created by and updated by properties of the object while inserting/updating
     *
	 * @return Object - The value ie name/id ... of user who operated on the record.
	 */
	public Object getRecordOperator();
}
