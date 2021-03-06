/*
 * Copyright 2015 the original author or authors.
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
package leap.web.api;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import leap.core.BeanFactory;
import leap.core.annotation.Inject;
import leap.core.ioc.PostCreateBean;
import leap.core.meta.MTypeManager;
import leap.lang.Args;
import leap.lang.Strings;
import leap.lang.Types;
import leap.lang.exception.ObjectExistsException;
import leap.lang.exception.ObjectNotFoundException;
import leap.lang.http.QueryStringBuilder;
import leap.lang.net.Urls;
import leap.lang.reflect.ReflectClass;
import leap.lang.reflect.ReflectField;
import leap.web.App;
import leap.web.AppInitializable;
import leap.web.api.annotation.Resource;
import leap.web.api.annotation.ResourceWrapper;
import leap.web.api.config.*;
import leap.web.api.config.model.ApiModelConfig;
import leap.web.api.config.model.OAuthConfig;
import leap.web.api.meta.ApiMetadata;
import leap.web.api.meta.ApiMetadataFactory;
import leap.web.api.meta.model.MApiResponse;
import leap.web.api.meta.model.MApiPermission;
import leap.web.api.mvc.ApiInitializable;
import leap.web.api.permission.ResourcePermissions;
import leap.web.route.Route;

public class DefaultApis implements Apis, AppInitializable,PostCreateBean {
	
	protected @Inject ApiConfiguratorFactory configuratorFactory;
	protected @Inject ApiConfigProcessor[]   configProcessors;
	protected @Inject ApiMetadataFactory     metadataFactory;
	protected @Inject MTypeManager           typeManager;

	protected Map<String, ApiConfigurator> configurators  = new ConcurrentHashMap<String, ApiConfigurator>();
	protected Map<String, ApiConfig>       configurations = new ConcurrentHashMap<String, ApiConfig>();
	protected Map<String, ApiMetadata>     metadatas      = new ConcurrentHashMap<String, ApiMetadata>();

    protected OAuthConfig oauthConfig = new OAuthConfig(false,null,null);

    protected Map<String, MApiResponse>     commonResponses  = new LinkedHashMap<>();
    protected Map<Class<?>, ApiModelConfig> commonModelTypes = new LinkedHashMap<>();

    @Override
    public ApiConfigurator tryGetConfigurator(String name) {
        return configurators.get(name.toLowerCase());
    }

    @Override
    public Set<ApiConfigurator> getConfigurators() {
	    return new LinkedHashSet<>(configurators.values());
    }
	
	@Override
    public Set<ApiConfig> getConfigurations() {
	    return new LinkedHashSet<>(configurations.values());
    }

    @Override
    public ApiMetadata tryGetMetadata(String name) {
        return metadatas.get(name.toLowerCase());
    }

    @Override
    public ApiConfigurator add(String name, String basePath) throws ObjectExistsException {
		Args.notEmpty(name,     "name");
		Args.notEmpty(basePath, "basePath");
		
		String key = name.toLowerCase();
		if(configurators.containsKey(key)) {
			throw new ObjectExistsException("The api '" + name + "' already exists");
		}
 
		ApiConfigurator configurator = configuratorFactory.createConfigurator(this, name, basePath);
		configurators.put(key, configurator);
		configurations.put(key, configurator.config());
		return configurator;
    }

	@Override
    public ApiConfigurator getConfigurator(String name) throws ObjectNotFoundException {
		Args.notEmpty(name, "name");
		
		ApiConfigurator c = configurators.get(name.toLowerCase());
		if(null == c) {
			throw new ObjectNotFoundException("The api '" + name + "' not found");
		}
	    return c;
    }
	
	@Override
    public boolean isDefaultOAuthEnabled() {
        return oauthConfig.isEnabled();
    }

    @Override
    public String getDefaultOAuthAuthorizationUrl() {
        return oauthConfig.getAuthzEndpointUrl();
    }

    @Override
    public String getDefaultOAuthTokenUrl() {
        return oauthConfig.getTokenEndpointUrl();
    }

    @Override
    public Map<String, MApiResponse> getCommonResponses() {
        return commonResponses;
    }

    @Override
    public Map<Class<?>, ApiModelConfig> getCommonModelTypes() {
        return commonModelTypes;
    }

    @Override
    public Apis setDefaultOAuthEnabled(boolean enabled) {
        oauthConfig.setEnabled(enabled);
        return this;
    }

    @Override
    public Apis setDefaultOAuthAuthorizationUrl(String url) {
        this.oauthConfig.setAuthzEndpointUrl(url);
        return this;
    }
    
    @Override
    public Apis setDefaultOAuthAuthorizationUrl(String endpoint, String clientId, String redirectUri) {
        Args.notEmpty(endpoint, "endpoint");
        
        QueryStringBuilder qs = new QueryStringBuilder();
        qs.add("client_id", clientId)
          .add("redirect_uri", redirectUri)
          .add("response_type", "token");
        
        this.oauthConfig.setAuthzEndpointUrl(Urls.appendQueryString(endpoint, qs.build()));
        return this;
    }

    @Override
    public Apis setDefaultOAuthTokenUrl(String url) {
        this.oauthConfig.setTokenEndpointUrl(url);
        return this;
    }

    @Override
    public void postCreate(BeanFactory factory) throws Throwable {
        ApiConfigurations configs = factory.getAppConfig().getExtension(ApiConfigurations.class);
        if(configs == null){
            return;
        }

        if(configs.getDefaultOAuthConfig() != null){
            this.oauthConfig = configs.getDefaultOAuthConfig();
        }

        configs.getCommonResponses().forEach((key, builder)->{
            builder.setTypeManager(typeManager);
            commonResponses.put(key,builder.build());
        });

        configs.getCommonModelTypes().forEach((t,c) -> {
            commonModelTypes.put(t,c);
        });

        configs.getConfigurators().forEach((name, api)->{

            //oauth TODO
            if(api.config().getOAuthConfig() == null){
                api.setOAuthConfig(oauthConfig);
            }else{
                OAuthConfig oc = api.config().getOAuthConfig();

                if(Strings.isEmpty(oc.getAuthzEndpointUrl())){
                    oc.setAuthzEndpointUrl(oauthConfig.getAuthzEndpointUrl());
                }

                if(Strings.isEmpty(oc.getTokenEndpointUrl())){
                    oc.setTokenEndpointUrl(oauthConfig.getTokenEndpointUrl());
                }
            }

            //common model types.
            commonModelTypes.forEach((t,c) -> {
                if(!api.config().getModelTypes().containsKey(t)) {
                    api.putModelType(t, c);
                }
            });

            configurators.put(name.toLowerCase(), api);
            configurations.put(name.toLowerCase(), api.config());
        });
    }

    @Override
    public void postAppInit(App app) throws Throwable {

		for(Entry<String, ApiConfigurator> entry : configurators.entrySet()) {
			doConfiguration(app, entry.getValue());
		}

        for(Entry<String, ApiConfigurator> entry : configurators.entrySet()) {
            ApiConfigurator c = entry.getValue();

            //configure by processors.
            for(ApiConfigProcessor p : configProcessors) {
                p.preProcess(c);
            }

            for(ApiConfigProcessor p : configProcessors) {
                p.postProcess(c.config());
            }

            //post config
            postConfigApi(app, c.config());

            //create metadata
            String key = entry.getKey();
            ApiMetadata m = createMetadata(c);
            metadatas.put(key, m);

            //post load
            postLoadApi(app, c.config(), m);
        }
	}
	
	protected void doConfiguration(App app, ApiConfigurator c) {
        //common
        doCommonConfiguration(app, c);

		//resolve routes of api.
		resolveRoutes(app, c);

	}

    protected void doCommonConfiguration(App app, ApiConfigurator c) {
        //todo : oauth

        //todo : cors

        commonResponses.forEach(c::putCommonResponse);
    }
	
    protected void resolveRoutes(App app, ApiConfigurator c) {
		String basePath	= c.config().getBasePath();
		for(Route route : app.routes()) {
			String pathTemplate = route.getPathTemplate().getTemplate();
			if(pathTemplate.equals(basePath) || pathTemplate.startsWith(basePath)) {
				
				if(!c.config().isCorsDisabled() && !route.isCorsDisabled()) {
					route.setCorsEnabled(true);
				}

                resolveResourceType(app, c, route);

				c.addRoute(route);
			}
		}
	}

    protected void resolveResourceType(App app, ApiConfigurator c , Route route) {
        Class<?> resourceType = null;

        Resource resource = route.getAction().searchAnnotation(Resource.class);
        if(null != resource) {
            resourceType = resource.value();
        }

        if(null != route.getAction().getController()) {
            ResourceWrapper rw = route.getAction().getControllerAnnotation(ResourceWrapper.class);
            if(null != rw) {
                Class<?> cls = route.getAction().getController().getClass();

                if(cls.getTypeParameters().length == 1) {
                    resourceType = cls.getTypeParameters()[0].getGenericDeclaration();
                }else {
                    Class<?>[] types = Types.getActualTypeArguments(cls.getGenericSuperclass());
                    if(types.length == 1) {
                        resourceType = types[0];
                    }
                }
            }
        }

        if(null != resourceType) {
            c.setResourceType(route, resourceType);

            resolveResourcePermissions(app, c, route, resourceType);
        }
    }

    protected void resolveResourcePermissions(App app, ApiConfigurator c, Route route, Class<?> resourceType) {
        if(null != route.getPermissions()) {
            return;
        }

        ResourcePermissions rps =
                c.config().getResourcePermissionsSet().tryGetResourcePermissions(resourceType);

        if(null == rps) {
            return;
        }

        MApiPermission[] permissions = rps.resolvePermissions(route, resourceType);
        if(null != permissions && permissions.length > 0) {
            String[] values = new String[permissions.length];
            for(int i=0;i<values.length;i++) {
                values[i] = permissions[i].getValue();
            }

            route.setPermissions(values);

            for(MApiPermission p : permissions) {
                c.tryAddPermission(p);
            }
        }
    }

    protected void postConfigApi(App app, ApiConfig c) {
        for(Route route : c.getRoutes()) {
            if(c.isDefaultAnonymous() && null == route.getAllowAnonymous()) {
                route.setAllowAnonymous(true);
            }
        }
    }

    protected void postLoadApi(App app, ApiConfig c, ApiMetadata m) {

        Set<Object> controllers = new HashSet<>();

        for(Route route : c.getRoutes()) {
            //Inject ApiConfig & ApiMetadata.
            Object controller = route.getAction().getController();
            if(null != controller && !controllers.contains(controller)) {
                controllers.add(controller);

                ReflectClass rc = ReflectClass.of(controller.getClass());

                for(ReflectField rf : rc.getFields()) {
                    if(rf.getType().equals(ApiConfig.class)) {
                        rf.setValue(controller, c);
                        break;
                    }

                    if(rf.getType().equals(ApiMetadata.class)) {
                        rf.setValue(controller, m);
                    }
                }

                if(controller instanceof ApiInitializable) {
                    ((ApiInitializable) controller).postApiInitialized(c, m);
                }

            }
        }

        controllers.clear();
    }

	protected ApiMetadata createMetadata(ApiConfigurator c) {
		return metadataFactory.createMetadata(c.config());
	}
}