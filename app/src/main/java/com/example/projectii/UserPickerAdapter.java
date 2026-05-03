package com.example.projectii;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;

public class UserPickerAdapter extends RecyclerView.Adapter<UserPickerAdapter.ViewHolder> {

    Context context;
    ArrayList<Users> usersList;
    OnUserClickListener listener;

    // Interface to handle user click
    public interface OnUserClickListener {
        void onUserClick(Users user);
    }

    public UserPickerAdapter(Context context, ArrayList<Users> usersList, OnUserClickListener listener) {
        this.context = context;
        this.usersList = usersList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.user_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Users user = usersList.get(position);

        // Set username and status
        holder.username.setText(user.getUsername());
        holder.userstatus.setText(user.getStatus());

        // Load profile image from Base64
        if (user.getProfilepic() == null || user.getProfilepic().isEmpty()
                || user.getProfilepic().equals("default")) {
            holder.userimg.setImageResource(R.drawable.profile);
        } else {
            try {
                byte[] decodedBytes = Base64.decode(user.getProfilepic(), Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                holder.userimg.setImageBitmap(bitmap);
            } catch (Exception e) {
                holder.userimg.setImageResource(R.drawable.profile);
            }
        }

        // When user is clicked notify the listener
        holder.itemView.setOnClickListener(v -> listener.onUserClick(user));
    }

    @Override
    public int getItemCount() {
        return usersList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        CircleImageView userimg;
        TextView username, userstatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            userimg = itemView.findViewById(R.id.userimg);
            username = itemView.findViewById(R.id.username);
            userstatus = itemView.findViewById(R.id.userstatus);
        }
    }
}