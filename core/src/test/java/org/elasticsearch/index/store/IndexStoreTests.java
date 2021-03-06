/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.elasticsearch.index.store;

import com.carrotsearch.randomizedtesting.generators.RandomPicks;
import org.apache.lucene.store.*;
import org.apache.lucene.util.Constants;
import org.elasticsearch.Version;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.IndexModule;
import org.elasticsearch.index.IndexSettings;
import org.elasticsearch.index.shard.ShardId;
import org.elasticsearch.index.shard.ShardPath;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.test.IndexSettingsModule;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Locale;

/**
 */
public class IndexStoreTests extends ESTestCase {

    public void testStoreDirectory() throws IOException {
        final Path tempDir = createTempDir().resolve("foo").resolve("0");
        final IndexModule.Type[] values = IndexModule.Type.values();
        final IndexModule.Type type = RandomPicks.randomFrom(random(), values);
        Settings settings = Settings.settingsBuilder().put(IndexModule.STORE_TYPE, type.name().toLowerCase(Locale.ROOT))
                .put(IndexMetaData.SETTING_VERSION_CREATED, Version.CURRENT).build();
        IndexSettings indexSettings = IndexSettingsModule.newIndexSettings(new Index("foo"), settings, Collections.EMPTY_LIST);
        FsDirectoryService service = new FsDirectoryService(indexSettings, null, new ShardPath(false, tempDir, tempDir, "foo", new ShardId("foo", 0)));
        try (final Directory directory = service.newFSDirectory(tempDir, NoLockFactory.INSTANCE)) {
            switch (type) {
                case NIOFS:
                    assertTrue(type + " " + directory.toString(), directory instanceof NIOFSDirectory);
                    break;
                case MMAPFS:
                    assertTrue(type + " " + directory.toString(), directory instanceof MMapDirectory);
                    break;
                case SIMPLEFS:
                    assertTrue(type + " " + directory.toString(), directory instanceof SimpleFSDirectory);
                    break;
                case FS:
                case DEFAULT:
                   if (Constants.WINDOWS) {
                        if (Constants.JRE_IS_64BIT && MMapDirectory.UNMAP_SUPPORTED) {
                            assertTrue(type + " " + directory.toString(), directory instanceof MMapDirectory);
                        } else {
                            assertTrue(type + " " + directory.toString(), directory instanceof SimpleFSDirectory);
                        }
                    }  else if (Constants.JRE_IS_64BIT && MMapDirectory.UNMAP_SUPPORTED) {
                        assertTrue(type + " " + directory.toString(), directory instanceof FileSwitchDirectory);
                    } else  {
                        assertTrue(type + " " + directory.toString(), directory instanceof NIOFSDirectory);
                    }
                    break;
            }
        }
    }

    public void testStoreDirectoryDefault() throws IOException {
        final Path tempDir = createTempDir().resolve("foo").resolve("0");
        FsDirectoryService service = new FsDirectoryService(IndexSettingsModule.newIndexSettings(new Index("foo"), Settings.settingsBuilder().put(IndexMetaData.SETTING_VERSION_CREATED, Version.CURRENT).build(), Collections.EMPTY_LIST), null, new ShardPath(false, tempDir, tempDir, "foo", new ShardId("foo", 0)));
        try (final Directory directory = service.newFSDirectory(tempDir, NoLockFactory.INSTANCE)) {
            if (Constants.WINDOWS) {
                assertTrue(directory.toString(), directory instanceof MMapDirectory || directory instanceof SimpleFSDirectory);
            } else if (Constants.JRE_IS_64BIT) {
                assertTrue(directory.toString(), directory instanceof FileSwitchDirectory);
            } else {
                assertTrue(directory.toString(), directory instanceof NIOFSDirectory);
            }
        }
    }
}
