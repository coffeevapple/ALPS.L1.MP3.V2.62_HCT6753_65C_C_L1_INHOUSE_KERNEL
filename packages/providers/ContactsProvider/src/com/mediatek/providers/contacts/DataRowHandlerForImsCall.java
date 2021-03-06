/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License
 */
package com.mediatek.providers.contacts;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.provider.CallLog.Calls;
import android.provider.ContactsContract.CommonDataKinds.ImsCall;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;

import com.android.providers.contacts.ContactsDatabaseHelper;
import com.android.providers.contacts.ContactsDatabaseHelper.PhoneLookupColumns;
import com.android.providers.contacts.DataRowHandler;
import com.android.providers.contacts.TransactionContext;
import com.android.providers.contacts.ContactsDatabaseHelper.Tables;
import com.android.providers.contacts.DataRowHandler.DataDeleteQuery;
import com.android.providers.contacts.DataRowHandler.DataUpdateQuery;
import com.android.providers.contacts.SearchIndexManager.IndexBuilder;
import com.android.providers.contacts.aggregation.ContactAggregator;
import com.mediatek.providers.contacts.DialerSearchSupport.DialerSearchLookupColumns;
import com.mediatek.providers.contacts.DialerSearchSupport.DialerSearchLookupType;

/**
 * Handler for note data rows.
 */
public class DataRowHandlerForImsCall extends DataRowHandler {
    private static final String TAG = "DataRowHandlerForImsCall";
    private static final boolean DBG = ContactsProviderUtils.DBG_DIALER_SEARCH;
    private Context mContext;
    private ContactsDatabaseHelper mDbHelper;

    private SQLiteStatement mDialerSearchNumDelByCallLogIdDelete;;
    private SQLiteStatement mDialerSearchNewRecordInsert;
    private SQLiteStatement mCallsNewInsertDataIdUpdate;
    private SQLiteStatement mLatestCallLogIdQuery;
    private SQLiteStatement mCallsReplaceDataIdUpdate;
    private SQLiteStatement mDialerSearchNumDelByCallLogDataId;
    private SQLiteStatement mDialerSearchCallLogIdUpdateByContactNumberUpdated;
    private SQLiteStatement mDialerSearchNoNameCallLogNumDataIdUpdate;
    private SQLiteStatement mDialerSearchContactNumDelete;

    public DataRowHandlerForImsCall(
            Context context, ContactsDatabaseHelper dbHelper, ContactAggregator aggregator) {
        super(context, dbHelper, aggregator, ImsCall.CONTENT_ITEM_TYPE);
        mContext = context;
        mDbHelper = dbHelper;
    }

    @Override
    public boolean hasSearchableData() {
        return true;
    }

    @Override
    public boolean containsSearchableColumns(ContentValues values) {
        return values.containsKey(ImsCall.URL);
    }

    @Override
    public void appendSearchableData(IndexBuilder builder) {
        builder.appendContentFromColumn(ImsCall.URL);
    }

    @Override
    public int delete(SQLiteDatabase db, TransactionContext txContext, Cursor c) {

        long dataId = c.getLong(DataDeleteQuery._ID);

        int count = super.delete(db, txContext, c);

        /// delete IMS number in phone_lookup table. @{
        long rawContactId = c.getLong(DataDeleteQuery.RAW_CONTACT_ID);
        updatePhoneLookup(db, rawContactId, dataId, null);
        /// @}

        if (ContactsProviderUtils.isSearchDbSupport()) {
            LogUtils.d(TAG,"[delete] dataId: " + dataId);
            // For callLog, remove raw_contact_id and data_id
            if (mCallsReplaceDataIdUpdate == null) {
                mCallsReplaceDataIdUpdate = db.compileStatement(
                        "UPDATE " + Tables.CALLS +
                                " SET " + Calls.DATA_ID + "=?, " +
                                Calls.RAW_CONTACT_ID + "=? " +
                                " WHERE " + Calls.DATA_ID + " =? ");
            }
            mCallsReplaceDataIdUpdate.bindNull(1);
            mCallsReplaceDataIdUpdate.bindNull(2);
            mCallsReplaceDataIdUpdate.bindLong(3, dataId);
            mCallsReplaceDataIdUpdate.execute();
            LogUtils.d(TAG,"[delete] Remove raw_contact_id and data_id data in CallLog. ");

            // For dialer search, CallLog contacts
            // change the number record to NO NAME CALLLOG in dialer search
            // table
            if (mDialerSearchNoNameCallLogNumDataIdUpdate == null) {
                mDialerSearchNoNameCallLogNumDataIdUpdate = db.compileStatement(
                        "UPDATE " + Tables.DIALER_SEARCH +
                                " SET " + DialerSearchLookupColumns.RAW_CONTACT_ID + " = -"
                                + DialerSearchLookupColumns.CALL_LOG_ID + "," +
                                DialerSearchLookupColumns.DATA_ID + " = -"
                                + DialerSearchLookupColumns.CALL_LOG_ID +
                                " WHERE " + DialerSearchLookupColumns.DATA_ID + " = ? AND " +
                                DialerSearchLookupColumns.CALL_LOG_ID + " > 0 AND " +
                                DialerSearchLookupColumns.NAME_TYPE + " = "
                                + DialerSearchLookupType.PHONE_EXACT);
            }
            mDialerSearchNoNameCallLogNumDataIdUpdate.bindLong(1, dataId);
            mDialerSearchNoNameCallLogNumDataIdUpdate.execute();
            LogUtils.d(TAG,"[update] Change old record in dialer_search table to a NO NAME CALLLOG. ");
            // For dialer search, No call log contacts
            if (mDialerSearchContactNumDelete == null) {
                mDialerSearchContactNumDelete = db.compileStatement(
                        "DELETE FROM " + Tables.DIALER_SEARCH +
                                " WHERE " + DialerSearchLookupColumns.DATA_ID + " =? AND " +
                                DialerSearchLookupColumns.CALL_LOG_ID + " = 0 AND " +
                                DialerSearchLookupColumns.NAME_TYPE + " = "
                                + DialerSearchLookupType.PHONE_EXACT);
            }
            mDialerSearchContactNumDelete.bindLong(1, dataId);
            mDialerSearchContactNumDelete.execute();
            LogUtils.d(TAG,"[delete] delete dialer search table.");
        }
        return count;
    }

