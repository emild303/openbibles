package com.konibee.bible;

import android.app.Activity;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

public class About extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.about);
		TextView txtAboutContent = (TextView) findViewById(R.id.about_content);
		txtAboutContent.setMovementMethod(LinkMovementMethod.getInstance());
	}
}
