package com.example.nisim.messengerapp.adapters;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
import com.example.nisim.messengerapp.classes.Contact;
import com.example.nisim.messengerapp.classes.GlideApp;
import com.example.nisim.messengerapp.classes.Helper;

import java.util.ArrayList;

public class ContactsAdapter extends ArrayAdapter<Contact> {
    private static final int TAG_UID = R.string.profilePhotoTagContacts;
    private Context context;
    private ArrayList<Contact> objects;
    private ArrayList<View> oldViews;

    public ContactsAdapter(@NonNull Context context, @NonNull ArrayList<Contact> objects) {
        super(context, R.layout.contact_list_item, objects);

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
        View view = layoutInflater.inflate(R.layout.contact_list_item, parent, false);
        Contact contact = this.objects.get(position);

        TextView tvName = view.findViewById(R.id.tvName);
        TextView tvPhone = view.findViewById(R.id.tvPhone);
        ImageView ivProfilePhoto = view.findViewById(R.id.ivProfilePhoto);

        ImageView ivOldProfilePhoto = ivProfilePhoto; //prevent "blinking" when loading photo
        if (this.oldViews.size() > 0) {
            for (View currentView : this.oldViews) {
                ImageView temp = currentView.findViewById(R.id.ivProfilePhoto);
                if (temp.getTag(TAG_UID).equals(contact.getUserUid())) {
                    ivOldProfilePhoto = temp;
                }
            }
        }

        //load photo
        if (contact.getPhoto() != null && !((Activity) context).isFinishing()) {
            GlideApp.with(context)
                    .load(contact.getPhoto())
                    .dontAnimate()
                    .placeholder(ivOldProfilePhoto.getDrawable())
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true).into(ivProfilePhoto);
        }

        ivProfilePhoto.setTag(TAG_UID, contact.getUserUid());

        tvName.setText(contact.getName());
        tvPhone.setText(Helper.removeAreaCode(contact.getPhoneNumber()));

        return view;
    }
}