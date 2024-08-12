package com.metrix.architecture.designer;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

import com.metrix.architecture.utilities.MetrixStringHelper;

import java.util.HashMap;
import java.util.List;

public class MetadataDiff<T> extends DiffUtil.Callback {
    private final String uniqueKey;
    private final List<T> oldList;
    private final List<T> newList;

    public MetadataDiff(@NonNull String uniqueKey, @NonNull List<T> oldList,
                        @NonNull List<T> newList) {
        this.uniqueKey = uniqueKey;
        this.newList = newList;
        this.oldList = oldList;
    }

    @Override
    public int getOldListSize() {
        return oldList.size();
    }

    @Override
    public int getNewListSize() {
        return newList.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        final Object newValue = newList.get(newItemPosition);
        final Object oldValue = oldList.get(oldItemPosition);

        if (newValue instanceof String && oldValue instanceof String)
            return MetrixStringHelper.valueIsEqual((String) newValue, (String) oldValue);

        if (newValue instanceof HashMap && oldValue instanceof HashMap) {
            final HashMap<String, ?> newRow =  (HashMap<String, ?>) newValue;
            final HashMap<String, ?> oldRow =  (HashMap<String, ?>) oldValue;
            return MetrixStringHelper.valueIsEqual((String)newRow.get(uniqueKey), (String)oldRow.get(uniqueKey));
        }

        return false;
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        final Object newValue = newList.get(newItemPosition);
        final Object oldValue = oldList.get(oldItemPosition);

        if (newValue instanceof String && oldValue instanceof String)
            return MetrixStringHelper.valueIsEqual((String) newValue, (String) oldValue);

        if ((newValue instanceof String && oldValue instanceof HashMap) || newValue instanceof HashMap && oldValue instanceof String)
            return false;

        final HashMap<String, ?> newRow =  (HashMap<String, ?>) newValue;
        final HashMap<String, ?> oldRow =  (HashMap<String, ?>) oldValue;

        for (String key : oldRow.keySet()) {
            final Object newData = newRow.get(key);
            final Object oldData = oldRow.get(key);

            if (newData == null && oldData == null)
                continue;

            final boolean isSame = newData != null && oldData != null && newValue.equals(oldValue);
            if (!isSame)
                return false;
        }
        return true;
    }
}