package com.example.rongcloud_android_imdemo_voicetotext;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import io.rong.imkit.RongIM;
import io.rong.imkit.fragment.ConversationListFragment;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;

public class ConversationListActivity extends AppCompatActivity {

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static final String TAG = "ConversationListAct";
    private Context mContext;
    private static String[] PERMISSIONS_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.conversationlist);
        this.mContext = this;
        verifyStoragePermissions(this);
        initView();
    }

    private void initView() {
        RongIM.connect("pniGLSXhzq+IUY8F/G5rNB/kOM2lGBObZXedyq1fNWQ=@3deq.cn.rongnav.com;3deq.cn.rongcfg.com", new RongIMClient.ConnectCallback() {

            @Override
            public void onSuccess(String s) {
                initFragment();
            }

            @Override
            public void onError(RongIMClient.ConnectionErrorCode connectionErrorCode) {
                Log.i("Error", connectionErrorCode.getValue() + "");
            }

            @Override
            public void onDatabaseOpened(RongIMClient.DatabaseOpenStatus databaseOpenStatus) {
                RongIMClient.getInstance().setConversationNotificationStatus(Conversation.ConversationType.PRIVATE, "1002", Conversation.ConversationNotificationStatus.DO_NOT_DISTURB, new RongIMClient.ResultCallback<Conversation.ConversationNotificationStatus>() {
                    @Override
                    public void onSuccess(Conversation.ConversationNotificationStatus conversationNotificationStatus) {
                        Log.d(TAG, "onSuccess: " + conversationNotificationStatus);
                    }

                    @Override
                    public void onError(RongIMClient.ErrorCode errorCode) {
                        Log.d(TAG, "onError: " + errorCode);
                    }
                });
            }


        });

    }

    private void initFragment() {
        ConversationListFragment mConversationListFragment = new ConversationListFragment();
        Uri uri = Uri.parse("rong://" + getApplicationInfo().packageName).buildUpon()
                .appendPath("conversationlist")
                .appendQueryParameter(Conversation.ConversationType.PRIVATE.getName(), "false")
                .appendQueryParameter(Conversation.ConversationType.GROUP.getName(), "false")
                .appendQueryParameter(Conversation.ConversationType.PUBLIC_SERVICE.getName(), "false")
                .appendQueryParameter(Conversation.ConversationType.APP_PUBLIC_SERVICE.getName(), "false")
                .appendQueryParameter(Conversation.ConversationType.SYSTEM.getName(), "true")
                .build();
        mConversationListFragment.setUri(uri);
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        transaction.replace(R.id.conversationlist, mConversationListFragment);
        transaction.commit();
    }

    //然后通过一个函数来申请
    public static void verifyStoragePermissions(Activity activity) {
        try {
            //检测是否有写的权限
            int permission = ActivityCompat.checkSelfPermission(activity,
                    "android.permission.WRITE_EXTERNAL_STORAGE");
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // 没有写的权限，去申请写的权限，会弹出对话框
                ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
