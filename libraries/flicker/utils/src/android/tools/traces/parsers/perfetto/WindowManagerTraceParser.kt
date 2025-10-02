/*
 * Copyright (C) 2024 The Android Open Source Project
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

package android.tools.traces.parsers.perfetto

import android.tools.parsers.AbstractTraceParser
import android.tools.traces.wm.WindowManagerState
import android.tools.traces.wm.WindowManagerTrace

/** Parser for [WindowManagerTrace] objects containing traces */
class WindowManagerTraceParser :
    AbstractTraceParser<
        TraceProcessorSession,
        WindowManagerState,
        WindowManagerState,
        WindowManagerTrace,
    >() {
    override val traceName: String = "WM Trace"

    override fun doDecodeByteArray(bytes: ByteArray): TraceProcessorSession {
        error("This parser can only read from perfetto trace processor")
    }

    override fun createTrace(entries: Collection<WindowManagerState>): WindowManagerTrace =
        WindowManagerTrace(entries)

    override fun getEntries(input: TraceProcessorSession): List<WindowManagerState> {
        return input.query("INCLUDE PERFETTO MODULE android.winscope.windowmanager;") {
            val realToElapsedTimeOffsetNs =
                queryRealToElapsedTimeOffsetNs(input, SNAPSHOT_TABLE_NAME)
            val traceEntries = mutableListOf<WindowManagerState>()
            val snapshotIds = getSqlSnapshotIds(input)

            for (snapshotId in snapshotIds) {
                val entry =
                    input.query(getSqlQuerySnapshot(snapshotId)) { snapshotRows ->
                        val containerRows = input.query(getSqlQueryContainers(snapshotId)) { it }
                        buildTraceEntry(realToElapsedTimeOffsetNs, snapshotRows, containerRows)
                    }
                traceEntries.add(entry)
            }
            traceEntries
        }
    }

    override fun getTimestamp(entry: WindowManagerState) = entry.timestamp

    override fun doParseEntry(entry: WindowManagerState) = entry

    private fun buildTraceEntry(
        realToElapsedTimeOffsetNs: Long,
        snapshotRows: List<Row>,
        containerRows: List<Row>,
    ): WindowManagerState {
        val snapshotArgs = Args.build(snapshotRows)

        val containers =
            containerRows
                .groupBy { it["container_row_id"].toString() }
                .map { (rowId, rows) ->
                    val firstRow = rows[0]
                    Pair(
                        rowId,
                        WindowContainerBuilder()
                            .setArgs(Args.build(rows))
                            .setTitle(firstRow["title"] as String? ?: "")
                            .setToken((firstRow["token"] as Long).toInt())
                            .setParentToken(
                                firstRow["parent_token"]?.let { (it as Long).toInt() } ?: null
                            )
                            .setIsVisible(firstRow["is_visible"] == 1L)
                            .setContainerType(firstRow["container_type"] as String? ?: "")
                            .setNameOverride(firstRow["name_override"] as String?)
                            .build(),
                    )
                }
                .sortedBy { it.first.toInt() }
                .map { it.second }

        return WindowManagerStateBuilder()
            .setRealToElapsedTimeOffsetNs(realToElapsedTimeOffsetNs)
            .setEntry(snapshotArgs)
            .setContainers(containers)
            .build()
    }

    companion object {
        private const val SNAPSHOT_TABLE_NAME = "android_windowmanager"
        private const val CONTAINER_TABLE_NAME = "android_windowmanager_windowcontainer"

        private fun getSqlSnapshotIds(input: TraceProcessorSession): List<Long> {
            val sql =
                """
                SELECT id FROM $SNAPSHOT_TABLE_NAME ORDER BY ts;
            """
                    .trimIndent()
            return input.query(sql) { rows ->
                val ids = rows.map { it["id"] as Long }
                ids
            }
        }

        private fun getSqlQuerySnapshot(snapshotId: Long): String {
            return """
                       SELECT
                           args.key as key,
                           args.display_value as value,
                           args.value_type as value_type
                       FROM $SNAPSHOT_TABLE_NAME AS snapshot
                       INNER JOIN args ON snapshot.arg_set_id = args.arg_set_id
                       WHERE snapshot.id = $snapshotId;
                   """
                .trimIndent()
        }

        private fun getSqlQueryContainers(snapshotId: Long): String {
            return """
                       SELECT
                           container.snapshot_id,
                           container.id as container_row_id,
                           container.token,
                           container.title,
                           container.parent_token,
                           container.is_visible,
                           container.container_type,
                           args.key as key,
                           args.display_value as value,
                           args.value_type
                       FROM
                           $CONTAINER_TABLE_NAME as container
                       INNER JOIN args ON container.arg_set_id = args.arg_set_id
                       WHERE snapshot_id = $snapshotId
                       ORDER BY container.id;
            """
                .trimIndent()
        }
    }
}
