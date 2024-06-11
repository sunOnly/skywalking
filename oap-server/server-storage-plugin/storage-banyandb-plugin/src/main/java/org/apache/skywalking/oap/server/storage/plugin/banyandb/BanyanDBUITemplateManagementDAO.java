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

package org.apache.skywalking.oap.server.storage.plugin.banyandb;

import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.banyandb.v1.client.TagAndValue;
import org.apache.skywalking.banyandb.v1.client.metadata.Property;
import org.apache.skywalking.oap.server.core.management.ui.template.UITemplate;
import org.apache.skywalking.oap.server.core.query.input.DashboardSetting;
import org.apache.skywalking.oap.server.core.query.type.DashboardConfiguration;
import org.apache.skywalking.oap.server.core.query.type.TemplateChangeStatus;
import org.apache.skywalking.oap.server.core.storage.management.UITemplateManagementDAO;
import org.apache.skywalking.oap.server.storage.plugin.banyandb.stream.AbstractBanyanDBDAO;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class BanyanDBUITemplateManagementDAO extends AbstractBanyanDBDAO implements UITemplateManagementDAO {
    public static final String GROUP = "sw";

    public BanyanDBUITemplateManagementDAO(BanyanDBStorageClient client) {
        super(client);
    }

    @Override
    public DashboardConfiguration getTemplate(String id) throws IOException {
        Property p = getClient().queryProperty(GROUP, UITemplate.INDEX_NAME, id);
        if (p == null) {
            return null;
        }
        return fromEntity(parse(p));
    }

    @Override
    public List<DashboardConfiguration> getAllTemplates(Boolean includingDisabled) throws IOException {
        List<Property> propertyList = getClient().listProperties(GROUP, UITemplate.INDEX_NAME);
        return propertyList.stream().map(p -> fromEntity(parse(p)))
                .filter(conf -> includingDisabled || !conf.isDisabled())
                .collect(Collectors.toList());
    }

    @Override
    public TemplateChangeStatus addTemplate(DashboardSetting setting) {
        Property newTemplate = applyAll(setting.toEntity());
        try {
            this.getClient().define(newTemplate);
            return TemplateChangeStatus.builder()
                    .status(true)
                    .id(newTemplate.id())
                    .build();
        } catch (IOException ioEx) {
            log.error("fail to add new template", ioEx);
            return TemplateChangeStatus.builder().status(false).id(setting.getId()).message("Can't add a new template")
                    .build();
        }
    }

    @Override
    public TemplateChangeStatus changeTemplate(DashboardSetting setting) {
        Property newTemplate = applyConfiguration(setting.toEntity());
        try {
            this.getClient().define(newTemplate);
            return TemplateChangeStatus.builder()
                    .status(true)
                    .id(newTemplate.id())
                    .build();
        } catch (IOException ioEx) {
            log.error("fail to modify the template", ioEx);
            return TemplateChangeStatus.builder().status(false).id(setting.getId()).message("Can't change an existed template")
                    .build();
        }
    }

    @Override
    public TemplateChangeStatus disableTemplate(String id) throws IOException {
        Property oldProperty = this.getClient().queryProperty(GROUP, UITemplate.INDEX_NAME, id);
        if (oldProperty == null) {
            return TemplateChangeStatus.builder().status(false).id(id).message("Can't find the template")
                    .build();
        }
        UITemplate uiTemplate = parse(oldProperty);
        try {
            this.getClient().define(applyStatus(uiTemplate));
            return TemplateChangeStatus.builder()
                    .status(true)
                    .id(uiTemplate.id().build())
                    .build();
        } catch (IOException ioEx) {
            log.error("fail to disable the template", ioEx);
            return TemplateChangeStatus.builder().status(false).id(uiTemplate.id().build()).message("Can't disable the template")
                    .build();
        }
    }

    public DashboardConfiguration fromEntity(UITemplate uiTemplate) {
        DashboardConfiguration conf = new DashboardConfiguration();
        conf.fromEntity(uiTemplate);
        return conf;
    }

    public UITemplate parse(Property property) {
        UITemplate uiTemplate = new UITemplate();
        uiTemplate.setTemplateId(property.id());

        for (TagAndValue<?> tagAndValue : property.tags()) {
            if (tagAndValue.getTagName().equals(UITemplate.CONFIGURATION)) {
                uiTemplate.setConfiguration((String) tagAndValue.getValue());
            } else if (tagAndValue.getTagName().equals(UITemplate.DISABLED)) {
                uiTemplate.setDisabled(((Number) tagAndValue.getValue()).intValue());
            } else if (tagAndValue.getTagName().equals(UITemplate.UPDATE_TIME)) {
                uiTemplate.setUpdateTime(((Number) tagAndValue.getValue()).longValue());
            }
        }
        return uiTemplate;
    }

    public Property applyAll(UITemplate uiTemplate) {
        return Property.create(GROUP, UITemplate.INDEX_NAME, uiTemplate.id().build())
                .addTag(TagAndValue.newStringTag(UITemplate.CONFIGURATION, uiTemplate.getConfiguration()))
                .addTag(TagAndValue.newLongTag(UITemplate.DISABLED, uiTemplate.getDisabled()))
                .addTag(TagAndValue.newLongTag(UITemplate.UPDATE_TIME, uiTemplate.getUpdateTime()))
                .build();
    }

    /**
     * Partial apply status, i.e. disable tag.
     *
     * @param uiTemplate previous UITemplate
     * @return new property (patch) to be applied
     */
    public Property applyStatus(UITemplate uiTemplate) {
        return Property.create(GROUP, UITemplate.INDEX_NAME, uiTemplate.id().build())
                .addTag(TagAndValue.newLongTag(UITemplate.DISABLED, uiTemplate.getDisabled()))
                .addTag(TagAndValue.newLongTag(UITemplate.UPDATE_TIME, uiTemplate.getUpdateTime()))
                .build();
    }

    /**
     * Partial apply configuration, i.e. configuration tag.
     *
     * @param uiTemplate previous UITemplate
     * @return new property (patch) to be applied
     */
    public Property applyConfiguration(UITemplate uiTemplate) {
        return Property.create(GROUP, UITemplate.INDEX_NAME, uiTemplate.id().build())
                .addTag(TagAndValue.newStringTag(UITemplate.CONFIGURATION, uiTemplate.getConfiguration()))
                .addTag(TagAndValue.newLongTag(UITemplate.UPDATE_TIME, uiTemplate.getUpdateTime()))
                .build();
    }
}
