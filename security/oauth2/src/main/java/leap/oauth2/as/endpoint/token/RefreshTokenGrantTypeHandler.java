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
package leap.oauth2.as.endpoint.token;

import leap.core.annotation.Inject;
import leap.core.security.UserPrincipal;
import leap.lang.Strings;
import leap.oauth2.OAuth2Errors;
import leap.oauth2.OAuth2Params;
import leap.oauth2.as.OAuth2AuthzServerConfig;
import leap.oauth2.as.authc.AuthzAuthentication;
import leap.oauth2.as.authc.SimpleAuthzAuthentication;
import leap.oauth2.as.client.AuthzClient;
import leap.oauth2.as.client.AuthzClientManager;
import leap.oauth2.as.token.AuthzAccessToken;
import leap.oauth2.as.token.AuthzRefreshToken;
import leap.oauth2.as.token.AuthzTokenManager;
import leap.web.Request;
import leap.web.Response;
import leap.web.security.SecurityConfig;
import leap.web.security.authc.AuthenticationManager;
import leap.web.security.user.UserDetails;
import leap.web.security.user.UserManager;
import leap.web.security.user.UserStore;

import java.util.function.Consumer;

import static leap.oauth2.Oauth2MessageKey.*;

/**
 * grant_type=refresh_token
 */
public class RefreshTokenGrantTypeHandler extends AbstractGrantTypeHandler implements GrantTypeHandler {
	
	protected @Inject OAuth2AuthzServerConfig config;
	protected @Inject AuthzTokenManager       tokenManager;
	protected @Inject AuthenticationManager   authcManager;
	protected @Inject AuthzClientManager      clientManager;
	protected @Inject SecurityConfig          sc;
	protected @Inject UserManager             um;
	
	@Override
	public void handleRequest(Request request, Response response, OAuth2Params params, Consumer<AuthzAccessToken> callback) {
		String refreshToken = params.getRefreshToken();
		if(Strings.isEmpty(refreshToken)) {
			handleError(request,response,params,
					getOauth2Error(key -> OAuth2Errors.invalidRequestError(request,key,"refresh_token required"),INVALID_REQUEST_REFRESH_TOKEN_REQUIRED));
			return;
		}
		
		//Load token.
		AuthzRefreshToken token = tokenManager.loadRefreshToken(refreshToken);
		if(null == token) {
			handleError(request,response,params,
					getOauth2Error(key -> OAuth2Errors.invalidGrantError(request,key,"invalid refresh token"),ERROR_INVALID_GRANT_INVALID_REFRESH_TOKEN,refreshToken));
			return;
		}
		
		//Check expired?
		if(token.isExpired()) {
			tokenManager.removeRefreshToken(token);
			handleError(request,response,params,
					getOauth2Error(key -> OAuth2Errors.invalidGrantError(request,key,"refresh token expired"),
							ERROR_INVALID_GRANT_REFRESH_TOKEN_EXPIRED,refreshToken));
			return;
		}
		
		//Authenticate user.
		UserPrincipal user = null;
		if(!token.isClientOnly()) {
			//Authenticate user.
			UserStore   us = sc.getUserStore();
		    UserDetails ud = us.loadUserDetailsByIdString(token.getUserId());
			if(null == ud || !ud.isEnabled()) {
				tokenManager.removeRefreshToken(token);
				handleError(request,response,params,
						getOauth2Error(key -> OAuth2Errors.invalidGrantError(request,key,"invalid user"),INVALID_REQUEST_INVALID_USERNAME,token.getUserId()));
				return;
			}
			user = ud;
		}
		
		//Authenticate client.
		AuthzClient client = null;
		if(!Strings.isEmpty(token.getClientId())) {
			client = clientManager.loadClientById(token.getClientId());
			if(null == client || !client.isEnabled()) {
				tokenManager.removeRefreshToken(token);
				handleError(request,response,params,
						getOauth2Error(key -> OAuth2Errors.invalidGrantError(request,key,"invalid client"),INVALID_REQUEST_INVALID_CLIENT,token.getClientId()));
				return;
			}
		}
		
		AuthzAuthentication oauthAuthc = new SimpleAuthzAuthentication(params, client, um.getUserDetails(user));
		
		//Generates a new token.
		callback.accept(tokenManager.createAccessToken(oauthAuthc, token));
	}

}