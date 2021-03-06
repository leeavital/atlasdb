/**
 * Copyright 2015 Palantir Technologies
 *
 * Licensed under the BSD-3 License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.palantir.atlasdb.cli.services;

import java.util.Set;

import javax.net.ssl.SSLSocketFactory;

import org.immutables.value.Value;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.palantir.atlasdb.config.AtlasDbConfig;
import com.palantir.atlasdb.factory.TransactionManagers;
import com.palantir.atlasdb.spi.AtlasDbFactory;
import com.palantir.atlasdb.table.description.Schema;

@Value.Immutable
public abstract class ServicesConfig {

    public abstract AtlasDbConfig atlasDbConfig();

    @Value.Derived
    public AtlasDbFactory atlasDbFactory() {
        return TransactionManagers.getKeyValueServiceFactory(atlasDbConfig().keyValueService().type());
    }

    @Value.Default
    public Set<Schema> schemas() {
        return ImmutableSet.of();
    }

    @Value.Default
    public boolean allowAccessToHiddenTables() {
        return true;
    }

    public abstract Optional<SSLSocketFactory> sslSocketFactory();

}
