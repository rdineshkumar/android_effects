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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.SystemClock;

public class ViewRubber extends ViewBase {

	private static final int COUNT_EDGE = 20;
	private static final int COUNT_VERTICES = 4 * COUNT_EDGE;

	private static final float[][][] FACE_DATA = {
			{ { .3f, .5f, 1f }, { 0, 1, 2, 3 } },
			{ { .3f, .5f, 1f }, { 10, 6, 9, 11 } },
			{ { 1f, .5f, .3f }, { 4, 2, 6, 5 } },
			{ { 1f, .5f, .3f }, { 7, 9, 1, 8 } },
			{ { .5f, 1f, .3f }, { 12, 7, 13, 0 } },
			{ { .5f, 1f, .3f }, { 3, 14, 5, 15 } } };
	private static final int[][] FACE_LINES = { { 0, 8, 2 }, { 0, 9, 1 },
			{ 2, 10, 3 }, { 1, 11, 3 }, { 2, 12, 6 }, { 3, 13, 7 },
			{ 6, 14, 7 }, { 4, 15, 0 }, { 5, 16, 1 }, { 4, 17, 5 },
			{ 6, 18, 4 }, { 7, 19, 5 }, { 4, 18, 6 }, { 6, 12, 2 },
			{ 1, 16, 5 }, { 5, 19, 7 } };
	private static final float[][] FACE_VERTICES = { { -1, 1, 1 },
			{ -1, -1, 1 }, { 1, 1, 1 }, { 1, -1, 1 }, { -1, 1, -1 },
			{ -1, -1, -1 }, { 1, 1, -1 }, { 1, -1, -1 }, { 0, 1, 1 },
			{ -1, 0, 1 }, { 1, 0, 1 }, { 0, -1, 1 }, { 1, 1, 0 }, { 1, -1, 0 },
			{ 1, 0, -1 }, { -1, 1, 0 }, { -1, -1, 0 }, { -1, 0, -1 },
			{ 0, 1, -1 }, { 0, -1, -1 } };
	private static final float[][] FACE_VERTICES_SOURCE = new float[20][];
	private static final float[][] FACE_VERTICES_TARGET = new float[20][];

	private FloatBuffer mBufferFace;
	private float[] mEyeSource = { 0, 0, 5 };
	private float[] mEyeTarget = { 0, 0, 5 };

	private float[] mMatrixProjection = new float[16];
	private float[] mMatrixView = new float[16];
	private long mRenderTime;
	private boolean[] mShaderCompilerSupport = new boolean[1];
	private EffectsShader mShaderRubber = new EffectsShader();

	public ViewRubber(Context context) {
		super(context);

		ByteBuffer buffer = ByteBuffer.allocateDirect(4 * 2 * COUNT_VERTICES);
		mBufferFace = buffer.order(ByteOrder.nativeOrder()).asFloatBuffer();
		for (int i = 0; i < COUNT_EDGE; ++i) {
			float t = (float) i / (COUNT_EDGE - 1);
			mBufferFace.put(0).put(t).put(1).put(t);
		}
		for (int i = 0; i < COUNT_EDGE; ++i) {
			float t = (float) i / (COUNT_EDGE - 1);
			mBufferFace.put(2).put(t).put(3).put(t);
		}
		mBufferFace.position(0);

		for (int i = 0; i < 20; ++i) {
			FACE_VERTICES_SOURCE[i] = new float[3];
			FACE_VERTICES_TARGET[i] = new float[3];
		}

		setEGLContextClientVersion(2);
		setRenderer(this);
		setRenderMode(RENDERMODE_CONTINUOUSLY);
	}

