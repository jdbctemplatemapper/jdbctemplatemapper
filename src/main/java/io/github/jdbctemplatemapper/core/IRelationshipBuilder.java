package io.github.jdbctemplatemapper.core;

public interface IRelationshipBuilder<T> {
    IMapperExtractionBuilder<T>  hasMany(Class<?> relatedClazz, String propertyName);
    IMapperExtractionBuilder<T>  hasOne(Class<?> relatedClazz, String propertyName);
}
