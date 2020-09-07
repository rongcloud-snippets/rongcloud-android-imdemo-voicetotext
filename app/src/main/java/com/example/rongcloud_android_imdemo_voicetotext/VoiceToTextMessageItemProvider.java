package com.example.rongcloud_android_imdemo_voicetotext;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources.NotFoundException;
import android.graphics.drawable.AnimationDrawable;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.net.Uri;
import android.os.Build.VERSION;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;

import io.rong.common.RLog;
import io.rong.eventbus.EventBus;
import io.rong.imkit.R.bool;
import io.rong.imkit.R.drawable;
import io.rong.imkit.R.id;
import io.rong.imkit.R.string;
import io.rong.imkit.destruct.DestructManager;
import io.rong.imkit.manager.AudioPlayManager;
import io.rong.imkit.manager.AudioRecordManager;
import io.rong.imkit.manager.IAudioPlayListener;
import io.rong.imkit.model.Event.AudioListenedEvent;
import io.rong.imkit.model.Event.PlayAudioEvent;
import io.rong.imkit.model.ProviderTag;
import io.rong.imkit.model.UIMessage;
import io.rong.imkit.widget.AutoLinkTextView;
import io.rong.imkit.widget.provider.VoiceMessageItemProvider;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.RongIMClient.DestructCountDownTimerListener;
import io.rong.imlib.RongIMClient.ResultCallback;
import io.rong.imlib.model.Message.MessageDirection;
import io.rong.message.VoiceMessage;

@ProviderTag(
        messageContent = VoiceMessage.class,
        showReadState = true
)
public class VoiceToTextMessageItemProvider extends VoiceMessageItemProvider {
    private static final String TAG = "VoiceToTextMessageItemProvider";

    public VoiceToTextMessageItemProvider(Context context) {
        super(context);
    }

    public View newView(Context context, ViewGroup group) {
        View view = LayoutInflater.from(context).inflate(R.layout.rc_item_voicetotext_message, (ViewGroup) null);
        VoiceToTextMessageItemProvider.ViewHolder holder = new VoiceToTextMessageItemProvider.ViewHolder();
        holder.left = (TextView) view.findViewById(id.rc_left);
        holder.right = (TextView) view.findViewById(id.rc_right);
        holder.img = (ImageView) view.findViewById(id.rc_img);
        holder.unread = (ImageView) view.findViewById(id.rc_voice_unread);
        holder.sendFire = (FrameLayout) view.findViewById(id.fl_send_fire);
        holder.receiverFire = (FrameLayout) view.findViewById(id.fl_receiver_fire);
        holder.receiverFireImg = (ImageView) view.findViewById(id.iv_receiver_fire);
        holder.receiverFireText = (TextView) view.findViewById(id.tv_receiver_fire);
        holder.speechToText = (AutoLinkTextView) view.findViewById(R.id.rc_voicet_totext);
        view.setTag(holder);

        return view;
    }

    @SuppressLint("WrongConstant")
    public void bindView(View v, int position, VoiceMessage content, UIMessage message) {
        VoiceToTextMessageItemProvider.ViewHolder holder = (VoiceToTextMessageItemProvider.ViewHolder) v.getTag();
        if (content.isDestruct()) {
            if (message.getMessageDirection() == MessageDirection.SEND) {
                holder.sendFire.setVisibility(0);
                holder.receiverFire.setVisibility(8);
            } else {
                holder.sendFire.setVisibility(8);
                holder.receiverFire.setVisibility(0);
                DestructManager.getInstance().addListener(message.getUId(), new VoiceToTextMessageItemProvider.DestructListener(holder, message), "VoiceToTextMessageItemProvider");
                if (message.getMessage().getReadTime() > 0L) {
                    holder.receiverFireText.setVisibility(0);
                    holder.receiverFireImg.setVisibility(8);
                    String unFinishTime;
                    if (TextUtils.isEmpty(message.getUnDestructTime())) {
                        unFinishTime = DestructManager.getInstance().getUnFinishTime(message.getUId());
                    } else {
                        unFinishTime = message.getUnDestructTime();
                    }

                    holder.receiverFireText.setText(unFinishTime);
                    DestructManager.getInstance().startDestruct(message.getMessage());
                } else {
                    holder.receiverFireText.setVisibility(8);
                    holder.receiverFireImg.setVisibility(0);
                }
            }
        } else {
            holder.sendFire.setVisibility(8);
            holder.receiverFire.setVisibility(8);
        }

        boolean listened;
        Uri playingUri;
        if (message.continuePlayAudio) {
            playingUri = AudioPlayManager.getInstance().getPlayingUri();
            if (playingUri == null || !playingUri.equals(content.getUri())) {
                listened = message.getMessage().getReceivedStatus().isListened();
                AudioPlayManager.getInstance().startPlay(v.getContext(), content.getUri(), new VoiceToTextMessageItemProvider.VoiceMessagePlayListener(v.getContext(), message, holder, listened));
            }
        } else {
            playingUri = AudioPlayManager.getInstance().getPlayingUri();
            if (playingUri != null && playingUri.equals(content.getUri())) {
                this.setLayout(v.getContext(), holder, message, true);
                listened = message.getMessage().getReceivedStatus().isListened();
                AudioPlayManager.getInstance().setPlayListener(new VoiceToTextMessageItemProvider.VoiceMessagePlayListener(v.getContext(), message, holder, listened));
            } else {
                this.setLayout(v.getContext(), holder, message, false);
            }
        }
        if (message.getExtra() != null) {
            holder.speechToText.setVisibility(View.VISIBLE);
            holder.speechToText.setText(message.getExtra());
        }

    }