    @Override
    public long insert(SQLiteDatabase db, TransactionContext txContext,
            long rawContactId, ContentValues values) {

        long dataId;
        dataId = super.insert(db, txContext, rawContactId, values);

        if (values.containsKey(ImsCall.DATA)) {

            /// insert IMS number into phone_lookup table. @{
            String imsNumber = values.getAsString(ImsCall.DATA);
            updatePhoneLookup(db, rawContactId, dataId, imsNumber);
            /// @}

            if (ContactsProviderUtils.isSearchDbSupport()) {
                // update call Log record, and get the Latest call_log_id of the
                // inserted number
                int mLatestCallLogId = updateCallsInfoForNewInsertNumber(db, imsNumber,
                        rawContactId, dataId);
                LogUtils.d(TAG, "[insert] latest call log id: " + mLatestCallLogId);
                // delete NO Name CALLLOG in dialer search table.
                if (mLatestCallLogId > 0) {
                    if (mDialerSearchNumDelByCallLogIdDelete == null) {
                        mDialerSearchNumDelByCallLogIdDelete = db.compileStatement("DELETE FROM "
                                + Tables.DIALER_SEARCH + " WHERE "
                                + DialerSearchLookupColumns.CALL_LOG_ID + " =? " + " AND "
                                + DialerSearchLookupColumns.NAME_TYPE + " = "
                                + DialerSearchLookupType.PHONE_EXACT);
                    }

                    mDialerSearchNumDelByCallLogIdDelete.bindLong(1, mLatestCallLogId);
                    mDialerSearchNumDelByCallLogIdDelete.execute();
                    LogUtils.d(TAG, "[insert]delete no name call log. ");
                }
                // insert new data into dialer search table with latest call log
                // id.
                if (mDialerSearchNewRecordInsert == null) {
                    mDialerSearchNewRecordInsert = db.compileStatement("INSERT INTO "
                            + Tables.DIALER_SEARCH + "(" + DialerSearchLookupColumns.RAW_CONTACT_ID
                            + "," + DialerSearchLookupColumns.DATA_ID + ","
                            + DialerSearchLookupColumns.NORMALIZED_NAME + ","
                            + DialerSearchLookupColumns.NAME_TYPE + ","
                            + DialerSearchLookupColumns.CALL_LOG_ID + ","
                            + DialerSearchLookupColumns.NORMALIZED_NAME_ALTERNATIVE + ")"
                            + " VALUES (?,?,?,?,?,?)");
                }
                mDialerSearchNewRecordInsert.bindLong(1, rawContactId);
                mDialerSearchNewRecordInsert.bindLong(2, dataId);
                bindString(mDialerSearchNewRecordInsert, 3, imsNumber);
                mDialerSearchNewRecordInsert.bindLong(4, DialerSearchLookupType.PHONE_EXACT);
                mDialerSearchNewRecordInsert.bindLong(5, mLatestCallLogId);
                bindString(mDialerSearchNewRecordInsert, 6, imsNumber);
                mDialerSearchNewRecordInsert.executeInsert();
                LogUtils.d(TAG, "[insert] insert new data into dialer search table. ");

            }
        }
        return dataId;
    }

