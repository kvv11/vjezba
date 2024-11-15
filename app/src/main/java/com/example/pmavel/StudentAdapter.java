package com.example.pmavel;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class StudentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<Student> studenti;

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_STUDENT = 1;

    public StudentAdapter(List<Student> studenti) {
        this.studenti = studenti;
    }

    @Override
    public int getItemViewType(int position) {

        return position == 0 ? VIEW_TYPE_HEADER : VIEW_TYPE_STUDENT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_HEADER) {
            View headerView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.activity_header, parent, false);
            return new HeaderViewHolder(headerView);
        } else {
            View studentView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.activity_kartica, parent, false);
            return new StudentViewHolder(studentView);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {

            HeaderViewHolder headerViewHolder = (HeaderViewHolder) holder;
            headerViewHolder.header.setText("Lista Studenata");
        } else if (holder instanceof StudentViewHolder) {

            StudentViewHolder studentViewHolder = (StudentViewHolder) holder;
            Student student = studenti.get(position - 1);
            studentViewHolder.ime.setText(student.getIme());
            studentViewHolder.prezime.setText(student.getPrezime());
            studentViewHolder.predmet.setText(student.getPredmet());
        }
    }

    @Override
    public int getItemCount() {
        return studenti.size() + 1;
    }

    private static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView header;

        public HeaderViewHolder(View view) {
            super(view);
            header = view.findViewById(R.id.header);
        }
    }

    private static class StudentViewHolder extends RecyclerView.ViewHolder {
        TextView ime, prezime, predmet;

        public StudentViewHolder(View itemView) {
            super(itemView);
            ime = itemView.findViewById(R.id.ime);
            prezime = itemView.findViewById(R.id.prezime);
            predmet = itemView.findViewById(R.id.predmet);
        }
    }
}
