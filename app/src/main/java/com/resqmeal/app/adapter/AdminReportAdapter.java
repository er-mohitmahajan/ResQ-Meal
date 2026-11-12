package com.resqmeal.app.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.resqmeal.app.databinding.ItemAdminReportBinding;
import com.resqmeal.app.model.Report;

import java.util.ArrayList;
import java.util.List;

public class AdminReportAdapter extends RecyclerView.Adapter<AdminReportAdapter.ViewHolder> {
    public interface Listener {
        void onReportClick(Report report);
    }

    private final Listener listener;
    private final List<Report> reports = new ArrayList<>();

    public AdminReportAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setReports(List<Report> reports) {
        this.reports.clear();
        if (reports != null) {
            this.reports.addAll(reports);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemAdminReportBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(reports.get(position));
    }

    @Override
    public int getItemCount() {
        return reports.size();
    }

    final class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemAdminReportBinding binding;

        ViewHolder(ItemAdminReportBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Report report) {
            binding.tvReportTarget.setText(report.getTargetType() + " • " + report.getTargetId());
            binding.tvReportReason.setText(report.getReason() == null ? "" : report.getReason());
            binding.tvReportDescription.setText(report.getDescription() == null
                    ? "" : report.getDescription());
            binding.getRoot().setOnClickListener(v -> listener.onReportClick(report));
        }
    }
}
