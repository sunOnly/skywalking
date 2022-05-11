## 9.1.0

#### Project

* Upgrade OAP dependencies zipkin to 2.23.16, H2 to 2.1.212, Apache Freemarker to 2.3.31, gRPC-java 1.46.0, netty to
  4.1.76.
* Upgrade Webapp dependencies, spring-cloud-dependencies to 2021.0.2, logback-classic to 1.2.11

#### OAP Server

* Add component definition(ID=127) for `Apache ShenYu (incubating)`.
* Fix Zipkin receiver: Decode spans error, missing `Layer` for V9 and wrong time bucket for generate Service and
  Endpoint.
* [Refactor] Move SQLDatabase(H2/MySQL/PostgreSQL), ElasticSearch and BanyanDB specific configurations out of column.
* Support BanyanDB global index for entities. Log and Segment record entities declare this new feature.
* Remove unnecessary analyzer settings in columns of templates. Many were added due to analyzer's default value.
* Simplify the Kafka Fetch configuration in cluster mode.
* [Breaking Change] Update the eBPF Profiling task to the service level, please delete
  index/table: `ebpf_profiling_task`, `process_traffic`.
* Fix event can't split service ID into 2 parts.
* Fix OAP Self-Observability metric `GC Time` calculation.
* Set `SW_QUERY_MAX_QUERY_COMPLEXITY` default value to `1000`
* Webapp module (for UI) enabled compression.
* [Breaking Change] Add layer field to event, report an event without layer is not allowed.
* Fix ES flush thread stops when flush schedule task throws exception, such as ElasticSearch flush failed.
* Fix ES BulkProcessor in BatchProcessEsDAO was initialized multiple times and created multiple ES flush schedule tasks.
* HTTPServer support the handler register with allowed HTTP methods.
* [Critical] Revert [**Enhance DataCarrier#MultipleChannelsConsumer to add
  priority**](https://github.com/apache/skywalking/pull/8664) to avoid consuming issues.
* Fix the problem that some configurations (such as group.id) did not take effect due to the override order when using
  the kafkaConsumerConfig property to extend the configuration in Kafka Fetcher.
* Remove build time from the OAP version.
* Add data-generator module to run OAP in testing mode, generating mock data for testing.
* Support receive Kubernetes processes from gRPC protocol.
* Fix the problem that es index(TimeSeriesTable, eg. endpoint_traffic, alarm_record) didn't create even after rerun with
  init-mode. This problem caused the OAP server to fail to start when the OAP server was down for more than a day.
* Support autocomplete tags in traces query.
* [Breaking Change] Replace all configurations `**_JETTY_**` to `**_REST_**`.
* Add the support eBPF profiling field into the process entity.
* E2E: fix log test miss verify LAL and metrics.
* Enhance Converter mechanism in kernel level to make BanyanDB native feature more effective.
* Add TermsAggregation properties collect_mode and execution_hint.
* Add "execution_hint": "map", "collect_mode": "breadth_first" for aggregation and topology query to improve 5-10x performance.
* Clean up scroll contexts after used.
* Support autocomplete tags in logs query.
* Enhance Deprecated MetricQuery(v1) getValues querying to asynchronous concurrency query
* Fix the pod match error when the service has multiple selector in kubernetes environment.
* VM monitoring adapts the 0.50.0 of the `opentelemetry-collector`.
* Add Envoy internal cost metrics.
* Remove `Layer` concept from `ServiceInstance`.
* Remove unnecessary `onCompleted` on gRPC `onError` callback.

#### UI

* General service instance: move `Thread Pool` from JVM to Overview, fix `JVM GC Count` calculation.
* Add Apache ShenYu (incubating) component LOGO.
* Show more metrics on service/instance/endpoint list on the dashboards.
* Support average values of metrics on the service/list/endpoint table widgets, with pop-up linear graph.
* Fix viewLogs button query no data.
* Fix UTC when page loads.
* Implement the eBPF profile widget on dashboard.
* Optimize the trace widget.
* Avoid invalid query for topology metrics.
* Add the alarm and log tag tips.
* Fix spans details and task logs.
* Verify query params to avoid invalid queries.
* Mobile terminal adaptation.
* Fix: set dropdown for the Tab widget, init instance/endpoint relation selectors, update sankey graph.
* Add eBPF Profiling widget into General service, Service Mesh and Kubernetes tabs.
* Fix jump to endpoint-relation dashboard template.
* Fix set graph options.
* Remove the `Layer` filed from the Instance and Process.

#### Documentation

All issues and pull requests are [here](https://github.com/apache/skywalking/milestone/128?closed=1)
