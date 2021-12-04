package com.github.adee42.keyboardvisibility;

import android.app.Activity;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import androidx.annotation.NonNull;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.PluginRegistry;

public class KeyboardVisibilityPlugin implements FlutterPlugin, ActivityAware, EventChannel.StreamHandler, ViewTreeObserver.OnGlobalLayoutListener {
    private static final String STREAM_CHANNEL_NAME = "github.com/adee42/flutter_keyboard_visibility";
    View mainView = null;
    EventChannel.EventSink eventsSink;
    boolean isVisible;

    private void init(BinaryMessenger messenger) {
        final EventChannel eventChannel = new EventChannel(messenger, STREAM_CHANNEL_NAME);
        eventChannel.setStreamHandler(this);
    }

    @Override
    public void onGlobalLayout() {
        Rect r = new Rect();

        if (mainView != null) {
			mainView.getWindowVisibleDisplayFrame(r);

			// check if the visible part of the screen is less than 85%
			// if it is then the keyboard is showing
			boolean newState = ((double)r.height() / (double)mainView.getRootView().getHeight()) < 0.85;

			if (newState != isVisible) {
				isVisible = newState;
				if (eventsSink != null) {
					eventsSink.success(isVisible ? 1 : 0);
				}
			}
		}
    }

    private void listenForKeyboard(Activity activity) {
        mainView = activity.<ViewGroup>findViewById(android.R.id.content);
        mainView.getViewTreeObserver().addOnGlobalLayoutListener(this);
        try {
            mainView = activity.<ViewGroup>findViewById(android.R.id.content);
            mainView.getViewTreeObserver().addOnGlobalLayoutListener(this);
        }
        catch (Exception e) {
            // do nothing
        }
    }

    private void unregisterListener() {
        if (mainView != null) {
            mainView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            mainView = null;
        }
    }

    public static void registerWith(PluginRegistry.Registrar registrar) {
        final KeyboardVisibilityPlugin plugin = new KeyboardVisibilityPlugin();
        plugin.init(registrar.messenger());
        plugin.listenForKeyboard(registrar.activity());
    }

    @Override
    public void onListen(Object arguments, EventChannel.EventSink events) {
        this.eventsSink = events;

        // is keyboard is visible at startup, let our subscriber know
        if (isVisible) {
            eventsSink.success(1);
        }
    }

    @Override
    public void onCancel(Object arguments) {
        eventsSink = null;
    }

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {
        init(binding.getBinaryMessenger());
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        unregisterListener();
    }

    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
        listenForKeyboard(binding.getActivity());
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
        unregisterListener();

    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
        listenForKeyboard(binding.getActivity());
    }

    @Override
    public void onDetachedFromActivity() {
        unregisterListener();
    }
}
