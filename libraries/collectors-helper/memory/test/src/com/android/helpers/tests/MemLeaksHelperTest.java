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
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.doReturn;

import androidx.test.runner.AndroidJUnit4;

import com.android.helpers.MemLeaksHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.io.IOException;
import java.util.Map;

/**
 * Android Unit tests for {@link MemLeaksHelper}.
 *
 * <p>To run: atest CollectorsHelperTest:com.android.helpers.tests.MemLeaksHelperTest
 */
@RunWith(AndroidJUnit4.class)
public class MemLeaksHelperTest {
    private @Spy MemLeaksHelper mMemLeaksHelper;

    @Before
    public void setUp() {
        mMemLeaksHelper = new MemLeaksHelper();
        MockitoAnnotations.initMocks(this);
    }

    /**
     * Test the parser works if the dump contains the correct unreachable memory bytes and
     * allocations.
     */
    @Test
    public void testGetMetrics() throws IOException {
        assertTrue(mMemLeaksHelper.startCollecting());
        String memLeaksPidSampleOutput =
                "system  25905 410 13715708 78536 do_freezer_trap 0 S com.android.chrome";
        String memLeaksSampleOutput =
                "Applications Memory Usage (in Kilobytes):\n"
                    + "Uptime: 94638627 Realtime: 102961738\n"
                    + "** MEMINFO in pid 25905 [com.android.chrome] **\n"
                    + "                   Pss  Private  Private  SwapPss      Rss     Heap    "
                    + " Heap     Heap\n"
                    + "                 Total    Dirty    Clean    Dirty    Total     Size   "
                    + " Alloc     Free\n"
                    + "                ------   ------   ------   ------   ------   ------  "
                    + " ------   ------\n"
                    + "  Native Heap     2024      812        4     1230     4476    19112    "
                    + " 4070     2209\n"
                    + "  Dalvik Heap     4229     1292        0       24    10160    10462    "
                    + " 2270     8192\n"
                    + " Dalvik Other     1208      528        4      326     2736\n"
                    + "        Stack      138      136        0      124      144\n"
                    + "       Ashmem       16        0        0        0      892\n"
                    + "    Other dev       16        0       16        0      288\n"
                    + "     .so mmap     5934      120      732        0    16864\n"
                    + "    .jar mmap      679        0        0        0    25676\n"
                    + "    .apk mmap     1947        0      136        0     8540\n"
                    + "    .dex mmap    10153       20    10096        0    10592\n"
                    + "    .oat mmap     4961        0     2976        0     8240\n"
                    + "    .art mmap     4999      364      104      114    14080\n"
                    + "   Other mmap       97        4        0        0      676\n"
                    + "      Unknown      305      132        0      184      716\n"
                    + "        TOTAL    38708     3408    14068     2002   104080    29574    "
                    + " 6340    10401\n"
                    + " App Summary\n"
                    + "                       Pss(KB)                        Rss(KB)\n"
                    + "                        ------                         ------\n"
                    + "           Java Heap:     1760                          24240\n"
                    + "         Native Heap:      812                           4476\n"
                    + "                Code:    14096                          69980\n"
                    + "               Stack:      136                            144\n"
                    + "            Graphics:        0                              0\n"
                    + "       Private Other:      672\n"
                    + "              System:    21232\n"
                    + "             Unknown:                                    5240\n"
                    + "           TOTAL PSS:    38708            TOTAL RSS:   104080       TOTAL"
                    + " SWAP PSS:     2002\n"
                    + " \n"
                    + " Objects\n"
                    + "               Views:        0         ViewRootImpl:        0\n"
                    + "         AppContexts:        8           Activities:        0\n"
                    + "              Assets:       27        AssetManagers:        0\n"
                    + "       Local Binders:       15        Proxy Binders:       27\n"
                    + "       Parcel memory:        3         Parcel count:       14\n"
                    + "    Death Recipients:        0      OpenSSL Sockets:        0\n"
                    + "            WebViews:        0\n"
                    + " SQL\n"
                    + "         MEMORY_USED:        0\n"
                    + "  PAGECACHE_OVERFLOW:        0          MALLOC_SIZE:        0\n"
                    + " \n"
                    + " Unreachable memory\n"
                    + "  1632 bytes in 10 unreachable allocations\n"
                    + "  ABI: 'arm'\n"
                    + "\n"
                    + "  336 bytes unreachable at e31c1350\n"
                    + "   first 20 bytes of contents:\n"
                    + "   e31c1350: 88 13 e8 07 00 00 00 00 9e 9f 15 08 00 00 00 00"
                    + " ................\n"
                    + "   e31c1360: 2c bb 17 08 00 00 00 00 23 8a 89 08 00 00 00 00"
                    + " ,.......#.......\n"
                    + "\n"
                    + "  144 bytes unreachable at e86c9430\n"
                    + "   first 20 bytes of contents:\n"
                    + "   e86c9430: 09 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00"
                    + " ................\n"
                    + "   e86c9440: 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00"
                    + " ................\n"
                    + "\n";
        doReturn(memLeaksPidSampleOutput)
                .when(mMemLeaksHelper)
                .executeShellCommand(matches(mMemLeaksHelper.ALL_PROCESS));
        doReturn(memLeaksSampleOutput)
                .when(mMemLeaksHelper)
                .executeShellCommand(
                        matches(String.format(mMemLeaksHelper.DUMPSYS_MEMIFNO, 25905)));
        Map<String, Long> metrics = mMemLeaksHelper.getMetrics();

        assertFalse(metrics.isEmpty());
        assertTrue(metrics.containsKey(mMemLeaksHelper.PROC_MEM_BYTES + "com.android.chrome"));
        assertTrue(
                metrics.get(mMemLeaksHelper.PROC_MEM_BYTES + "com.android.chrome").equals(1632L));
        assertTrue(metrics.containsKey(mMemLeaksHelper.PROC_ALLOCATIONS + "com.android.chrome"));
        assertTrue(
                metrics.get(mMemLeaksHelper.PROC_ALLOCATIONS + "com.android.chrome").equals(10L));
    }

