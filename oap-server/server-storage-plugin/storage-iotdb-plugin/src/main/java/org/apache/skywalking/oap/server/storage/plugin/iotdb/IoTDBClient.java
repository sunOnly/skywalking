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

package org.apache.skywalking.oap.server.storage.plugin.iotdb;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.iotdb.rpc.IoTDBConnectionException;
import org.apache.iotdb.rpc.StatementExecutionException;
import org.apache.iotdb.rpc.TSStatusCode;
import org.apache.iotdb.session.pool.SessionDataSetWrapper;
import org.apache.iotdb.session.pool.SessionPool;
import org.apache.iotdb.tsfile.file.metadata.enums.TSDataType;
import org.apache.iotdb.tsfile.read.common.Field;
import org.apache.iotdb.tsfile.read.common.RowRecord;
import org.apache.skywalking.oap.server.core.storage.StorageData;
import org.apache.skywalking.oap.server.core.storage.type.Convert2Entity;
import org.apache.skywalking.oap.server.core.storage.type.StorageBuilder;
import org.apache.skywalking.oap.server.library.client.Client;
import org.apache.skywalking.oap.server.library.client.healthcheck.DelegatedHealthChecker;
import org.apache.skywalking.oap.server.library.client.healthcheck.HealthCheckable;
import org.apache.skywalking.oap.server.library.util.HealthChecker;
import org.apache.skywalking.oap.server.storage.plugin.iotdb.base.IoTDBInsertRequest;
import org.apache.skywalking.oap.server.storage.plugin.iotdb.utils.IoTDBDataConverter;
import org.apache.skywalking.oap.server.storage.plugin.iotdb.utils.IoTDBUtils;

@Slf4j
public class IoTDBClient implements Client, HealthCheckable {
    private final DelegatedHealthChecker healthChecker = new DelegatedHealthChecker();
    private final IoTDBStorageConfig config;

    private SessionPool sessionPool;
    private final String storageGroup;

    public static final String DOT = ".";
    public static final String ALIGN_BY_DEVICE = " align by device";

    public static final String TIME_BUCKET = "time_bucket";
    public static final String TIME = "Time";
    public static final String TIMESTAMP = "timestamp";

    public IoTDBClient(IoTDBStorageConfig config) {
        this.config = config;
        storageGroup = config.getStorageGroup();
    }

    @Override
    public void connect() throws IoTDBConnectionException, StatementExecutionException {
        try {
            final int sessionPoolSize = config.getSessionPoolSize() == 0 ?
                    Runtime.getRuntime().availableProcessors() * 2 : config.getSessionPoolSize();
            log.info("SessionPool Size: {}", sessionPoolSize);
            sessionPool = new SessionPool(config.getHost(), config.getRpcPort(),
                                          config.getUsername(), config.getPassword(),
                                          sessionPoolSize, false, false);
            sessionPool.setStorageGroup(storageGroup);

            healthChecker.health();
        } catch (StatementExecutionException e) {
            if (e.getStatusCode() != TSStatusCode.PATH_ALREADY_EXIST_ERROR.getStatusCode()) {
                healthChecker.unHealth(e);
                throw e;
            }
        }
    }

    @Override
    public void shutdown() {
        sessionPool.close();
        this.healthChecker.health();
    }

    @Override
    public void registerChecker(HealthChecker healthChecker) {
        this.healthChecker.register(healthChecker);
    }

    public SessionPool getSessionPool() {
        return sessionPool;
    }

    public IoTDBStorageConfig getConfig() {
        return config;
    }

    /**
     * Write data to IoTDB
     *
     * @param request an IoTDBInsertRequest
     * @throws IOException IoTDBConnectionException or StatementExecutionException
     */
    public void write(IoTDBInsertRequest request) throws IOException {
        if (log.isDebugEnabled()) {
            log.debug("Writing data to IoTDB: {}", request);
        }

        StringBuilder devicePath = new StringBuilder();
        devicePath.append(storageGroup).append(IoTDBClient.DOT).append(request.getModelName());
        try {
            // make an index value as a layer name of the storage path
            // every storage path has a fix order which has been set in IoTDBTableMetaInfo
            if (!request.getIndexes().isEmpty()) {
                request.getIndexValues().forEach(
                        value -> devicePath.append(IoTDBClient.DOT)
                                           .append(IoTDBUtils.indexValue2LayerName(value)));
            }
            sessionPool.insertRecord(devicePath.toString(), request.getTime(),
                                     request.getMeasurements(), request.getMeasurementTypes(),
                                     request.getMeasurementValues()
            );
            healthChecker.health();
        } catch (IoTDBConnectionException | StatementExecutionException e) {
            healthChecker.unHealth(e);
            throw new IOException(e);
        }
    }

