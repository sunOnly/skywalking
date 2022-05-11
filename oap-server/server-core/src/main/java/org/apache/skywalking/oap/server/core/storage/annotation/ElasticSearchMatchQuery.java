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
import lombok.Getter;

/**
 * Match query is designed for ElasticSearch match query with specific analyzer. It is a fuzzy query implementation
 * powered by analyzer.
 *
 * @since 9.1.0 This used to be {@link Column}'s matchQuery and analyzer attributes.
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ElasticSearchMatchQuery {
    /**
     * The storage analyzer mode.
     *
     * @since 9.1.0 created as a new annotation.
     * @since 8.4.0 added in {@link Column}
     */
    AnalyzerType analyzer() default AnalyzerType.OAP_ANALYZER;

    /**
     * The analyzer declares the text analysis mode.
     */
    enum AnalyzerType {
        /**
         * The default analyzer.
         */
        OAP_ANALYZER("oap_analyzer"),
        /**
         * The log analyzer.
         */
        OAP_LOG_ANALYZER("oap_log_analyzer");

        @Getter
        private final String name;

        AnalyzerType(final String name) {
            this.name = name;
        }
    }
}