    /**
     * Test the parser works if the dump does not contain the unreachable memory bytes and
     * allocations.
     */
    @Test
    public void testNoUnreachableMemory() throws IOException {
        assertTrue(mMemLeaksHelper.startCollecting());
        String memLeaksPidSampleOutput =
                "root          8616     2       0      0 worker_thread       0 I [kworker/6:0]\n";
        String memLeaksSampleOutput =
                "Applications Memory Usage (in Kilobytes):\n"
                        + "Uptime: 433368844 Realtime: 441691956\n"
                        + "                   Pss  Private  Private     Swap      Rss     Heap   "
                        + "  Heap     Heap\n"
                        + "                 Total    Dirty    Clean    Dirty    Total     Size   "
                        + " Alloc     Free\n"
                        + "                ------   ------   ------   ------   ------   ------   "
                        + "------   ------\n"
                        + "  Native Heap        0        0        0        0        0        0   "
                        + "     0        0\n"
                        + "  Dalvik Heap        0        0        0        0        0        0   "
                        + "     0        0\n"
                        + "      Unknown        0        0        0        0        0            "
                        + "               \n"
                        + "        TOTAL        0        0        0        0        0        0   "
                        + "     0        0\n"
                        + " \n"
                        + " App Summary\n"
                        + "                       Pss(KB)                        Rss(KB)\n"
                        + "                        ------                         ------\n"
                        + "           Java Heap:        0                              0\n"
                        + "         Native Heap:        0                              0\n"
                        + "                Code:        0                              0\n"
                        + "               Stack:        0                              0\n"
                        + "            Graphics:        0                              0\n"
                        + "       Private Other:        0\n"
                        + "              System:        0\n"
                        + "             Unknown:                                       0\n"
                        + " \n"
                        + "           TOTAL PSS:        0            TOTAL RSS:        0      "
                        + "TOTAL SWAP (KB):        0\n";
        doReturn(memLeaksPidSampleOutput)
                .when(mMemLeaksHelper)
                .executeShellCommand(matches(mMemLeaksHelper.ALL_PROCESS));
        doReturn(memLeaksSampleOutput)
                .when(mMemLeaksHelper)
                .executeShellCommand(matches(String.format(mMemLeaksHelper.DUMPSYS_MEMIFNO, 8616)));
        Map<String, Long> metrics = mMemLeaksHelper.getMetrics();

        assertTrue(metrics.isEmpty());
    }
}
