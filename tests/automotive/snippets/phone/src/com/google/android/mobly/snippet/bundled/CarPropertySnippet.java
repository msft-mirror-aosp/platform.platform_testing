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
    private final VehicleActions mActions;

    private static UiAutomation mUiAutomation;

    public CarPropertySnippet() {
        mActions = new VehicleActions(
                InstrumentationRegistry.getInstrumentation().getContext(),
                newDirectExecutorService()
        );

        mUiAutomation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
        mUiAutomation.adoptShellPermissionIdentity();
    }

    @Override
    public void shutdown() throws Exception {
        Snippet.super.shutdown();
        mUiAutomation.dropShellPermissionIdentity();
    }

    @Rpc(description = "Set the cabin temperature for the driver side")
    public void setDriverHvacTemperature(String fahrenheit) {
        setHvacTemperature(fahrenheit, SeatActions.SEAT_ROW_1_LEFT);
    }

    @Rpc(description = "Set the cabin temperature for the passenger side")
    public void setPassengerHvacTemperature(String fahrenheit) {
        setHvacTemperature(fahrenheit, SeatActions.SEAT_ROW_1_RIGHT);
    }

    private void setHvacTemperature(String fahrenheit, String seat) {
        ListenableFuture<ErrorOr<SetResult>> ignored = mActions
                .getSeatActions().setHvacTargetTemperature(new UpdateTargetTemperatureRequest(
                        ImmutableSet.of(seat),
                        new Temperature(
                                Float.parseFloat(fahrenheit),
                                Temperature.TemperatureUnit.FAHRENHEIT
                        ),
                        true
                ));
    }

    @Rpc(description = "Get the desired cabin temperature for the driver side")
    public float getDriverHvacTemperature() {
        return getHvacTemperature(SeatActions.SEAT_ROW_1_LEFT);
    }

    @Rpc(description = "Get the desired cabin temperature for the passenger side")
    public float getPassengerHvacTemperature() {
        return getHvacTemperature(SeatActions.SEAT_ROW_1_RIGHT);
    }

    private float getHvacTemperature(String seat) {
        try {
            return mActions.getSeatActions().getHvacTargetTemperature(
                    ImmutableSet.of(seat)
            ).get().value().elementToValue().get(seat).value().component1();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while retrieving target temperature", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Target temperature retrieval threw an exception", e);
        }
    }

    @Rpc(description = "Get the desired seat temperature for the driver side")
    public int getDriverSeatTemperature() {
        return getSeatTemperature(SeatActions.SEAT_ROW_1_LEFT);
    }

    @Rpc(description = "Get the desired seat temperature for the passenger side")
    public int getPassengerSeatTemperature() {
        return getSeatTemperature(SeatActions.SEAT_ROW_1_RIGHT);
    }

    private int getSeatTemperature(String seat) {
        try {
            return mActions.getSeatActions().getSeatHeatingLevel(
                    ImmutableSet.of(seat)
            ).get().value().elementToValue().get(seat).value();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while retrieving seat temperature", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Seat temperature retrieval threw an exception", e);
        }
    }
    @Rpc(description = "Select drive on the vehicle's transmission")
    public void shiftToDrive() {
        PropertyCommand.start()
                .set()
                .propertyName("GEAR_SELECTION")
                .intValue(VehicleGear.GEAR_DRIVE)
                .execute(this);
    }

    @Rpc(description = "Select reverse on the vehicle's transmission")
    public void shiftToReverse() {
        PropertyCommand.start()
                .set()
                .propertyName("GEAR_SELECTION")
                .intValue(VehicleGear.GEAR_REVERSE)
                .execute(this);
    }

    @Rpc(description = "Select neutral on the vehicle's transmission")
    public void shiftToNeutral() {
        PropertyCommand.start()
                .set()
                .propertyName("GEAR_SELECTION")
                .intValue(VehicleGear.GEAR_NEUTRAL)
                .execute(this);
    }

    @Rpc(description = "Select park on the vehicle's transmission")
    public void shiftToPark() {
        PropertyCommand.start()
                .set()
                .propertyName("GEAR_SELECTION")
                .intValue(VehicleGear.GEAR_PARK)
                .execute(this);
    }

    @Rpc(description = "Set the engine's rpm")
    public void setEngineRpm(String rpm) {
        PropertyCommand.start()
                .set()
                .propertyName("ENGINE_RPM")
                .floatValue(Float.parseFloat(rpm))
                .execute(this);
    }

    @Rpc(description = "Set the vehicle's speed in meters per second")
    public void setVehicleSpeed(String metersPerSecond) {
        PropertyCommand.start()
                .set()
                .propertyName("PERF_VEHICLE_SPEED")
                .floatValue(Float.parseFloat(metersPerSecond))
                .execute(this);
    }

    private interface CommandStart {
        GetSet get();
        GetSet set();
    }

    private interface GetSet {
        PropertyName propertyName(String propertyName);
    }

    private interface PropertyName {
        PropertyValue floatValue(float value);
        PropertyValue intValue(int value);
    }

    private interface PropertyValue {
        void execute(CarPropertySnippet s);
    }

    private static class PropertyCommand
        implements CommandStart, GetSet, PropertyName, PropertyValue
    {
        private final StringBuilder command =
                new StringBuilder("dumpsys android.hardware.automotive.vehicle.IVehicle/default ");

        private PropertyCommand() {

        }

        public static CommandStart start() {
            return new PropertyCommand();
        }

        public GetSet get() {
            command.append("--get ");
            return this;
        }

        public GetSet set() {
            command.append("--set ");
            return this;
        }

        public PropertyName propertyName(String propertyName) {
            command.append(propertyName);
            command.append(" ");
            return this;
        }

        public PropertyValue floatValue(float value) {
            command.append("-f ");
            command.append(value);
            return this;
        }

        public PropertyValue intValue(int value) {
            command.append("-i ");
            command.append(value);
            return this;
        }

        public void execute(CarPropertySnippet s) {
            s.executeShellCommand(command.toString());
        }
    }

    private void executeShellCommand(String command) {
        //noinspection EmptyTryBlock
        try (ParcelFileDescriptor ignored = mUiAutomation.executeShellCommand(command)) {

        } catch (IOException e) {
            throw new RuntimeException("IOException while executing command: " + command, e);
        }
    }
}
