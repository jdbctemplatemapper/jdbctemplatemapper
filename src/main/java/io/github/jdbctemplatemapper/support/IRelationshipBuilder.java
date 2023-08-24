package io.github.jdbctemplatemapper.support;

public interface IRelationshipBuilder<T> {
    IMapperExtractionBuilder<T>  hasMany(Class<?> relatedClazz, String propertyName);
    IMapperExtractionBuilder<T>  hasOne(Class<?> relatedClazz, String propertyName);
}
