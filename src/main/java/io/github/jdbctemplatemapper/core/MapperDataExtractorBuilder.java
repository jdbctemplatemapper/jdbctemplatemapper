package io.github.jdbctemplatemapper.core;

import java.util.ArrayList;
import java.util.List;

public class MapperDataExtractorBuilder<T> implements IMapperExtractionBuilder<T>, IRelationshipBuilder<T> {
    private Class<T> extractorType;
    private SelectMapper<?>[] selectMappers = {};
    private List<Relationship> relationships = new ArrayList<>();
    private Relationship tmpRelationship;

    private boolean fullyBuilt = false;
    
    private MapperDataExtractorBuilder(Class<T> extractorType, SelectMapper<?>... selectMappers) {
        this.extractorType = extractorType;
        this.selectMappers = selectMappers;
    }
    
    public static <T> IMapperExtractionBuilder<T> builder(Class<T> extractorType, SelectMapper<?>... selectMappers) {
  //      return new MapperResultSetExtractor<T>(extractorType, selectMappers);
    return null;
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
        return this;
    }

    public IMapperExtractionBuilder<T> hasOne(Class<?> relatedClazz, String propertyName) {
        tmpRelationship.setRelatedClazz(relatedClazz);
        tmpRelationship.setPropertyName(propertyName);
        tmpRelationship.setRelationshipType(RelationshipType.HAS_ONE);
        relationships.add(tmpRelationship);
        return this;
    }

    public MapperResultSetExtractor<T> build() {
        MapperResultSetExtractor<T> obj = new MapperResultSetExtractor<T>(extractorType, relationships, selectMappers);
        fullyBuilt = true;
        return obj;
        //return null;
    }
}
