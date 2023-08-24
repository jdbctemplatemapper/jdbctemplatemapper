package io.github.jdbctemplatemapper.core;

public interface IMapperExtractionBuilder<T> {
    IRelationshipBuilder <T> relationship(Class<?> mainClazz);
    MapperResultSetExtractor<T> build();

}
