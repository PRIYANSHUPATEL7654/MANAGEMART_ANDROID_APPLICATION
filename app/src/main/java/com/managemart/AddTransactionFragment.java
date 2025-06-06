package com.managemart;

import static android.widget.Toast.LENGTH_SHORT;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.managemart.Account;
import com.managemart.AccountsAdapter;
import com.managemart.CategoriesAdapter;
import com.managemart.Category;
import com.managemart.DataAdapter;
import com.managemart.databinding.FragmentAddTransactionBinding;
import com.managemart.databinding.ListDialogBinding;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class AddTransactionFragment extends BottomSheetDialogFragment {

    private FragmentAddTransactionBinding binding;
    private FirebaseAuth mAuth;
    private DatabaseReference reference;
    private FirebaseDatabase db;
    private String date, amount, category, account, type = "Income", month, year;
    private OnTransactionAddedListener onTransactionAddedListener;

    // Interface for transaction added callback
    public interface OnTransactionAddedListener {
        void onTransactionAdded();
    }

    public AddTransactionFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseDatabase.getInstance();
    }

    // Method to set the listener
    public void setOnTransactionAddedListener(OnTransactionAddedListener listener) {
        this.onTransactionAddedListener = listener;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAddTransactionBinding.inflate(inflater);

        binding.incomeBtn.setOnClickListener(view -> {
            type = "Income";
            binding.incomeBtn.setBackground(getContext().getDrawable(R.drawable.income_selector));
            binding.expenseBtn.setBackground(getContext().getDrawable(R.drawable.default_selector));
            binding.expenseBtn.setTextColor(getContext().getColor(R.color.textColor));
            binding.incomeBtn.setTextColor(getContext().getColor(R.color.greenColor));
        });

        binding.expenseBtn.setOnClickListener(view -> {
            type = "Expense";
            binding.incomeBtn.setBackground(getContext().getDrawable(R.drawable.default_selector));
            binding.expenseBtn.setBackground(getContext().getDrawable(R.drawable.expense_selector));
            binding.incomeBtn.setTextColor(getContext().getColor(R.color.textColor));
            binding.expenseBtn.setTextColor(getContext().getColor(R.color.redColor));
        });

        binding.date.setOnClickListener(v -> {
            MaterialDatePicker<Long> materialDatePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Select Order Date")
                    .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                    .setTheme(R.style.MyDatePickerTheme)
                    .build();

            materialDatePicker.addOnPositiveButtonClickListener(selection -> {
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(selection);
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM, yyyy", Locale.getDefault());
                String dateToShow = dateFormat.format(calendar.getTime());
                binding.date.setText(dateToShow);
                month = new SimpleDateFormat("MMMM", Locale.getDefault()).format(calendar.getTime());
                year = new SimpleDateFormat("yyyy", Locale.getDefault()).format(calendar.getTime());
                Log.d("DatePicker", "Selected: " + dateToShow + " | Month: " + month + " | Year: " + year);
            });

            materialDatePicker.show(((AppCompatActivity) getContext()).getSupportFragmentManager(), "MATERIAL_DATE_PICKER");
        });

        binding.category.setOnClickListener(c -> {
            ListDialogBinding dialogBinding = ListDialogBinding.inflate(inflater);
            AlertDialog categoryDialog = new AlertDialog.Builder(getContext()).create();
            categoryDialog.setView(dialogBinding.getRoot());

            ArrayList<Category> categories = new ArrayList<>();
            categories.add(new Category("Raw Materials"));
            categories.add(new Category("Finished Goods"));
            categories.add(new Category("Packaging Materials"));
            categories.add(new Category("Maintenance"));
            categories.add(new Category("Utilities"));
            categories.add(new Category("Salaries & Wages"));
            categories.add(new Category("Returns & Refunds"));
            categories.add(new Category("Transportation"));
            categories.add(new Category("Office Supplies"));
            categories.add(new Category("Other"));

            CategoriesAdapter adapter = new CategoriesAdapter(getContext(), categories, category -> {
                binding.category.setText(category.getCategoryName());
                categoryDialog.dismiss();
            });

            dialogBinding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            dialogBinding.recyclerView.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));
            dialogBinding.recyclerView.setAdapter(adapter);

            categoryDialog.show();
        });

        binding.account.setOnClickListener(c -> {
            ListDialogBinding dialogBinding = ListDialogBinding.inflate(inflater);
            AlertDialog accountsDialog = new AlertDialog.Builder(getContext()).create();
            accountsDialog.setView(dialogBinding.getRoot());

            ArrayList<Account> accounts = new ArrayList<>();
            accounts.add(new Account("Cash"));
            accounts.add(new Account("Cheque"));
            accounts.add(new Account("NetBanking"));
            accounts.add(new Account("RTGS"));
            accounts.add(new Account("Other"));

            AccountsAdapter adapter = new AccountsAdapter(getContext(), accounts, account -> {
                binding.account.setText(account.getAccountName());
                accountsDialog.dismiss();
            });
            dialogBinding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            dialogBinding.recyclerView.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));
            dialogBinding.recyclerView.setAdapter(adapter);

            accountsDialog.show();
        });

        binding.saveTransactionBtn.setOnClickListener(v -> {
            date = binding.date.getText().toString();
            amount = binding.amount.getText().toString();
            category = binding.category.getText().toString();
            account = binding.account.getText().toString();

            if (!date.isEmpty() && !amount.isEmpty() && !category.isEmpty() && !account.isEmpty()) {
                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null) {
                    String userId = user.getUid();
                    reference = db.getReference().child("Transactions").child(userId);

                    if (type == null || type.isEmpty()) {
                        Toast.makeText(getContext(), "Please select a transaction type (Income/Expense)", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    DataAdapter transaction = new DataAdapter(date, amount, category, account, type, month, year);
                    reference.push().setValue(transaction).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            clearFields();
                            Toast.makeText(getContext(), "Transaction Added Successfully", Toast.LENGTH_SHORT).show();
                            if (onTransactionAddedListener != null) {
                                onTransactionAddedListener.onTransactionAdded();
                            }
                            dismiss(); // Close the bottom sheet
                        } else {
                            Toast.makeText(getContext(), "Failed to add transaction: " + task.getException().getMessage(), LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
            }
        });

        return binding.getRoot();
    }

    private void clearFields() {
        binding.date.setText("");
        binding.amount.setText("");
        binding.category.setText("");
        binding.account.setText("");
    }
}