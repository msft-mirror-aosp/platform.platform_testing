/*
 * Copyright (C) 2023 The Android Open Source Project
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

package android.tools.common

import org.junit.Assert
import org.junit.Before
import org.junit.Test

class CacheTest {
    @Before
    fun setup() {
        Cache.clear()
    }

    @Test
    fun testGet() {
        val element1 = Cache.get(Dummy(0))
        val element2 = Cache.get(Dummy(0))
        val element3 = Cache.get(Dummy(1))
        Assert.assertSame(element2, element1)
        Assert.assertNotEquals(element3, element1)
        Assert.assertEquals(2, Cache.size)
    }

    @Test
    fun testClear() {
        Cache.get(Dummy(0))
        Assert.assertEquals(1, Cache.size)
        Cache.clear()
        Assert.assertEquals(0, Cache.size)
    }

    @Test
    fun testBackup() {
        Cache.get(Dummy(0))
        Cache.get(Dummy(1))
        val copy = Cache.backup()
        Cache.get(Dummy(2))
        Assert.assertTrue(copy.cache.containsKey(Dummy(0)))
        Assert.assertTrue(copy.cache.containsKey(Dummy(1)))
        Assert.assertFalse(copy.cache.containsKey(Dummy(2)))
    }

    @Test
    fun testRestore() {
        Cache.get(Dummy(0))
        Cache.get(Dummy(1))
        val copy = Cache.backup()
        Cache.get(Dummy(2))
        Assert.assertEquals(3, Cache.size)
        Cache.restore(copy)
        Assert.assertTrue(copy.cache.containsKey(Dummy(0)))
        Assert.assertTrue(copy.cache.containsKey(Dummy(1)))
        Assert.assertFalse(copy.cache.containsKey(Dummy(2)))
    }

    data class Dummy(val value: Int)

    companion object {
        private val Cache.size
            get() = backup().cache.size
    }
}
