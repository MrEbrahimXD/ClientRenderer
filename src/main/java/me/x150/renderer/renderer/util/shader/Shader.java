package me.x150.renderer.renderer.util.shader;

import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Getter;
import lombok.SneakyThrows;
import me.x150.renderer.renderer.util.ShaderEffectDuck;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.PostEffectPass;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL11;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class Shader {
    @Getter
    final PostEffectProcessor shader;
    int previousWidth, previousHeight;

    @SneakyThrows
    private Shader(Identifier ident, Consumer<Shader> init) {
        MinecraftClient client = MinecraftClient.getInstance();
        //        this.effect = ShaderEffectManager.getInstance().manage(ident, init);
        this.shader = new PostEffectProcessor(client.getTextureManager(), client.getResourceManager(), client.getFramebuffer(), ident);
        checkUpdateDimensions();
        init.accept(this);
    }

    public static Shader create(String progName, Consumer<Shader> callback) {
        return new Shader(new Identifier("renderer", String.format("shaders/post/%s.json", progName)), callback);
    }

    void checkUpdateDimensions() {
        MinecraftClient client = MinecraftClient.getInstance();
        int currentWidth = client.getWindow().getFramebufferWidth();
        int currentHeight = client.getWindow().getFramebufferHeight();
        if (previousWidth != currentWidth || previousHeight != currentHeight) {
            this.shader.setupDimensions(currentWidth, currentHeight);
            previousWidth = currentWidth;
            previousHeight = currentHeight;
        }
    }

    public void setUniformf(String name, float value) {
        List<PostEffectPass> passes = ((ShaderEffectDuck) shader).getPasses();
        passes.stream().map(postEffectPass -> postEffectPass.getProgram().getUniformByName(name)).filter(Objects::nonNull).forEach(glUniform -> glUniform.set(value));
    }

    public void setUniformSampler(String name, Framebuffer framebuffer) {
        List<PostEffectPass> passes = ((ShaderEffectDuck) shader).getPasses();
        for (PostEffectPass pass : passes) {
            pass.getProgram().bindSampler(name, framebuffer::getColorAttachment);
        }
    }

    public void render(float delta) {
        checkUpdateDimensions();
        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.enableTexture();
        RenderSystem.resetTextureMatrix();
        shader.render(delta);
        MinecraftClient.getInstance().getFramebuffer().beginWrite(true);
        RenderSystem.disableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA); // restore blending
        RenderSystem.enableDepthTest();

    }
}
