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

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.opengl.GLSurfaceView;
import android.os.Bundle;

public class EffectsActivity extends Activity implements ActionBar.TabListener {

	private GLSurfaceView mGLSurfaceView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Tab tab;
		ActionBar actionBar = getActionBar();

		actionBar.setTitle(R.string.title);
		actionBar.setSubtitle(R.string.subtitle);
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		actionBar.setBackgroundDrawable(getResources().getDrawable(
				R.drawable.bg_pattern_light));
		actionBar.setStackedBackgroundDrawable(getResources().getDrawable(
				R.drawable.bg_pattern_dark));

		tab = actionBar.newTab();
		tab.setTabListener(this);
		tab.setText(R.string.tab_fractal);
		tab.setTag(new ViewFractal(this));
		actionBar.addTab(tab);

		tab = actionBar.newTab();
		tab.setTabListener(this);
		tab.setText(R.string.tab_blob);
		tab.setTag(new ViewBlob(this));
		actionBar.addTab(tab);

		tab = actionBar.newTab();
		tab.setTabListener(this);
		tab.setText(R.string.tab_particles);
		tab.setTag(new ViewParticles(this));
		actionBar.addTab(tab);

		tab = actionBar.newTab();
		tab.setTabListener(this);
		tab.setText(R.string.tab_textures);
		tab.setTag(new ViewTextures(this));
		actionBar.addTab(tab);

	}

	@Override
	public void onPause() {
		super.onPause();
		if (mGLSurfaceView != null) {
			mGLSurfaceView.onPause();
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		if (mGLSurfaceView != null) {
			mGLSurfaceView.onResume();
		}
	}

	@Override
	public void onTabReselected(Tab tab, FragmentTransaction ft) {
	}

	@Override
	public void onTabSelected(Tab tab, FragmentTransaction ft) {
		mGLSurfaceView = (GLSurfaceView) tab.getTag();
		setContentView(mGLSurfaceView);
	}

	@Override
	public void onTabUnselected(Tab tab, FragmentTransaction ft) {
	}

}
