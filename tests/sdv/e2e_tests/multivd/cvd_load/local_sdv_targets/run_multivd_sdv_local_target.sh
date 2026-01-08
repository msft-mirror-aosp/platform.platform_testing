# Copyright (C) 2026 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

source ~/udc-swcar-dev/build/envsetup.sh
lunch sdv_core_cf-trunk_staging-userdebug
cvd reset -y
m

for fname in ~/udc-swcar-dev/vendor/google_testing/software_defined_vehicle/tests/multivd/cvd_load/local_sdv_targets/sdv.json
do
   sed -e "s=/tmp/vsoc_x86_64=$ANDROID_PRODUCT_OUT=;" -e "s=/tmp/linux-x86=$ANDROID_HOST_OUT=;" $fname > /tmp/sdv.json
done

cvd load /tmp/sdv.json
make catbox
NOTIFY_AS_NATIVE=0.0.0.0:6520,0.0.0.0:6521 ./out/host/linux-x86/catbox/android-catbox/tools/catbox-tradefed run commandAndExit sdv-local-targets-load-test --{device1}serial 0.0.0.0:6520 --{device2}serial 0.0.0.0:6521