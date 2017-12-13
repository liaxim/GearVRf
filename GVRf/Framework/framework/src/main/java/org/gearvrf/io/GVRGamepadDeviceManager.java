/* Copyright 2015 Samsung Electronics Co., LTD
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gearvrf.io;

import android.opengl.Matrix;
import android.util.Log;
import android.util.SparseArray;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;

import org.gearvrf.GVRBaseSensor;
import org.gearvrf.GVRContext;
import org.gearvrf.GVRCursorController;
import org.gearvrf.GVRPicker;
import org.gearvrf.GVRScene;
import org.gearvrf.GVRSceneObject;
import org.gearvrf.GVRTransform;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.Set;

/**
 * Use this class to translate MotionEvents and KeyEvents generated by the
 * Controller/Gamepad to manipulate {@link GVRGamepadController}s.
 */
class GVRGamepadDeviceManager {
    private static final String TAG = GVRGamepadDeviceManager.class
            .getSimpleName();
    private static final int DELAY_MILLISECONDS = 16;
    private static final String THREAD_NAME = "GVRGamepadManagerThread";
    private static final Set<Integer> ACTIVE_BUTTONS = new HashSet<Integer>();

    private EventHandlerThread thread;
    private SparseArray<GVRGamepadController> controllers;
    private boolean threadStarted = false;

    static {
        // Add the buttons that define the active state
        ACTIVE_BUTTONS.add(KeyEvent.KEYCODE_BUTTON_A);
        ACTIVE_BUTTONS.add(KeyEvent.KEYCODE_BUTTON_B);
        ACTIVE_BUTTONS.add(KeyEvent.KEYCODE_BUTTON_X);
        ACTIVE_BUTTONS.add(KeyEvent.KEYCODE_BUTTON_Y);
        ACTIVE_BUTTONS.add(KeyEvent.KEYCODE_BUTTON_L1);
        ACTIVE_BUTTONS.add(KeyEvent.KEYCODE_BUTTON_R1);
        ACTIVE_BUTTONS.add(KeyEvent.KEYCODE_BUTTON_L2);
        ACTIVE_BUTTONS.add(KeyEvent.KEYCODE_BUTTON_R2);
        ACTIVE_BUTTONS.add(KeyEvent.KEYCODE_BUTTON_Y);
        ACTIVE_BUTTONS.add(KeyEvent.KEYCODE_BUTTON_Z);
    }

    /**
     * {@link GVRGamepadDeviceManager} is a helper class that can be used to
     * receive continuous x, y and z coordinate updates for a controller or a
     * gamepad device that is used with an Android device.
     *
     * The main functions of this helper class are <br>
     *
     * 1) To provide a separate thread to handle all the input events generated
     * by Android. This ensures that the main UI thread is not blocked while
     * performing GVRf actions. <br>
     *
     * 2) Provide x, y, and z displacement values to the app. <br>
     *
     * 3) Create a new {@link GVRGamepadController} object whenever a new
     * controller/gamepad device is detected by the {@link GVRInputManager}.
     *
     */
    GVRGamepadDeviceManager() {
        thread = new EventHandlerThread(THREAD_NAME);
        controllers = new SparseArray<GVRGamepadController>();
    }

    GVRCursorController getCursorController(GVRContext context, String name,
                                          int vendorId, int productId) {
        startThread();
        GVRGamepadController controller = new GVRGamepadController(context,
                GVRControllerType.GAMEPAD, name, vendorId, productId, this);
        int id = controller.getId();
        controllers.append(id, controller);
        return controller;
    }

    void removeCursorController(GVRCursorController controller) {
        int id = controller.getId();
        controllers.remove(id);

        // stop the thread if no more devices are online
        if (controllers.size() == 0) {
            Log.d(TAG, "Stopping " + THREAD_NAME);
            forceStopThread();
        }
    }

    private static class GVRGamepadController extends GVRCursorController {
        private static final float[] UP_VECTOR = {0.0f, 1.0f, 0.0f, 1.0f};
        private static final float[] RIGHT_VECTOR = {1.0f, 0.0f, 0.0f, 1.0f};

        private static final float ONE_RADIAN = 1.0f;
        private static final float DEPTH_STEP = (float) (1
                / Math.toDegrees(ONE_RADIAN));

        // Change this value to increase or decrease the controller cursor speed
        private static final float SPEED = 30f;

        private GVRGamepadDeviceManager deviceManager;
        private GVRTransform tempTrans;