	@Override
	public void onDrawFrame(GL10 unused) {

		GLES20.glClearColor(0f, 0f, 0f, 1f);
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

		if (mShaderCompilerSupport[0] == false) {
			return;
		}

		long time = SystemClock.uptimeMillis();
		if (time - mRenderTime > 2000) {
			for (int i = 0; i < 3; ++i) {
				mEyeSource[i] = mEyeTarget[i];
				mEyeTarget[i] = (float) (Math.random() * 10 - 5);
			}
			for (int i = 0; i < 20; ++i) {
				for (int j = 0; j < 3; ++j) {
					FACE_VERTICES_SOURCE[i][j] = FACE_VERTICES_TARGET[i][j];
					FACE_VERTICES_TARGET[i][j] = FACE_VERTICES[i][j]
							+ FACE_VERTICES[i][j]
							* (float) (Math.random() - 0.5);
				}
			}
			mRenderTime = time;
		}

		float t = (time - mRenderTime) / 2000f;
		t = t * t * (3 - 2 * t);

		final float[] eye = new float[3];
		for (int i = 0; i < 3; ++i) {
			eye[i] = mEyeSource[i] + (mEyeTarget[i] - mEyeSource[i]) * t;
		}

		Matrix.setLookAtM(mMatrixView, 0, eye[0], eye[1], eye[2], 0, 0, 0, 0,
				1, 0);

		mShaderRubber.useProgram();

		GLES20.glEnable(GLES20.GL_CULL_FACE);
		GLES20.glEnable(GLES20.GL_DEPTH_TEST);

		GLES20.glUniformMatrix4fv(mShaderRubber.getHandle("uViewM"), 1, false,
				mMatrixView, 0);
		GLES20.glUniformMatrix4fv(mShaderRubber.getHandle("uProjectionM"), 1,
				false, mMatrixProjection, 0);

		GLES20.glVertexAttribPointer(mShaderRubber.getHandle("aPosition"), 2,
				GLES20.GL_FLOAT, false, 0, mBufferFace);
		GLES20.glEnableVertexAttribArray(mShaderRubber.getHandle("aPosition"));

		for (float[][] face : FACE_DATA) {
			GLES20.glUniform3fv(mShaderRubber.getHandle("uColor"), 1, face[0],
					0);

			final float[] lines = new float[36];
			for (int i = 0; i < 4; ++i) {
				int[] indices = FACE_LINES[(int) face[1][i]];
				for (int j = 0; j < 3; ++j) {
					float[] verticesSource = FACE_VERTICES_SOURCE[indices[j]];
					float[] verticesTarget = FACE_VERTICES_TARGET[indices[j]];
					for (int k = 0; k < 3; ++k) {
						float value = verticesSource[k]
								+ (verticesTarget[k] - verticesSource[k]) * t;
						lines[i * 9 + j * 3 + k] = value;
					}
				}
			}

			GLES20.glUniform3fv(mShaderRubber.getHandle("uLine0"), 3, lines, 0);
			GLES20.glUniform3fv(mShaderRubber.getHandle("uLine1"), 3, lines, 9);
			GLES20.glUniform3fv(mShaderRubber.getHandle("uLine2"), 3, lines, 18);
			GLES20.glUniform3fv(mShaderRubber.getHandle("uLine3"), 3, lines, 27);
			GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, COUNT_VERTICES);
		}

	}

	@Override
	public void onSurfaceChanged(GL10 unused, int width, int height) {
		float aspect = (float) width / height;
		Matrix.perspectiveM(mMatrixProjection, 0, 60f, aspect, .1f, 10f);
	}

	@Override
	public void onSurfaceCreated(GL10 unused, EGLConfig config) {
		// Check if shader compiler is supported.
		GLES20.glGetBooleanv(GLES20.GL_SHADER_COMPILER, mShaderCompilerSupport,
				0);

		// If not, show user an error message and return immediately.
		if (!mShaderCompilerSupport[0]) {
			String msg = getContext().getString(R.string.error_shader_compiler);
			showError(msg);
			return;
		}

		try {
			String vertexSource = loadRawString(R.raw.rubber_vs);
			String fragmentSource = loadRawString(R.raw.rubber_fs);
			mShaderRubber.setProgram(vertexSource, fragmentSource);
		} catch (Exception ex) {
			showError(ex.getMessage());
		}
	}

}
