/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package leap.core.config;

import leap.lang.convert.Converts;

/**
 * A wrapper interface for configuration property.
 *
 * @param <T> the type of property value.
 */
public interface Property<T> {

    /**
     * Returns the property value. May be null.
     */
    T get();

    /**
     * Converts the property value to the given type.
     */
    default <T1> T1 get(Class<T1> type) {
        return Converts.convert(get(), type);
    }

}