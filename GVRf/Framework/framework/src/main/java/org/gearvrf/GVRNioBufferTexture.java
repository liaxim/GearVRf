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

import android.opengl.GLES30;

import java.nio.Buffer;

/**
 * A texture that will be updated from a Java NIO's Buffer.
 */
public class GVRNioBufferTexture extends GVRTexture {

    /**
     * Prepare the texture. Use the postBuffer method to load the texture data.
     */
    public GVRNioBufferTexture(GVRContext gvrContext, GVRTextureParameters textureParameters) {
        super(gvrContext, NativeBaseTexture.bareConstructor(textureParameters.getCurrentValuesArray()));

        getGVRContext().runOnGlThread(new Runnable() {
            @Override
            public void run() {
                mTextureId = NativeTexture.getId(getNative());
            }
        });
    }

    /**
     * Prepare the texture. Use the postBuffer method to load the texture data. Uses default
     * texture parameters (see GVRTextureParameters).
     *
     * @param gvrContext
     */
    public GVRNioBufferTexture(GVRContext gvrContext) {
        this(gvrContext, new GVRTextureParameters(gvrContext));
    }

    /**
     * Schedule a load for the texture data supplied in pixels. The target is always GL_TEXTURE_2D.
     * For detailed information see glTexImage2D's internalformat, width, height, format, type,
     * pixels parameters. The buffer is not copied!
     */
    public void postBuffer(final int internalFormat, final int width, final int height, final int format, final int type, final Buffer pixels) {
        getGVRContext().runOnGlThread(new Runnable() {
            @Override
            public void run() {
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mTextureId);
                GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, internalFormat, width, height, 0, format, type, pixels);
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);
            }
        });
    }
}
