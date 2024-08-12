package com.metrix.metrixmobile;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.designer.MetrixTabScreenManager;
import com.metrix.architecture.ui.widget.MobileUIHelper;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixAttachmentHelper;
import com.metrix.architecture.utilities.MetrixCurrentKeysHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DebReqNotes {

    private static CommentAdapter _commentAdapter = null;

    public static void populateNotesList(RecyclerView recyclerView, MetrixTabScreenManager.TabChildInfo tci, String maxRows, String searchCriteria) {

        try {
            List<HashMap<String, Object>> table = getNotesListData(maxRows, searchCriteria);

            tci.mCount = table.size();

            if (recyclerView != null) {
                _commentAdapter = new CommentAdapter(table);
                recyclerView.setAdapter(_commentAdapter);
            } else {
                throw new Exception("RecyclerView is null");
            }

        } catch (Exception e) {
            LogManager.getInstance().error(e);
        }
    }

    public static void updateNotesList(String searchCriteria) {

        if (_commentAdapter != null)
            _commentAdapter.getFilter().filter(searchCriteria);

    }

    private static List<HashMap<String, Object>> getNotesListData(String maxRows, String searchCriteria) {

        MetrixCursor cursor = null;
        List<HashMap<String, Object>> table = new ArrayList<HashMap<String, Object>>();
        try {
            StringBuilder query = new StringBuilder();
            query.append("select request_text.metrix_row_id, text_line_code.description, request_text.text, person.first_name, person.last_name, request_text.created_dttm, person.image_id");
            query.append(" from request_text");
            query.append(" left outer join person on request_text.created_by = person.person_id ");
            query.append(" left outer join text_line_code on request_text.text_line_code = text_line_code.text_line_code where request_text.request_id = '"
                    + MetrixCurrentKeysHelper.getKeyValue("request", "request_id") + "'");

            if (!MetrixStringHelper.isNullOrEmpty(searchCriteria)) {
                query.append(String.format(" and (request_text.metrix_row_id LIKE '%%%1$s%%' or text_line_code.description LIKE '%%%1$s%%' or request_text.text LIKE '%%%1$s%%' or person.first_name LIKE '%%%1$s%%' or person.last_name LIKE '%%%1$s%%' or request_text.created_dttm LIKE '%%%1$s%%' or person.image_id LIKE '%%%1$s%%' ", searchCriteria));
            }

            query.append(" order by request_text.modified_dttm desc");

            if (!MetrixStringHelper.isNullOrEmpty(maxRows)) {
                query.append(" limit " + maxRows);
            }

            cursor = MetrixDatabaseManager.rawQueryMC(query.toString(), null);

            if (cursor == null || !cursor.moveToFirst()) {
                return table;
            }

            String[] from = new String[]{"request_text.metrix_row_id", "text_line_code.description", "request_text.text", "person.full_name", "request_text.created_dttm", "person.image_id"};

            while (cursor.isAfterLast() == false) {
                HashMap<String, Object> row = new HashMap<String, Object>();

                row.put(from[0], cursor.getString(0));
                row.put(from[1], cursor.getString(1));
                row.put(from[2], cursor.getString(2));

                String firstName = cursor.getString(3);
                String lastName = cursor.getString(4);

                String fullName = null;
                if ((firstName == null) && (lastName == null)) {
                    fullName = "";
                }
                else{
                    fullName = String.format("%1$s %2$s", firstName, lastName);
                }

                row.put(from[3], fullName);
                row.put(from[4], cursor.getString(5));
                row.put(from[5], cursor.getString(6));

                table.add(row);
                cursor.moveToNext();
            }
        } catch (Exception e) {
            LogManager.getInstance().error(e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return table;
    }



    //-- RecycleView and Adapter ---------------------------------

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageViewPersonPic;
        private final TextView textViewMetrixRowId;
        private final TextView textViewLineCodeDescription;
        private final TextView textViewRequestText;
        private final TextView textViewRequestTextCreatedBy;
        private final TextView textViewRequestTextCreatedOn;

        CommentViewHolder(View itemView) {
            super(itemView);
            imageViewPersonPic = itemView.findViewById(R.id.imageView_task_text_person);
            textViewMetrixRowId = itemView.findViewById(R.id.textView_metrix_row_id);
            textViewLineCodeDescription = itemView.findViewById(R.id.textView_description);
            textViewRequestText = itemView.findViewById(R.id.textView_text);
            textViewRequestTextCreatedBy = itemView.findViewById(R.id.textView_created_by);
            textViewRequestTextCreatedOn = itemView.findViewById(R.id.textView_created_on);
        }
    }

    static class CommentAdapter extends RecyclerView.Adapter<CommentViewHolder> implements Filterable {
        private List<HashMap<String, Object>> mList;
        private List<HashMap<String, Object>> mListBk;

        CommentAdapter(List<HashMap<String, Object>> list) {
            mList = list;
            mListBk = list;
        }

        @NonNull
        @Override
        public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            final View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.debrief_task_text_list_item, parent, false);
            return new CommentViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
            final HashMap<String, Object> item = mList.get(position);

            if (item == null)
                return;

            String metrixRowId = (String) item.get("request_text.metrix_row_id");
            String lineCodeDescription = (String) item.get("text_line_code.description");
            String requestText = (String) item.get("request_text.text");
            String requestTextCreated = (String) item.get("person.full_name");
            String requestTextCreatedOn = (String) item.get("request_text.created_dttm");
            String imageId = (String) item.get("person.image_id");

            try {
                if (!MetrixStringHelper.isNullOrEmpty(imageId))
                    MetrixAttachmentHelper.applyImageWithNoScale(imageId, holder.imageViewPersonPic);
                else
                    Glide.with(holder.itemView.getContext())
                            .load(R.drawable.comments_profile)
                            .into(holder.imageViewPersonPic);
            } catch (Exception e) {
                LogManager.getInstance().error(e);
            }

            holder.textViewMetrixRowId.setText(metrixRowId);
            holder.textViewLineCodeDescription.setText(lineCodeDescription);
            holder.textViewRequestText.setText(requestText);
            holder.textViewRequestTextCreatedBy.setText(requestTextCreated);

            String customPeriod = MobileUIHelper.jodaPeriod(requestTextCreatedOn);
            holder.textViewRequestTextCreatedOn.setText(customPeriod);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getItemCount() {
            return mList.size();
        }

        @Override
        public Filter getFilter() {
            Filter filter = new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {

                    FilterResults filterResults = new FilterResults();
                    if (constraint == null || constraint.length() == 0){
                        filterResults.values = mListBk;
                        filterResults.count = mListBk.size();
                    }
                    else{
                        String searchCriteria = constraint.toString().toLowerCase();

                        if (!MetrixStringHelper.isNullOrEmpty(searchCriteria)) {
                            List<HashMap<String, Object>> searchedTable = new ArrayList<HashMap<String, Object>>();
                            for (HashMap<String, Object> row : mListBk) {
                                for (Map.Entry<String, Object> entry : row.entrySet()) {
                                    Object value = entry.getValue();
                                    if (value != null && value instanceof String) {
                                        String strValue = (String) value;
                                        if (strValue.toLowerCase().contains(searchCriteria.toLowerCase())) {
                                            searchedTable.add(row);
                                            break;
                                        }
                                    }
                                }
                            }
                            filterResults.values = searchedTable;
                            filterResults.count = searchedTable.size();
                        }
                    }
                    return filterResults;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {

                    mList = (List<HashMap<String, Object>>)results.values;
                    notifyDataSetChanged();
                }
            };
            return filter;
        }
    }
}
