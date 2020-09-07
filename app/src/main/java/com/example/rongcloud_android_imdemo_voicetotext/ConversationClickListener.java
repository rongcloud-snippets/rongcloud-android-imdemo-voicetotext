package com.example.rongcloud_android_imdemo_voicetotext;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.example.rongcloud_android_imdemo_voicetotext.util.AudioDecode;
import com.example.rongcloud_android_imdemo_voicetotext.util.JsonParser;
import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechEvent;
import com.iflytek.cloud.SpeechRecognizer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

import io.rong.imkit.RongContext;
import io.rong.imkit.RongIM;
import io.rong.imkit.utilities.OptionsPopupDialog;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.UserInfo;
import io.rong.message.VoiceMessage;

public class ConversationClickListener implements RongIM.ConversationClickListener {

    // 语音听写对象
    private SpeechRecognizer mIat;
    // 听写结果内容
    private EditText mResultText;
    // 用HashMap存储听写结果
    private HashMap<String, String> mIatResults = new LinkedHashMap<String, String>();
    private String mEngineType = "cloud";
    private SharedPreferences mSharedPreferences;
    private static String TAG = "ConversationClickListener";
    private Context mContext;
    private Toast mToast;
    private AudioDecode audioDecode;

    private Message mMessage;

    @Override
    public boolean onUserPortraitClick(Context context, Conversation.ConversationType conversationType, UserInfo user, String targetId) {
        return false;
    }

    @Override
    public boolean onUserPortraitLongClick(Context context, Conversation.ConversationType conversationType, UserInfo user, String targetId) {
        return false;
    }

    @Override
    public boolean onMessageClick(Context context, View view, Message message) {
        return false;
    }

    @Override
    public boolean onMessageLinkClick(Context context, String link, Message message) {
        return false;
    }

    @Override
    public boolean onMessageLongClick(final Context context, View view, final Message message) {
        this.mContext = context;
        // 删除，撤回，更多
        if (message.getContent() instanceof VoiceMessage) {
            String[] items = new String[]{"翻译"};
            /**
             * newInstance() 初始化OptionsPopupDialog
             * @param items弹出菜单功能选项
             * setOptionsPopupDialogListener()设置点击弹出菜单的监听
             * @param which表示点击的哪一个菜单项,与items的顺序一致
             * show()显示pop dialog
             */
            OptionsPopupDialog.newInstance(view.getContext(), items).setOptionsPopupDialogListener(new OptionsPopupDialog.OnOptionsItemClickedListener() {

                @Override
                public void onOptionsItemClicked(int which) {
                    if (which == 0) {
                        startToText(context, message);
                    }
                }
            }).show();
            return true;
        }
        return false;
    }

    private void startToText(Context context, final Message message) {
        int ret = 0;// 函数调用返回值
        mMessage = message;
        mIat = SpeechRecognizer.createRecognizer(context, mInitListener);

    }

    /**
     * 初始化监听器。
     */
    private InitListener mInitListener = new InitListener() {

        @Override
        public void onInit(int code) {
            Log.d(TAG, "SpeechRecognizer init() code = " + code);
            if (code != ErrorCode.SUCCESS) {
                showTip("初始化失败，错误码：" + code + ",请点击网址https://www.xfyun.cn/document/error-code查询解决方案");
            }

            mSharedPreferences = mContext.getSharedPreferences("c", Activity.MODE_PRIVATE);
            mToast = Toast.makeText(mContext, "", Toast.LENGTH_SHORT);
            mIat.setParameter(SpeechConstant.AUDIO_SOURCE, "-1");
            int ret = mIat.startListening(new RecognizerListener() {
                @Override
                public void onBeginOfSpeech() {
                    // 此回调表示：sdk内部录音机已经准备好了，用户可以开始语音输入
                }

                @Override
                public void onError(SpeechError error) {
                    Log.d(TAG, "onError: " + error);
                    // Tips：
                    // 错误码：10118(您没有说话)，可能是录音机权限被禁，需要提示用户打开应用的录音权限。
                }

                @Override
                public void onEndOfSpeech() {
                    // 此回调表示：检测到了语音的尾端点，已经进入识别过程，不再接受语音输入
                }

                @Override
                public void onResult(RecognizerResult results, boolean isLast) {
                    String text = JsonParser.parseIatResult(results.getResultString());
                    if (isLast) {
                        //TODO 最后的结果
                        mMessage.setExtra(text);
                        RongContext.getInstance().getEventBus().post(mMessage);
                    }
                }

                @Override
                public void onVolumeChanged(int volume, byte[] data) {
                    //("当前正在说话，音量大小：" + volume);
                }

                @Override
                public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
                    // 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
                    // 若使用本地能力，会话id为null
                    if (SpeechEvent.EVENT_SESSION_ID == eventType) {
                        String sid = obj.getString(SpeechEvent.KEY_EVENT_AUDIO_URL);
                    }
                }
            });

            if (ret != ErrorCode.SUCCESS) {
                showTip("识别失败,错误码：" + ret + ",请点击网址https://www.xfyun.cn/document/error-code查询解决方案");
            } else {
                try {
                    audioDecode = AudioDecode.newInstance();
                    audioDecode.setFilePath(((VoiceMessage) mMessage.getContent()).getUri().toString());
                    audioDecode.prepare();
                    audioDecode.setOnCompleteListener(new AudioDecode.OnCompleteListener() {
                        @Override
                        public void completed(final ArrayList<byte[]> pcmData) {
                            if (pcmData != null) {
                                //写入音频文件数据，数据格式必须是采样率为8KHz或16KHz（本地识别只支持16K采样率，云端都支持），位长16bit，单声道的wav或者pcm
                                //必须要先保存到本地，才能被讯飞识别
                                for (byte[] data : pcmData) {
                                    mIat.writeAudio(data, 0, data.length);
                                }
                                mIat.stopListening();
                            } else {
                                mIat.cancel();
                                Log.d(TAG, "--->读取音频流失败");
                            }
                            audioDecode.release();
                        }
                    });
                    audioDecode.startAsync();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    };

    private void showTip(final String str) {
        mToast.setText(str);
        mToast.show();
    }


}
