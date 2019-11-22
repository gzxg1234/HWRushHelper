package com.sanron.hwrushhelper;

import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import androidx.annotation.StringRes;


/**
 * 用户统一Toast调用~
 * Created with IntelliJ IDEA.
 * User: Jace
 * Date: 12-12-27
 * Time: 下午6:10
 * To change this template use File | Settings | File Templates.
 *
 * @author sanron
 */
public class ToastOfJH {

    private static Toast sToast;

    public static void show(Context context, String text, int duration, int gravity) {
        if (context == null) {
            Log.e(ToastOfJH.class.getSimpleName(), "context is null");
            return;
        }
        context = context.getApplicationContext();
        if (sToast != null) {
            sToast.cancel();
        }
        sToast = Toast.makeText(context, text, duration);
        sToast.setText(text);
        if (gravity != 0) {
            sToast.setGravity(gravity, 0, 0);
        }
        sToast.show();
    }

    public static void show(Context context, String text, int duration) {
        show(context, text, duration, Gravity.CENTER);
    }

    public static void show(Context context, @StringRes int resId) {
        show(context, context.getResources().getString(resId));
    }

    public static void show(Context context, String text) {
        show(context, text, Toast.LENGTH_SHORT);
    }

    public static void show(String msg, int duration) {
        show(App.getInstance(), msg, duration);
    }

    public static void show(String msg) {
        show(App.getInstance(), msg);
    }

    public static void show(@StringRes int res) {
        show(App.getInstance(), res);
    }

}
