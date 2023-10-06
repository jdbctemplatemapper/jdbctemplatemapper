package io.github.jdbctemplatemapper.core;

/**
 * An implementation of this interface should return the value used to populate properties annotated
 * with {@literal @}CreatedBy and {@literal @}UpdatedBy.
 *
 * @author ajoseph
 */
public interface IRecordOperatorResolver {
  /**
   * The implementation should return the value (name/id etc) which is used to populate the created
   * by and updated by properties of the object while inserting/updating
   *
   * @return Object - The value ie name/id etc of user who operated on the record.
   */
  public Object getRecordOperator();
}
