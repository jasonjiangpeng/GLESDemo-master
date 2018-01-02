package idv.chatea.gldemo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import idv.chatea.gldemo.gles20.Skybox;
import idv.chatea.gldemo.lighting.Light;
import idv.chatea.gldemo.lighting.MultipleLightObjObject;
import idv.chatea.gldemo.objloader.BasicObjLoader;
import idv.chatea.gldemo.objloader.SmoothObjLoader;
import idv.chatea.gldemo.shadow.MultipleShadowTexture;
import idv.chatea.gldemo.shadow.Shadow;
import idv.chatea.gldemo.shadow.ShadowEffectData;

/**
 * Used to demo the multiple shadow.
 * Key point is render different light's shadow in different FBO, then map it to final scene.
 */
public class GLES2_Multiple_Shadows_Activity extends AppCompatActivity {

    private GLSurfaceView mGLSurfaceView;
    private MyRenderer mRenderer;

    private float mPreviousY;
    private float mPreviousX;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mGLSurfaceView = new GLSurfaceView(this);
        mGLSurfaceView.setEGLContextClientVersion(2);
        // Just in case... explicitly specify using alpha bit.
        mGLSurfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        // explicitly specify the buffer size.
        mGLSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 8, 8);
        mGLSurfaceView.setDebugFlags(GLSurfaceView.DEBUG_CHECK_GL_ERROR);
        mRenderer = new MyRenderer();
        mGLSurfaceView.setRenderer(mRenderer);
        mGLSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        setContentView(mGLSurfaceView);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:
                final float dx = x - mPreviousX;
                final float dy = y - mPreviousY;
                mRenderer.handleDrag(dx, dy);
                break;
        }

        mPreviousX = x;
        mPreviousY = y;
        return true;
    }

    private static final float[] DESKTOP_SIZE = {300, 300, 300};

    /**
     * Customized a Mirror class. Use it to simulate the view.
     */
    private static class Desktop extends MultipleShadowTexture {

        public Desktop(Context context, Bitmap bitmap) {
            super(context, bitmap);
        }

        @Override
        public void draw(float[] vpMatrix, float[] moduleMatrix, float[] eyePosition, ShadowEffectData[] shadowEffectData) {
            float[] correctModuleMatrix = new float[16];
            /**
             * Default size of Plane is (1,1,1), scale it as our size
             */
            Matrix.scaleM(correctModuleMatrix, 0, moduleMatrix, 0, DESKTOP_SIZE[0], DESKTOP_SIZE[1], DESKTOP_SIZE[2]);
            /**
             * The original normal of plane is +z, make it as +y
             */
            Matrix.rotateM(correctModuleMatrix, 0, -90, 1, 0, 0);
            super.draw(vpMatrix, correctModuleMatrix, eyePosition, shadowEffectData);
            Matrix.rotateM(correctModuleMatrix, 0, 180, 1, 0, 0);
            super.draw(vpMatrix, correctModuleMatrix, eyePosition, shadowEffectData);
        }
    }

    private class MyRenderer implements GLSurfaceView.Renderer {
        private float[] mProjectMatrix = new float[16];
        private float[] mViewMatrix = new float[16];

        private static final float MOVEMENT_FACTOR_THETA = 180.0f / 320;
        private static final float MOVEMENT_FACTOR_PHI = 90.0f / 320;

        private float[] mEyePoint = new float[3];
        private float mViewDistance = 400;
        private float mTheta = 60;
        private float mPhi = 0;

        private Skybox mSkybox;
        private float[] mSkyboxModule = new float[16];

        private MultipleLightObjObject mTeapot;
        private float[] mTeapotModule = new float[16];

        private Light[] mLights;

        private Shadow[] mShadows;

        private Desktop mDesktop;
        private float[] mDesktopModule = new float[16];

        @Override
        public void onSurfaceCreated(GL10 unused, EGLConfig config) {
            GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
            GLES20.glEnable(GLES20.GL_DEPTH_TEST);
            GLES20.glEnable(GLES20.GL_CULL_FACE);

            Context context = GLES2_Multiple_Shadows_Activity.this;

            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.skybox);
            mSkybox = new Skybox(bitmap);
            bitmap.recycle();
            Matrix.setIdentityM(mSkyboxModule, 0);
            Matrix.scaleM(mSkyboxModule, 0, mViewDistance - 1f, mViewDistance - 1f, mViewDistance - 1f);

            BasicObjLoader loader = new SmoothObjLoader();
            mTeapot = new MultipleLightObjObject(context, loader.loadObjFile(context, "teapot/teapot.obj"));
            Matrix.setIdentityM(mTeapotModule, 0);
            Matrix.translateM(mTeapotModule, 0, 40, 0, -40);
            Matrix.rotateM(mTeapotModule, 0, -60, 0, 1, 0);

            mLights = new Light[2];
            mLights[0] = new Light();
            mLights[0].position = new float[]{mViewDistance, mViewDistance * 2, -mViewDistance, 1.0f};
            mLights[0].ambientChannel = new float[]{0.15f, 0.15f, 0.15f, 1.0f};
            mLights[0].diffusionChannel = new float[]{0.25f, 0.25f, 0.25f, 1.0f};
            mLights[0].specularChannel = new float[]{0.275f, 0.275f, 0.275f, 1.0f};

            mLights[1] = new Light();
            mLights[1].position = new float[]{-mViewDistance, mViewDistance * 2, -mViewDistance, 1.0f};
            mLights[1].ambientChannel = new float[]{0.15f, 0.15f, 0.15f, 1.0f};
            mLights[1].diffusionChannel = new float[]{0.25f, 0.25f, 0.25f, 1.0f};
            mLights[1].specularChannel = new float[]{0.275f, 0.275f, 0.275f, 1.0f};

            bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.tiles);
            mDesktop = new Desktop(context, bitmap);
            bitmap.recycle();
            Matrix.setIdentityM(mDesktopModule, 0);
        }

        @Override
        public void onSurfaceChanged(GL10 unused, int width, int height) {
            GLES20.glViewport(0, 0, width, height);

            float ratio = (float) width / height;
            Matrix.frustumM(mProjectMatrix, 0, -ratio * mViewDistance / 4, ratio * mViewDistance / 4,
                    -1 * mViewDistance / 4, 1 * mViewDistance / 4, mViewDistance, mViewDistance * 3);

            mShadows = new Shadow[2];
            mShadows[0] = new Shadow(width, height);
            mShadows[1] = new Shadow(width, height);
        }

        @Override
        public void onDrawFrame(GL10 unused) {

            float[] rotateMatrix = new float[16];
            Matrix.setRotateM(rotateMatrix, 0, 1, 0, 1, 0);
            Matrix.multiplyMM(mTeapotModule, 0, rotateMatrix, 0, mTeapotModule, 0);
            Matrix.multiplyMM(mDesktopModule, 0, rotateMatrix, 0, mDesktopModule, 0);

            updateEyePosition();

            /**
             * Prepare common rendering resources
             */
            Matrix.setLookAtM(mViewMatrix, 0,
                    mEyePoint[0], mEyePoint[1], mEyePoint[2],
                    0f, 0f, 0f,
                    0f, mTheta % 360 < 180 ? 1.0f : -1.0f, 0f);

            float[] vpMatrix = new float[16];

            Matrix.multiplyMM(vpMatrix, 0, mProjectMatrix, 0, mViewMatrix, 0);

            /**
             * Prepare lighting and shadowing resources
             */
            float[] lightViewMatrix = new float[16];

            ShadowEffectData[] shadowEffectDatas = new ShadowEffectData[2];

            for (int i = 0; i < shadowEffectDatas.length; i++) {
                Matrix.setLookAtM(lightViewMatrix, 0,
                        mLights[i].position[0], mLights[i].position[1], mLights[i].position[2],
                        0, 0, 0,
                        0f, mTheta % 360 < 180? 1.0f : -1.0f, 0f);

                float[] lightVPMatrix = new float[16];
                Matrix.multiplyMM(lightVPMatrix, 0, mProjectMatrix, 0, lightViewMatrix, 0);

                mShadows[i].startDrawShadowTexture();
                GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT);
                mTeapot.draw(lightVPMatrix, mTeapotModule, mLights, mLights[i].position);
                mShadows[i].stopDrawShadowTexture();

                shadowEffectDatas[i] = new ShadowEffectData(mShadows[i], mLights[i], lightVPMatrix);
            }

            /**
             * Render all scene.
             */
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

            mTeapot.draw(vpMatrix, mTeapotModule, mLights, mEyePoint);

            mDesktop.draw(vpMatrix, mDesktopModule, mEyePoint, shadowEffectDatas);

            mSkybox.draw(vpMatrix, mSkyboxModule);
        }

        public void handleDrag(final float dx, final float dy) {
            mGLSurfaceView.queueEvent(new Runnable() {
                @Override
                public void run() {
                    mTheta -= MOVEMENT_FACTOR_THETA * dy;
                    while (mTheta < 0) {
                        mTheta += 360;
                    }
                    mTheta = mTheta % 360;

                    mPhi -= MOVEMENT_FACTOR_PHI * dx * (mTheta < 180? 1: -1);
                    while (mPhi < 0) {
                        mPhi += 360;
                    }
                    mPhi = mPhi % 360;
                }
            });
        }

        private void updateEyePosition() {
            float theta = mTheta % 360;
            float phi = mPhi % 360;

            double radianceTheta = theta * Math.PI / 180;
            double radiancePhi = phi * Math.PI / 180;

            mEyePoint[0] = (float) (2 * mViewDistance * Math.sin(radianceTheta) * Math.sin(radiancePhi));
            mEyePoint[1] = (float) (2 * mViewDistance * Math.cos(radianceTheta));
            mEyePoint[2] = (float) (2 * mViewDistance * Math.sin(radianceTheta) * Math.cos(radiancePhi));
        }
    }
}