        public GVRGamepadController(GVRContext context,
                                    GVRControllerType controllerType, String name, int vendorId,
                                    int productId, GVRGamepadDeviceManager deviceManager) {
            super(context, controllerType, name, vendorId, productId);
            tempTrans = new GVRSceneObject(context).getTransform();
            tempTrans.setPosition(0.0f, 0.0f, -1.0f);
            this.deviceManager = deviceManager;
        }

        @Override
        public void setEnable(boolean flag) {
            if (!enable && flag) {
                enable = true;
                deviceManager.startThread();
                //set the enabled flag on the handler thread
                deviceManager.thread.setEnable(getId(), true);
                mConnected = true;
            } else if (enable && !flag) {
                enable = false;
                //set the disabled flag on the handler thread
                deviceManager.thread.setEnable(getId(), false);
                deviceManager.stopThread();
                mConnected = false;
                context.getInputManager().removeCursorController(this);
            }
        }

        @Override
        public void setScene(GVRScene scene) {
            if (!deviceManager.threadStarted) {
                super.setScene(scene);
            } else {
                deviceManager.thread.setScene(getId(), scene);
            }
        }

        @Override
        public void invalidate() {
            if (!deviceManager.threadStarted) {
                //do nothing
                return;
            }
            deviceManager.thread.sendInvalidate(getId());
        }

        void callParentSetEnable(boolean enable){
            super.setEnable(enable);
        }

        void callParentSetScene(GVRScene scene) {
            super.setScene(scene);
        }

        void callParentInvalidate() {
            super.invalidate();
        }

        @Override
        protected void setKeyEvent(KeyEvent keyEvent) {
            int action = keyEvent.getAction();

            if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_BUTTON_L1)
            {
                if (action == KeyEvent.ACTION_DOWN)
                {
                    setActive(true);
                }
                else if (action == KeyEvent.ACTION_UP)
                {
                    setActive(false);
                }
            }
            super.setKeyEvent(keyEvent);
        }

        @Override
        protected void setMotionEvent(MotionEvent motionEvent) {
            super.setMotionEvent(motionEvent);
        }

        @Override
        public boolean dispatchKeyEvent(KeyEvent event)
        {
            if (deviceManager.thread.submitKeyEvent(getId(), event))
            {
                return !mSendEventsToActivity;
            }
            return false;
        }

        @Override
        public boolean dispatchMotionEvent(MotionEvent event)
        {
            if (deviceManager.thread.submitMotionEvent(getId(), event))
            {
                return !mSendEventsToActivity;
            }
            return false;
        }

        private void processControllerEvent(float x, float y, float z) {
            GVRScene scene = context.getMainScene();
            if (scene != null) {
                float[] viewMatrix = scene.getMainCameraRig().getHeadTransform()
                        .getModelMatrix();
                float[] xAxis = new float[4];
                float[] yAxis = new float[4];

                Matrix.multiplyMV(xAxis, 0, viewMatrix, 0, UP_VECTOR, 0);
                Matrix.multiplyMV(yAxis, 0, viewMatrix, 0, RIGHT_VECTOR, 0);
                float sensitivity = SPEED / 100f;
                if (x != 0 || y != 0) {
                    float angle = (float) Math.atan2(y, x);
                    float displacementX = (float) Math.cos(angle);
                    float displacementY = (float) Math.sin(angle);
                    tempTrans.setRotation(1.0f, 0.0f, 0.0f, 0.0f);

                    tempTrans.rotateByAxisWithPivot(
                            -displacementX * sensitivity, xAxis[0], xAxis[1],
                            xAxis[2], 0.0f, 0.0f, 0.0f);
                    tempTrans.rotateByAxisWithPivot(
                            displacementY * sensitivity, yAxis[0], yAxis[1],
                            yAxis[2], 0.0f, 0.0f, 0.0f);
                }
                float[] controllerPosition = new float[]{
                        tempTrans.getPositionX(),
                        tempTrans.getPositionY(),
                        tempTrans.getPositionZ()};

                if (z != 0.0f) {
                    float step = (z < 0) ? DEPTH_STEP * sensitivity
                            : -DEPTH_STEP * sensitivity;

                    float[] point = {
                            controllerPosition[0]
                                    + controllerPosition[0] * step,
                            controllerPosition[1]
                                    + controllerPosition[1] * step,
                            controllerPosition[2]
                                    + controllerPosition[2] * step};

                    if (checkBounds(point)) {
                        tempTrans.setPosition(point[0], point[1], point[2]);
                    }
                }
                super.setPosition(tempTrans.getPositionX(),
                                  tempTrans.getPositionY(),
                                  tempTrans.getPositionZ());
            }
        }