    @Override
    public boolean update(SQLiteDatabase db, TransactionContext txContext,
            ContentValues values, Cursor c, boolean callerIsSyncAdapter) {

        if (!super.update(db, txContext, values, c, callerIsSyncAdapter)) {
            return false;
        }

        if (values.containsKey(ImsCall.DATA)) {
            long dataId = c.getLong(DataUpdateQuery._ID);
            long rawContactId = c.getLong(DataUpdateQuery.RAW_CONTACT_ID);
            String imsNumber = values.getAsString(ImsCall.DATA);

            // update IMS number in phone_lookup table.
            updatePhoneLookup(db, rawContactId, dataId, imsNumber);

            if (ContactsProviderUtils.isSearchDbSupport()) {
                String mStrDataId = String.valueOf(dataId);
                String mStrRawContactId = String.valueOf(rawContactId);
                LogUtils.d(TAG,"[update]update: imsNumber: " + imsNumber + " || mStrRawContactId: "
                        + mStrRawContactId + " || mStrDataId: " + mStrDataId);
                // update calls table to clear raw_contact_id and data_id, if
                // the changing number or the changed number exists in call log.
                int mDeletedCallLogId = 0;

                // update records in calls table to no name callLog
                if (mCallsReplaceDataIdUpdate == null) {
                    mCallsReplaceDataIdUpdate = db.compileStatement(
                            "UPDATE " + Tables.CALLS +
                                    " SET " + Calls.DATA_ID + "=?, " +
                                    Calls.RAW_CONTACT_ID + "=? " +
                                    " WHERE " + Calls.DATA_ID + " =? ");
                }
                mCallsReplaceDataIdUpdate.bindNull(1);
                mCallsReplaceDataIdUpdate.bindNull(2);
                mCallsReplaceDataIdUpdate.bindLong(3, dataId);
                mCallsReplaceDataIdUpdate.execute();
                LogUtils.d(TAG,"[update] Change the old records in calls table to a NO NAME CALLLOG.");
                // update records in dialer search table to no name call if
                // callLogId>0
                if (mDialerSearchNoNameCallLogNumDataIdUpdate == null) {
                    mDialerSearchNoNameCallLogNumDataIdUpdate = db.compileStatement(
                            "UPDATE " + Tables.DIALER_SEARCH +
                                    " SET " + DialerSearchLookupColumns.RAW_CONTACT_ID + " = -"
                                    + DialerSearchLookupColumns.CALL_LOG_ID + "," +
                                    DialerSearchLookupColumns.DATA_ID + " = -"
                                    + DialerSearchLookupColumns.CALL_LOG_ID +
                                    " WHERE " + DialerSearchLookupColumns.DATA_ID + " = ? AND " +
                                    DialerSearchLookupColumns.CALL_LOG_ID + " > 0 AND " +
                                    DialerSearchLookupColumns.NAME_TYPE + " = "
                                    + DialerSearchLookupType.PHONE_EXACT);
                }
                mDialerSearchNoNameCallLogNumDataIdUpdate.bindLong(1, dataId);
                mDialerSearchNoNameCallLogNumDataIdUpdate.execute();
                LogUtils.d(TAG,"[update]Change old records in dialer_search to NO NAME CALLLOG FOR its callLogId>0.");
                // delete records in dialer search table if callLogId = 0
                if (mDialerSearchContactNumDelete == null) {
                    mDialerSearchContactNumDelete = db.compileStatement(
                            "DELETE FROM " + Tables.DIALER_SEARCH +
                                    " WHERE " + DialerSearchLookupColumns.DATA_ID + " =? AND " +
                                    DialerSearchLookupColumns.CALL_LOG_ID + " = 0 AND " +
                                    DialerSearchLookupColumns.NAME_TYPE + " = "
                                    + DialerSearchLookupType.PHONE_EXACT);
                }
                mDialerSearchContactNumDelete.bindLong(1, dataId);
                mDialerSearchContactNumDelete.execute();
                LogUtils.d(TAG,"[update]Delete old records in dialer_search FOR its callLogId=0.");
                // update new number's callLog info(dataId & rawContactId) if
                // exists
                int mLatestCallLogId = updateCallsInfoForNewInsertNumber(db, imsNumber,
                        rawContactId, dataId);
                LogUtils.d(TAG,"[update] latest call log id: " + mLatestCallLogId);
                // delete NO Name CALLLOG in dialer search table.
                if (mLatestCallLogId > 0) {
                    if (mDialerSearchNumDelByCallLogIdDelete == null) {
                        mDialerSearchNumDelByCallLogIdDelete = db.compileStatement(
                                "DELETE FROM " + Tables.DIALER_SEARCH +
                                        " WHERE " + DialerSearchLookupColumns.CALL_LOG_ID + " =? " +
                                        " AND " + DialerSearchLookupColumns.NAME_TYPE + " = "
                                        + DialerSearchLookupType.PHONE_EXACT);
                    }
                    mDialerSearchNumDelByCallLogIdDelete.bindLong(1, mLatestCallLogId);
                    mDialerSearchNumDelByCallLogIdDelete.execute();
                    LogUtils.d(TAG,"[update]delete no name call log for udpated number. ");
                }
                // insert new number into dialer search table with latest call
                // log id.
                if (mDialerSearchNewRecordInsert == null) {
                    mDialerSearchNewRecordInsert = db.compileStatement(
                            "INSERT INTO " + Tables.DIALER_SEARCH + "(" +
                                    DialerSearchLookupColumns.RAW_CONTACT_ID + "," +
                                    DialerSearchLookupColumns.DATA_ID + "," +
                                    DialerSearchLookupColumns.NORMALIZED_NAME + "," +
                                    DialerSearchLookupColumns.NAME_TYPE + "," +
                                    DialerSearchLookupColumns.CALL_LOG_ID + "," +
                                    DialerSearchLookupColumns.NORMALIZED_NAME_ALTERNATIVE + ")" +
                                    " VALUES (?,?,?,?,?,?)");
                }
                mDialerSearchNewRecordInsert.bindLong(1, rawContactId);
                mDialerSearchNewRecordInsert.bindLong(2, dataId);
                bindString(mDialerSearchNewRecordInsert, 3, imsNumber);
                mDialerSearchNewRecordInsert.bindLong(4, DialerSearchLookupType.PHONE_EXACT);
                mDialerSearchNewRecordInsert.bindLong(5, mLatestCallLogId);
                bindString(mDialerSearchNewRecordInsert, 6, imsNumber);
                mDialerSearchNewRecordInsert.executeInsert();
                LogUtils.d(TAG,"[update] insert new data into dialer search table. ");

            }
        }
        return true;

    }

