package com.chat.app.mychatapp.main;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.chat.app.mychatapp.R;

import java.util.List;

public class ConversationsRecyclerViewAdapter extends RecyclerView.Adapter<ConversationsRecyclerViewAdapter.ConversationsViewHolder>{

    private OnConversationClicked listener;
    private List<Conversation> conversations;
    public ConversationsRecyclerViewAdapter(List<Conversation> conversations, OnConversationClicked listener){
        this.conversations = conversations;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ConversationsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.conversation_row, parent, false);
        return new ConversationsViewHolder(view);
    }


    @Override
    public void onBindViewHolder(@NonNull ConversationsViewHolder viewHolder, int position) {
        if(conversations.get(position).getConversationPic() != null)
            viewHolder.conversationImage.setImageBitmap(conversations.get(position).getConversationPic());
        viewHolder.conversationTitle.setText(conversations.get(position).getConversationTopic());
        viewHolder.conversationTag.setText(conversations.get(position).getConversationTag());
        viewHolder.conversationBodySnippet.setText(conversations.get(position).getConversationBodySnippet(30));
    }

    @Override
    public int getItemCount() {
        return conversations.size();
    }



    public class ConversationsViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        ImageView conversationImage;
        TextView conversationTitle;
        TextView conversationBodySnippet;
        TextView conversationTag;
        public ConversationsViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            conversationImage = itemView.findViewById(R.id.conversationPicture);
            conversationTitle = itemView.findViewById(R.id.conversationTitle);
            conversationBodySnippet = itemView.findViewById(R.id.conversationBodySnippet);
            conversationTag = itemView.findViewById(R.id.conversationTag);
        }

        @Override
        public void onClick(View v) {
            listener.moveToChatActivity(getAdapterPosition());
        }
    }

    interface OnConversationClicked{
        void moveToChatActivity(int conversationID);
    }




}
