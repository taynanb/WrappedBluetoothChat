package br.com.kanamobi.testandroidplugin;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

public class Test {

    public static void startTestA(Context context){
        context.startActivity(new Intent(context, TestActivity.class));
    }

    public static void startTestB(Activity context){
        context.startActivity(new Intent(context, TestActivity.class));
    }

}
