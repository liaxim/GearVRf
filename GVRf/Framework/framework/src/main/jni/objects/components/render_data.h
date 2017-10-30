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
 * Containing data about how to render an object.
 ***************************************************************************/

#ifndef RENDER_DATA_H_
#define RENDER_DATA_H_

#include <memory>
#include <vector>
#include <sstream>
#include <engine/renderer/renderer.h>

#include "gl/gl_program.h"
#include "glm/glm.hpp"
#include "java_component.h"
#include "objects/shader_data.h"
#include "objects/render_pass.h"

typedef unsigned long Long;
namespace gvr {

class Mesh;
class ShaderData;
class Light;
class Batch;
class TextureCapturer;
class RenderPass;
struct RenderState;

template<typename T>
std::string to_string(T value) {

    //create an output string stream
    std::ostringstream os;

    //throw the value into the string stream
    os << value;

    //convert the string stream into a string and return
    return os.str();
}

class RenderData: public JavaComponent {
public:
    enum Queue {
        Stencil = -1000, Background = 1000, Geometry = 2000, Transparent = 3000, Overlay = 4000
    };

    enum RenderMaskBit {
        Left = 0x1, Right = 0x2
    };

    enum CullFace {
        CullBack = 0, CullFront, CullNone
    };

    RenderData() :
            JavaComponent(RenderData::getComponentType()), mesh_(0),
            use_light_(false), use_lightmap_(false), batching_(true),
            render_mask_(DEFAULT_RENDER_MASK), batch_(nullptr),
            rendering_order_(DEFAULT_RENDERING_ORDER), hash_code_dirty_(true),
            offset_(false), offset_factor_(0.0f), offset_units_(0.0f),
            depth_test_(true), depth_mask_(true), alpha_blend_(true), alpha_to_coverage_(false),
            sample_coverage_(1.0f), invert_coverage_mask_(GL_FALSE),
            source_alpha_blend_func_(GL_ONE), dest_alpha_blend_func_(GL_ONE_MINUS_SRC_ALPHA),
            draw_mode_(GL_TRIANGLES), texture_capturer(0), cast_shadows_(true),
            bones_ubo_(nullptr),dirty_(false)
    {
    }

    virtual JNIEnv* set_java(jobject javaObj, JavaVM* jvm);

    void copy(const RenderData& rdata) {
        Component(rdata.getComponentType());
        hash_code = rdata.hash_code;
        mesh_ = rdata.mesh_;
        use_light_ = rdata.use_light_;
        use_lightmap_ = rdata.use_lightmap_;
        batching_ = rdata.batching_;
        render_mask_ = rdata.render_mask_;
        bones_ubo_ = rdata.bones_ubo_;
        cast_shadows_ = rdata.cast_shadows_;
        batch_ = rdata.batch_;
        for(int i=0;i<rdata.render_pass_list_.size();i++) {
            render_pass_list_.push_back((rdata.render_pass_list_)[i]);
        }
        rendering_order_ = rdata.rendering_order_;
        hash_code_dirty_ = rdata.hash_code_dirty_;
        dirty_ = rdata.dirty_;
        offset_ = rdata.offset_;
        offset_factor_ = rdata.offset_factor_;
        offset_units_ = rdata.offset_units_;
        depth_test_ = rdata.depth_test_;
        depth_mask_ = rdata.depth_mask_;
        alpha_blend_ = rdata.alpha_blend_;
        source_alpha_blend_func_ = rdata.source_alpha_blend_func_;
        dest_alpha_blend_func_ = rdata.dest_alpha_blend_func_;
        alpha_to_coverage_ = rdata.alpha_to_coverage_;
        sample_coverage_ = rdata.sample_coverage_;
        invert_coverage_mask_ = rdata.invert_coverage_mask_;
        draw_mode_ = rdata.draw_mode_;
        texture_capturer = rdata.texture_capturer;

        stencilTestFlag_ = rdata.stencilTestFlag_;
        stencilMaskMask_ = rdata.stencilMaskMask_;
        stencilFuncFunc_ = rdata.stencilFuncFunc_;
        stencilFuncRef_ = rdata.stencilFuncRef_;
        stencilFuncMask_ = rdata.stencilFuncMask_;
        stencilOpSfail_ = rdata.stencilOpSfail_;
        stencilOpDpfail_ = rdata.stencilOpDpfail_;
        stencilOpDppass_ = rdata.stencilOpDppass_;
    }

