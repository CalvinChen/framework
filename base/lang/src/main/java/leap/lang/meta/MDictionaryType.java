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

import leap.lang.New;

public class MDictionaryType extends MComplexType {

    public static final MDictionaryType INSTANCE = new MDictionaryType(MObjectType.TYPE,MObjectType.TYPE);

    private final MType keyType;
    private final MType valueType;

    public MDictionaryType(MType keyType, MType valueType) {
        super("dict", "dict", null, null, null, New.arrayList(), false);
        this.keyType   = keyType;
        this.valueType = valueType;
    }

    public MType getKeyType() {
        return keyType;
    }

    public MType getValueType() {
        return valueType;
    }

    @Override
    public final boolean isDictionaryType() {
        return true;
    }
}