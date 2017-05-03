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

package leap.orm.event;

import leap.lang.Buildable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EntityListenersBuilder implements Buildable<EntityListeners> {

    protected final Map<PreCreateListener,  Boolean> preCreateListeners = new LinkedHashMap<>();
    protected final Map<PostCreateListener, Boolean> postCreateListeners = new LinkedHashMap<>();
    protected final Map<PreUpdateListener,  Boolean> preUpdateListeners = new LinkedHashMap<>();
    protected final Map<PostUpdateListener, Boolean> postUpdateListeners = new LinkedHashMap<>();
    protected final Map<PreDeleteListener,  Boolean> preDeleteListeners = new LinkedHashMap<>();
    protected final Map<PostDeleteListener, Boolean> postDeleteListeners = new LinkedHashMap<>();
    protected final Map<PostLoadListener, Boolean>   postLoadListeners = new LinkedHashMap<>();

    public EntityListenersBuilder addPreCreateListener(PreCreateListener listener, boolean transactional) {
        preCreateListeners.put(listener, transactional);
        return this;
    }

    public EntityListenersBuilder addPostCreateListener(PostCreateListener listener, boolean transactional) {
        postCreateListeners.put(listener, transactional);
        return this;
    }

    public EntityListenersBuilder addPreUpdateListener(PreUpdateListener listener, boolean transactional) {
        preUpdateListeners.put(listener, transactional);
        return this;
    }

    public EntityListenersBuilder addPostUpdateListener(PostUpdateListener listener, boolean transactional) {
        postUpdateListeners.put(listener, transactional);
        return this;
    }

    public EntityListenersBuilder addPreDeleteListener(PreDeleteListener listener, boolean transactional) {
        preDeleteListeners.put(listener, transactional);
        return this;
    }

    public EntityListenersBuilder addPostDeleteListener(PostDeleteListener listener, boolean transactional) {
        postDeleteListeners.put(listener, transactional);
        return this;
    }

    public EntityListenersBuilder addPostLoadListener(PostLoadListener listener, boolean transactional) {
        postLoadListeners.put(listener, transactional);
        return this;
    }

    @Override
    public EntityListeners build() {

        List<PreCreateListener> noTransPreCreateListeners   = new ArrayList<>();
        List<PreCreateListener> inTransPreCreateListeners   = new ArrayList<>();
        List<PostCreateListener> noTransPostCreateListeners = new ArrayList<>();
        List<PostCreateListener> inTransPostCreateListeners = new ArrayList<>();
        preCreateListeners.forEach((listener, trans) -> {
            if(trans) {
                inTransPreCreateListeners.add(listener);
            } else {
                noTransPreCreateListeners.add(listener);
            }
        });
        postCreateListeners.forEach((listener, trans) -> {
            if(trans) {
                inTransPostCreateListeners.add(listener);
            } else {
                noTransPostCreateListeners.add(listener);
            }
        });


        List<PreUpdateListener> noTransPreUpdateListeners   = new ArrayList<>();
        List<PreUpdateListener> inTransPreUpdateListeners   = new ArrayList<>();
        List<PostUpdateListener> noTransPostUpdateListeners = new ArrayList<>();
        List<PostUpdateListener> inTransPostUpdateListeners = new ArrayList<>();
        preUpdateListeners.forEach((listener, trans) -> {
            if(trans) {
                inTransPreUpdateListeners.add(listener);
            } else {
                noTransPreUpdateListeners.add(listener);
            }
        });
        postUpdateListeners.forEach((listener, trans) -> {
            if(trans) {
                inTransPostUpdateListeners.add(listener);
            } else {
                noTransPostUpdateListeners.add(listener);
            }
        });

        List<PreDeleteListener> noTransPreDeleteListeners   = new ArrayList<>();
        List<PreDeleteListener> inTransPreDeleteListeners   = new ArrayList<>();
        List<PostDeleteListener> noTransPostDeleteListeners = new ArrayList<>();
        List<PostDeleteListener> inTransPostDeleteListeners = new ArrayList<>();
        preDeleteListeners.forEach((listener, trans) -> {
            if(trans) {
                inTransPreDeleteListeners.add(listener);
            } else {
                noTransPreDeleteListeners.add(listener);
            }
        });
        postDeleteListeners.forEach((listener, trans) -> {
            if(trans) {
                inTransPostDeleteListeners.add(listener);
            } else {
                noTransPostDeleteListeners.add(listener);
            }
        });

        List<PostLoadListener> noTransPostLoadListeners = new ArrayList<>();
        List<PostLoadListener> inTransPostLoadListeners = new ArrayList<>();
        postLoadListeners.forEach((listener, trans) -> {
            if(trans) {
                inTransPostLoadListeners.add(listener);
            } else {
                noTransPostLoadListeners.add(listener);
            }
        });

        return new EntityListeners(noTransPreCreateListeners.toArray(new PreCreateListener[0]),
                                   inTransPreCreateListeners.toArray(new PreCreateListener[0]),
                                   noTransPostCreateListeners.toArray(new PostCreateListener[0]),
                                   inTransPostCreateListeners.toArray(new PostCreateListener[0]),

                                    noTransPreUpdateListeners.toArray(new PreUpdateListener[0]),
                                    inTransPreUpdateListeners.toArray(new PreUpdateListener[0]),
                                    noTransPostUpdateListeners.toArray(new PostUpdateListener[0]),
                                    inTransPostUpdateListeners.toArray(new PostUpdateListener[0]),

                                    noTransPreDeleteListeners.toArray(new PreDeleteListener[0]),
                                    inTransPreDeleteListeners.toArray(new PreDeleteListener[0]),
                                    noTransPostDeleteListeners.toArray(new PostDeleteListener[0]),
                                    inTransPostDeleteListeners.toArray(new PostDeleteListener[0]),

                                    noTransPostLoadListeners.toArray(new PostLoadListener[0]),
                                    inTransPostLoadListeners.toArray(new PostLoadListener[0]));
    }

}
