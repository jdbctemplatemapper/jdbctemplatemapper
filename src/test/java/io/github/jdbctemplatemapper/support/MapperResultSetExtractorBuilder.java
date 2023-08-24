package io.github.jdbctemplatemapper.support;

import io.github.jdbctemplatemapper.core.SelectMapper;

public class MapperResultSetExtractorBuilder<T> {

    private SelectMapper<?> [] selectMappers = {};
    
    public MapperResultSetExtractorBuilder(SelectMapper<?> ... selectMappers){
         this.selectMappers = selectMappers;
    }

    public MapperResultSetExtractorBuilder<T> relationship(Class<?> clazz) {
        return this;
    }
    
    public MapperResultSetExtractorBuilder<T> toMany(Class<?> clazz, String propertyName) {
        return this;
    }
    
    public MapperResultSetExtractorBuilder<T> toOne(Class<?> clazz, String propertyName) {
        return this;
    }
    
    public MapperResultSetExtractor<T> build(){
        return  new MapperResultSetExtractor<T>(this);
    }
}
