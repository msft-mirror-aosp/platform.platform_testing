/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.helpers.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.test.runner.AndroidJUnit4;
import com.android.helpers.HeapDumpHelper;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Android Unit tests for {@link HeapDumpHelper}.
 *
 * To run:
 * atest CollectorsHelperTest:com.android.helpers.tests.HeapDumpHelperTest
 */
@RunWith(AndroidJUnit4.class)
public class HeapDumpHelperTest {
    private static final String TAG = HeapDumpHelperTest.class.getSimpleName();
    private HeapDumpHelper mHeapDumpHelper;

    @Before
    public void setUp() {
        mHeapDumpHelper = new HeapDumpHelper();
    }

    @After
    public void tearDown() {
        if (mHeapDumpHelper.getResultsFile() != null) {
            mHeapDumpHelper.getResultsFile().delete();
        }
    }

    @Test
    public void testSuccessfulHeapDumpCollection() {
        assertTrue(mHeapDumpHelper.startCollecting(true, "sample-heapdump-1-"));
        Map<String, String> metrics = mHeapDumpHelper.getMetrics();
        assertTrue(metrics.size() == 1);
        assertTrue(
                mHeapDumpHelper.getResultsFile() != null
                        && mHeapDumpHelper.getResultsFile().length() > 0);
    }

    @Test
    public void testHeapDumpCollectionDisabled() {
        assertFalse(mHeapDumpHelper.startCollecting(false, "sample-heapdump-2-"));
        Map<String, String> metrics = mHeapDumpHelper.getMetrics();
        assertTrue(metrics.size() == 0);
    }

    @Test
    public void testHeapDumpNotCollectedWithEmptyId() {
        assertFalse(mHeapDumpHelper.startCollecting(true, ""));
        Map<String, String> metrics = mHeapDumpHelper.getMetrics();
        assertTrue(metrics.size() == 0);
    }

    @Test
    public void testHeapDumpNotCollectedWithNullId() {
        assertFalse(mHeapDumpHelper.startCollecting(true, null));
        Map<String, String> metrics = mHeapDumpHelper.getMetrics();
        assertTrue(metrics.size() == 0);
    }
}
