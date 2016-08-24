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

package org.gearvrf;

import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.view.KeyEvent;

import org.gearvrf.utility.Log;
import org.gearvrf.utility.VrAppSettings;

/**
 * {@inheritDoc}
 */
final class OvrActivityDelegate implements GVRActivity.GVRActivityDelegate {
    private GVRActivity mActivity;
    private OvrViewManagerImpl mActiveViewManager;
    private OvrActivityNative mActivityNative;
    private boolean mUseFallback;

    @Override
    public void onCreate(GVRActivity activity) {
        mActivity = activity;

        mActivityNative = new OvrActivityNative(mActivity, mActivity.getAppSettings(), mRenderingCallbacks);

        try {
            mActivityHandler = new OvrVrapiActivityHandler(activity, mActivityNative, mRenderingCallbacks);
        } catch (final Exception ignored) {
            // GVRf will fallback to GoogleVR in this case.
            mUseFallback = true;
        }
    }

    @Override
    public OvrActivityNative getActivityNative() {
        return mActivityNative;
    }

    @Override
    public GVRViewManager makeViewManager(AssetManager assetManager, String dataFilename) {
        final OvrXMLParser xmlParser = new OvrXMLParser(assetManager, dataFilename, mActivity.getAppSettings());
        if (!mUseFallback) {
            return new OvrViewManagerImpl(mActivity, mActivity.getScript(), xmlParser);
        } else {
            return new OvrGoogleVRViewManager(mActivity, mActivity.getScript(), xmlParser);
        }
    }

    @Override
    public OvrMonoscopicViewManager makeMonoscopicViewManager(AssetManager assetManager, String dataFilename) {
        final OvrXMLParser xmlParser = new OvrXMLParser(assetManager, dataFilename, mActivity.getAppSettings());
        return new OvrMonoscopicViewManager(mActivity, mActivity.getScript(), xmlParser);
    }

    @Override
    public GVRCameraRig makeCameraRig(GVRContext context) {
        return new OvrCameraRig(context);
    }

    @Override
    public GVRConfigurationManager makeConfigurationManager(GVRActivity activity) {
        return new OvrConfigurationManager(activity);
    }

    @Override
    public void onPause() {
        if (null != mActivityHandler) {
            mActivityHandler.onPause();
        }
    }

    @Override
    public void onResume() {
        if (null != mActivityHandler) {
            mActivityHandler.onResume();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
    }

    @Override
    public void setScript(GVRScript gvrScript, String dataFileName) {
        if (mUseFallback) {
            mActivityHandler = null;
        } else if (null != mActivityHandler) {
            mActivityHandler.onSetScript();
        }
    }

    @Override
    public void setViewManager(GVRViewManager viewManager) {
        mActiveViewManager = (OvrViewManagerImpl)viewManager;
    }

    @Override
    public void onInitAppSettings(VrAppSettings appSettings) {
        if(mUseFallback){
            // This is the only place where the setDockListenerRequired flag can be set before
            // the check in GVRActivityBase.
            mActivity.getConfigurationManager().setDockListenerRequired(false);
        }
    }

    @Override
    public VrAppSettings makeVrAppSettings() {
        return new OvrVrAppSettings();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (KeyEvent.KEYCODE_BACK == keyCode) {
            event.startTracking();
            return true;
        }
        return false;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (!mActivity.isPaused() && KeyEvent.KEYCODE_BACK == keyCode) {
            if (null != mActivityHandler) {
                return mActivityHandler.onBack();
            }
        }
        return false;
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        if (KeyEvent.KEYCODE_BACK == keyCode) {
            if (null != mActivityHandler) {
                return mActivityHandler.onBackLongPress();
            }
        }
        return false;
    }

    private final OvrActivityHandlerRenderingCallbacks mRenderingCallbacks = new OvrActivityHandlerRenderingCallbacks() {
        @Override
        public void onSurfaceCreated() {
            mActiveViewManager.onSurfaceCreated();
        }

        @Override
        public void onSurfaceChanged(int width, int height) {
            mActiveViewManager.onSurfaceChanged(width, height);
        }

        @Override
        public void onBeforeDrawEyes() {
            mActiveViewManager.beforeDrawEyes();
            mActiveViewManager.onDrawFrame();
        }

        @Override
        public void onAfterDrawEyes() {
            mActiveViewManager.afterDrawEyes();
        }

        @Override
        public void onDrawEye(int eye) {
            try {
                mActiveViewManager.onDrawEyeView(eye);
            } catch (final Exception e) {
                Log.e(TAG, "error in onDrawEyeView", e);
            }
        }
    };

    private OvrActivityHandler mActivityHandler;
    private final static String TAG = "OvrActivityDelegate";
}
