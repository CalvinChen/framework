/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package leap.orm.mapping;

import leap.core.AppConfig;
import leap.core.AppConfigAware;
import leap.core.AppConfigException;
import leap.core.BeanFactory;
import leap.core.annotation.Inject;
import leap.core.annotation.M;
import leap.core.ds.DataSourceManager;
import leap.core.ioc.AbstractReadonlyBean;
import leap.core.metamodel.ReservedMetaFieldName;
import leap.db.DbMetadata;
import leap.db.model.DbColumn;
import leap.db.model.DbColumnBuilder;
import leap.db.model.DbTable;
import leap.db.model.DbTableBuilder;
import leap.lang.Args;
import leap.lang.Classes;
import leap.lang.Strings;
import leap.lang.TypeInfo;
import leap.lang.beans.BeanProperty;
import leap.lang.beans.BeanType;
import leap.lang.jdbc.JdbcTypes;
import leap.lang.logging.Log;
import leap.lang.logging.LogFactory;
import leap.lang.meta.MSimpleType;
import leap.lang.meta.MSimpleTypes;
import leap.lang.meta.MType;
import leap.orm.OrmConfig;
import leap.orm.OrmConstants;
import leap.orm.OrmContext;
import leap.orm.annotation.*;
import leap.orm.config.OrmModelsConfig;
import leap.orm.config.OrmModelsConfigs;
import leap.orm.domain.EntityDomain;
import leap.orm.domain.FieldDomain;
import leap.orm.generator.IdGenerator;
import leap.orm.metadata.MetadataContext;
import leap.orm.metadata.MetadataException;
import leap.orm.model.Model;
import leap.orm.model.ModelField;
import leap.orm.naming.NamingStrategy;
import leap.orm.serialize.FieldSerializer;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.Types;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class DefaultMappingStrategy extends AbstractReadonlyBean implements MappingStrategy, AppConfigAware  {
	
	private static final Log log = LogFactory.get(DefaultMappingStrategy.class);

    protected @Inject @M MappingProcessor[] processors;
    protected @Inject @M IdGenerator        defaultIdGenerator;
	
	protected OrmModelsConfigs modelsConfigs;
	protected String defaultDatasourceName;
	
	public void setDefaultIdGenerator(IdGenerator defaultIdGenerator) {
		this.defaultIdGenerator = defaultIdGenerator;
	}
	
	@Override
    public void setAppConfig(AppConfig config) {
		this.modelsConfigs = config.getExtension(OrmModelsConfigs.class);
    }

	@Override
	protected void doInit(BeanFactory beanFactory) throws Exception {
		if(this.modelsConfigs != null){
			defaultDatasourceName = beanFactory.tryGetBean(DataSourceManager.class).getDefaultDatasourceBeanName();
			this.modelsConfigs.getModelsConfigMap().forEach((k,v)->{
				if(Strings.isEmpty(v.getDataSource())){
					v.setDataSource(defaultDatasourceName);
				}
			});
		}
		super.doInit(beanFactory);
	}

	@Override
    public boolean isContextModel(OrmContext context, Class<?> cls) {
		//The datasource's name.
		String ds = context.getName();
		
		if(null != modelsConfigs) {
			for(Entry<String, OrmModelsConfig> entry : modelsConfigs.getModelsConfigMap().entrySet()) {
				OrmModelsConfig models 				= entry.getValue();
				String 			name   				= Strings.isEmpty(models.getDataSource())?entry.getKey():models.getDataSource(); //the datasource's name
				boolean 		isDefaultDatasource = Strings.equals(name,defaultDatasourceName); // is default datasource
				if(name.equalsIgnoreCase(ds)) {
					if(models.contains(cls)){
						return true;
					}else if(isContextModelAnnotated(context, cls)){
						return true;
					}else{
						if(!isDefaultDatasource){
							// if this context is default datasource context, there can not return false;
							return false;
						}
					}
				}else{
					if(models.contains(cls)) {
						if(!isDefaultDatasource){
							return false;
						}
					}
				}
			}
		}

		DataSource a  = cls.getAnnotation(DataSource.class);
		if(null != a) {
			if(a.value().equalsIgnoreCase(ds)) {
				return true;
			}else{
				return false;
			}
		}
		
		return true;
	}
	
	protected boolean isContextModelAnnotated(OrmContext context, Class<?> cls) {
		DataSource a  = cls.getAnnotation(DataSource.class);
		if(null != a && a.value().equalsIgnoreCase(context.getName())) {
			return true;
		}else{
			return false;
		}
	}

	@Override
    public boolean isExplicitEntity(MetadataContext context,Class<?> javaType) {
		if(isExplicitNonEntity(context, javaType)) {
			return false;
		}
		
		if(Model.class.isAssignableFrom(javaType) && !Modifier.isAbstract(javaType.getModifiers())) {
			return true;
		}
		
		for(Annotation a : javaType.getAnnotations()){
			if(a.annotationType().isAnnotationPresent(AEntity.class)){
				return true;
			}
		}
		return false;
    }

	@Override
    public boolean isExplicitNonEntity(MetadataContext context,Class<?> javaType) {
	    return javaType.isAnnotationPresent(NonEntity.class);
    }

	@Override
    public boolean isExplicitField(MetadataContext context,BeanProperty beanProperty) {
	    return beanProperty.isAnnotationPresent(leap.orm.annotation.Column.class);
    }
	
	@Override
    public boolean isExplicitRelation(MetadataContext context, BeanProperty beanProperty) {
		for(Annotation a : beanProperty.getAnnotations()){
			if(a.annotationType().isAnnotationPresent(ARelation.class)){
				return true;
			}
		}
	    return false;
    }

	@Override
    public boolean isAutoGeneratedColumn(MetadataContext context,DbColumn column) {
		String fieldName = context.getNamingStrategy().columnToFieldName(column.getName());
		
		OrmConfig config = context.getConfig();
		
		if(fieldName.equalsIgnoreCase(config.getOptimisticLockFieldName())){
			return true;
		}
		
		for(String autoGeneratedField : config.getAutoGeneratedFieldNames()){
			if(fieldName.equalsIgnoreCase(autoGeneratedField)){
				return true;
			}
		}
		
		return false;
	}

	@Override
    public boolean isConventionalField(MetadataContext context,BeanProperty beanProperty) {
	    if (!beanProperty.isReadable() || beanProperty.isTransient() || !beanProperty.isField()) {
	    	return false;
	    }
	    Class<?> type = beanProperty.getType();
	    
	    if(Iterable.class.isAssignableFrom(type)){
	    	return false;
	    }
	    
	    if(Map.class.isAssignableFrom(type)){
	    	return false;
	    }
	    
	    //TODO : support complex type and related entity type
	    return beanProperty.getTypeInfo().isSimpleType();
    }

	@Override
    public boolean isExplicitNonField(MetadataContext context,BeanProperty beanProperty) {
	    return beanProperty.isAnnotationPresent(NonColumn.class);
    }

	/**
	 * Creates default primary key field for the given entity.
	 */
    protected FieldMappingBuilder createAutoIdentityMapping(MetadataContext context, EntityMappingBuilder emb) {
	    FieldMappingBuilder id =
	    		new FieldMappingBuilder()
	    				.setFieldName("id")
	    				.setDataType(MSimpleTypes.INTEGER)
	    				.setColumn(new DbColumnBuilder());
	    
	    postMappingFieldConventional(context, emb, id);
	    
	    id.getColumn().setPrimaryKey(true);
	    id.setIdGenerator(defaultIdGenerator);
	    defaultIdGenerator.mapping(context, emb, id);
	    
	    return id;
    }

	@Override
    public EntityMappingBuilder createEntityClassMapping(MetadataContext context, Class<?> cls) {
		Args.notNull(cls,"class");
		Args.assertFalse(isExplicitNonEntity(context,cls),
						  "The class '" + cls.getName() + "' was declared as not an entity type explicitly");

		EntityMappingBuilder emb = new EntityMappingBuilder().setEntityClass(cls);
		
		preMapping(context, emb);
		preMappingEntity(context, emb);
		postMappingEntity(context, emb);

		//mapping entity's properties
		BeanType beanType = BeanType.of(cls);
		
		log.debug("Creating entity mapping for type : " + cls.getName());
		
		mappingBeanProperties(context,emb,beanType);
		
		postMapping(context, emb);
		
		//fields must be defined
		if(emb.getFieldMappings().isEmpty()){
			throw new MetadataException("Entity's fields must not be empty in the java type '" + cls.getName() + "'");
		}
		
		//primary keys must be defined
		if(emb.getIdFieldMappings().isEmpty()){
			throw new MetadataException("Entity's primary key(s) must not be empty in the java type '" + cls.getName() + "'");
		}
		
	    return emb;
    }
	
	@Override
    public EntityMappingBuilder createModelMapping(MetadataContext context, Class<? extends Model> modelClass) throws MetadataException {
		Args.notNull(modelClass,"model class");
		
		EntityMappingBuilder emb = new EntityMappingBuilder().setModelClass(modelClass);

		preMapping(context, emb);
		preMappingEntity(context, emb);
		postMappingEntity(context, emb);
		
		BeanType beanType = BeanType.of(modelClass);
		log.debug("Creating entity mapping for model : " + modelClass.getName());
		
		Field[] fields = modelClass.getFields();
		try {
	        for(Field field : fields){
	        	if(field.getType().equals(ModelField.class)){
	        		ModelField attr = (ModelField)field.get(null);
	        		
	        		if(null == beanType.tryGetProperty(attr.getName())){
		        		FieldMappingBuilder fmb = new FieldMappingBuilder();
		        		fmb.setFieldName(attr.getName());
		        		fmb.setJavaType(attr.getType());
		        		fmb.setAnnotations(field.getAnnotations());
		        		
						preMappingField(context, emb, fmb);
						postMappingField(context, emb, fmb);
		        		
		        		emb.addFieldMapping(fmb);
	        		}
	        	}
	        }
        } catch (Exception e) {
        	throw new MetadataException("Error loading attributes in model class '" + modelClass.getName() + "'",e);
        }
		
		mappingBeanProperties(context,emb,beanType);
		
		postMapping(context, emb);
		
		return emb;
    }

    @Override
    public EntityMappingBuilder createEntityMappingByTable(MetadataContext context, DbTable t) throws MetadataException {
        //todo : to be implemented.
        DbTableBuilder       table = new DbTableBuilder(t);
        EntityMappingBuilder emb   = new EntityMappingBuilder().setTable(table);

        //entity
        emb.setEntityName(context.getNamingStrategy().tableToEntityName(table.getName()));
        emb.setTableNameDeclared(true);

        //fields.
        for(DbColumnBuilder col : table.getColumns()) {
            FieldMappingBuilder fmb = createFieldMappingByColumn(context, emb, col.build());
            fmb.setColumnNameDeclared(true);
            emb.addFieldMapping(fmb);
        }

        //post mappings
        postMapping(context, emb);

        return emb;
    }

    protected void mappingBeanProperties(MetadataContext context, EntityMappingBuilder emb, BeanType beanType) {
		for(BeanProperty bp : beanType.getProperties()){
			if(isExplicitNonField(context,bp)){
				continue;
			}
			
			if(isExplicitRelation(context, bp)) {
				RelationMappingBuilder rmb = new RelationMappingBuilder(bp);
				
				preMappingRelation(context, emb, rmb);
				postMappingRelation(context, emb, rmb);
				
				emb.addRelationMapping(rmb);
			}
			
			if( isExplicitField(context,bp) || isConventionalField(context,bp)){
				FieldMappingBuilder fmb = new FieldMappingBuilder().setBeanProperty(bp);
				
				preMappingField(context, emb, fmb);
				postMappingField(context, emb, fmb);
				
				emb.addFieldMapping(fmb);
			}
		}
	}
	
	@Override
    public FieldMappingBuilder createFieldMappingByColumn(MetadataContext context, EntityMappingBuilder emb, DbColumn column) {
		DbColumnBuilder cb = new DbColumnBuilder(column);
		FieldMappingBuilder fmb = new FieldMappingBuilder();
		fmb.setColumn(cb);
		fmb.setFieldName(context.getNamingStrategy().columnToFieldName(column.getName()));
		fmb.setJavaType(JdbcTypes.forTypeCode(column.getTypeCode()).getDefaultReadType());
		
		FieldDomain domain = context.getMetadata().domains().tryGetFieldDomain(emb.getEntityName(), fmb.getFieldName());
		
		if(null != domain){
			configFieldMappingByDomain(emb, fmb, domain);
		}
		
		preMappingField(context, emb, fmb);
		postMappingField(context, emb, fmb);
		
		return fmb;
    }

    @Override
    public FieldMappingBuilder createFieldMappingByTemplate(MetadataContext context, EntityMappingBuilder emb, FieldMappingBuilder template) {
        FieldMappingBuilder fmb = new FieldMappingBuilder(template);

        FieldDomain domain = context.getMetadata().domains().tryGetFieldDomain(emb.getEntityName(), fmb.getFieldName());
        if(null != domain){
            configFieldMappingByDomain(emb, fmb, domain);
        }

        preMappingField(context, emb, fmb);
        postMappingField(context, emb, fmb);

        return fmb;
    }

    @Override
	public FieldMappingBuilder createFieldMappingByDomain(MetadataContext context,EntityMappingBuilder emb,String domainName){
		FieldDomain domain = context.getMetadata().domains().getFieldDomain(domainName);
		
		FieldMappingBuilder fmb = new FieldMappingBuilder();
		fmb.setFieldName(domain.getName());
		fmb.setDomain(domain);
		
		configFieldMappingByDomain(emb, fmb, domain);
		
		preMappingField(context, emb, fmb);
		postMappingField(context, emb, fmb);
		
		return fmb;
	}

	@Override
	public FieldMappingBuilder createFieldMappingByJoinField(MetadataContext         context,
													         EntityMappingBuilder    localEntity,
													         EntityMappingBuilder    targetEntity,
													         RelationMappingBuilder  relation,
													         JoinFieldMappingBuilder joinField){
		
		FieldMappingBuilder ref = targetEntity.findFieldMappingByName(joinField.getReferencedFieldName());
		if(null == ref){
			throw new IllegalStateException("The referenced field '" + joinField.getReferencedFieldName() + "' does not exists");
		}
		
		FieldMappingBuilder local = new FieldMappingBuilder();
		local.setFieldName(joinField.getLocalFieldName());
		local.setJavaType(ref.getJavaType());
		local.setDataType(ref.getDataType());
		
		DbColumnBuilder col = new DbColumnBuilder(ref.getColumn().build());
		//clear some properties
		col.setDefaultValue(null);//clear default value
		col.setPrimaryKey(false);
		col.setAutoIncrement(false);
		col.setUnique(false);
		
		//set other properties
		if(!Strings.isEmpty(joinField.getLocalColumnName())){
			col.setName(joinField.getLocalColumnName());
		}else{
			col.setName(context.getNamingStrategy().fieldToColumnName(local.getFieldName()));
		}

        if(null != relation.getOptional()) {
            col.setNullable(relation.getOptional());
        }

		local.setColumn(col);
		
		return local;
    }
	
	@Override
    public void updateFieldMappingByJoinField(MappingConfigContext context, 
    									      EntityMappingBuilder emb, 
    									      EntityMappingBuilder targetEmb,
    									      RelationMappingBuilder rmb, 
    									      JoinFieldMappingBuilder jfmb, 
    									      FieldMappingBuilder lfmb) {

		FieldMappingBuilder rfmb = targetEmb.findFieldMappingByName(jfmb.getReferencedFieldName());

		if(!lfmb.getColumn().isPrimaryKey() && null != rmb.getOptional()) {
			lfmb.setNullable(rmb.getOptional());
			lfmb.getColumn().setNullable(rmb.getOptional());
		}

		//force update length,precision,scale
		lfmb.setMaxLength(rfmb.getMaxLength());
		lfmb.setPrecision(rfmb.getPrecision());
		lfmb.setScale(rfmb.getScale());

		lfmb.getColumn().setLength(rfmb.getColumn().getLength());
		lfmb.getColumn().setPrecision(rfmb.getColumn().getPrecision());
		lfmb.getColumn().setScale(rfmb.getColumn().getScale());
    }

    /*
    public void configFieldMappingConventional(MetadataContext context, FieldMappingBuilder fmb) {
        DbColumnBuilder c = fmb.getColumn();
        if (Strings.isEmpty(c.getName())) {
            c.setName(context.getNamingStrategy().fieldToColumnName(fmb.getFieldName()));
        } else {
            c.setName(context.getNamingStrategy().columnName(c.getName()));
        }

        MType dataType = fmb.getDataType();
        if(null != dataType && dataType.isSimpleType()){
            MSimpleType st = dataType.asSimpleType();

            if(null == c.getTypeName()){
                c.setTypeName(st.getJdbcType().getName());
            }

            if(null == c.getLength()){
                c.setLength(st.getDefaultLength());
            }

            if(null == c.getPrecision()){
                c.setPrecision(st.getDefaultPrecision());
            }

            if(null == c.getScale()){
                c.setScale(st.getDefaultScale());
            }
        }

        c.trySetScale(fmb.getScale());
        c.trySetNullable(fmb.getNullable());
        c.trySetLength(fmb.getMaxLength());
        c.trySetPrecision(fmb.getPrecision());
        c.trySetDefaultValue(fmb.getDefaultValue());

        //Auto set optimistic lock
        if(fmb.getFieldName().equalsIgnoreCase(context.getConfig().getOptimisticLockFieldName()) &&
                isOptimisticLockFieldType(fmb.getColumn().getTypeCode())){
            fmb.setOptimisticLock(true);
            fmb.setNewOptimisticLockFieldName(context.getNamingStrategy().getFieldNameForNewValue(fmb.getFieldName()));
        }

        //Auto set reservedMetaFieldName
        if(null == fmb.getReservedMetaFieldName()) {
            fmb.setReservedMetaFieldName(ReservedMetaFieldName.tryForName(fmb.getFieldName()));
        }
    }
    */

    @Override
    public void configFieldMappingByDomain(EntityMappingBuilder emb, FieldMappingBuilder f, FieldDomain d) {
		DbColumnBuilder c = f.getColumn();
		
		f.setDomain(d);
		
		if(null != d.getType()){
			f.trySetFieldType(d.getType().getDefaultReadType());
			c.trySetTypeCode(d.getType().getCode());
		}
		
		if(Strings.isEmpty(c.getName()) && !Strings.isEmpty(d.getDefaultColumnName())) {
		    c.setName(d.getDefaultColumnName());
		}

		f.trySetNullable(d.getNullable());
		f.trySetMaxLength(d.getLength());
		f.trySetPrecision(d.getPrecision());
		f.trySetScale(d.getScale());
		f.trySetDefaultValue(d.getDefaultValue());
		f.trySetInsert(d.getInsert());
		f.trySetUpdate(d.getUpdate());
		f.trySetInsertValue(d.getInsertValue());
		f.trySetUpdateValue(d.getUpdateValue());
		f.trySetSortOrder(d.getSortOrder());

        c.trySetScale(d.getScale());
        c.trySetNullable(d.getNullable());
        c.trySetLength(d.getLength());
        c.trySetPrecision(d.getPrecision());
        c.trySetDefaultValue(d.getDefaultValue());
    }

	protected void preMapping(MetadataContext context,EntityMappingBuilder emb){
		for(MappingProcessor p : processors){
			p.preMapping(context, emb);
		}
	}
	
	protected void preMappingEntity(MetadataContext context,EntityMappingBuilder emb){
		preMappingEntityConventional(context, emb);
		
		for(MappingProcessor p : processors){
			p.preMappingEntity(context, emb);
		}
	}
	
	protected void postMappingEntity(MetadataContext context,EntityMappingBuilder emb){
		postMappingEntityConventional(context, emb);

		for(MappingProcessor p : processors){
			p.postMappingEntity(context, emb);
		}

	}
	
	protected void preMappingField(MetadataContext context,EntityMappingBuilder emb,FieldMappingBuilder fmb){
		preMappingFieldConventional(context, emb, fmb);
		
		for(MappingProcessor p : processors){
			p.preMappingField(context, emb, fmb);
		}
	}
	
	protected void postMappingField(MetadataContext context,EntityMappingBuilder emb,FieldMappingBuilder fmb){
		postMappingFieldConventional(context, emb, fmb);
		
		for(MappingProcessor p : processors){
			p.postMappingField(context, emb, fmb);
		}
	}
	
	protected void preMappingRelation(MetadataContext context,EntityMappingBuilder emb,RelationMappingBuilder rmb) {
		for(MappingProcessor p : processors){
			p.preMappingRelation(context, emb, rmb);
		}
	}
	
	protected void postMappingRelation(MetadataContext context,EntityMappingBuilder emb,RelationMappingBuilder rmb) {
		for(MappingProcessor p : processors){
			p.postMappingRelation(context, emb, rmb);
		}
	}
	
	protected void postMapping(MetadataContext context,EntityMappingBuilder emb) {
		postMappingConventional(context, emb);

		for(MappingProcessor p : processors){
			p.postMapping(context, emb);
		}
		
		postMappingFinally(context, emb);
	}
	
	protected void preMappingEntityConventional(MetadataContext context,EntityMappingBuilder emb){
		Class<?> sourceClass = emb.getSourceClass();
		if(null != sourceClass){
			emb.setAbstract(Modifier.isAbstract(emb.getSourceClass().getModifiers()));	
		}
	}
	
	protected void postMappingEntityConventional(MetadataContext context,EntityMappingBuilder emb){
		Class<?> sourceClass = emb.getSourceClass();
		
		if(Strings.isEmpty(emb.getEntityName())){
			emb.setEntityName(sourceClass.getSimpleName());
		}
		emb.setEntityName(context.getNamingStrategy().entityName(emb.getEntityName()));
		
		EntityDomain domain = emb.getDomain();
		if(null == domain){
			domain = context.getMetadata().domains().tryGetEntityDomainByNameOrAlias(emb.getEntityName());
			if(null != domain){
				emb.setDomain(domain);
			}
		}
		
		if(Strings.isEmpty(emb.getTableName())){
			emb.setTableName(context.getNamingStrategy().entityToTableName(emb.getEntityName()));
		}else{
			emb.setTableName(context.getNamingStrategy().tableName(emb.getTableName()));
		}
	}
	
	protected void preMappingFieldConventional(MetadataContext context, EntityMappingBuilder emb,FieldMappingBuilder fmb){

	}
	
	protected void postMappingFieldConventional(MetadataContext context, EntityMappingBuilder emb,FieldMappingBuilder fmb){
        if(Strings.isEmpty(fmb.getFieldName())){
            fmb.setFieldName(fmb.getBeanProperty().getName());
        }
        fmb.setFieldName(context.getNamingStrategy().fieldName(fmb.getFieldName()));

        if(null != fmb.getIdGenerator()) {
            fmb.getColumn().trySetLength(fmb.getIdGenerator().getDefaultColumnLength());
        }

        if(null == fmb.getDataType()){
            Class<?> javaType = fmb.getJavaType();
            if(null != javaType) {
                TypeInfo ti = leap.lang.Types.getTypeInfo(javaType);

                if(ti.isSimpleType()) {
                    MSimpleType dataType = MSimpleTypes.forClass(javaType);
                    if(null == dataType){
                        throw new MetadataException("Unsupported java type '" + javaType +
                                "' in field '" + fmb.getBeanProperty().getName() + "', class '" + emb.getEntityClass().getName() + "'");
                    }
                    fmb.setDataType(dataType);
                }else{
                    //Found a serialize field.
                    String format = fmb.getSerializeFormat();
                    if(Strings.isEmpty(format)) {
                        format = context.getConfig().getDefaultSerializer();
                    }

                    OrmConfig.SerializeConfig sc =
                            context.getConfig().getSerializeConfig(format);

                    DbColumnBuilder column = fmb.getColumn();
                    column.trySetTypeCode(sc.getDefaultColumnType().getCode());
                    column.trySetLength(sc.getDefaultColumnLength());

                    FieldSerializer serializer =
                            context.getAppContext().getBeanFactory().tryGetBean(FieldSerializer.class, format);

                    if(null == serializer) {
                        throw new AppConfigException("Bean '" + format + "' of type '" +
                                                     FieldSerializer.class.getName() + "' must be exists!");
                    }

                    fmb.setSerializer(serializer);
                }
            }
        }

        if(null == fmb.getDomain()){
            String       entityName   = emb.getEntityName();
            String       fieldName    = fmb.getFieldName();
            EntityDomain entityDomain = emb.getDomain();
            FieldDomain  fieldDomain  = null;

            if(null != entityDomain){
                fieldDomain = context.getMetadata().domains().tryGetFieldDomain(entityDomain,fieldName);
            }else{
                fieldDomain = context.getMetadata().domains().tryGetFieldDomain(entityName,fieldName);
            }
			NonDomain nonDomain = Classes.getAnnotation(fmb.getAnnotations(),NonDomain.class);
			if(null != fieldDomain && fieldDomain.isAutoMapping() && nonDomain == null) {
                log.trace("Found domain '{}' matched the field '{}' of entity '{}'",fieldDomain.getName(),fieldName,entityName);
                configFieldMappingByDomain(emb, fmb, fieldDomain);
            }
        }

        DbColumnBuilder c = fmb.getColumn();
        if (Strings.isEmpty(c.getName())) {
            c.setName(context.getNamingStrategy().fieldToColumnName(fmb.getFieldName()));
        } else {
            c.setName(context.getNamingStrategy().columnName(c.getName()));
        }

        MType dataType = fmb.getDataType();
        if(null != dataType && dataType.isSimpleType()){
            MSimpleType st = dataType.asSimpleType();

            if(null == c.getTypeName()){
                c.setTypeName(st.getJdbcType().getName());
            }

            c.trySetLength(st.getDefaultLength());
            c.trySetPrecision(st.getDefaultPrecision());
            c.trySetScale(st.getDefaultScale());
        }

        //Auto set optimistic lock
        if(fmb.getFieldName().equalsIgnoreCase(context.getConfig().getOptimisticLockFieldName()) &&
                isOptimisticLockFieldType(fmb.getColumn().getTypeCode())){
            fmb.setOptimisticLock(true);
            fmb.setNewOptimisticLockFieldName(context.getNamingStrategy().getFieldNameForNewValue(fmb.getFieldName()));
        }

        //Auto set reservedMetaFieldName
        if(null == fmb.getReservedMetaFieldName()) {
            fmb.setReservedMetaFieldName(ReservedMetaFieldName.tryForName(fmb.getFieldName()));
        }
	}
	
	protected boolean isOptimisticLockFieldType(int typeCode){
		return Types.INTEGER == typeCode || Types.BIGINT == typeCode;
	}
	
	protected void postMappingConventional(MetadataContext context, EntityMappingBuilder emb){
		//Auto recognize primary key
		if(!emb.hasPrimaryKey()){
			for(FieldMappingBuilder fmb : emb.getFieldMappings()){
				if(fmb.getFieldName().equalsIgnoreCase(OrmConstants.ID)){
					fmb.getColumn().setPrimaryKey(true);
					break;
				}
			}
		}
		
		//Ensure primary key exists : auto created identity field if table not exists
		if(!emb.hasPrimaryKey()){
			DbTable table = emb.getPhysicalTable();
			if(null == table){
				FieldMappingBuilder primaryKey = createAutoIdentityMapping(context, emb);
				log.warn("primary key of "+ emb.getEntityName() + 
						" is not exists, leap will auto create a primary key named "+
						primaryKey.getFieldName() +"[column:"+ primaryKey.getColumnName() +
						"] for it.");
				emb.addPrimaryKey(primaryKey);
			}
		}
	}
	
	protected void postMappingFinally(MetadataContext context, EntityMappingBuilder emb){
		//Auto set id generator
		List<FieldMappingBuilder> idFieldMappings = emb.getIdFieldMappings();
		if(idFieldMappings.size() == 1){
			FieldMappingBuilder fmb = idFieldMappings.get(0);
			
			if(fmb.getIdGenerator() == null){
				fmb.setIdGenerator(defaultIdGenerator);
			}
			
			fmb.getIdGenerator().mapping(context, emb, fmb);
		}
		
		//Auto generate fields if db table not created in underlying database.
		//Only Model class can apply this feature.
        if(context.getConfig().isAutoGenerateColumns() && emb.isModel() && null == emb.getPhysicalTable()) {
            autoGeneratedFieldsForModel(context, emb);
        }
	}
	
	protected boolean checkTableExists(MetadataContext context,EntityMappingBuilder emb){
		DbMetadata dbmeta = context.getDb().getMetadata();
		return null != dbmeta.getSchema(emb.getTableCatalog(), emb.getTableSchema()).findTable(emb.getTableName());
	}
	
	protected void autoGeneratedFieldsForModel(MetadataContext context,EntityMappingBuilder emb){
	    Class<?> modelClass = emb.getModelClass();
	    if(null != modelClass) {
	        AutoGenerateColumns a = modelClass.getAnnotation(AutoGenerateColumns.class);
	        if(null != a && !a.value()) {
	            return;
	        }
	    }
	    
		float order = FieldMappingBuilder.LAST_SORT_ORDER;
		
		if(context.getConfig().isAutoGenerateOptimisticLock()) {
			if(emb.findFieldMappingByName(context.getConfig().getOptimisticLockFieldName()) == null){
				emb.addFieldMapping(
						createFieldMappingByDomain(context, emb, context.getConfig().getOptimisticLockFieldName())
						.setSortOrder((order++)));
			}
		}
		
		for(String autoGeneratedField : context.getConfig().getAutoGeneratedFieldNames()){
			if(emb.findFieldMappingByName(autoGeneratedField) == null && 
			   emb.findFieldMappingByMetaName(autoGeneratedField) == null){
				
				emb.addFieldMapping(
						createFieldMappingByDomain(context, emb, autoGeneratedField)
						.setSortOrder((order++)));
			}
		}
	}
}