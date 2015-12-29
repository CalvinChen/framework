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
package leap.core.cache;

import java.util.Map;

/**
 * All implementations must be thread safe.
 */
public interface Cache<K,V> {

	/**
	 * Gets an entry mapping to the given key in this cache.
	 * 
	 * <p>
	 * Returns <code>null</code> if the entry does not exists. 
	 */
	V get(K key);
	
	/**
	 * Returns a new created map contains all the cached items.
	 */
	Map<K, V> getAll();
	
	/**
	 * Associates the specified value with the specified key in the cache.
	 */
	void put(K key,V value);
	
	/**
	 * Returns <code>true</code> if the entry mapping to the given key is present.
	 * 
	 * <p>
	 * Returns <code>false</code> if the given key does not exists.
	 */
	boolean containsKey(K key);

	/**
	 * Removes the entry mapping to the given key if it is present.
	 * 
	 * <p>
	 * Returns <code>true</code> if the entry is present.
	 * 
	 * <p>
	 * Returns <code>false</code> if the entry does not exists.
	 */
	boolean remove(K key);
	
	/**
	 * Removes the entry mapping to the given key if it is present.
	 * 
	 * <p>
	 * Returns the entry's value if it is present.
	 * 
	 * <p>
	 * Returns <code>null</code> it the entry does not exists.
	 */
	V getAndRemove(K key);
	
	/**
	 * Clears all entries in this cache. 
	 */
	void clear();
}