    @SuppressLint("WrongConstant")
    public void onItemClick(View view, int position, VoiceMessage content, UIMessage message) {
        RLog.d("VoiceToTextMessageItemProvider", "Item index:" + position);
        if (content != null) {
            VoiceToTextMessageItemProvider.ViewHolder holder = (VoiceToTextMessageItemProvider.ViewHolder) view.getTag();
            if (AudioPlayManager.getInstance().isPlaying()) {
                if (AudioPlayManager.getInstance().getPlayingUri().equals(content.getUri())) {
                    AudioPlayManager.getInstance().stopPlay();
                    return;
                }
                AudioPlayManager.getInstance().stopPlay();
            }
            if (!AudioPlayManager.getInstance().isInNormalMode(view.getContext()) && AudioPlayManager.getInstance().isInVOIPMode(view.getContext())) {
                Toast.makeText(view.getContext(), view.getContext().getString(string.rc_voip_occupying), 0).show();
            } else {
                holder.unread.setVisibility(8);
                boolean listened = message.getMessage().getReceivedStatus().isListened();
                AudioPlayManager.getInstance().startPlay(view.getContext(), content.getUri(), new VoiceToTextMessageItemProvider.VoiceMessagePlayListener(view.getContext(), message, holder, listened));
            }
        }
    }

    @SuppressLint("WrongConstant")
    private void setLayout(Context context, VoiceToTextMessageItemProvider.ViewHolder holder, UIMessage message, boolean playing) {
        VoiceMessage content = (VoiceMessage) message.getContent();
        //int minWidth = 70;
        //int maxWidth = 204;
        float scale = context.getResources().getDisplayMetrics().density;
        int minWidth = (int) ((float) 70 * scale + 0.5F);
        int maxWidth = (int) ((float) 204 * scale + 0.5F);
        int duration = AudioRecordManager.getInstance().getMaxVoiceDuration();
        holder.img.getLayoutParams().width = minWidth + (maxWidth - minWidth) / duration * content.getDuration();
        AnimationDrawable animationDrawable;
        if (message.getMessageDirection() == MessageDirection.SEND) {
            holder.left.setText(String.format("%s\"", content.getDuration()));
            holder.left.setVisibility(0);
            holder.right.setVisibility(8);
            holder.unread.setVisibility(8);
            holder.img.setScaleType(ScaleType.FIT_END);
            holder.img.setBackgroundResource(drawable.rc_ic_bubble_right);
            animationDrawable = (AnimationDrawable) context.getResources().getDrawable(drawable.rc_an_voice_sent);
            if (playing) {
                holder.img.setImageDrawable(animationDrawable);
                if (animationDrawable != null) {
                    animationDrawable.start();
                }
            } else {
                holder.img.setImageDrawable(holder.img.getResources().getDrawable(drawable.rc_ic_voice_sent));
                if (animationDrawable != null) {
                    animationDrawable.stop();
                }
            }
        } else {
            holder.right.setText(String.format("%s\"", content.getDuration()));
            holder.right.setVisibility(0);
            holder.left.setVisibility(8);
            if (!message.getReceivedStatus().isListened()) {
                holder.unread.setVisibility(0);
            } else {
                holder.unread.setVisibility(8);
            }

            holder.img.setBackgroundResource(drawable.rc_ic_bubble_left);
            animationDrawable = (AnimationDrawable) context.getResources().getDrawable(drawable.rc_an_voice_receive);
            if (playing) {
                holder.img.setImageDrawable(animationDrawable);
                if (animationDrawable != null) {
                    animationDrawable.start();
                }
            } else {
                holder.img.setImageDrawable(holder.img.getResources().getDrawable(drawable.rc_ic_voice_receive));
                if (animationDrawable != null) {
                    animationDrawable.stop();
                }
            }

            holder.img.setScaleType(ScaleType.FIT_START);
        }

    }

    public Spannable getContentSummary(VoiceMessage data) {
        return null;
    }

    public Spannable getContentSummary(Context context, VoiceMessage data) {
        return data.isDestruct() ? new SpannableString(context.getString(string.rc_message_content_burn)) : new SpannableString(context.getString(string.rc_message_content_voice));
    }

