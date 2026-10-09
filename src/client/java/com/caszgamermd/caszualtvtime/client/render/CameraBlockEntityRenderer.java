package com.caszgamermd.caszualtvtime.client.render;

import com.caszgamermd.caszualtvtime.block.CameraBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Draws the turret/lens separately from the static mounting foot.
 * The camera's block coordinates do not change, only its rendered orientation.
 * Its lens points north at zero rotation, matching the feed's yaw of 180 degrees.
 */
public final class CameraBlockEntityRenderer
    implements BlockEntityRenderer<CameraBlockEntity, CameraBlockEntityRenderState> {

    private static final Identifier BODY =
        Identifier.fromNamespaceAndPath("minecraft", "textures/block/iron_block.png");
    private static final Identifier RIM =
        Identifier.fromNamespaceAndPath("minecraft", "textures/block/copper_block.png");
    private static final Identifier LENS =
        Identifier.fromNamespaceAndPath("minecraft", "textures/block/black_concrete.png");

    public CameraBlockEntityRenderer(BlockEntityRendererProvider.Context ignored) {}

    @Override public CameraBlockEntityRenderState createRenderState() {
        return new CameraBlockEntityRenderState();
    }

    @Override public void extractRenderState(
        CameraBlockEntity camera,
        CameraBlockEntityRenderState state,
        float partialTick, Vec3 cameraPos,
        @Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay
    ) {
        BlockEntityRenderer.super.extractRenderState(
            camera, state, partialTick, cameraPos, crumblingOverlay
        );
        state.yaw = camera.yawDegrees();
        state.tilt = camera.tilt();
    }

    @Override public void submit(
        CameraBlockEntityRenderState state,
        PoseStack matrices,
        SubmitNodeCollector queue,
        CameraRenderState cameraState
    ) {
        matrices.pushPose();
        matrices.translate(0.5f, 0.5f, 0.5f);
        // north-facing camera head corresponds to Minecraft yaw 180 degrees
        matrices.mulPose(Axis.YP.rotationDegrees(180.0f - state.yaw));
        matrices.mulPose(Axis.XP.rotationDegrees(-state.tilt));
        matrices.translate(-0.5f, -0.5f, -0.5f);

        queue.submitCustomGeometry(
            matrices, RenderTypes.entityTranslucent(BODY),
            (pose, vertices) -> cube(pose, vertices, state.lightCoords,
                4f/16, 4f/16, 4f/16, 12f/16, 12f/16, 12f/16)
        );
        queue.submitCustomGeometry(
            matrices, RenderTypes.entityTranslucent(RIM),
            (pose, vertices) -> cube(pose, vertices, state.lightCoords,
                5f/16, 5f/16, 3f/16, 11f/16, 11f/16, 4f/16)
        );
        queue.submitCustomGeometry(
            matrices, RenderTypes.entityTranslucent(LENS),
            (pose, vertices) -> cube(pose, vertices, state.lightCoords,
                6f/16, 6f/16, 2.5f/16, 10f/16, 10f/16, 3f/16)
        );
        matrices.popPose();
    }

    private static void quad(PoseStack.Pose pose, VertexConsumer v, int light,
                             float x0, float y0, float z0,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float x3, float y3, float z3,
                             int nx, int ny, int nz) {
        vertex(pose,v,light,x0,y0,z0,0,0,nx,ny,nz);
        vertex(pose,v,light,x1,y1,z1,1,0,nx,ny,nz);
        vertex(pose,v,light,x2,y2,z2,1,1,nx,ny,nz);
        vertex(pose,v,light,x3,y3,z3,0,1,nx,ny,nz);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer v, int light,
                                float x,float y,float z,float u,float w,
                                int nx,int ny,int nz) {
        v.addVertex(pose,x,y,z)
            .setColor(-1).setUv(u,w)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(light).setNormal(pose,nx,ny,nz);
    }

    private static void cube(PoseStack.Pose pose, VertexConsumer v, int light,
                              float x0,float y0,float z0,float x1,float y1,float z1) {
        quad(pose,v,light,x0,y1,z0, x1,y1,z0, x1,y0,z0, x0,y0,z0,0,0,-1);
        quad(pose,v,light,x1,y1,z1, x0,y1,z1, x0,y0,z1, x1,y0,z1,0,0,1);
        quad(pose,v,light,x0,y1,z1, x0,y1,z0, x0,y0,z0, x0,y0,z1,-1,0,0);
        quad(pose,v,light,x1,y1,z0, x1,y1,z1, x1,y0,z1, x1,y0,z0,1,0,0);
        quad(pose,v,light,x0,y1,z1, x1,y1,z1, x1,y1,z0, x0,y1,z0,0,1,0);
        quad(pose,v,light,x0,y0,z0, x1,y0,z0, x1,y0,z1, x0,y0,z1,0,-1,0);
    }
}
