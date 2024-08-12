package com.metrix.metrixmobile;

import android.app.Activity;
import android.content.Intent;
import android.location.Location;

import com.metrix.architecture.assistants.MetrixLocationAssistant;
import com.metrix.architecture.constants.MetrixTransactionTypes;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.managers.MetrixUpdateManager;
import com.metrix.architecture.metadata.MetrixSqlData;
import com.metrix.architecture.metadata.MetrixTransaction;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.DataField;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixCurrentKeysHelper;
import com.metrix.architecture.utilities.MetrixDateTimeHelper;
import com.metrix.architecture.utilities.MetrixFloatHelper;
import com.metrix.architecture.utilities.User;

import java.util.ArrayList;
import java.util.Locale;

// This class exists to ensure that the architecture does not interpret the corresponding tab child as a codeless screen.
// It will also provide manager-class functionality for this particular tab.
public class ProdHistDetail {
    public static void attemptGPSUpdate(Activity activity) {
        Location currentLocation = MetrixLocationAssistant.getCurrentLocation(activity);
        if (currentLocation != null) {
            try {
                String productID = MetrixCurrentKeysHelper.getKeyValue("product", "product_id");
                String metrixRowID = MetrixDatabaseManager.getFieldStringValue("product", "metrix_row_id", String.format("product_id = '%s'", productID));
                String latitude = MetrixFloatHelper.convertNumericFromForcedLocaleToDB(String.valueOf(currentLocation.getLatitude()), Locale.US);
                String longitude = MetrixFloatHelper.convertNumericFromForcedLocaleToDB(String.valueOf(currentLocation.getLongitude()), Locale.US);

                ArrayList<MetrixSqlData> prodToUpdate = new ArrayList<MetrixSqlData>();
                MetrixSqlData productTrans = new MetrixSqlData("product", MetrixTransactionTypes.UPDATE, "metrix_row_id=" + metrixRowID);
                productTrans.dataFields.add(new DataField("metrix_row_id", metrixRowID));
                productTrans.dataFields.add(new DataField("product_id", productID));
                productTrans.dataFields.add(new DataField("geocode_lat", latitude));
                productTrans.dataFields.add(new DataField("geocode_long", longitude));
                productTrans.dataFields.add(new DataField("modified_by", User.getUser().personId));
                productTrans.dataFields.add(new DataField("modified_dttm", MetrixDateTimeHelper.getCurrentDate(MetrixDateTimeHelper.DATE_TIME_FORMAT_WITH_SECONDS, true)));
                prodToUpdate.add(productTrans);
                MetrixTransaction transactionInfo = new MetrixTransaction();
                boolean result = MetrixUpdateManager.update(prodToUpdate, true, transactionInfo, AndroidResourceHelper.getMessage("ProductGPSUpdate"), activity);

                if (result == true) {
                    Intent intent = MetrixActivityHelper.createActivityIntent(activity, ProductHistory.class);
                    MetrixActivityHelper.startNewActivityAndFinish(activity, intent);
                }
            } catch (Exception e) {
                LogManager.getInstance().error(e);
            }
        }
    }
}
