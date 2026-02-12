/**
 * Copyright (C) 2026 The Android Open Source Project
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package android.media.cts.codecdb;

import com.android.compatibility.common.util.ReportLog;
import com.android.compatibility.common.util.ResultType;
import com.android.compatibility.common.util.ResultUnit;

import com.google.auto.value.AutoBuilder;
import com.google.common.collect.ImmutableList;

import javax.annotation.Nullable;

/** Data class representing the configuration of a video encoder. */
public final record VideoEncoderConfig(
        String reference,
        ImmutableList<Long> bitrates,
        String codec,
        int maxBFrames,
        int profile,
        int level,
        @Nullable Integer priority,
        @Nullable Double operatingRate,
        int outputWidth,
        int outputHeight) implements ReportLoggable {

    private static final String KEY_PREFIX = "config_";

    /**
     * Writes the configuration fields to the provided ReportLog.
     *
     * @param log The ReportLog to write to.
     */
    public void writeTo(ReportLog log) {
        log.addValue(KEY_PREFIX + "reference", reference, ResultType.NEUTRAL, ResultUnit.NONE);
        log.addValues(
                KEY_PREFIX + "bitrate",
                bitrates.stream().mapToLong(Long::longValue).toArray(),
                ResultType.NEUTRAL,
                ResultUnit.NONE);
        log.addValue(KEY_PREFIX + "codec", codec, ResultType.NEUTRAL, ResultUnit.NONE);
        log.addValue(
                KEY_PREFIX + "max_b_frames", maxBFrames, ResultType.NEUTRAL, ResultUnit.NONE);
        log.addValue(KEY_PREFIX + "profile", profile, ResultType.NEUTRAL, ResultUnit.NONE);
        log.addValue(KEY_PREFIX + "level", level, ResultType.NEUTRAL, ResultUnit.NONE);
        if (priority != null) {
        log.addValue(KEY_PREFIX + "priority", priority, ResultType.NEUTRAL, ResultUnit.NONE);
        }
        if (operatingRate != null) {
            log.addValue(
                    KEY_PREFIX + "operating_rate",
                    operatingRate,
                    ResultType.NEUTRAL,
                    ResultUnit.NONE);
        }
        log.addValue(
                KEY_PREFIX + "output_width", outputWidth, ResultType.NEUTRAL, ResultUnit.NONE);
        log.addValue(
                KEY_PREFIX + "output_height",
                outputHeight,
                ResultType.NEUTRAL,
                ResultUnit.NONE);
    }

    /** Returns a new Builder for VideoEncoderConfig. */
    public static Builder builder() {
        return new AutoBuilder_VideoEncoderConfig_Builder();
    }

    /** Builder for VideoEncoderConfig. */
    @AutoBuilder
    public interface Builder {
        Builder setBitrates(ImmutableList<Long> value);

        ImmutableList.Builder<Long> bitratesBuilder();

        default Builder addBitrate(long bitrate) {
            bitratesBuilder().add(bitrate);
            return this;
        }

        default Builder addBitrates(ImmutableList<Long> bitrates) {
            bitratesBuilder().addAll(bitrates);
            return this;
        }

        Builder setReference(String value);

        Builder setCodec(String value);

        Builder setMaxBFrames(int value);

        Builder setProfile(int value);

        Builder setLevel(int value);

        Builder setPriority(@Nullable Integer value);

        Builder setOperatingRate(@Nullable Double value);

        Builder setOutputWidth(int value);

        Builder setOutputHeight(int value);

        VideoEncoderConfig build();
    }
}
