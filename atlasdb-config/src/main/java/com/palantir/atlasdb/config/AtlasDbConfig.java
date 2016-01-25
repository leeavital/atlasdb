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
package com.palantir.atlasdb.config;

import org.immutables.value.Value;
import org.immutables.value.Value.Check;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.palantir.atlasdb.AtlasDbConstants;
import com.palantir.atlasdb.spi.AtlasDbFactory;
import com.palantir.atlasdb.spi.KeyValueServiceConfig;
import com.palantir.atlasdb.spi.TimestampServiceConfig;
import com.palantir.atlasdb.spi.TransactionServiceConfig;
import com.palantir.leader.NotCurrentLeaderException;

@JsonDeserialize(as = ImmutableAtlasDbConfig.class)
@JsonSerialize(as = ImmutableAtlasDbConfig.class)
@Value.Immutable
public abstract class AtlasDbConfig {

    public abstract KeyValueServiceConfig keyValueService();

    /**
     * This is server config that will determine how a timestamp service is created.
     * This config object will be passed to {@link AtlasDbFactory#createTimestampService(Optional, com.palantir.atlasdb.keyvalue.api.KeyValueService)}
     */
    public abstract Optional<TimestampServiceConfig> timestampService();

    /**
     * This is server config that will determine how a transaction service is created.
     * This config object will be passed to {@link AtlasDbFactory#createTransactionService(Optional, com.palantir.atlasdb.keyvalue.api.KeyValueService)}
     */
    public abstract Optional<TransactionServiceConfig> transactionService();

    /**
     * Server config to start a leader node.
     * <p>
     * A leader server also exposes a lock server and timestamp server that block
     * on leadership.  Only one of the servers will be active at one time.  The rest
     * will throw {@link NotCurrentLeaderException}.
     * <p>
     * If leader is specified {@link #timestamp()} and {@link #lock()} are not
     * needed and will just be set to the server list from
     * <code>leader().get().leaders()</code>
     */
    public abstract Optional<LeaderConfig> leader();

    /**
     * Client config to connect to a running lock server.
     */
    public abstract Optional<ServerListConfig> lock();

    /**
     * Client config to connect to a running timestamp server.
     */
    public abstract Optional<ServerListConfig> timestamp();

    /**
     * An {@link AtlasDbFactory} of this type will be constructed to create each
     * component.
     */
    @Value.Default
    public String getFactoryType() {
        return keyValueService().type();
    }

    /**
     * The transaction read timeout is the maximum amount of
     * time a read only transaction can safely run. Read only
     * transactions that run any longer may fail if they attempt
     * to perform additional reads.
     * <p>
     * The benefit of making this smaller is making overwritten
     * data 'unreadable' more quickly. This allows the background
     * sweeper to delete overwritten data sooner.
     */
    @Value.Default
    public long getTransactionReadTimeoutMillis() {
        return AtlasDbConstants.DEFAULT_TRANSACTION_READ_TIMEOUT;
    }

    /**
     * The punch interval is how frequently a row mapping the
     * current wall clock time to the maximum timestamp is
     * recorded.
     * <p>
     * These records allow wall clock times (used by the
     * transaction read timeout) to be translated to timestamps.
     */
    @Value.Default
    public long getPunchIntervalMillis() {
        return AtlasDbConstants.DEFAULT_PUNCH_INTERVAL_MILLIS;
    }

    /**
     * Scrubbing is the process of removing overwritten or deleted
     * cells from the underlying key value store after a hard-delete
     * transaction has committed (as opposed to shadowing such data,
     * which still leaves the data available to transactions that
     * started before the overwrite or deletion).
     * <p>
     * Scrubbing non-aggressively will cause scrubbing to be delayed
     * until the transaction read timeout passes ensuring that no
     * (well behaved, shorter than read timeout) transactions will
     * attempt to read scrubbed data. (Note: Badly behaved transactions
     * that do so will abort with an exception).
     * <p>
     * Scrubbing aggressively will cause the deletion to occur
     * immediately, which will cause any active transactions that
     * attempt to read the deleted cell to abort and fail with an
     * exception.
     */
    @Value.Default
    public boolean backgroundScrubAggressively() {
        return AtlasDbConstants.DEFAULT_BACKGROUND_SCRUB_AGGRESSIVELY;
    }

    /**
     * The number of background threads to use to perform scrubbing.
     */
    @Value.Default
    public int getBackgroundScrubThreads() {
        return AtlasDbConstants.DEFAULT_BACKGROUND_SCRUB_THREADS;
    }

    /**
     * The number of background threads to use to read from the scrub queue.
     */
    @Value.Default
    public int getBackgroundScrubReadThreads() {
        return AtlasDbConstants.DEFAULT_BACKGROUND_SCRUB_READ_THREADS;
    }

    /**
     * The frequency with which the background sweeper runs to clean up
     * cells that have been non-aggressively scrubbed.
     */
    @Value.Default
    public long getBackgroundScrubFrequencyMillis() {
        return AtlasDbConstants.DEFAULT_BACKGROUND_SCRUB_FREQUENCY_MILLIS;
    }

    /**
     * The number of cells to scrub per batch by the background scrubber.
     */
    @Value.Default
    public int getBackgroundScrubBatchSize() {
        return AtlasDbConstants.DEFAULT_BACKGROUND_SCRUB_BATCH_SIZE;
    }

    /**
     * If true, a background thread will periodically delete cells that
     * have been overwritten or deleted. This differs from scrubbing
     * because it is an untargeted cleaning process that scans all data
     * looking for cells to delete.
     */
    @Value.Default
    public boolean enableSweep() {
        return AtlasDbConstants.DEFAULT_ENABLE_SWEEP;
    }

    /**
     * The number of milliseconds to wait between each batch of cells
     * processed by the background sweeper.
     */
    @Value.Default
    public long getSweepPauseMillis() {
        return AtlasDbConstants.DEFAULT_SWEEP_PAUSE_MILLIS;
    }

    /**
     * The number of cells to process per batch by the background
     * sweeper.
     */
    @Value.Default
    public int getSweepBatchSize() {
        return AtlasDbConstants.DEFAULT_SWEEP_BATCH_SIZE;
    }

    @Check
    final void check() {
        if (leader().isPresent()) {
            Preconditions.checkArgument(!lock().isPresent(), "If leader is specified, lock will be set to the leader settings and may not be specified.");
            Preconditions.checkArgument(!timestamp().isPresent(), "If leader is specified, timestamp will be set to the leader settings and may not be specified.");
        }
    }
}
