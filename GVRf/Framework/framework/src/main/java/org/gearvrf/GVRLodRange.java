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

/**
 * Scene objects that want to use LOD need to add and configure this component. Then
 * these scene objects must have an ancestor scene object that has the GVRLodGroup
 * component added to it. @see {@link GVRLodGroup}
 */
public final class GVRLodRange extends GVRBehavior {
    static private long TYPE_LODRANGE = newComponentType(GVRLodRange.class);

    /**
     * @param minRange The closest distance to the camera rig in which this object should be shown.
     *                 This should be a positive number between 0 and Float.MAX_VALUE.
     * @param maxRange The farthest distance to the camera rig in which this object should be shown.
     *                 This should be a positive number between 0 and Float.MAX_VALUE.
     */
    public GVRLodRange(GVRContext gvrContext, final float minRange, final float maxRange) {
        super(gvrContext, 0);
        mType = getComponentType();
        setLODRange(minRange, maxRange);
    }

    static public long getComponentType() {
        return TYPE_LODRANGE;
    }

    private float mMinRange;
    private float mMaxRange;

    /**
     * Sets the range of distances from the camera where this object will be shown.
     *
     * @param minRange The closest distance to the camera rig in which this object should be shown.
     *                 This should be a positive number between 0 and Float.MAX_VALUE.
     * @param maxRange The farthest distance to the camera rig in which this object should be shown.
     *                 This should be a positive number between 0 and Float.MAX_VALUE.
     */
    public void setLODRange(float minRange, float maxRange) {
        if (minRange < 0 || maxRange < 0) {
            throw new IllegalArgumentException(
                    "minRange and maxRange must be between 0 and Float.MAX_VALUE");
        }
        if (minRange > maxRange) {
            throw new IllegalArgumentException(
                    "minRange should not be greater than maxRange");
        }
        mMinRange = minRange*minRange;
        mMaxRange = maxRange*maxRange;
    }

    /**
     * Get the minimum distance from the camera in which to show this object.
     *
     * @return the minimum distance from the camera in which to show this object.  Default value is 0.
     */
    float getLODMinRange() {
        return mMinRange;
    }

    /**
     * Get the maximum distance from the camera in which to show this object.
     *
     * @return the maximum distance from the camera in which to show this object.  Default value is Float.MAX_VALUE.
     */
    float getLODMaxRange() {
        return mMaxRange;
    }

}