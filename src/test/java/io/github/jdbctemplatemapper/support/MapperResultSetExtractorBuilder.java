package io.github.jdbctemplatemapper.support;

import java.util.ArrayList;
import java.util.List;

import io.github.jdbctemplatemapper.core.SelectMapper;

public class MapperResultSetExtractorBuilder<T> implements IMapperExtractionBuilder<T>, IRelationshipBuilder<T> {
    Class<T> rootClazz;
    SelectMapper<?>[] selectMappers = {};
    List<Relationship> relationships = new ArrayList<>();

    private Relationship tmpRelationship;

    private MapperResultSetExtractorBuilder(Class<T> rootClazz,
            SelectMapper<?>... selectMappers) {
        this.rootClazz = rootClazz;

        this.selectMappers = selectMappers;
    }

    public static <T> IMapperExtractionBuilder<T> newMapperResultSetExtractorBuilder(Class<T> rootClazz,
            SelectMapper<?>... selectMappers) {

        return new MapperResultSetExtractorBuilder<T>(rootClazz, selectMappers);
    }

    public IRelationshipBuilder<T> relationship(Class<?> clazz) {
        this.tmpRelationship = new Relationship(clazz);
        return this;
    }

    public IMapperExtractionBuilder<T> hasMany(Class<?> relatedClazz, String propertyName) {
        tmpRelationship.setRelatedClazz(relatedClazz);
        tmpRelationship.setPropertyName(propertyName);
        tmpRelationship.setRelationshipType(RelationshipType.HAS_MANY);

        relationships.add(tmpRelationship);
        tmpRelationship = null;
        return this;
    }

    public IMapperExtractionBuilder<T> hasOne(Class<?> relatedClazz, String propertyName) {
        tmpRelationship.setRelatedClazz(relatedClazz);
        tmpRelationship.setPropertyName(propertyName);
        tmpRelationship.setRelationshipType(RelationshipType.HAS_ONE);
        relationships.add(tmpRelationship);
        tmpRelationship = null;
        return this;
    }

    public MapperResultSetExtractor<T> build() {
        return new MapperResultSetExtractor<T>(rootClazz, relationships, selectMappers);
    }

}
