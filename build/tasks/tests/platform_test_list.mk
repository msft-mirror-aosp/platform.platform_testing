platform_tests += \
    ActivityManagerPerfTests \
    ActivityManagerPerfTestsStubApp1 \
    ActivityManagerPerfTestsStubApp2 \
    ActivityManagerPerfTestsStubApp3 \
    ActivityManagerPerfTestsTestApp \
    AdServicesScenarioTests \
    AndroidTVJankTests \
    AndroidXComposeStartupApp \
    ApiDemos \
    AppCompatibilityTest \
    AppLaunch \
    AppTransitionTests \
    BackgroundDexOptServiceIntegrationTests \
    BandwidthEnforcementTest \
    BandwidthTests \
    BootHelperApp \
    BusinessCard \
    CalculatorFunctionalTests \
    CalendarTests \
    camera_client_test \
    camera_metadata_tests \
    CellBroadcastReceiverTests \
    ConnectivityManagerTest \
    ContactsTests \
    CtsCameraTestCases \
    Development \
    DeviceHealthChecks \
    DynamicCodeLoggerIntegrationTests \
    DialerJankTests \
    DownloadManagerTestApp \
    StubIME \
    flatland \
    FrameworkPerf \
    FrameworkPermissionTests \
    FrameworksCoreTests \
    FrameworksMockingCoreTests \
    FrameworksPrivacyLibraryTests \
    FrameworksUtilTests \
    InternalLocTestApp \
    JankMicroBenchmarkTests \
    LauncherIconsApp \
    long_trace_binder_config.textproto \
    long_trace_config.textproto \
    MemoryUsage \
    OverviewFunctionalTests \
    perfetto_trace_processor_shell \
    PerformanceAppTest \
    PerformanceLaunch \
    PermissionFunctionalTests \
    PermissionTestAppMV1 \
    PermissionUtils \
    PlatformCommonScenarioTests \
    PowerPerfTest \
    SdkSandboxPerfScenarioTests \
    SettingsUITests \
    SimpleServiceTestApp1 \
    SimpleServiceTestApp2 \
    SimpleServiceTestApp3 \
    SimpleTestApp \
    sl4a \
    SmokeTest \
    SmokeTestApp \
    trace_config.textproto \
    trace_config_boot_time.textproto \
    trace_config_boot_time_stop.textproto \
    trace_config_detailed.textproto \
    trace_config_detailed_heapdump.textproto \
    trace_config_experimental.textproto \
    trace_config_multi_user_cuj_tests.textproto \
    trace_config_post_boot.textproto \
    trace_config_power.textproto \
    UbSystemUiJankTests \
    UbWebViewJankTests \
    UiBench \
    UiBenchJankTests \
    UiBenchJankTestsWear \
    UiBenchMicrobenchmark \
    uwb_snippet \
    WifiStrengthScannerUtil \
    wifi_direct_mobly_snippet \
    wifi_aware_snippet_new \

ifneq ($(strip $(BOARD_PERFSETUP_SCRIPT)),)
platform_tests += perf-setup
endif

ifneq ($(filter vsoc_arm vsoc_arm64 vsoc_x86 vsoc_x86_64, $(TARGET_BOARD_PLATFORM)),)
  platform_tests += \
    CuttlefishRilTests \
    CuttlefishWifiTests
endif

ifeq ($(HOST_OS),linux)
platform_tests += root-canal
endif
