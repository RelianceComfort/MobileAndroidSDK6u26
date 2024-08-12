package com.metrix.architecture.database;

import android.content.Context;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.metrix.architecture.assistants.MetrixApplicationAssistant;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixFileHelper;
import com.metrix.architecture.utilities.SettingsHelper;

/**
 * Created by edlius on 10/6/2016.
 */

public class MetrixDatabaseHelper extends SQLiteOpenHelper {
    Context rescontext;
    private int ridSystem = 0;
    private int ridBusiness = 0;
    private static MetrixDatabaseHelper instance;

    MetrixDatabaseHelper(Context context, int databaseVersion) {
        super(context, "metrix.sqlite", null, databaseVersion);
        rescontext = context;
    }

    MetrixDatabaseHelper(Context context, int databaseVersion, int rid_system, int rid_business) {
        super(context, "metrix.sqlite", null, databaseVersion);
        rescontext = context;
        ridSystem = rid_system;
        ridBusiness = rid_business;
    }

    public static synchronized MetrixDatabaseHelper getHelper(Context context)
    {
        int sys_tables = SettingsHelper.getIntegerSetting(MobileApplication.getAppContext(), SettingsHelper.SystemDatabaseId);
        int bus_tables = SettingsHelper.getIntegerSetting(MobileApplication.getAppContext(), SettingsHelper.BusinessDatabaseId);

        if (instance == null)
            instance = new MetrixDatabaseHelper(context, MetrixApplicationAssistant.getMetaIntValue(context, "DatabaseVersion"), sys_tables, bus_tables);

        return instance;
    }

    public static synchronized MetrixDatabaseHelper getHelper(Context context, int databaseVerson, int rid_system, int rid_business)
    {
        boolean recreateHelper = false;
        if(instance == null || instance.ridSystem != rid_system || instance.ridBusiness != rid_business) {
            recreateHelper = true;
        }

        if (instance == null || recreateHelper)
            instance = new MetrixDatabaseHelper(context, databaseVerson, rid_system, rid_business);

        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        String[] statements;

        try {
            Resources resources = rescontext.getResources();

            if(ridSystem>0) {
                statements = resources.getStringArray(ridSystem);
                for (String statement : statements) {
                    try {
                        database.execSQL(statement);
                    } catch (Exception ex) {
                        LogManager.getInstance(rescontext).error(ex);
                    }
                }
            }

            if(ridBusiness > 0) {
                statements = resources.getStringArray(ridBusiness);
                for (String statement : statements) {
                    try {
                        database.execSQL(statement);
                    } catch (Exception ex) {
                        LogManager.getInstance(rescontext).error(ex);
                    }
                }
            }
        } catch (Exception ex) {
            LogManager.getInstance(rescontext).error(ex);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        try {
            LogManager.getInstance(rescontext).info("Upgrading database from version " + oldVersion + " to " + newVersion + ", which will destroy all old data");
            String dbPath = MetrixFileHelper.getMetrixDatabasePath(rescontext); //"/data/data/com.metrix.metrixmobile/databases/metrix.sqlite";
            rescontext.deleteDatabase(dbPath);

            onCreate(database);
        } catch (Exception ex) {
            LogManager.getInstance(rescontext).error(ex);
        }
    }
}