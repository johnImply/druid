/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.druid.catalog.guice;

import com.google.inject.Binder;
import org.apache.druid.catalog.MetadataCatalog;
import org.apache.druid.catalog.http.CatalogListenerResource;
import org.apache.druid.catalog.model.SchemaRegistry;
import org.apache.druid.catalog.model.SchemaRegistryImpl;
import org.apache.druid.catalog.sql.LiveCatalogResolver;
import org.apache.druid.catalog.sync.CachedMetadataCatalog;
import org.apache.druid.catalog.sync.CatalogClient;
import org.apache.druid.catalog.sync.CatalogClientConfig;
import org.apache.druid.catalog.sync.CatalogSource;
import org.apache.druid.catalog.sync.CatalogUpdateListener;
import org.apache.druid.catalog.sync.CatalogUpdateReceiver;
import org.apache.druid.discovery.NodeRole;
import org.apache.druid.guice.Jerseys;
import org.apache.druid.guice.JsonConfigProvider;
import org.apache.druid.guice.LazySingleton;
import org.apache.druid.guice.LifecycleModule;
import org.apache.druid.guice.ManageLifecycle;
import org.apache.druid.guice.annotations.ExcludeScope;
import org.apache.druid.guice.annotations.LoadScope;
import org.apache.druid.initialization.DruidModule;
import org.apache.druid.sql.calcite.planner.CatalogResolver;

/**
 * Configures the metadata catalog on the Broker and Overlord to use a cache
 * and network communications for pull and push updates.
 * <p>
 * {@link ExcludeScope} is set for this module when run as the coordinator due to conflicting bindings between this and
 * {@link CatalogCoordinatorModule} (this can occur when running in combined coordinator and overlord mode). The
 * bindings of {@link CatalogCoordinatorModule} can satisfy everything required by the overlord when operating in
 * combined mode.
 */
@LoadScope(roles = {NodeRole.BROKER_JSON_NAME, NodeRole.OVERLORD_JSON_NAME})
@ExcludeScope(roles = {NodeRole.COORDINATOR_JSON_NAME})
public class CatalogClientModule implements DruidModule
{
  @Override
  public void configure(Binder binder)
  {
    // config for catalog client
    JsonConfigProvider.bind(binder, "druid.catalog.client", CatalogClientConfig.class);

    // The catalog client maintains a cached metadata catalog.
    binder
        .bind(CachedMetadataCatalog.class)
        .in(LazySingleton.class);
    // Catalog metadata for the various purposes. This will override the base binding.
    binder
        .bind(MetadataCatalog.class)
        .to(CachedMetadataCatalog.class)
        .in(LazySingleton.class);

    // The cached metadata catalog needs a "pull" source, which is the network client.
    binder
        .bind(CatalogSource.class)
        .to(CatalogClient.class)
        .in(LazySingleton.class);

    // The cached metadata catalog is the listener for "push" events.
    binder
        .bind(CatalogUpdateListener.class)
        .to(CachedMetadataCatalog.class)
        .in(LazySingleton.class);

    // At present, the set of schemas is fixed.
    binder
        .bind(SchemaRegistry.class)
        .to(SchemaRegistryImpl.class)
        .in(LazySingleton.class);

    // Lifecycle-managed class to periodically the metadata cache
    binder
        .bind(CatalogUpdateReceiver.class)
        .in(ManageLifecycle.class);
    LifecycleModule.register(binder, CatalogUpdateReceiver.class);

    // Catalog resolver for the planner. This will override the base binding.
    binder
        .bind(CatalogResolver.class)
        .to(LiveCatalogResolver.class)
        .in(LazySingleton.class);

    // The listener resource sends to the catalog listener (the cached catalog.)
    Jerseys.addResource(binder, CatalogListenerResource.class);
  }
}
