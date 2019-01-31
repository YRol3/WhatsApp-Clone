package com.chat.app.mychatapp.main;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.chat.app.mychatapp.R;

import java.util.List;

public class ContactsRecyclerViewAdapter  extends RecyclerView.Adapter<ContactsRecyclerViewAdapter.ContactsViewHolder> {

    List<Contact> contacts;
    OnContactClicked listener;
    public ContactsRecyclerViewAdapter(List<Contact> contacts, OnContactClicked listener){
        this.contacts = contacts;
        this.listener = listener;

    }

    @NonNull
    @Override
    public ContactsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.contact_row, parent, false);
        return new ContactsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactsViewHolder holder, int position) {
        holder.contactName.setText(contacts.get(position).name);
        holder.contactNumber.setText(contacts.get(position).phoneNumber);
        Bitmap picture = contacts.get(position).getPicture();
        if(picture != null)
            holder.contactPic.setImageBitmap(picture);
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    public class ContactsViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        ImageView contactPic;
        TextView contactName;
        TextView contactNumber;
        public ContactsViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            contactPic = itemView.findViewById(R.id.contactPic);
            contactName = itemView.findViewById(R.id.contactName);
            contactNumber = itemView.findViewById(R.id.contactNumber);
        }

        @Override
        public void onClick(View v) {
            listener.contactClick(getAdapterPosition());
        }
    }

    interface OnContactClicked{
        void contactClick(int position);

    }
}
