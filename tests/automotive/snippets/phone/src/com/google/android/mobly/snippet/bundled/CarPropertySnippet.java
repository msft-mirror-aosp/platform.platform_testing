/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.google.android.mobly.snippet.bundled;

import static com.google.common.util.concurrent.MoreExecutors.newDirectExecutorService;

import android.app.UiAutomation;
import android.car.VehicleGear;
import android.os.ParcelFileDescriptor;

import androidx.test.platform.app.InstrumentationRegistry;

import com.google.android.libraries.automotive.val.api.ErrorOr;
import com.google.android.libraries.automotive.val.api.SeatActions;
import com.google.android.libraries.automotive.val.api.SetResult;
import com.google.android.libraries.automotive.val.api.Temperature;
import com.google.android.libraries.automotive.val.api.UpdateTargetTemperatureRequest;
import com.google.android.libraries.automotive.val.api.VehicleActions;
import com.google.android.mobly.snippet.Snippet;
import com.google.android.mobly.snippet.rpc.Rpc;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class CarPropertySnippet implements Snippet {
    private static final String GEAR_SELECT_COMMAND =
            "cmd car_service inject-vhal-event GEAR_SELECTION ";

    private final VehicleActions mActions;

    private final UiAutomation mUiAutomation;

    public CarPropertySnippet() {
        mActions = new VehicleActions(
                InstrumentationRegistry.getInstrumentation().getContext(),
                newDirectExecutorService())
        ;

        mUiAutomation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
        mUiAutomation.adoptShellPermissionIdentity();
    }

    @Rpc(description = "Set the cabin temperature for the driver side")
    public void setDriverHvacTemperature(String fahrenheit) {
        ListenableFuture<ErrorOr<SetResult>> ignored = mActions
                .getSeatActions().setHvacTargetTemperature(new UpdateTargetTemperatureRequest(
                        ImmutableSet.of(SeatActions.SEAT_ROW_1_LEFT),
                        new Temperature(
                                Float.parseFloat(fahrenheit),
                                Temperature.TemperatureUnit.FAHRENHEIT
                        ),
                        true
                ));
    }

    @Rpc(description = "Get the desired cabin temperature for the driver side")
    public float getDriverHvacTemperature() {
        try {
            return mActions.getSeatActions().getHvacTargetTemperature(
                    ImmutableSet.of(SeatActions.SEAT_ROW_1_LEFT)
            ).get().value().elementToValue().get(SeatActions.SEAT_ROW_1_LEFT).value().component1();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while retrieving target temperature", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Target temperature retrieval threw an exception", e);
        }
    }

    @Rpc(description = "Select drive on the vehicle's transmission")
    public void shiftToDrive() {
        executeShellCommand(GEAR_SELECT_COMMAND + VehicleGear.GEAR_DRIVE);
    }

    @Rpc(description = "Select reverse on the vehicle's transmission")
    public void shiftToReverse() {
        executeShellCommand(GEAR_SELECT_COMMAND + VehicleGear.GEAR_REVERSE);
    }

    @Rpc(description = "Select neutral on the vehicle's transmission")
    public void shiftToNeutral() {
        executeShellCommand(GEAR_SELECT_COMMAND + VehicleGear.GEAR_NEUTRAL);
    }

    @Rpc(description = "Select park on the vehicle's transmission")
    public void shiftToPark() {
        executeShellCommand(GEAR_SELECT_COMMAND + VehicleGear.GEAR_PARK);
    }

    private void executeShellCommand(String command) {
        //noinspection EmptyTryBlock
        try (ParcelFileDescriptor ignored = mUiAutomation.executeShellCommand(command)) {

        } catch (IOException e) {
            throw new RuntimeException("IOException while executing command: " + command, e);
        }
    }
}
