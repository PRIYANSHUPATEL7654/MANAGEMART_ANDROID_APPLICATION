package com.managemart;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.managemart.databinding.RowCategoryBinding;

import java.util.ArrayList;

public class CategoriesAdapter extends RecyclerView.Adapter<CategoriesAdapter.CategoriesViewHolder> {

    Context context;
    ArrayList<Category> categoryArrayList;

    public interface CategoriesClickListener {
        void onCategorySelected(Category category);
    }

    CategoriesClickListener categoriesClickListener;

    public CategoriesAdapter(Context context, ArrayList<Category> categoryArrayList, CategoriesClickListener categoriesClickListener) {
        this.context = context;
        this.categoryArrayList = categoryArrayList;
        this.categoriesClickListener = categoriesClickListener;
    }

    @NonNull
    @Override
    public CategoriesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new CategoriesViewHolder(LayoutInflater.from(context).inflate(R.layout.row_category, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull CategoriesViewHolder holder, int position) {
        Category category = categoryArrayList.get(position);
        holder.binding.categoryName.setText(category.getCategoryName());
        holder.itemView.setOnClickListener(c -> {
            categoriesClickListener.onCategorySelected(category);
        });
    }

    @Override
    public int getItemCount() {
        return categoryArrayList.size();
    }

    public class CategoriesViewHolder extends RecyclerView.ViewHolder {

        RowCategoryBinding binding;

        public CategoriesViewHolder(@NonNull View itemView) {
            super(itemView);
            binding = RowCategoryBinding.bind(itemView);
        }
    }
}

