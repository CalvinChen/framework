/*
 * Copyright 2016 the original author or authors.
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
package leap.lang.path;

import java.util.Objects;

public abstract class AbstractPathPattern implements PathPattern,Comparable<PathPattern> {

    @Override
    public int hashCode() {
        return pattern().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof AntPathPattern) {
            return Objects.equals(pattern(), ((AntPathPattern) obj).pattern());
        }
        return false;
    }

    @Override
    public String toString() {
        return pattern();
    }

    @Override
    public int compareTo(PathPattern o) {
        return DEFAULT_COMPARATOR.compare(this, o);
    }
}