    RenderData(const RenderData& rdata) {
        copy(rdata);
    }

    virtual ~RenderData();

    static long long getComponentType() {
        return COMPONENT_TYPE_RENDER_DATA;
    }

    Mesh* mesh() const {
        return mesh_;
    }

    virtual bool updateGPU(Renderer*,Shader*);
    void set_mesh(Mesh* mesh);

    void add_pass(RenderPass* render_pass);
    void remove_pass(int pass);
    RenderPass* pass(int pass);
    const RenderPass* pass(int pass) const;
    const int pass_count() const {
        return render_pass_list_.size();
    }

    ShaderData* material(int pass) const ;

    /**
     * Select or generate a shader for this render data.
     * This function executes a Java task on the Framework thread.
     */
    void bindShader(JNIEnv* env, jobject localSceneObject, bool);
    void markDirty() {
        dirty_ = true;
    }

    bool isDirty() const {
        return dirty_;
    }

    void clearDirty() {
        dirty_ = false;
    }

    void enable_light() {
        use_light_ = true;
        hash_code_dirty_ = true;
    }

    void disable_light() {
        use_light_ = false;
        hash_code_dirty_ = true;
    }

    bool light_enabled() {
        return use_light_;
    }

    void enable_lightmap() {
        use_lightmap_ = true;
        hash_code_dirty_ = true;
    }

    void disable_lightmap() {
        use_lightmap_ = false;
        hash_code_dirty_ = true;
    }

    int render_mask() const {
        return render_mask_;
    }

    void set_render_mask(int render_mask) {
        render_mask_ = render_mask;
        hash_code_dirty_ = true;
    }

    int rendering_order() const {
        return rendering_order_;
    }

    void set_rendering_order(int rendering_order) {
        rendering_order_ = rendering_order;
    }

    bool cast_shadows() {
        return cast_shadows_;
    }

    void set_cast_shadows(bool cast_shadows) {
        cast_shadows_ = cast_shadows;
    }

    Batch* getBatch() {
        return batch_;
    }

    void set_batching(bool status) {
        batching_ = status;
    }

    bool batching() {
        return batching_;
    }

    void setBatch(Batch* batch) {
        this->batch_ = batch;
    }

    void setBatchNull() {
        batch_ = nullptr;
    }

    bool cull_face(int pass=0) const ;

    bool offset() const {
        return offset_;
    }

    void set_offset(bool offset) {
        offset_ = offset;
        hash_code_dirty_ = true;
    }

    float offset_factor() const {
        return offset_factor_;
    }

    void set_offset_factor(float offset_factor) {
        offset_factor_ = offset_factor;
        hash_code_dirty_ = true;
    }

    float offset_units() const {
        return offset_units_;
    }

    void set_offset_units(float offset_units) {
        offset_units_ = offset_units;
        hash_code_dirty_ = true;
    }

    bool depth_test() const {
        return depth_test_;
    }

    bool depth_mask() const {
        return depth_mask_;
    }

    void set_depth_test(bool depth_test) {
        depth_test_ = depth_test;
        hash_code_dirty_ = true;
    }

    void set_depth_mask(bool depth_mask) {
        depth_mask_ = depth_mask;
        hash_code_dirty_ = true;
    }

    void set_alpha_blend_func(int sourceblend, int destblend) {
        source_alpha_blend_func_ = sourceblend;
        dest_alpha_blend_func_ = destblend;
    }

    int source_alpha_blend_func() const {
        return source_alpha_blend_func_;
    }

    int dest_alpha_blend_func() const {
        return dest_alpha_blend_func_;
    }

    bool alpha_blend() const {
        return alpha_blend_;
    }

    void set_alpha_blend(bool alpha_blend) {
        alpha_blend_ = alpha_blend;
        hash_code_dirty_ = true;
    }

    bool alpha_to_coverage() const {
        return alpha_to_coverage_;
    }

    void set_alpha_to_coverage(bool alpha_to_coverage) {
        alpha_to_coverage_ = alpha_to_coverage;
        hash_code_dirty_ = true;
    }

    void set_sample_coverage(float sample_coverage) {
        sample_coverage_ = sample_coverage;
        hash_code_dirty_ = true;
    }

