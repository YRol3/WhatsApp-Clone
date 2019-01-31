package com.chat.app.mychatapp.Chat;


import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.RecyclerView;
import android.util.LayoutDirection;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.chat.app.mychatapp.R;

import java.util.List;

public class ChatRecyclerViewAdapter extends RecyclerView.Adapter<ChatViewHolder>{
    List<Message> messages;
    public ChatRecyclerViewAdapter(List<Message> messages){
        this.messages = messages;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int layout) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(layout, viewGroup, false);
        return new ChatViewHolder(view);
    }

    @Override
    public int getItemViewType(int i) {
        /*
            Check the message above and current message
            And sends int of the layout to inflate
            4 Different Layouts
            First row sender R.layout.chat_row_send_first
            First row Receiver R.layout.chat_row_recive_first
            Normal row sender R.layout.chat_row_send
            Normal row Receiver R.layout_chat_row_recive
         */
        if(i == 0){
            if(messages.get(i).isReceiver()){
                return  R.layout.chat_row_recive_first;
            }else{
                return R.layout.chat_row_send_first;
            }
        }else {
            if ((i > 0 && messages.get(i - 1).isReceiver()) && messages.get(i).isReceiver()) {
                return R.layout.chat_row_recive;
            } else if ((i > 0 && messages.get(i - 1).isReceiver()) && !messages.get(i).isReceiver()) {
                return R.layout.chat_row_send_first;
            } else if ((i > 0 && !messages.get(i - 1).isReceiver()) && messages.get(i).isReceiver()) {
               return R.layout.chat_row_recive_first;
            } else if ((i > 0 && !messages.get(i - 1).isReceiver()) && !messages.get(i).isReceiver()) {
                return R.layout.chat_row_send;
            }
        }
        return R.layout.chat_row_send;
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder chatViewHolder, int i) {
        if (chatViewHolder.chatRead != null) {
            //TODO Add functionally to the read/not read messages, currently allways not read
            if (messages.get(i).isMessagedRead()) {
                chatViewHolder.chatRead.setImageResource(R.drawable.message_got_read_receipt_from_target);
            } else {
                chatViewHolder.chatRead.setImageResource(R.drawable.message_got_receipt_from_target);
            }
        }
        chatViewHolder.chatBody.setText(messages.get(i).getMessageBody());
        chatViewHolder.chatTag.setText(messages.get(i).getMessageTag());
        chatViewHolder.view.setTag(i);
    }


    @Override
    public int getItemCount() {
        return messages.size();
    }
}

class ChatViewHolder extends RecyclerView.ViewHolder{

    TextView chatBody;
    TextView chatTag;
    ImageView chatRead;
    View view;
    public ChatViewHolder(@NonNull View viewGroup) {
        super(viewGroup);
        view = viewGroup.findViewById(R.id.constrainLayout);
        chatBody = viewGroup.findViewById(R.id.chatBody);
        chatTag = viewGroup.findViewById(R.id.chatTag);
        if(viewGroup.getTag().equals("sender")) {
            //Only sender messages has the check drawables
            //Make sure the layout of the send and send_first
            //have tag "sender" in the main GroupView
            chatRead = viewGroup.findViewById(R.id.readIcon);
        }
    }
}