    @TargetApi(8)
    private boolean muteAudioFocus(Context context, boolean bMute) {
        if (context == null) {
            RLog.d("VoiceToTextMessageItemProvider", "muteAudioFocus context is null.");
            return false;
        } else if (VERSION.SDK_INT < 8) {
            RLog.d("VoiceToTextMessageItemProvider", "muteAudioFocus Android 2.1 and below can not stop music");
            return false;
        } else {
            boolean bool = false;
            AudioManager am = (AudioManager) context.getSystemService("audio");
            int result;
            if (bMute) {
                result = am.requestAudioFocus((OnAudioFocusChangeListener) null, 3, 2);
                bool = result == 1;
            } else {
                result = am.abandonAudioFocus((OnAudioFocusChangeListener) null);
                bool = result == 1;
            }

            RLog.d("VoiceToTextMessageItemProvider", "muteAudioFocus pauseMusic bMute=" + bMute + " result=" + bool);
            return bool;
        }
    }

    private static class DestructListener implements DestructCountDownTimerListener {
        private WeakReference<ViewHolder> mHolder;
        private UIMessage mUIMessage;

        public DestructListener(VoiceToTextMessageItemProvider.ViewHolder pHolder, UIMessage pUIMessage) {
            this.mHolder = new WeakReference(pHolder);
            this.mUIMessage = pUIMessage;
        }

        @SuppressLint("WrongConstant")
        public void onTick(long millisUntilFinished, String pMessageId) {
            if (this.mUIMessage.getUId().equals(pMessageId)) {
                VoiceToTextMessageItemProvider.ViewHolder viewHolder = (VoiceToTextMessageItemProvider.ViewHolder) this.mHolder.get();
                if (viewHolder != null) {
                    viewHolder.receiverFireText.setVisibility(0);
                    viewHolder.receiverFireImg.setVisibility(8);
                    String unDestructTime = String.valueOf(Math.max(millisUntilFinished, 1L));
                    viewHolder.receiverFireText.setText(unDestructTime);
                    this.mUIMessage.setUnDestructTime(unDestructTime);
                }
            }

        }

        @SuppressLint("WrongConstant")
        public void onStop(String messageId) {
            if (this.mUIMessage.getUId().equals(messageId)) {
                VoiceToTextMessageItemProvider.ViewHolder viewHolder = (VoiceToTextMessageItemProvider.ViewHolder) this.mHolder.get();
                if (viewHolder != null) {
                    viewHolder.receiverFireText.setVisibility(8);
                    viewHolder.receiverFireImg.setVisibility(0);
                    this.mUIMessage.setUnDestructTime((String) null);
                }
            }

        }
    }


    private class VoiceMessagePlayListener implements IAudioPlayListener {
        private Context context;
        private UIMessage message;
        private VoiceToTextMessageItemProvider.ViewHolder holder;
        private boolean listened;

        public VoiceMessagePlayListener(Context context, UIMessage message, VoiceToTextMessageItemProvider.ViewHolder holder, boolean listened) {
            this.context = context;
            this.message = message;
            this.holder = holder;
            this.listened = listened;
        }

        public void onStart(Uri uri) {
            this.message.continuePlayAudio = false;
            this.message.setListening(true);
            this.message.getReceivedStatus().setListened();
            RongIMClient.getInstance().setMessageReceivedStatus(this.message.getMessageId(), this.message.getReceivedStatus(), (ResultCallback) null);
            VoiceToTextMessageItemProvider.this.setLayout(this.context, this.holder, this.message, true);
            EventBus.getDefault().post(new AudioListenedEvent(this.message.getMessage()));
            if (this.message.getContent().isDestruct() && this.message.getMessageDirection().equals(MessageDirection.RECEIVE)) {
                DestructManager.getInstance().stopDestruct(this.message.getMessage());
            }

        }

        public void onStop(Uri uri) {
            if (this.message.getContent() instanceof VoiceMessage) {
                this.message.setListening(false);
                VoiceToTextMessageItemProvider.this.setLayout(this.context, this.holder, this.message, false);
                if (this.message.getContent().isDestruct() && this.message.getMessageDirection().equals(MessageDirection.RECEIVE)) {
                    DestructManager.getInstance().startDestruct(this.message.getMessage());
                }
            }

        }

        public void onComplete(Uri uri) {
            PlayAudioEvent event = PlayAudioEvent.obtain();
            event.messageId = this.message.getMessageId();
            if (this.message.isListening() && this.message.getMessageDirection().equals(MessageDirection.RECEIVE)) {
                try {
                    event.continuously = this.context.getResources().getBoolean(bool.rc_play_audio_continuous);
                } catch (NotFoundException var4) {
                    var4.printStackTrace();
                }
            }

            if (event.continuously && !this.message.getContent().isDestruct()) {
                EventBus.getDefault().post(event);
            }

            this.message.setListening(false);
            VoiceToTextMessageItemProvider.this.setLayout(this.context, this.holder, this.message, false);
            if (this.message.getContent().isDestruct() && this.message.getMessageDirection().equals(MessageDirection.RECEIVE)) {
                DestructManager.getInstance().startDestruct(this.message.getMessage());
            }

        }
    }


    private static class ViewHolder {
        ImageView img;
        TextView left;
        TextView right;
        ImageView unread;
        FrameLayout sendFire;
        FrameLayout receiverFire;
        ImageView receiverFireImg;
        TextView receiverFireText;
        AutoLinkTextView speechToText;

        private ViewHolder() {
        }
    }
}
