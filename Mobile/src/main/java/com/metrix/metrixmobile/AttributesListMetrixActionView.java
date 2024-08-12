package com.metrix.metrixmobile;

import android.view.Menu;

import com.metrix.architecture.utilities.AndroidResourceHelper;

/**
 * Created by hesplk on 2/17/2016.
 */
public class AttributesListMetrixActionView {
    public static final String MODIFY = AndroidResourceHelper.getMessage("Modify");
    public static final String ADD = AndroidResourceHelper.getMessage("Add");
    public static final String DELETE = AndroidResourceHelper.getMessage("Delete");

    public static void onCreateMetrixActionView(Menu menu,int attrCount) {
        menu.clear();
        if(attrCount > 0) {
            menu.add(0, Menu.NONE, 0, AttributesListMetrixActionView.MODIFY);
            menu.add(0, Menu.NONE, 0, AttributesListMetrixActionView.ADD);
            menu.add(0, Menu.NONE, 0, AttributesListMetrixActionView.DELETE);
        }
        else {
            menu.add(0, Menu.NONE, 0, AttributesListMetrixActionView.ADD);
        }
    }

}