    /**
     * Write a list of data into IoTDB
     *
     * @param requestList a list of IoTDBInsertRequest
     * @throws IOException IoTDBConnectionException or StatementExecutionException
     */
    public void write(List<IoTDBInsertRequest> requestList) throws IOException {
        if (log.isDebugEnabled()) {
            for (IoTDBInsertRequest request : requestList) {
                log.debug("Writing data to IoTDB: {}", request);
            }
        }

        List<String> devicePathList = new ArrayList<>();
        List<Long> timeList = new ArrayList<>();
        List<List<String>> timeseriesListList = new ArrayList<>();
        List<List<TSDataType>> typesList = new ArrayList<>();
        List<List<Object>> valuesList = new ArrayList<>();

        requestList.forEach(request -> {
            StringBuilder devicePath = new StringBuilder();
            devicePath.append(storageGroup).append(IoTDBClient.DOT).append(request.getModelName());
            // make an index value as a layer name of the storage path
            if (!request.getIndexes().isEmpty()) {
                request.getIndexValues().forEach(
                        value -> devicePath.append(IoTDBClient.DOT)
                                           .append(IoTDBUtils.indexValue2LayerName(value)));
            }
            devicePathList.add(devicePath.toString());
            timeList.add(request.getTime());
            timeseriesListList.add(request.getMeasurements());
            typesList.add(request.getMeasurementTypes());
            valuesList.add(request.getMeasurementValues());
        });

        try {
            sessionPool.insertRecords(devicePathList, timeList, timeseriesListList, typesList, valuesList);
            healthChecker.health();
        } catch (IoTDBConnectionException | StatementExecutionException e) {
            healthChecker.unHealth(e);
            throw new IOException(e);
        }
    }

    /**
     * Normal filter query for a list of data. querySQL must contain "align by device"
     *
     * @param modelName      model name
     * @param querySQL       the SQL for query which must contain "align by device"
     * @param storageBuilder storage builder for transforming storage result map to entity
     * @return a list of result data
     * @throws IOException IoTDBConnectionException or StatementExecutionException
     */
    public List<? super StorageData> filterQuery(String modelName, String querySQL,
                                                 StorageBuilder<? extends StorageData> storageBuilder)
            throws IOException {
        if (!querySQL.contains("align by device")) {
            throw new IOException("querySQL must contain \"align by device\"");
        }
        SessionDataSetWrapper wrapper = null;
        List<? super StorageData> storageDataList = new ArrayList<>();
        try {
            wrapper = sessionPool.executeQueryStatement(querySQL);
            if (log.isDebugEnabled()) {
                log.debug("SQL: {}, columnNames: {}", querySQL, wrapper.getColumnNames());
            }

            IoTDBTableMetaInfo tableMetaInfo = IoTDBTableMetaInfo.get(modelName);
            List<String> indexes = tableMetaInfo.getIndexes();
            List<String> columnNames = wrapper.getColumnNames();
            while (wrapper.hasNext()) {
                RowRecord rowRecord = wrapper.next();
                Convert2Entity convert2Entity =
                        new IoTDBDataConverter.ToEntity(tableMetaInfo, indexes, columnNames, rowRecord);
                storageDataList.add(storageBuilder.storage2Entity(convert2Entity));
            }
            healthChecker.health();
        } catch (IoTDBConnectionException | StatementExecutionException e) {
            healthChecker.unHealth(e);
            throw new IOException(e.getMessage() + System.lineSeparator() + "SQL Statement: " + querySQL, e);
        } finally {
            if (wrapper != null) {
                sessionPool.closeResultSet(wrapper);
            }
        }
        return storageDataList;
    }

    /**
     * Query with aggregation function: count, sum, avg, last_value, first_value, min_time, max_time, min_value,
     * max_value
     *
     * @param querySQL the SQL for query which should contain aggregation function
     * @return the result of aggregation function
     * @throws IOException IoTDBConnectionException or StatementExecutionException
     */
    public List<Double> queryWithAgg(String querySQL) throws IOException {
        SessionDataSetWrapper wrapper = null;
        List<Double> results = new ArrayList<>();
        try {
            wrapper = sessionPool.executeQueryStatement(querySQL);
            if (log.isDebugEnabled()) {
                log.debug("SQL: {}, columnNames: {}", querySQL, wrapper.getColumnNames());
            }

            if (wrapper.hasNext()) {
                RowRecord rowRecord = wrapper.next();
                List<Field> fields = rowRecord.getFields();
                for (Field field : fields) {
                    String stringValue = field.getStringValue();
                    if (!stringValue.equals("null")) {
                        results.add(Double.parseDouble(stringValue));
                    }
                }
            }
            healthChecker.health();
            return results;
        } catch (IoTDBConnectionException | StatementExecutionException e) {
            healthChecker.unHealth(e);
            throw new IOException(e.getMessage() + System.lineSeparator() + "SQL Statement: " + querySQL, e);
        } finally {
            if (wrapper != null) {
                sessionPool.closeResultSet(wrapper);
            }
        }
    }

    /**
     * Delete data &lt;= deleteTime in one timeseries
     *
     * @param device     device name
     * @param deleteTime deleteTime
     * @throws IOException IoTDBConnectionException or StatementExecutionException
     */
    public void deleteData(String device, long deleteTime) throws IOException {
        try {
            sessionPool.deleteData(storageGroup + IoTDBClient.DOT + device, deleteTime);
            healthChecker.health();
        } catch (IoTDBConnectionException | StatementExecutionException e) {
            healthChecker.unHealth(e);
            throw new IOException(e);
        }
    }

    public String getStorageGroup() {
        return storageGroup;
    }
}
