/*
 * Licensed to Elastic Search and Shay Banon under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Elastic Search licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
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

package org.elasticsearch.index.mapper.xcontent.merge.test1;

import org.elasticsearch.index.mapper.DocumentMapper;
import org.elasticsearch.index.mapper.xcontent.MapperTests;
import org.elasticsearch.index.mapper.xcontent.XContentDocumentMapper;
import org.testng.annotations.Test;

import static org.elasticsearch.common.io.Streams.*;
import static org.elasticsearch.index.mapper.DocumentMapper.MergeFlags.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

/**
 * @author kimchy (shay.banon)
 */
@Test
public class Test1MergeMapperTests {

    @Test public void test1Merge() throws Exception {
        String stage1Mapping = copyToStringFromClasspath("/org/elasticsearch/index/mapper/xcontent/merge/test1/stage1.json");
        XContentDocumentMapper stage1 = MapperTests.newParser().parse(stage1Mapping);
        String stage2Mapping = copyToStringFromClasspath("/org/elasticsearch/index/mapper/xcontent/merge/test1/stage2.json");
        XContentDocumentMapper stage2 = MapperTests.newParser().parse(stage2Mapping);

        DocumentMapper.MergeResult mergeResult = stage1.merge(stage2, mergeFlags().simulate(true));
        assertThat(mergeResult.hasConflicts(), equalTo(false));
        // since we are simulating, we should not have the age mapping
        assertThat(stage1.mappers().smartName("age"), nullValue());
        // now merge, don't simulate
        mergeResult = stage1.merge(stage2, mergeFlags().simulate(false));
        // there is still merge failures
        assertThat(mergeResult.hasConflicts(), equalTo(false));
        // but we have the age in
        assertThat(stage1.mappers().smartName("age"), notNullValue());
    }
}
