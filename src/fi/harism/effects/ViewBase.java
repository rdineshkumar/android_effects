package fi.harism.effects;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.widget.Toast;

abstract class ViewBase extends GLSurfaceView implements GLSurfaceView.Renderer {

	public ViewBase(Context context) {
		super(context);
	}

	/**
	 * Loads String from raw resources with given id.
	 */
	protected String loadRawString(int rawId) throws Exception {
		InputStream is = getContext().getResources().openRawResource(rawId);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] buf = new byte[1024];
		int len;
		while ((len = is.read(buf)) != -1) {
			baos.write(buf, 0, len);
		}
		return baos.toString();
	}

	/**
	 * Shows Toast on screen with given message.
	 */
	protected void showError(final String errorMsg) {
		post(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(getContext(), errorMsg, Toast.LENGTH_LONG)
						.show();
			}
		});
	}

}
