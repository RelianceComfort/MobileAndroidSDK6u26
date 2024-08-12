package com.metrix.architecture.utilities;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

/**
 * Created by RaWiLK on 3/8/2017.
 */

public class StockCountData implements Serializable {
    private List<HashMap<String, String>> mListObjects;

    public StockCountData(List<HashMap<String, String>>  listObjects) {
        this.mListObjects = listObjects;
    }

    public List<HashMap<String, String>> getListObjects() {
        return mListObjects;
    }

    public void setListObjects(List<HashMap<String, String>> mListObjects) {
        this.mListObjects = mListObjects;
    }
}