    float sample_coverage() const {
        return sample_coverage_;
    }

    void set_invert_coverage_mask(GLboolean invert_coverage_mask) {
        invert_coverage_mask_ = invert_coverage_mask;
        hash_code_dirty_ = true;
    }

    GLboolean invert_coverage_mask() const {
        return invert_coverage_mask_;
    }

    GLenum draw_mode() const {
        return draw_mode_;
    }

    float camera_distance()
    {
        if (nullptr != cameraDistanceLambda_)
        {
            camera_distance_ = cameraDistanceLambda_();
            cameraDistanceLambda_ = nullptr;
        }
        return camera_distance_;
    }

    void set_draw_mode(GLenum draw_mode)
    {
        draw_mode_ = draw_mode;
        hash_code_dirty_ = true;
    }
    bool isHashCodeDirty()  { return hash_code_dirty_; }
    void set_texture_capturer(TextureCapturer *capturer) { texture_capturer = capturer; }

    // TODO: need to consider texture_capturer in hash_code ?
    TextureCapturer *get_texture_capturer() { return texture_capturer; }

    void set_shader(int pass, int shaderid, bool isMultiview)
    {
        LOGD("SHADER: RenderData:setNativeShader %d %p", shaderid, this);
        render_pass_list_[pass]->set_shader(shaderid, isMultiview);
    }

    int isValid(Renderer* renderer, const RenderState& scene);

    int             get_shader(bool useMultiview =false, int pass =0) const { return render_pass_list_[pass]->get_shader(useMultiview); }
    std::string     getHashCode();
    void            setCameraDistanceLambda(std::function<float()> func);

    void setStencilFunc(int func, int ref, int mask);

    void setStencilOp(int sfail, int dpfail, int dppass);

    void setStencilMask(unsigned int mask);

    unsigned int getStencilMask() { return stencilMaskMask_; }

    bool stencil_test() { return stencilTestFlag_; }
    int stencil_func_func() { return stencilFuncFunc_; }
    int stencil_func_ref() { return stencilFuncRef_; }
    int stencil_func_mask() { return stencilFuncMask_; }
    int stencil_op_sfail() { return stencilOpSfail_; }
    int stencil_op_dpfail() { return stencilOpDpfail_; }
    int stencil_op_dppass() { return stencilOpDppass_; }
    UniformBlock* getBonesUbo() {
        return bones_ubo_;
    }
    void adjustRenderingOrderForTransparency(bool hasAlpha);

private:
    RenderData(RenderData&& render_data) = delete;
    RenderData& operator=(const RenderData& render_data) = delete;
    RenderData& operator=(RenderData&& render_data) = delete;

protected:
    static const int DEFAULT_RENDER_MASK = Left | Right;
    static const int DEFAULT_RENDERING_ORDER = Geometry;
    jmethodID bindShaderMethod_;
    Mesh* mesh_;
    UniformBlock* bones_ubo_;
    Batch* batch_;
    bool hash_code_dirty_;
    bool dirty_;
    std::string hash_code;
    std::vector<RenderPass*> render_pass_list_;
    int source_alpha_blend_func_;
    int dest_alpha_blend_func_;
    bool use_light_;
    bool batching_;
    bool use_lightmap_;
    int render_mask_;
    int rendering_order_;
    bool offset_;
    float offset_factor_;
    float offset_units_;
    bool depth_test_;
    bool depth_mask_;
    bool alpha_blend_;
    bool alpha_to_coverage_;
    bool cast_shadows_;
    float sample_coverage_;
    GLboolean invert_coverage_mask_;
    GLenum draw_mode_;
    float camera_distance_;
    TextureCapturer *texture_capturer;
    std::function<float()> cameraDistanceLambda_ = nullptr;

    int stencilFuncFunc_ = 0;
    int stencilFuncRef_ = 0;
    int stencilFuncMask_ = 0;
    int stencilOpSfail_ = 0;
    int stencilOpDpfail_ = 0;
    int stencilOpDppass_ = 0;
    unsigned int stencilMaskMask_ = 0;
    bool stencilTestFlag_ = false;

public:
    void setStencilTest(bool flag);
};

bool compareRenderDataByOrderShaderDistance(RenderData* i, RenderData* j);
}
#endif
