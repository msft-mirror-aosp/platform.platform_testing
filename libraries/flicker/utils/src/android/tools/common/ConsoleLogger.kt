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

class ConsoleLogger : ILogger {
    override fun v(tag: String, msg: String) = println("(V) $tag $msg")

    override fun d(tag: String, msg: String) = println("(D) $tag $msg")

    override fun i(tag: String, msg: String) = println("(I) $tag $msg")

    override fun w(tag: String, msg: String) = println("(W) $tag $msg")

    override fun e(tag: String, msg: String, error: Throwable?) {
        println("(e) $tag $msg $error")
        error?.printStackTrace()
    }

    override fun <T> withTracing(name: String, predicate: () -> T): T =
        try {
            println("(withTracing#start) $name")
            predicate()
        } finally {
            println("(withTracing#end) $name")
        }
}
