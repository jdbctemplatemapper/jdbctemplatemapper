package io.github.jdbctemplatemapper.core;

import java.util.ArrayList;
import java.util.List;

public class MapperResultSetExtractorBuilder<T>  implements IMapperExtractionBuilder<T>, IRelationshipBuilder<T> {
    
    private Class<T> extractorType;
    private SelectMapper<?>[] selectMappers = {};
    private List<Relationship> relationships = new ArrayList<>();
    private Relationship tmpRelationship;
    
    private MapperResultSetExtractorBuilder(Class<T> extractorType, SelectMapper<?>... selectMappers) {
        this.extractorType = extractorType;
        this.selectMappers = selectMappers;
    }
    
    // using a static method instead of the constructor because the generic alphabet soup was getting too much.
    public static <T> IMapperExtractionBuilder<T> initialize(Class<T> extractorType, SelectMapper<?>... selectMappers) {
        return new MapperResultSetExtractorBuilder<T>(extractorType, selectMappers);
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
        return new MapperResultSetExtractor<T>(extractorType, relationships, selectMappers);
    }
}
