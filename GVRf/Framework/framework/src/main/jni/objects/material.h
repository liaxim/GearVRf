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

/***************************************************************************
 * Links textures and shaders.
 ***************************************************************************/

#ifndef MATERIAL_H_
#define MATERIAL_H_

#include <map>
#include <memory>
#include <string>

#include "glm/glm.hpp"

#include "objects/hybrid_object.h"
#include "objects/textures/texture.h"
#include "objects/components/render_data.h"

namespace gvr {

class Material: public HybridObject {
public:
    enum ShaderType {
        BEING_GENERATED = -1,
        UNLIT_HORIZONTAL_STEREO_SHADER = 0,
        UNLIT_VERTICAL_STEREO_SHADER = 1,
        OES_SHADER = 2,
        OES_HORIZONTAL_STEREO_SHADER = 3,
        OES_VERTICAL_STEREO_SHADER = 4,
        CUBEMAP_SHADER = 5,
        CUBEMAP_REFLECTION_SHADER = 6,
        TEXTURE_SHADER = 7,
        EXTERNAL_RENDERER_SHADER = 8,
        ASSIMP_SHADER = 9,
        BOUNDING_BOX_SHADER = 10,
        LIGHTMAP_SHADER = 11,
        DISTORTION_SHADER = 90, // this shader is implemented and loaded in the distorter

        UNLIT_FBO_SHADER = 20,       

        TEXTURE_SHADER_NOLIGHT = 100,
        BUILTIN_SHADER_SIZE = 101
    };

    explicit Material(ShaderType shader_type) :
            shader_type_(shader_type),
            textures_(),
            floats_(),
            vec2s_(),
            vec3s_(),
            vec4s_(),
            shader_feature_set_(0),
            renderdata_dirty_flag_(std::make_shared<bool>(false))
    {
        switch (shader_type) {
        default:
            vec3s_["color"] = glm::vec3(1.0f, 1.0f, 1.0f);
            floats_["opacity"] = 1.0f;
            break;
        }
    }

    ~Material() {
    }

    ShaderType shader_type() const {
        return shader_type_;
    }

    void set_shader_type(ShaderType shader_type) {
        shader_type_ = shader_type;
        *renderdata_dirty_flag_ = true;
    }

    Texture* getTexture(const std::string& key) const {
        auto it = textures_.find(key);
        if (it != textures_.end()) {
            return it->second;
        } else {
            std::string error = "Material::getTexture() : " + key
                    + " not found";
            throw error;
        }
    }

    //A new api to return a texture even it is NULL without throwing a error,
    //otherwise it will be captured abruptly by the error handler
    Texture* getTextureNoError(const std::string& key) const {
        auto it = textures_.find(key);
        if (it != textures_.end()) {
            return it->second;
        } else {
            return NULL;
        }
    }

    void setTexture(const std::string& key, Texture* texture) {
        textures_[key] = texture;
        //By the time the texture is being set to its attaching material, it is ready
        //This is guaranteed by upper java layer scheduling
        texture->setReady(true);
        if (key == "main_texture") {
            main_texture = texture;
        }
        *renderdata_dirty_flag_ = true;
    }

    float getFloat(const std::string& key) {
        auto it = floats_.find(key);
        if (it != floats_.end()) {
            return it->second;
        } else {
            std::string error = "Material::getFloat() : " + key + " not found";
            throw error;
        }
    }
    void setFloat(const std::string& key, float value) {
        floats_[key] = value;
        *renderdata_dirty_flag_ = true;
    }

    glm::vec2 getVec2(const std::string& key) {
        auto it = vec2s_.find(key);
        if (it != vec2s_.end()) {
            return it->second;
        } else {
            std::string error = "Material::getVec2() : " + key + " not found";
            throw error;
        }
    }

    void setVec2(const std::string& key, glm::vec2 vector) {
        vec2s_[key] = vector;
        *renderdata_dirty_flag_ = true;
    }

    glm::vec3 getVec3(const std::string& key) {
        auto it = vec3s_.find(key);
        if (it != vec3s_.end()) {
            return it->second;
        } else {
            std::string error = "Material::getVec3() : " + key + " not found";
            throw error;
        }
    }

    void setVec3(const std::string& key, glm::vec3 vector) {
        vec3s_[key] = vector;
        *renderdata_dirty_flag_ = true;
    }

    glm::vec4 getVec4(const std::string& key) {
        auto it = vec4s_.find(key);
        if (it != vec4s_.end()) {
            return it->second;
        } else {
            std::string error = "Material::getVec4() : " + key + " not found";
            throw error;
        }
    }

    void setVec4(const std::string& key, glm::vec4 vector) {
        vec4s_[key] = vector;
        *renderdata_dirty_flag_ = true;
    }

    glm::mat4 getMat4(const std::string& key) {
        auto it = mat4s_.find(key);
        if (it != mat4s_.end()) {
            return it->second;
        } else {
            std::string error = "Material::getMat4() : " + key + " not found";
            throw error;
        }
    }

    bool hasUniform(const std::string& key) const {
        if (vec3s_.find(key) != vec3s_.end()) {
            return true;
        }
        if (vec2s_.find(key) != vec2s_.end()) {
            return true;
        }
        if (vec4s_.find(key) != vec4s_.end()) {
            return true;
        }
        if (mat4s_.find(key) != mat4s_.end()) {
            return true;
        }
        if (floats_.find(key) != floats_.end()) {
            return true;
        }
        return false;
    }

    void setMat4(const std::string& key, glm::mat4 matrix) {
        mat4s_[key] = matrix;
        *renderdata_dirty_flag_ = true;
    }

    int get_shader_feature_set() {
        return shader_feature_set_;
    }

    void set_shader_feature_set(int feature_set) {
        shader_feature_set_ = feature_set;
    }
    bool isMainTextureReady() {
        return (main_texture != NULL) && main_texture->isReady();
    }

    void set_dirty_flag(const std::shared_ptr<bool> renderdata_dirty_flag) {
        renderdata_dirty_flag_ = renderdata_dirty_flag;
    }

private:
    Material(const Material& material);
    Material(Material&& material);
    Material& operator=(const Material& material);
    Material& operator=(Material&& material);

private:
    ShaderType shader_type_;
    std::map<std::string, Texture*> textures_;
    Texture* main_texture = NULL;
    std::map<std::string, float> floats_;
    std::map<std::string, glm::vec2> vec2s_;
    std::map<std::string, glm::vec3> vec3s_;
    std::map<std::string, glm::vec4> vec4s_;
    std::map<std::string, glm::mat4> mat4s_;
    std::shared_ptr<bool> renderdata_dirty_flag_;

    unsigned int shader_feature_set_;
};

}
#endif
