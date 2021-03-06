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
package leap.oauth2.as.entity;

import java.util.HashMap;
import java.util.Map;

import leap.oauth2.OAuth2ExpirableEntity;
import leap.orm.annotation.Column;
import leap.orm.annotation.Id;
import leap.orm.annotation.Table;

@Table("oauth2_access_token")
public class AuthzAccessTokenEntity extends OAuth2ExpirableEntity {

    @Id
    @Token
    protected String token;

    @Column(name="token_type", length=20)
    protected String tokenType;

    @ClientId
    protected String clientId;

    @UserId
    protected String userId;

    @Token
    @Column("refresh_token")
    protected String refreshToken;

    @Column("ex_data")
	private Map<String,Object> exData=new HashMap<>();

    protected Boolean authenticated;

    @Column
    @Scope
    protected String scope;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public Boolean getAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(Boolean authenticated) {
        this.authenticated = authenticated;
    }

	public Map<String,Object> getExData() {
		return exData;
	}

	public void setExData(Map<String,Object> exData) {
		this.exData = exData;
	}
}