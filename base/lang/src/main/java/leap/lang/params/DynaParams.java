/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package leap.lang.params;

import leap.lang.beans.DynaBean;

import java.util.Map;

public class DynaParams implements Params {

	private final DynaBean bean;

	public DynaParams(DynaBean bean) {
        this.bean = bean;
	}

    @Override
    public Map<String, Object> map() {
        return bean.getProperties();
    }

    @Override
    public boolean contains(String name) {
        return bean.getProperties().containsKey(name);
    }

    @Override
    public Object get(String name) {
        return bean.getProperty(name);
    }

    @Override
    public Params set(String name, Object value) {
        bean.setProperty(name, value);
        return this;
    }

    @Override
    public Params setAll(Map<String, ? extends Object> m) {
        bean.getProperties().putAll(m);
        return this;
    }

    @Override
    public boolean isEmpty() {
        return bean.getProperties().isEmpty();
    }
}