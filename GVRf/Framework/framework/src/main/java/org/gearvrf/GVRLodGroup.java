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

import org.joml.Vector4f;


/**
 * If you want to use LOD, the scene objects that participate
 * must be children of a scene object that an instance of
 * this component has been added to.
 *
 * Example:
 * <pre>
 * root.attachComponent(new GVRLodGroup(gvrContext));
 * ...
 * sphereHighDensity.attachComponent(new GVRLodRange(gvrContext, 0f, 5f));
 * root.addChildObject(sphereHighDensity);
 * ...
 * sphereMediumDensity.attachComponent(new GVRLodRange(gvrContext, 5f, 9f));
 * root.addChildObject(sphereMediumDensity);
 * ..
 * sphereLowDensity.attachComponent(new GVRLodRange(gvrContext, 9f, Float.MAX_VALUE));
 * root.addChildObject(sphereLowDensity);
 * </pre>
 */
public final class GVRLodGroup extends GVRBehavior {
    static private long TYPE_LODGROUP = newComponentType(GVRLodGroup.class);

    public GVRLodGroup(GVRContext gvrContext) {
        super(gvrContext, 0);
        mType = getComponentType();
    }

    static public long getComponentType() {
        return TYPE_LODGROUP;
    }

    private final Vector4f mCenter = new Vector4f();
    private final Vector4f mVector = new Vector4f();

    /**
     * Do not call.
     * @deprecated
     */
    public void onDrawFrame(float frameTime) {
        final GVRSceneObject owner = getOwnerObject();
        if (owner == null) {
            return;
        }

        final GVRTransform t = getGVRContext().getMainScene().getMainCameraRig().getCenterCamera().getTransform();
        for (final GVRSceneObject child : owner.rawGetChildren()) {
            final GVRLodRange lodRange = (GVRLodRange)child.getComponent(GVRLodRange.getComponentType());
            if (null == lodRange) {
                continue;
            }

            final float[] values = child.getBoundingVolumeRawValues();
            mCenter.set(values[0], values[1], values[2], 1.0f);
            mVector.set(t.getPositionX(), t.getPositionY(), getTransform().getPositionZ(), 1.0f);

            mVector.sub(mCenter);
            mVector.negate();

            float distance = mVector.dot(mVector);

            if (distance >= lodRange.getLODMinRange() && distance < lodRange.getLODMaxRange()) {
                child.setEnable(true);
            } else {
                child.setEnable(false);
            }
        }
    }

}