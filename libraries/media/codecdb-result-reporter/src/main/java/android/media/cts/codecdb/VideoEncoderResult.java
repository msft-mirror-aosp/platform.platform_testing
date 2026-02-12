/**
 * Copyright (C) 2026 The Android Open Source Project
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
package android.media.cts.codecdb;

import com.android.compatibility.common.util.ReportLog;
import com.android.compatibility.common.util.ResultType;
import com.android.compatibility.common.util.ResultUnit;
import com.google.auto.value.AutoBuilder;
import com.google.common.collect.ImmutableList;

/**
 * Data class representing the result of a video encoder test.
 */
public final record VideoEncoderResult(ImmutableList<Double> bitrates, ImmutableList<Double> vmafs)
        implements ReportLoggable {

    private static final String KEY_PREFIX = "result_";

    /**
     * Writes the result fields to the provided ReportLog.
     *
     * @param log The ReportLog to write to.
     */
    public void writeTo(ReportLog log) {
        log.addValues(
                KEY_PREFIX + "bitrate",
                bitrates.stream().mapToDouble(Double::doubleValue).toArray(),
                ResultType.NEUTRAL,
                ResultUnit.NONE);
        log.addValues(
                KEY_PREFIX + "vmaf",
                vmafs.stream().mapToDouble(Double::doubleValue).toArray(),
                ResultType.HIGHER_BETTER,
                ResultUnit.SCORE);
    }

    /**
     * Returns a new Builder for VideoEncoderResult.
     */
    public static Builder builder() {
        return new AutoBuilder_VideoEncoderResult_Builder();
    }

    /**
     * Builder for VideoEncoderResult.
     */
    @AutoBuilder
    public interface Builder {
        ImmutableList.Builder<Double> bitratesBuilder();

        default Builder addBitrate(double bitrate) {
            bitratesBuilder().add(bitrate);
            return this;
        }

        default Builder addBitrates(ImmutableList<Double> bitrates) {
            bitratesBuilder().addAll(bitrates);
            return this;
        }

        ImmutableList.Builder<Double> vmafsBuilder();

        default Builder addVmaf(double vmaf) {
            vmafsBuilder().add(vmaf);
            return this;
        }

        default Builder addVmafs(ImmutableList<Double> vmafs) {
            vmafsBuilder().addAll(vmafs);
            return this;
        }

        VideoEncoderResult build();
    }
}