    int updateCallsInfoForNewInsertNumber(SQLiteDatabase db,
            String number, long rawContactId, long dataId) {
        if (mCallsNewInsertDataIdUpdate == null) {
            mCallsNewInsertDataIdUpdate = db.compileStatement(
                    "UPDATE " + Tables.CALLS +
                            " SET " + Calls.DATA_ID + "=?, " +
                            Calls.RAW_CONTACT_ID + "=? " +
                            " WHERE " + Calls.NUMBER + "=? AND " +
                            Calls.DATA_ID + " IS NULL ");
        }
        if (mLatestCallLogIdQuery == null) {
            mLatestCallLogIdQuery = db.compileStatement(
                    "SELECT " + Calls._ID + " FROM " + Tables.CALLS +
                            " WHERE " + Calls.DATE + " = (" +
                            " SELECT MAX( " + Calls.DATE + " ) " +
                            " FROM " + Tables.CALLS +
                            " WHERE " + Calls.DATA_ID + " =? )");
        }
        mCallsNewInsertDataIdUpdate.bindLong(1, dataId);
        mCallsNewInsertDataIdUpdate.bindLong(2, rawContactId);
        bindString(mCallsNewInsertDataIdUpdate, 3, number);
        mCallsNewInsertDataIdUpdate.execute();
        int mCallLogId = 0;
        try {
            mLatestCallLogIdQuery.bindLong(1, dataId);
            mCallLogId = (int) mLatestCallLogIdQuery.simpleQueryForLong();
        } catch (android.database.sqlite.SQLiteDoneException e) {
            return 0;
        } catch (NullPointerException e) {
            return 0;
        }

        return mCallLogId;
    }

    void bindString(SQLiteStatement stmt, int index, String value) {
        if (value == null) {
            stmt.bindNull(index);
        } else {
            stmt.bindString(index, value);
        }
    }

    /**
     * Add for ALPS02190748, insert/update/delete phone look up data when IMS number change.
     */
    private void updatePhoneLookup(SQLiteDatabase db, long rawContactId, long dataId, String number) {
        mSelectionArgs1[0] = String.valueOf(dataId);
        db.delete(Tables.PHONE_LOOKUP, PhoneLookupColumns.DATA_ID + "=?", mSelectionArgs1);
        if (!TextUtils.isEmpty(number) && PhoneNumberUtils.isGlobalPhoneNumber(number)) {
            ContentValues values = new ContentValues();
            values.put(PhoneLookupColumns.RAW_CONTACT_ID, rawContactId);
            values.put(PhoneLookupColumns.DATA_ID, dataId);
            values.put(PhoneLookupColumns.NORMALIZED_NUMBER, number);
            values.put(PhoneLookupColumns.MIN_MATCH, PhoneNumberUtils.toCallerIDMinMatch(number));
            long id = db.insert(Tables.PHONE_LOOKUP, null, values);
        }
    }
}
