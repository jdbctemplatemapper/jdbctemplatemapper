package io.github.jdbctemplatemapper.support;

public interface IMapperExtractionBuilder<T> {
    IRelationshipBuilder <T> relationship(Class<?> mainClazz);
    MapperResultSetExtractor<T> build();

}
