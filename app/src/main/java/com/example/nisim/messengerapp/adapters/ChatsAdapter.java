package com.example.nisim.messengerapp.adapters;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.nisim.messengerapp.R;
import com.example.nisim.messengerapp.classes.Chat;
import com.example.nisim.messengerapp.classes.GlideApp;
import com.example.nisim.messengerapp.classes.Helper;
import com.example.nisim.messengerapp.classes.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class ChatsAdapter extends ArrayAdapter<Chat> {
    private static final int TAG_UID = R.string.profilePhotoTagChats;
    private Context context;
    private ArrayList<Chat> objects;
    private ArrayList<View> oldViews;

    public ChatsAdapter(@NonNull Context context, @NonNull ArrayList<Chat> objects) {
        super(context, R.layout.chat_list_item, objects);

        this.context = context;
        this.objects = objects;
    }

    public void setOldViews(ArrayList<View> oldViews) {
        this.oldViews = oldViews;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        LayoutInflater layoutInflater = ((Activity) context).getLayoutInflater();
        View view = layoutInflater.inflate(R.layout.chat_list_item, parent, false);

        Chat chat = objects.get(position);
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        TextView tvName = view.findViewById(R.id.tvName);
        TextView tvMessage = view.findViewById(R.id.tvMessage);
        TextView tvTime = view.findViewById(R.id.tvTime);
        TextView tvUnreadMessages = view.findViewById(R.id.tvUnreadMessages);
        ImageView ivProfilePhoto = view.findViewById(R.id.ivProfilePhoto);

        //check who is the sender, the user or the partner
        User other;
        if (firebaseUser.getUid().equals(chat.getFromUser().getUid())) { //user is the sender
            other = chat.getToUser();
        } else {
            other = chat.getFromUser();
        }

        String name = Helper.getContactName(context, other);

        if (chat.getTime() == 0) {
            chat.setTime(System.currentTimeMillis());
        }

        //show the amount of unread messages in a green circle
        if (chat.getUnreadMessages() > 0) {
            tvUnreadMessages.setVisibility(View.VISIBLE);
            tvUnreadMessages.setText(Integer.toString(chat.getUnreadMessages()));
            tvTime.setTextColor(Color.parseColor("#5db92f"));
        } else {
            tvUnreadMessages.setVisibility(View.INVISIBLE);
            tvTime.setTextColor(Color.parseColor("#505050"));
        }

        tvName.setText(name);
        tvMessage.setText(chat.getMessage());
        tvTime.setText(getTimeForChat(chat.getTime()));

        ImageView ivOldProfilePhoto = ivProfilePhoto; //prevent "blinking" when loading photo
        if (this.oldViews.size() > 0) {
            for (View currentView : this.oldViews) {
                ImageView temp = currentView.findViewById(R.id.ivProfilePhoto);
                if (temp.getTag(TAG_UID).equals(other.getUid())) {
                    ivOldProfilePhoto = temp;
                }
            }
        }

        //load photo
        if (chat.getPhoto() != null && !((Activity) context).isFinishing()) {
            GlideApp.with(context)
                    .load(chat.getPhoto())
                    .dontAnimate()
                    .placeholder(ivOldProfilePhoto.getDrawable())
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                    .into(ivProfilePhoto);
        }

        ivProfilePhoto.setTag(TAG_UID, other.getUid());

        return view;
    }

    private String getTimeForChat(long timestamp) {
        Date messageTime = new Date(timestamp);
        Date currentTime = Calendar.getInstance().getTime();

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(messageTime);
        int messageDay = calendar.get(Calendar.DATE);
        calendar.setTime(currentTime);
        int currentDay = calendar.get(Calendar.DATE);

        if (messageDay == currentDay) { //messages from today
            @SuppressLint("SimpleDateFormat") DateFormat df = new SimpleDateFormat("HH:mm");
            return df.format(messageTime);
        } else {
            @SuppressLint("SimpleDateFormat") DateFormat df = new SimpleDateFormat("dd.MM.yyyy");
            return df.format(messageTime);
        }
    }
}