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

package leap.lang.meta;

public interface MTypeStrategy {

    MTypeStrategy DEFAULT = new MTypeStrategy() { };

    /**
     * Returns the name of complex type.
     */
    default String getComplexTypeName(Class<?> type) {
        return getComplexTypeName(type.getSimpleName());
    }

    /**
     * Returns the name of complex type.
     */
    default String getComplexTypeName(String name) {
        return name;
    }

    /**
     * Returns <code>null</code> if full qualified name is not supported.
     */
    default String getComplexTypeFqName(MComplexType ct, Class<?> type) {
        return null;
    }

}