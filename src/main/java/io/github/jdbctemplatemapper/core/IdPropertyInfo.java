package io.github.jdbctemplatemapper.core;

public class IdPropertyInfo {
	private Class<?> parentClazz;
	private String propertyName;
	private boolean isIdAutoIncrement=false; 
	
	IdPropertyInfo(Class<?> parentClazz, String propertyName, Boolean isIdAutoIncrement){
		this.parentClazz = parentClazz;
		this.propertyName=propertyName;
		this.isIdAutoIncrement=isIdAutoIncrement;
	}

	public Class<?> getParentClazz() {
		return parentClazz;
	}

	public String getPropertyName() {
		return propertyName;
	}

	public boolean isIdAutoIncrement() {
		return isIdAutoIncrement;
	}
	

}
