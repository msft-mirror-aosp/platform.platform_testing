package android.platform.test.coverage;

import static com.google.common.truth.Truth.assertThat;

import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.metrics.proto.MetricMeasurement.Metric;
import com.android.tradefed.result.TestRunResult;
import com.android.tradefed.targetprep.TargetSetupError;
import com.android.tradefed.testtype.DeviceJUnit4ClassRunner;
import com.android.tradefed.testtype.junit4.BaseHostJUnit4Test;
import com.android.tradefed.testtype.junit4.DeviceTestRunOptions;

import org.junit.Test;
import org.junit.runner.RunWith;

/** Runs an instrumentation test and verifies the coverage report. */
@RunWith(DeviceJUnit4ClassRunner.class)
public final class CoverageSmokeTest extends BaseHostJUnit4Test {

    private static final String COVERAGE_MEASUREMENT_KEY = "coverageFilePath";

    @Test
    public void instrumentationTest_generatesJavaCoverage()
            throws DeviceNotAvailableException, TargetSetupError {
        installPackage("CoverageInstrumentationSampleTest.apk");

        runCoverageDeviceTests();

        TestRunResult testRunResult = getLastDeviceRunResults();
        Metric devicePathMetric = testRunResult.getRunProtoMetrics().get(COVERAGE_MEASUREMENT_KEY);
        assertThat(devicePathMetric).isNotNull();
        String testCoveragePath = devicePathMetric.getMeasurements().getSingleString();
        assertThat(testCoveragePath).isNotNull();
    }

    private void runCoverageDeviceTests() throws DeviceNotAvailableException {
        DeviceTestRunOptions options =
                new DeviceTestRunOptions("android.platform.test.coverage")
                        .setTestClassName(
                                "android.platform.test.coverage.CoverageInstrumentationTest")
                        .setTestMethodName("testCoveredMethod")
                        .addInstrumentationArg("coverage", "true");
        runDeviceTests(options);
    }
}
