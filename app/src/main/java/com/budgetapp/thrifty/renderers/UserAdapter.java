package com.budgetapp.thrifty.renderers;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.budgetapp.thrifty.R;
import com.budgetapp.thrifty.model.User;
import java.util.ArrayList;
import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    public interface OnUserActionListener {
        void onEdit(User user);
        void onDelete(User user);
    }

    private final Context context;
    private final OnUserActionListener actionListener;

    private List<User> fullList;       // All users
    private List<User> filteredList;   // Filtered based on search
    private List<User> paginatedList = new ArrayList<>();  // Current page

    private final int itemsPerPage = 10;
    private int currentPage = 0;

    public UserAdapter(Context context, List<User> userList, OnUserActionListener listener) {
        this.context = context;
        this.actionListener = listener;
        this.fullList = new ArrayList<>(userList);
        this.filteredList = new ArrayList<>(userList);
        updatePage();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.user_item, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = paginatedList.get(position);
        String display = user.getEmail();
        holder.userName.setText(display);

        holder.editButton.setOnClickListener(v -> actionListener.onEdit(user));
        holder.deleteButton.setOnClickListener(v -> actionListener.onDelete(user));
    }

    @Override
    public int getItemCount() {
        return paginatedList.size();
    }

    public void setPage(int page) {
        this.currentPage = page;
        updatePage();
    }

    public void removeUserById(String uid) {
        fullList.removeIf(user -> user.getUid().equals(uid));
        filteredList.removeIf(user -> user.getUid().equals(uid));
        setPage(currentPage); // Refresh current page
    }

    private void updatePage() {
        int start = currentPage * itemsPerPage;
        int end = Math.min(start + itemsPerPage, filteredList.size());
        if (start >= end) start = 0;
        paginatedList = filteredList.subList(start, end);
        notifyDataSetChanged();
    }

    public int getTotalPages() {
        return (int) Math.ceil((double) filteredList.size() / itemsPerPage);
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void updateData(List<User> newData) {
        this.fullList = new ArrayList<>(newData);
        this.filteredList = new ArrayList<>(newData);
        setPage(0);
    }

    public void filter(String text) {
        if (text.isEmpty()) {
            filteredList = new ArrayList<>(fullList);
        } else {
            List<User> temp = new ArrayList<>();
            for (User user : fullList) {
                String name = user.getDisplayName().toLowerCase();
                String email = user.getEmail().toLowerCase();
                if (name.contains(text.toLowerCase()) || email.contains(text.toLowerCase())) {
                    temp.add(user);
                }
            }
            filteredList = temp;
        }
        setPage(0);
    }

    public int getTotalUserCount() {
        return fullList.size(); // if you want the full count
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView userName;
        ImageButton editButton, deleteButton;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.user_name);
            editButton = itemView.findViewById(R.id.edit_button);
            deleteButton = itemView.findViewById(R.id.delete_button);
        }
    }
}
