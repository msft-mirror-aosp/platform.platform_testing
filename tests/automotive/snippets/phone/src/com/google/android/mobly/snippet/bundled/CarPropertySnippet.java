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
        set("GEAR_SELECTION").value(VehicleGear.GEAR_DRIVE).execute(mUiAutomation);
    }

    @Rpc(description = "Select reverse on the vehicle's transmission")
    public void shiftToReverse() {
        set("GEAR_SELECTION").value(VehicleGear.GEAR_REVERSE).execute(mUiAutomation);
    }

    @Rpc(description = "Select neutral on the vehicle's transmission")
    public void shiftToNeutral() {
        set("GEAR_SELECTION").value(VehicleGear.GEAR_NEUTRAL).execute(mUiAutomation);
    }

    @Rpc(description = "Select park on the vehicle's transmission")
    public void shiftToPark() {
        set("GEAR_SELECTION").value(VehicleGear.GEAR_PARK).execute(mUiAutomation);
    }

    @Rpc(description = "Set the engine's rpm")
    public void setEngineRpm(String rpm) {
        set("ENGINE_RPM").value(Float.parseFloat(rpm)).execute(mUiAutomation);
    }

    @Rpc(description = "Set the vehicle's speed in meters per second")
    public void setVehicleSpeed(String metersPerSecond) {
        set("PERF_VEHICLE_SPEED").value(Float.parseFloat(metersPerSecond)).execute(mUiAutomation);
    }

    @Rpc(description = "Engage or disengage the parking brake (pass 'true' or 'false')")
    public void setParkingBrake(String engage) {
        set("PARKING_BRAKE_ON").value(Boolean.parseBoolean(engage)).execute(mUiAutomation);
    }

    @Rpc(description = "Get the state of the parking brake")
    public boolean getParkingBrake() {
        return get("PARKING_BRAKE_ON").booleanValue(mUiAutomation);
    }

    private static SetProp set(String name) {
        return new SetProp(name);
    }

    private static class SetProp {
        private final ExecutableCommand mCommand;
        public SetProp(String name) {
            String command =
                    "dumpsys android.hardware.automotive.vehicle.IVehicle/default --set "
                            + name + " ";
            mCommand = new ExecutableCommand(command);
        }

        public ExecuteSet value(boolean value) {
            return new ExecuteSet(mCommand.append("-i ").append(value ? "1" : "0"));
        }

        public ExecuteSet value(float value) {
            return new ExecuteSet(mCommand.append("-f ").append("" + value));
        }

        public ExecuteSet value(int value) {
            return new ExecuteSet(mCommand.append("-i ").append("" + value));
        }
    }

    private static class ExecuteSet {
        private final ExecutableCommand mCommand;
        public ExecuteSet(ExecutableCommand command) {
            mCommand = command;
        }

        public void execute(UiAutomation uiAutomation) {
            mCommand.execute(uiAutomation);
        }
    }

    private static GetProp get(String name) {
        return new GetProp(name);
    }

    private static class GetProp {
        private final ExecutableCommand mCommand;
        public GetProp(String name) {
            String command =
                    "dumpsys android.hardware.automotive.vehicle.IVehicle/default --get " + name;
            mCommand = new ExecutableCommand(command);
        }

        public boolean booleanValue(UiAutomation uiAutomation) {
            return intValue(uiAutomation) != 0;
        }

        public float floatValue(UiAutomation uiAutomation) {
            return Float.parseFloat(execute(uiAutomation, "float"));
        }

        public int intValue(UiAutomation uiAutomation) {
            return Integer.parseInt(execute(uiAutomation, "int32"));
        }

        private String execute(UiAutomation uiAutomation, String typeName) {
            String VALUE_OBJECT_HEADER = "RawPropValues{";
            String output = mCommand.executeWithOutput(uiAutomation);
            int valuesObjectStart = output.indexOf(VALUE_OBJECT_HEADER);
            int valuesStart = valuesObjectStart + VALUE_OBJECT_HEADER.length();
            int valuesObjectEnd = output.indexOf("}", valuesStart);
            String values = output.substring(valuesStart, valuesObjectEnd);

            String typeHeader = typeName + "Values: [";
            int valueListStart = values.indexOf(typeHeader);
            int valueStart = valueListStart + typeHeader.length();
            int valueEnd = values.indexOf("]", valueStart);
            return values.substring(valueStart, valueEnd);
        }
    }
}
