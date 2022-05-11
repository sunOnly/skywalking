/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.skywalking.oap.server.core.storage.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Sharding key is used to group time series data per metric of one entity in one place (same sharding and/or same
 * row for column-oriented database).
 * For example,
 * ServiceA's traffic gauge, service call per minute, includes following timestamp values, then it should be sharded
 * by service ID
 * [ServiceA(encoded ID): 01-28 18:30 values-1, 01-28 18:31 values-2, 01-28 18:32 values-3, 01-28 18:32 values-4]
 *
 * BanyanDB is the 1st storage implementation supporting this. It would make continuous time series metrics stored
 * closely and compressed better.
 *
 * 1. One entity could have multiple sharding keys
 * 2. If no column is appointed for this, {@link org.apache.skywalking.oap.server.core.storage.StorageData#id}
 * would be used by the storage implementation accordingly.
 *
 * NOTICE, this sharding concept is NOT just for splitting data into different database instances or physical
 * files.
 *
 * Only work with {@link Column}
 *
 * @return non-negative if this column be used for sharding. -1 means not as a sharding key
 * @since 9.1.0 created as a new annotation.
 * @since 9.0.0 added in {@link Column}
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface BanyanDBShardingKey {
    /**
     * Relative entity tag
     *
     * @return index, from zero.
     */
    int index() default -1;
}
