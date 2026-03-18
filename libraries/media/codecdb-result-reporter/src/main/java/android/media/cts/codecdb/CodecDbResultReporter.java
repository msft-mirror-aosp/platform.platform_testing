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

import com.android.compatibility.common.util.MetricsReportLog;
import com.android.compatibility.common.util.ReportLog;
import com.android.compatibility.common.util.ResultType;
import com.android.compatibility.common.util.ResultUnit;
import com.android.tradefed.build.IBuildInfo;

/**
 * Result reporter for CodecDB CTS tests.
 */
public class CodecDbResultReporter {

    private static final String REPORT_LOG_NAME = "CodecDbTestCases";
    private static final String STREAM_NAME = "veq_results";

    private final IBuildInfo mBuildInfo;
    private final String mAbi;
    private final String mClassMethodName;
    private final String mTestVersion;

    /**
     * Constructs a new CodecDbResultReporter.
     *
     * @param buildInfo The build info of the device.
     * @param abi The ABI of the device.
     * @param classMethodName The name of the test class and method.
     * @param testVersion The version of the test.
     */
    public CodecDbResultReporter(IBuildInfo buildInfo, String abi, String classMethodName,
            String testVersion) {
        mBuildInfo = buildInfo;
        mAbi = abi;
        mClassMethodName = classMethodName;
        mTestVersion = testVersion;
    }

    /**
     * Creates a new CodecDbResultReporter.
     *
     * @param buildInfo The build info of the device.
     * @param abi The ABI of the device.
     * @param classMethodName The name of the test class and method.
     * @param testVersion The version of the test.
     * @return A new CodecDbResultReporter instance.
     */
    public static CodecDbResultReporter create(IBuildInfo buildInfo, String abi,
            String classMethodName, String testVersion) {
        return new CodecDbResultReporter(buildInfo, abi, classMethodName, testVersion);
    }

    /**
     * Reports the result of a test.
     *
     * @param loggables A varargs array of ReportLoggable objects to report.
     */
    public void reportResult(ReportLoggable... loggables) {
        MetricsReportLog log =
                new MetricsReportLog(mBuildInfo, mAbi, mClassMethodName, REPORT_LOG_NAME,
                        STREAM_NAME);
        log.addValue("test_version", mTestVersion, ResultType.NEUTRAL, ResultUnit.NONE);
        for (ReportLoggable loggable : loggables) {
            if (loggable != null) {
                loggable.writeTo(log);
            }
        }
        log.submit();
    }
}