        private boolean checkBounds(float[] point) {
            float lhs = square(point[0]) + square(point[1]) + square(point[2]);
            return (lhs <= getMaxRadius() && lhs >= getMinRadius());
        }

        private static float square(float x) {
            return x * x;
        }

        private float getMinRadius() {
            float nearDepth = getNearDepth();
            return nearDepth * nearDepth;
        }

        private float getMaxRadius() {
            float farDepth = getFarDepth();
            return farDepth * farDepth;
        }

        @Override
        public void setPosition(float x, float y, float z) {
            super.setPosition(x, y, z);
            tempTrans.setPosition(x, y, z);
            if (!deviceManager.threadStarted) {
                //do nothing
                return;
            }
            deviceManager.thread.sendInvalidate(getId());
        }
    }

    private class EventHandlerThread extends Thread {
        private final KeyEvent BUTTON_L2_DOWN = new KeyEvent(
                KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_L2);
        private final KeyEvent BUTTON_L2_UP = new KeyEvent(KeyEvent.ACTION_UP,
                KeyEvent.KEYCODE_BUTTON_L2);
        private final KeyEvent BUTTON_R2_DOWN = new KeyEvent(
                KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_R2);
        private final KeyEvent BUTTON_R2_UP = new KeyEvent(KeyEvent.ACTION_UP,
                KeyEvent.KEYCODE_BUTTON_R2);
        private int dpadState;
        private float x, y, ry;
        private Object lock = new Object();
        private boolean pedalDown = false;
        private SparseArray<EventDataHolder> holders;

        public static final int ENABLE = 0;
        public static final int DISABLE = 1;

        private class EventDataHolder {
            private MotionEvent event;
            private KeyEvent keyEvent;
            private GVRScene scene;
            private int enabled = -1;
            private boolean callInvalidate= false;
            private int id;


            public EventDataHolder(int id, MotionEvent event,
                                   KeyEvent keyEvent) {
                this.id = id;
                this.event = event;
                scene = null;
                this.keyEvent = keyEvent;
            }

            public void setId(int id) {
                this.id = id;
            }

            public void setMotionEvent(MotionEvent event) {
                this.event = event;
            }

            public void setKeyEvent(KeyEvent keyEvent) {
                this.keyEvent = keyEvent;
            }

        }

        EventHandlerThread(String name) {
            super(name);
            holders = new SparseArray<EventDataHolder>();
        }

        @Override
        public void run() {
            try {
                while (true) {
                    synchronized (lock) {
                        if (holders.size() == 0) {
                            lock.wait();
                        }

                        for (int i = 0; i < holders.size(); i++) {
                            int key = holders.keyAt(i);
                            EventDataHolder holder = holders.valueAt(i);

                            if (holder == null) {
                                continue;
                            }
                            int id = key;

                            GVRGamepadController controller = controllers.get(id);
                            MotionEvent event = holder.event;
                            KeyEvent keyEvent = holder.keyEvent;
                            int enabled = holder.enabled;

                            if (event != null) {
                                dispatchMotionEvent(controller, event);
                            }
                            if (keyEvent != null) {
                                dispatchKeyEvent(controller, keyEvent);
                            }

                            if (enabled != -1) {
                                controller.callParentSetEnable(holder.enabled == ENABLE);
                                holder.enabled = -1;
                            }

                            if (holder.scene != null) {
                                controller.callParentSetScene(holder.scene);
                                holder.scene = null;
                            }

                            if (holder.callInvalidate) {
                                controller.callParentInvalidate();
                                holder.callInvalidate = false;
                            }

                            if (holder.event == null) {
                                // reset holder when there is no motion event
                                holders.remove(controller.getId());
                            }

                            controller.processControllerEvent(this.x, this.y, this.ry);
                        }
                    }
                    Thread.sleep(DELAY_MILLISECONDS);
                }
            } catch (InterruptedException e) {
                Log.d(TAG, "Stopped " + THREAD_NAME);
            }
        }

        /**
         * Process the KeyEvent from the Gamepad.
         *
         * @param event the {@link KeyEvent}.
         * @return <code>true</code> if the key is from an active button,
         * <code>false</code> otherwise
         */
        private void dispatchKeyEvent(GVRGamepadController controller,
                                      KeyEvent event) {
            int keyCode = event.getKeyCode();
            int action = event.getAction();

            if (ACTIVE_BUTTONS.contains(keyCode)) {
                controller.setKeyEvent(event);
            } else {
                switch (keyCode) {
                    case KeyEvent.KEYCODE_DPAD_LEFT:
                        if (action == KeyEvent.ACTION_DOWN
                                && dpadState != KeyEvent.KEYCODE_DPAD_LEFT) {
                            dpadState = KeyEvent.KEYCODE_DPAD_LEFT;
                            x = -1.0f;
                        } else if (action == KeyEvent.ACTION_UP) {
                            dpadState = 0;
                        }
                        break;
                    case KeyEvent.KEYCODE_DPAD_RIGHT:
                        if (action == KeyEvent.ACTION_DOWN
                                && dpadState != KeyEvent.KEYCODE_DPAD_RIGHT) {
                            dpadState = KeyEvent.KEYCODE_DPAD_RIGHT;
                            x = 1.0f;
                        } else if (action == KeyEvent.ACTION_UP) {
                            dpadState = 0;
                        }
                        break;
                    case KeyEvent.KEYCODE_DPAD_UP:
                        if (action == KeyEvent.ACTION_DOWN
                                && dpadState != KeyEvent.KEYCODE_DPAD_UP) {
                            dpadState = KeyEvent.KEYCODE_DPAD_UP;
                            y = 1.0f;
                        } else if (action == KeyEvent.ACTION_UP) {
                            dpadState = 0;
                        }
                        break;
                    case KeyEvent.KEYCODE_DPAD_DOWN:
                        if (action == KeyEvent.ACTION_DOWN
                                && dpadState != KeyEvent.KEYCODE_DPAD_DOWN) {
                            dpadState = KeyEvent.KEYCODE_DPAD_DOWN;
                            y = -1.0f;
                        } else if (action == KeyEvent.ACTION_UP) {
                            dpadState = 0;
                        }
                        break;
                }
            }
        }

        boolean submitMotionEvent(int id, MotionEvent event) {
            if (threadStarted && event.isFromSource(InputDevice.SOURCE_GAMEPAD)
                    || event.isFromSource(InputDevice.SOURCE_JOYSTICK)) {
                MotionEvent clone = MotionEvent.obtain(event);
                synchronized (lock) {
                    EventDataHolder holder = holders.get(id);
                    if (holder == null) {
                        holder = new EventDataHolder(id,  clone, null);
                        holders.put(id, holder);
                    } else {
                        // if there is already an event recycle it
                        if (holder.event != null) {
                            holder.event.recycle();
                        }
                        holder.setId(id);
                        holder.setMotionEvent(clone);
                    }
                    lock.notify();
                }
                return true;
            } else {
                return false;
            }
        }

        boolean submitKeyEvent(int id, KeyEvent event) {
            if (threadStarted && event.isFromSource(InputDevice.SOURCE_GAMEPAD)
                    || event.isFromSource(InputDevice.SOURCE_JOYSTICK)) {
                synchronized (lock) {
                    EventDataHolder holder = holders.get(id);
                    if (holder == null) {
                        holder = new EventDataHolder(id, null, event);
                        holders.put(id, holder);
                    } else {
                        holder.setKeyEvent(event);
                    }
                    lock.notify();
                }
                return true;
            } else {
                return false;
            }
        }

        // The following methods are taken from the controller sample on the
        // Android Developer web site:
        // https://developer.android.com/training/game-controllers/controller-input.html
        private void dispatchMotionEvent(GVRGamepadController controller,
                                         MotionEvent event) {
            InputDevice device = event.getDevice();
            if (event.getAction() != MotionEvent.ACTION_MOVE
                    || device == null) {
                event.recycle();
                holders.remove(controller.getId());
                return;
            }

            float x = getCenteredAxis(event, device, MotionEvent.AXIS_X);
            if (x == 0) {
                x = getCenteredAxis(event, device, MotionEvent.AXIS_HAT_X);
            }

            float y = getCenteredAxis(event, device, MotionEvent.AXIS_Y);
            if (y == 0) {
                y = getCenteredAxis(event, device, MotionEvent.AXIS_HAT_Y);
            }

            float ry = 0.0f;
            int vendorId = device.getVendorId();
            int productId = device.getProductId();

            if (vendorId == GVRDeviceConstants.SAMSUNG_GAMEPAD_VENDOR_ID
                    && productId == GVRDeviceConstants.SAMSUNG_GAMEPAD_PRODUCT_ID) {
                ry = getCenteredAxis(event, device, MotionEvent.AXIS_RY);
            } else if ((vendorId == GVRDeviceConstants.SONY_DUALSHOCK_CONTROLLER_VENDOR_ID
                    && (productId == GVRDeviceConstants.SONY_DUALSHOCK_3_CONTROLLER_PRODUCT_ID
                    || productId == GVRDeviceConstants.SONY_DUALSHOCK_4_CONTROLLER_PRODUCT_ID))) {
                ry = getCenteredAxis(event, device, MotionEvent.AXIS_RZ);
            } else if ((vendorId == GVRDeviceConstants.STEELSERIES_CONTROLLER_VENDOR_ID
                    && productId == GVRDeviceConstants.STEELSERIES_CONTROLLER_PRODUCT_ID)) {
                ry = getCenteredAxis(event, device, MotionEvent.AXIS_RZ);

                float brakeAxis = getCenteredAxis(event, device,
                        MotionEvent.AXIS_BRAKE);
                float gasAxis = getCenteredAxis(event, device,
                        MotionEvent.AXIS_GAS);
                if (brakeAxis != 0 && pedalDown == false) {
                    pedalDown = true;
                    controller.setKeyEvent(BUTTON_L2_DOWN);
                } else if (brakeAxis == 0 && pedalDown == true) {
                    pedalDown = false;
                    controller.setKeyEvent(BUTTON_L2_UP);
                }
                if (gasAxis != 0 && pedalDown == false) {
                    pedalDown = true;
                    controller.setKeyEvent(BUTTON_R2_DOWN);
                } else if (gasAxis == 0 && pedalDown == true) {
                    pedalDown = false;
                    controller.setKeyEvent(BUTTON_R2_UP);
                }
            }

            this.x = x;
            this.y = -y;
            this.ry = ry;

            if (x == 0 && y == 0 && ry == 0) {
                event.recycle();
                holders.remove(controller.getId());
            } else {
                MotionEvent clone = MotionEvent.obtain(event);
                holders.get(controller.getId()).event = clone;
                controller.setMotionEvent(event);
            }
        }

        void setEnable(int id, boolean enable) {
            synchronized (lock) {
                EventDataHolder holder = holders.get(id);
                if (holder == null) {
                    holder = new EventDataHolder(id, null, null);
                    holder.enabled = enable ? ENABLE : DISABLE;
                    holders.put(id, holder);
                } else {
                    holder.enabled = enable ? ENABLE : DISABLE;
                }
                lock.notify();
            }
        }

        void setScene(int id, GVRScene scene){
            synchronized (lock) {
                EventDataHolder holder = holders.get(id);
                if (holder == null) {
                    holder = new EventDataHolder(id, null, null);
                    holder.scene = scene;
                    holders.put(id, holder);
                } else {
                    holder.scene = scene;
                }
                lock.notify();
            }
        }

        void sendInvalidate(int id) {
            synchronized (lock) {
                EventDataHolder holder = holders.get(id);
                if (holder == null) {
                    holder = new EventDataHolder(id, null, null);
                    holder.callInvalidate = true;
                    holders.put(id, holder);
                } else {
                    holder.callInvalidate = true;
                }
                lock.notify();
            }
        }

        private float getCenteredAxis(MotionEvent event, InputDevice device,
                                      int axis) {
            final InputDevice.MotionRange range = device.getMotionRange(axis,
                    event.getSource());
            if (range != null) {
                final float flat = range.getFlat();
                final float value = event.getAxisValue(axis);
                if (Math.abs(value) > flat) {
                    return value;
                }
            }
            return 0;
        }
    }

    void startThread(){
        if(!threadStarted){
            thread.start();
            threadStarted = true;
        }
    }

    void stopThread() {
        boolean foundEnabled = false;

        for(int i = 0 ;i< controllers.size(); i++){
            GVRCursorController controller = controllers.valueAt(i);
            if(controller.isEnabled()){
                foundEnabled = true;
                break;
            }
        }

        if (!foundEnabled && threadStarted) {
            thread.interrupt();
            thread = new EventHandlerThread(THREAD_NAME);
            threadStarted = false;
        }
    }

    void forceStopThread(){
        if (threadStarted) {
            thread.interrupt();
            thread = new EventHandlerThread(THREAD_NAME);
            threadStarted = false;
        }
    }

}