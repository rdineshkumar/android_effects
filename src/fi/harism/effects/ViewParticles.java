/*
   Copyright 2012 Harri Smatt

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package fi.harism.effects;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Vector;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.graphics.PointF;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.FloatMath;
import android.widget.Toast;

/**
 * Particles Renderer and GLSurfaceView.
 */
public class ViewParticles extends GLSurfaceView implements
		GLSurfaceView.Renderer {

	private ByteBuffer mBufferQuad;
	private Context mContext;
	private float mEmitterDir;
	private float mEmitterDirSource;
	private float mEmitterDirTarget;
	private PointF mEmitterPos = new PointF();
	private PointF mEmitterPosSource = new PointF();
	private PointF mEmitterPosTarget = new PointF();
	private float[] mMatrixProjection = new float[16];
	private Vector<Particle> mParticles = new Vector<Particle>();
	private boolean[] mShaderCompilerSupport = new boolean[1];
	private EffectsShader mShaderParticle = new EffectsShader();
	private Worker mWorker = new Worker();

	public ViewParticles(Context context) {
		super(context);

		mContext = context;

		// Full view quad buffer.
		final byte[] QUAD = { -1, 1, -1, -1, 1, 1, 1, -1 };
		mBufferQuad = ByteBuffer.allocateDirect(8);
		mBufferQuad.put(QUAD).position(0);

		setEGLContextClientVersion(2);
		setRenderer(this);
		setRenderMode(RENDERMODE_WHEN_DIRTY);
		queueEvent(mWorker);
	}

	/**
	 * Loads String from raw resources with given id.
	 */
	private String loadRawString(int rawId) throws Exception {
		InputStream is = mContext.getResources().openRawResource(rawId);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] buf = new byte[1024];
		int len;
		while ((len = is.read(buf)) != -1) {
			baos.write(buf, 0, len);
		}
		return baos.toString();
	}

	@Override
	public void onDrawFrame(GL10 unused) {
		GLES20.glClearColor(0f, 0f, 0f, 0f);
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT
				| GLES20.GL_STENCIL_BUFFER_BIT);

		if (mShaderCompilerSupport[0] == false) {
			return;
		}

		GLES20.glDisable(GLES20.GL_DEPTH_TEST);
		GLES20.glDisable(GLES20.GL_CULL_FACE);
		GLES20.glEnable(GLES20.GL_BLEND);
		GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

		mShaderParticle.useProgram();
		int uPosition = mShaderParticle.getHandle("uPosition");
		int uProjectionM = mShaderParticle.getHandle("uProjectionM");
		int uColor = mShaderParticle.getHandle("uColor");
		int aPosition = mShaderParticle.getHandle("aPosition");

		GLES20.glVertexAttribPointer(aPosition, 2, GLES20.GL_BYTE, false, 0,
				mBufferQuad);
		GLES20.glEnableVertexAttribArray(aPosition);

		GLES20.glUniformMatrix4fv(uProjectionM, 1, false, mMatrixProjection, 0);

		for (int i = 0; i < mParticles.size(); ++i) {
			float col = 1f;
			if (i < 1000) {
				col = i / 1000f;
			}
			GLES20.glUniform3f(uColor, col, col, col);

			Particle p = mParticles.get(i);
			GLES20.glUniform4f(uPosition, p.mPosition[0], p.mPosition[1], 0f,
					0.03f);
			GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
		}

		queueEvent(mWorker);
	}

	@Override
	public void onSurfaceChanged(GL10 unused, int width, int height) {
		float aspect = (float) width / height;
		Matrix.orthoM(mMatrixProjection, 0, -aspect, aspect, -1, 1, -1, 1);
	}

	@Override
	public void onSurfaceCreated(GL10 unused, EGLConfig config) {
		// Check if shader compiler is supported.
		GLES20.glGetBooleanv(GLES20.GL_SHADER_COMPILER, mShaderCompilerSupport,
				0);

		// If not, show user an error message and return immediately.
		if (!mShaderCompilerSupport[0]) {
			String msg = mContext.getString(R.string.error_shader_compiler);
			showError(msg);
			return;
		}

		try {
			String vertexSource, fragmentSource;
			vertexSource = loadRawString(R.raw.particle_vs);
			fragmentSource = loadRawString(R.raw.particle_fs);
			mShaderParticle.setProgram(vertexSource, fragmentSource);
		} catch (Exception ex) {
			showError(ex.getMessage());
		}
	}

	/**
	 * Shows Toast on screen with given message.
	 */
	private void showError(final String errorMsg) {
		new Handler(Looper.getMainLooper()).post(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(mContext, errorMsg, Toast.LENGTH_LONG).show();
			}
		});
	}

	/**
	 * Particle information container.
	 */
	private class Particle {
		public float[] mDirection = new float[2];
		public float[] mPosition = new float[2];
		public float mSpeed;
	}

	/**
	 * Worker runnable.
	 */
	private class Worker implements Runnable {

		private long mRenderTime;
		private long mRenderTimeLast;

		@Override
		public void run() {

			// First update emitter position and direction.
			long time = SystemClock.uptimeMillis();
			if (time - mRenderTime > 4000) {
				mEmitterPosSource.set(mEmitterPosTarget);
				mEmitterPosTarget.x = (float) (Math.random() * 2 - 1);
				mEmitterPosTarget.y = (float) (Math.random() * 2 - 1);
				mEmitterDirSource = mEmitterDirTarget;
				mEmitterDirTarget = (float) (Math.random() * 720);
				mRenderTime = time;
			}

			float t = (time - mRenderTime) / 4000f;
			t = t * t * (3 - 2 * t);

			mEmitterPos.x = mEmitterPosSource.x
					+ (mEmitterPosTarget.x - mEmitterPosSource.x) * t;
			mEmitterPos.y = mEmitterPosSource.y
					+ (mEmitterPosTarget.y - mEmitterPosSource.y) * t;
			mEmitterDir = mEmitterDirSource
					+ (mEmitterDirTarget - mEmitterDirSource) * t;

			// Emit particles.
			for (int i = 0; i < 100; ++i) {

				Particle p;
				if (mParticles.size() > 10000) {
					p = mParticles.remove(0);
				} else {
					p = new Particle();
				}

				p.mPosition[0] = mEmitterPos.x;
				p.mPosition[1] = mEmitterPos.y;

				float dir = (float) ((Math.PI * 2 * (mEmitterDir
						+ Math.random() * 40 - 20)) / 360);
				float len = (float) (Math.random() * 0.8 + 0.2);
				p.mDirection[0] = FloatMath.sin(dir) * len;
				p.mDirection[1] = FloatMath.cos(dir) * len;
				p.mSpeed = 0.8f;

				mParticles.add(p);
			}

			// Update particle positions.
			t = (time - mRenderTimeLast) / 1000f;
			for (int i = 0; i < mParticles.size(); ++i) {
				Particle p = mParticles.get(i);

				float dx = p.mPosition[0] - mEmitterPos.x;
				float dy = p.mPosition[1] - mEmitterPos.y;
				float len = FloatMath.sqrt(dx * dx + dy * dy);
				if (len > 0.0f && len < 0.2f) {
					p.mDirection[0] = p.mSpeed * p.mDirection[0]
							+ (1.0f - p.mSpeed) * dx / len;
					p.mDirection[1] = p.mSpeed * p.mDirection[1]
							+ (1.0f - p.mSpeed) * dy / len;
					p.mSpeed += 0.2f - len;
					if (p.mSpeed > 0.8f) {
						p.mSpeed = 0.8f;
					}
				}

				p.mPosition[0] += p.mDirection[0] * p.mSpeed * t;
				p.mPosition[1] += p.mDirection[1] * p.mSpeed * t;
				p.mSpeed *= 1.0 - t;

			}
			mRenderTimeLast = time;

			requestRender();
		}

	}

}
