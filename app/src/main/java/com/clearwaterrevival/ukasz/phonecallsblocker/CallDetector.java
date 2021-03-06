package com.clearwaterrevival.ukasz.phonecallsblocker;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.WindowManager;
import android.widget.Toast;

import com.clearwaterrevival.ukasz.androidsqlite.Block;
import com.clearwaterrevival.ukasz.androidsqlite.DatabaseHandler;
import com.clearwaterrevival.ukasz.androidsqlite.RegistryBlock;
import com.clearwaterrevival.ukasz.phonecallsblocker.notification_helper.NotificationID;
import com.clearwaterrevival.ukasz.phonecallsblocker.phone_number_helper.PhoneNumberHelper;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * Created by Łukasz Parysek on 2017-03-05.
 * This class describes Listener, and Retriever for Incoming and Outgoing calls.
 */
public class CallDetector
{
    /**
     * Local private class which describes Listener for Incoming calls.
     */
    private class CallStateListener extends PhoneStateListener
    {
        final AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        AlertDialog alertDialog = builder.create();
        final AlertDialog.Builder builderCategory = new AlertDialog.Builder(ctx);
        AlertDialog alertDialogCategory = builderCategory.create();
        int previousState = 0;

        /**
         * This method runs when the Listener is working, and call state has changed.
         * Creating a Toast and Notification when the calls incoming.
         * (incoming call).
         *
         * @param state information about state from TelephonyManager
         * @param incomingNumber contains the number of incoming call
         */
        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void onCallStateChanged(int state, final String incomingNumber)
        {
            //Settings data
            SharedPreferences data;
            data = ctx.getSharedPreferences("data", Context.MODE_PRIVATE);
            final boolean autoBlockEnabled = data.getBoolean("autoBlockEnabled", false);
            final boolean foreignBlockEnabled = data.getBoolean("foreignBlockEnabled", false);
            final boolean privateBlockEnabled = data.getBoolean("privateBlockEnabled", false);
            final boolean unknownBlockEnabled = data.getBoolean("unknownBlockEnabled", false);

            final boolean notificationBlockEnabled = data.getBoolean("notificationBlockEnabled", false);
            final boolean notificationAllowEnabled = data.getBoolean("notificationAllowEnabled", false);

            //Format phone number

            final String incomingNumberV = incomingNumber != null ? incomingNumber : "Numer prywatny";

            //Firebase blockings data
            Query blockings = mDatabase
                    .child("blockings")
                    .orderByChild("nrBlocked")
                    .equalTo(incomingNumberV);
            blockings.getRef().keepSynced(true);

            switch (state)
            {
                case TelephonyManager.CALL_STATE_RINGING:
                {
                    final String phoneNumberFormatted = !incomingNumberV.equals("Numer prywatny")
                            ? phoneNumberHelper.formatPhoneNumber(incomingNumberV, StartActivity.COUNTRY_CODE, PhoneNumberUtil.PhoneNumberFormat.E164)
                            : "Numer prywatny";


                    //get incoming contact name
                    final String incomingContactName = !incomingNumberV.isEmpty()
                            ? phoneNumberHelper.getContactName(ctx, incomingNumberV)
                            : null;

                    //set ringing flag
                    previousState = state;

                    //database and settings load
                    final DatabaseHandler db = new DatabaseHandler(ctx);

//                    getReasonCategory(phoneNumberFormatted, db);

                    //tolerated locally - always allow!
                    if(db.existBlock(myPhoneNumber, phoneNumberFormatted, false))
                    {
                        //if notification allow is enabled - show a notification
                        if (notificationAllowEnabled) notificationManager.notify(
                                NotificationID.getID(),
                                createNotification(phoneNumberFormatted, NOTIFICATION_ALLOWED).build()
                        );
                        registerPhoneBlock(db, phoneNumberFormatted, false);
                    }
                    else if(autoBlockEnabled) //auto blocking
                    {
                        //check for LOCAL BLOCKING
                        if((db.getNumberBlockingsCount(phoneNumberFormatted, true) > 0) //Phone number is blocked locally
                                || (foreignBlockEnabled && isForeignIncomingCall(phoneNumberFormatted)) //OR phone number is foreign and foreignBlock is enabled
                                || (privateBlockEnabled && incomingNumber == null) //OR phone number is private and privateBlock is enabled
                                || (unknownBlockEnabled)) //OR phone number is unknown and uknownBlock is enabled

                        {
                            //if notification block is enabled - show a notification
                            if(notificationBlockEnabled) notificationManager.notify(
                                    NotificationID.getID(),
                                    createNotification(phoneNumberFormatted, NOTIFICATION_BLOCKED).build()
                            );
                            //decline and register
                            declinePhone(ctx);
                            registerPhoneBlock(db, phoneNumberFormatted, true);
                        }
                        else if(incomingContactName != null) //always allow number in contacts if they haven't been blocked locally
                        {
                            //if notification allow is enabled - show a notification
                            if (notificationAllowEnabled) notificationManager.notify(
                                    NotificationID.getID(),
                                    createNotification(phoneNumberFormatted, NOTIFICATION_ALLOWED).build()
                            );
                            registerPhoneBlock(db, phoneNumberFormatted, false);
                        }
                        else //check for GLOBAL BLOCKING
                        {
                            blockings.addListenerForSingleValueEvent(new ValueEventListener()
                            {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot)
                                {
                                    //Counter for count blocking category to decide whether block or not
                                    int trueAmount = 0;
                                    int falseAmount = 0;
                                    for (DataSnapshot blockSnapshot : dataSnapshot.getChildren())
                                    {
                                        Block block = blockSnapshot.getValue(Block.class);
                                        assert block != null;
                                        if (block.getNrRating()) trueAmount++;
                                        else falseAmount++;
                                    }

                                    //GLOBAL BLOCK CONDITION - TODO: CONSIDER CONDITION!
                                    if (trueAmount > falseAmount)
                                    {
                                        //if notification block is enabled - show a notification
                                        if(notificationBlockEnabled) notificationManager.notify(
                                                NotificationID.getID(),
                                                createNotification(phoneNumberFormatted, NOTIFICATION_BLOCKED).build()
                                        );
                                        declinePhone(ctx);
                                        registerPhoneBlock(db, phoneNumberFormatted, true);
                                    }
                                    else //allow
                                    {
                                        //if notification allow is enabled - show a notification
                                        if (notificationAllowEnabled) notificationManager.notify(
                                                NotificationID.getID(),
                                                createNotification(phoneNumberFormatted, NOTIFICATION_ALLOWED).build()
                                        );
                                        registerPhoneBlock(db, phoneNumberFormatted, false);
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError)
                                {
                                    Toast.makeText(ctx, R.string.error, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                    else //manual blocking
                    {
                        //Can draw overlays depends on SDK version
                        boolean canDrawOverlays = true;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                        {
                            if (!Settings.canDrawOverlays(ctx)) canDrawOverlays = false;
                        }

                        if(canDrawOverlays)
                        {
                            //check for LOCAL BLOCKING
                            if (db.existBlock(myPhoneNumber, phoneNumberFormatted, true)) //Phone number is blocked locally
                            {
                                setIncomingCallDialogBlockedNumber(incomingNumberV, db);
                                showAlertDialogForManualBlocking();
                            }
                            else if(incomingContactName != null) //always allow number in contacts if they haven't been blocked locally
                            {
                                //if notification allow is enabled - show a notification
                                if (notificationAllowEnabled) notificationManager.notify(
                                        NotificationID.getID(),
                                        createNotification(phoneNumberFormatted, NOTIFICATION_ALLOWED).build()
                                );
                                registerPhoneBlock(db, phoneNumberFormatted, false);
                            }
                            else if(foreignBlockEnabled && isForeignIncomingCall(phoneNumberFormatted) //phone number is foreign and foreignBlock is enabled
                                        || unknownBlockEnabled)
                            {
                                setIncomingCallDialogGlobalUnknownForeignNumber(incomingNumberV, db, null);
                                showAlertDialogForManualBlocking();
                            }
                            else if((privateBlockEnabled && incomingNumber == null)) //phone number is private and privateBlock is enabled
                            {
                                setIncomingCallDialogPrivateNumber(ctx.getString(R.string.call_detector_private_number), phoneNumberFormatted, db);
                                showAlertDialogForManualBlocking();
                            }
                            else //check for GLOBAL BLOCKING
                            {
                                blockings.addListenerForSingleValueEvent(new ValueEventListener()
                                {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot)
                                    {
                                        //Counter for count blocking category to decide whether block or not
                                        int trueAmount = 0;
                                        int falseAmount = 0;
                                        for (DataSnapshot blockSnapshot : dataSnapshot.getChildren())
                                        {
                                            Block block = blockSnapshot.getValue(Block.class);
                                            assert block != null;
                                            if (block.getNrRating()) trueAmount++;
                                            else falseAmount++;
                                        }

                                        //GLOBAL BLOCK CONDITION - TODO: CONSIDER CONDITION!
                                        if (trueAmount > falseAmount)
                                        {
                                            //Alert dla global blocking
                                            setIncomingCallDialogGlobalUnknownForeignNumber(incomingNumberV, db, trueAmount);
                                            showAlertDialogForManualBlocking();
                                        }
                                        else //allow
                                        {
                                            //if notification allow is enabled - show a notification
                                            if (notificationAllowEnabled) notificationManager.notify(
                                                    NotificationID.getID(),
                                                    createNotification(phoneNumberFormatted, NOTIFICATION_ALLOWED).build()
                                            );
                                            registerPhoneBlock(db, phoneNumberFormatted, false);
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError)
                                    {
                                        Toast.makeText(ctx, R.string.error, Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }
                        else //allow
                        {
                            //if notification allow is enabled - show a notification
                            if (notificationAllowEnabled) notificationManager.notify(
                                    NotificationID.getID(),
                                    createNotification(phoneNumberFormatted, NOTIFICATION_ALLOWED).build()
                            );
                            registerPhoneBlock(db, phoneNumberFormatted, false);
                        }
                    }

                    break;
                }
                case TelephonyManager.CALL_STATE_OFFHOOK:
                {
                    if((previousState==TelephonyManager.CALL_STATE_RINGING))
                    {
                        //Answered Call which is ended
                        alertDialog.dismiss();
                        alertDialogCategory.dismiss();
                    }
                    previousState = state;
                    break;
                }
                case TelephonyManager.CALL_STATE_IDLE:
                {
                    //unset ringing flag
                    if((previousState==TelephonyManager.CALL_STATE_RINGING))
                    {
                        previousState=state;
                        alertDialog.dismiss();
                        alertDialogCategory.dismiss();
                    }
                    break;
                }
            }
        }

        /**
         * Sets a {@link AlertDialog.Builder} for incoming call with number
         * which exist in local list or is blocked by foreign or unknown (not stored in contacts) number.
         *
         * @param incomingNumber contains the number of incoming call
         * @param db database for receive number of blockings and allow update a local list
         */
        private void setIncomingCallDialogBlockedNumber(final String incomingNumber, final DatabaseHandler db)
        {
            //Get validator phone number lib to format
            PhoneNumberHelper formator = new PhoneNumberHelper();
            final String phoneNumberFormatted = formator.formatPhoneNumber(incomingNumber,
                StartActivity.COUNTRY_CODE,
                PhoneNumberUtil.PhoneNumberFormat.E164);

            builder.setTitle(incomingNumber + "\n" + ctx.getString(R.string.call_detector_is_blocked_by_you)+".");

            builder.setItems(R.array.incoming_blocked_number_options,
                    new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            switch(which)
                            {
                                case 0:
                                //Change to positive (white list) - false is positive (not blocked)
                                    updatePhoneBlock(db, phoneNumberFormatted, false);
                                    registerPhoneBlock(db, phoneNumberFormatted, false);

                                    Toast.makeText(ctx, R.string.call_detector_changed_to_positive, Toast.LENGTH_SHORT).show();
                                    break;

                                //Block
                                case 1:
                                    declinePhone(ctx);
                                    registerPhoneBlock(db, phoneNumberFormatted, true);
                                    Toast.makeText(ctx, R.string.call_detector_has_blocked, Toast.LENGTH_SHORT).show();
                                    break;

                                //Allow
                                case 2:
                                    Toast.makeText(ctx, R.string.call_detector_has_allowed, Toast.LENGTH_SHORT).show();
                                    registerPhoneBlock(db, phoneNumberFormatted, false);
                                    break;
                            }
                        }
                    });
        }

        /**
         * Sets a {@link AlertDialog.Builder} for incoming call with unknown or foreign number.
         * Also supports manage Firebase block.
         *
         * @param incomingNumber contains the number of incoming call
         * @param db database for receive number of blockings and allow save to local list
         * @param blockAmount optional param - amount of firebase blockings for firebase manual block
         */
        private void setIncomingCallDialogGlobalUnknownForeignNumber(final String incomingNumber, final DatabaseHandler db, Integer blockAmount)
        {
            //Get validator phone number lib to format
            PhoneNumberHelper formator = new PhoneNumberHelper();
            final String phoneNumberFormatted = formator.formatPhoneNumber(incomingNumber,
                    StartActivity.COUNTRY_CODE,
                    PhoneNumberUtil.PhoneNumberFormat.E164);

            builder.setTitle(incomingNumber);
            if(blockAmount != null) builder.setTitle(incomingNumber
                    + "\n"
                    + ctx.getString(R.string.call_detector_is_blocked_by_community)
                    + "\n"
                    + ctx.getString(R.string.call_detector_is_blocked_by_community_subtitle)
                    +  ": "
                    + blockAmount);
            builder.setItems(R.array.incoming_unknown_or_foreign_options,
                    new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int which)
                        {
                            switch (which)
                            {
                                //Save and block
                                case 0:
                                    setDialogForCategory(phoneNumberFormatted, db);
                                    showAlertDialogForCategory();
                                    break;
                                //Block
                                case 1:
                                    declinePhone(ctx);
                                    registerPhoneBlock(db, phoneNumberFormatted, true);
                                    Toast.makeText(ctx, R.string.call_detector_has_blocked, Toast.LENGTH_SHORT).show();
                                    break;

                                //Save as positive (white list)
                                case 2:
                                    addPhoneBlock(db, phoneNumberFormatted, 0, false);
                                    registerPhoneBlock(db, phoneNumberFormatted, false);
                                    Toast.makeText(ctx, R.string.call_detector_has_saved_positive, Toast.LENGTH_SHORT).show();

                                    //Allow
                                case 3:
                                    registerPhoneBlock(db, phoneNumberFormatted, false);
                                    Toast.makeText(ctx, R.string.call_detector_has_allowed, Toast.LENGTH_SHORT).show();
                                    break;
                            }
                        }
                    });
        }

        /**
         * Sets a {@link AlertDialog.Builder} for incoming call of private number (phone number is unknown).
         *
         * @param title title of the dialog to display
         * @param incomingNumber contains the number of incoming call (equals to Private number)
         * @param db database for registering blocking in registry list
         */
        private void setIncomingCallDialogPrivateNumber(String title, final String incomingNumber, final DatabaseHandler db)
        {
            builder.setTitle(title);

            builder.setItems(R.array.incoming_private_number_options,
                    new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            switch(which)
                            {
                                //Block
                                case 0:
                                    declinePhone(ctx);
                                    registerPhoneBlock(db, incomingNumber, true);
                                    Toast.makeText(ctx, R.string.call_detector_has_blocked, Toast.LENGTH_SHORT).show();
                                    break;

                                //Allow
                                case 1:
                                    Toast.makeText(ctx, R.string.call_detector_has_allowed, Toast.LENGTH_SHORT).show();
                                    registerPhoneBlock(db, incomingNumber, false);
                                    break;
                            }
                        }
                    });
        }

        /**
         * Shows an {@link AlertDialog} for manual blocking.
         * Content is based on class {@link AlertDialog.Builder}.
         */
        private void showAlertDialogForManualBlocking()
        {
            alertDialog = builder.create();
            alertDialog.getWindow().setType(getDialogLayoutFlag());
            alertDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
            alertDialog.show();
        }

        /**
         * Sets a {@link AlertDialog.Builder} for choose category for block.
         *
         * @param incomingNumber contains the number of incoming call (equals to Private number)
         * @param db database for fetching categories and registering blocking in registry list
         */
        private void setDialogForCategory(final String incomingNumber, final DatabaseHandler db)
        {
            final List<String> categories = db.getAllCategories();

            builderCategory.setTitle(R.string.call_detector_choose_category_title);
            CharSequence[] categoriesCharSequence = new CharSequence[categories.size()];

            //Build categories list
            int i=0;
            for(String cat:categories)
            {
                categoriesCharSequence[i] = cat;
                i++;
            }

            builderCategory.setItems(categoriesCharSequence,
                    new DialogInterface.OnClickListener()
                    {

                        @Override
                        public void onClick(DialogInterface dialog, int categoryId)
                        {
                            addPhoneBlock(db, incomingNumber, categoryId, true);
                            declinePhone(ctx);
                            registerPhoneBlock(db, incomingNumber, true);
                        }
                    }
            );
        }

        /**
         * Shows an {@link AlertDialog} for choose category.
         * Content is based on class {@link AlertDialog.Builder}.
         */
        private void showAlertDialogForCategory()
        {
            alertDialogCategory = builderCategory.create();
            alertDialogCategory.getWindow().setType(getDialogLayoutFlag());
            alertDialogCategory.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
            alertDialogCategory.show();
        }

        /**
         * Creates a notification for incoming number.
         *
         * @param incomingNumber contains the number of incoming call
         * @param type type of notification (blocked or allowed call)
         * @return {@link NotificationCompat.Builder} builder with created notification to build and show
         */
        private NotificationCompat.Builder createNotification(final String incomingNumber, int type)
        {
            // Create an explicit intent for an DetailsActivity after click on notification
            Intent detailsBlockIntent = new Intent(ctx, DetailsPhoneBlock.class);
            Bundle b = new Bundle();
            b.putString("phoneNumber", incomingNumber);
            detailsBlockIntent.putExtras(b);
            detailsBlockIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            //instance of pending intent with unique id
            //!!! FLAG_UPDATE_CURRENT - for redirecting to Registry (maybe in future)
            int uniquePendingIntentId = (int) (System.currentTimeMillis() & 0xfffffff);
            PendingIntent pendingIntent = PendingIntent.getActivity(ctx, uniquePendingIntentId, detailsBlockIntent, 0);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx, CHANNEL_CALL_DETECTOR_ID);

            //Notification body depends on notification type (allowed or blocked)
            switch(type)
            {
                case NOTIFICATION_BLOCKED:
                {
                    builder.setSmallIcon(R.drawable.ic_call_end_white_24dp)
                            .setContentText(ctx.getString(R.string.call_detector_has_blocked)+".");
                    break;
                }
                default:
                {
                    builder.setSmallIcon(R.drawable.ic_done_white_24dp)
                            .setContentText(ctx.getString(R.string.call_detector_has_allowed)+".");
                }
            }

            //Get validator phone number lib to format
            PhoneNumberHelper phoneNumberHelper = new PhoneNumberHelper();
            String contactName = phoneNumberHelper.getContactName(ctx, incomingNumber);

            builder.setContentTitle(contactName != null
                    ? contactName
                    : (incomingNumber.equals("Numer prywatny") ? incomingNumber : phoneNumberHelper.formatPhoneNumber(incomingNumber, StartActivity.COUNTRY_CODE, PhoneNumberUtil.PhoneNumberFormat.NATIONAL)))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    // Set the intent that will fire when the user taps the notification
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true);

            return builder;
        }

        /**
         * Checks if the incoming number is from foreign country.
         *
         * @param incomingNumber contains the number of incoming call
         * @return true if is from foreign country, false if not
         */
        private boolean isForeignIncomingCall(final String incomingNumber)
        {
            // get country-code from the phoneNumber
            PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
            try
            {
                Phonenumber.PhoneNumber numberProto = phoneUtil.parse(incomingNumber, Locale.getDefault().getCountry());
                if (phoneUtil.isValidNumber(numberProto))
                {
                    return myCountryDialCode != numberProto.getCountryCode();
                }
            }
            catch (NumberParseException e)
            {
            }

            return false;
        }

        /**
         * Gets stringified reason category of blocking of incoming call.
         *
         * @param incomingNumber contains the number of incoming call
         * @param db database for fetching the category
         * @return {@link String} category
         */
        private String getReasonCategory(String incomingNumber, DatabaseHandler db)
        {
            //Get util phone number lib to format
            PhoneNumberHelper formator = new PhoneNumberHelper();
            final String phoneNumberFormatted = formator.formatPhoneNumber(incomingNumber,
                    StartActivity.COUNTRY_CODE,
                    PhoneNumberUtil.PhoneNumberFormat.E164);

            String category;
            category = db.getCategory(db.getBlocking(myPhoneNumber, phoneNumberFormatted).getReasonCategory());
            return category;
        }
    }

    /**
     * Get the flag for Dialog Layout. Since Android O permission can no longer use the previous
     * system window type TYPE_SYSTEM_ALERT
     *
     * @return dialog layout flag
     */
    private int getDialogLayoutFlag()
    {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
    }

    /**
     * Adds phoneNumber to the blocking list.
     *
     * @param db {@link DatabaseHandler} to check if phoneNumber exists and add if not exists
     * @param phoneNumber phone number to add to blocking list
     * @param category category of added phone number
     * @param rating rating of added phone number, positive or negative
     */
    /* TODO: Consider refactor, adding phone number in one place! */
    @SuppressLint("HardwareIds")
    private void addPhoneBlock(DatabaseHandler db, String phoneNumber, int category, boolean rating)
    {
        if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.READ_SMS)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(ctx, Manifest.permission.READ_PHONE_NUMBERS)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(ctx, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) return;

        PhoneNumberHelper validator = new PhoneNumberHelper();

        if(StartActivity.COUNTRY_CODE.length() > 0 && phoneNumber.length() > 0)
        {
            if(validator.isValidPhoneNumber(phoneNumber))
            {
                boolean status = validator.validateUsingLibphonenumber(StartActivity.COUNTRY_CODE, phoneNumber);
                if(status)
                {
                    //Good - add phone number
                    final Block newBlock = new Block(myPhoneNumber, phoneNumber, category, "", rating);

                    //LOCAL SECTION! add to local blockings
                    if(!db.existBlock(newBlock))
                    {
                        db.addBlocking(newBlock);
                        //ADD to blocking list to make notify data changed possible for adapter
                        Toast.makeText(ctx, R.string.add_phone_block_added, Toast.LENGTH_SHORT).show();
                        PhoneBlockFragment.blockings.add(newBlock);
                        StartActivity.mSectionsPagerAdapter.notifyDataSetChanged();
                        PhoneBlockFragment.loadBlockingsExternal();
                    }
                    else
                    {
                        Toast.makeText(ctx, R.string.add_phone_block_already_exist, Toast.LENGTH_SHORT).show();
                    }

                    //GLOBAL SECTION! add to global blockings if sync is enabled
                    boolean syncEnabled =  ctx.getSharedPreferences("data", Context.MODE_PRIVATE)
                            .getBoolean("syncEnabled", false);

                    if(syncEnabled)
                    {
                        final DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference();
                        Query newBlockingRef = databaseRef
                                .child("blockings")
                                .orderByChild("nrDeclarantBlocked")
                                .equalTo(newBlock.getNrDeclarant() + "_" + newBlock.getNrBlocked())
                                .limitToFirst(1);


                        newBlockingRef.addListenerForSingleValueEvent(new ValueEventListener()
                        {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
                            {
                                if(!dataSnapshot.exists())
                                {
                                    databaseRef.child("blockings").push().setValue(newBlock);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError)
                            {
                                Toast.makeText(ctx, R.string.error, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
                else
                {
                    Toast.makeText(ctx,
                            ctx.getText(R.string.add_phone_block_error_invalid) + ": " + phoneNumber,
                            Toast.LENGTH_LONG).show();
                }
            }
            else
            {
                Toast.makeText(ctx,
                        ctx.getText(R.string.add_phone_block_error_invalid) + ": " + phoneNumber,
                        Toast.LENGTH_LONG).show();
            }
        }
        else
        {
            Toast.makeText(ctx,
                    ctx.getText(R.string.add_phone_block_error_empty),
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Registers a blocking of blocked or passed call.
     *
     * @param db {@link DatabaseHandler} to check if phoneNumber exists and add if not exists
     * @param phoneNumber phone number to add to blocking list
     * @param rating rating of added phone number, positive or negative
     */
    private void registerPhoneBlock(DatabaseHandler db, String phoneNumber, boolean rating)
    {
        db.addBlockingRegistry(new RegistryBlock(phoneNumber, rating, new Date()));
        RegistryFragment.loadRegistryBlockings();
        StartActivity.mSectionsPagerAdapter.notifyDataSetChanged();
    }

    /**
     * Updates the phone block with new rating value.
     * TODO: Almost the same like in PhoneBlockFragment - set blockings rating - consider keep it in one place
     *
     * @param db {@link DatabaseHandler} to make a update phoneNumber exists
     * @param phoneNumber phone number which rating will be updated
     * @param rating new blocking rating
     */
    private void updatePhoneBlock(DatabaseHandler db, final String phoneNumber, final boolean rating)
    {
        //LOCAL UPDATING
        Block updatedBlock = db.getBlocking(myPhoneNumber, phoneNumber);
        updatedBlock.setNrRating(rating);
        db.updateBlocking(updatedBlock);

        //Refresh blockings after update
        PhoneBlockFragment.loadBlockingsExternal();

        //GLOBAL UPDATING - if sync is enabled
        boolean syncEnabled =  ctx.getSharedPreferences("data", Context.MODE_PRIVATE)
                .getBoolean("syncEnabled", false);

        if(syncEnabled)
        {
            final Query blockings = mDatabase
                    .child("blockings")
                    .orderByChild("nrDeclarantBlocked")
                    .equalTo(myPhoneNumber + "_" + phoneNumber);

            blockings.addListenerForSingleValueEvent(new ValueEventListener()
            {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot)
                {
                    if(dataSnapshot.exists())
                    {
                        if(dataSnapshot.getChildren().iterator().hasNext())
                        {
                            HashMap<String, Object> updateData = new HashMap<>();
                            updateData.put("nrRating", rating);
                            dataSnapshot.getChildren().iterator().next().getRef().updateChildren(updateData);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError)
                {
                    Toast.makeText(ctx, R.string.error, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    /**
     * Method which decline/hang out/turn off incoming call.
     *
     * @param context context of application
     */
    private void declinePhone(Context context)
    {
        try
        {
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            Method m1 = tm.getClass().getDeclaredMethod("getITelephony");
            m1.setAccessible(true);
            Object iTelephony = m1.invoke(tm);

            Method m2 = iTelephony.getClass().getDeclaredMethod("silenceRinger");
            Method m3 = iTelephony.getClass().getDeclaredMethod("endCall");

            m2.invoke(iTelephony);
            m3.invoke(iTelephony);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private Context ctx;
    private TelephonyManager tm;
    private String myPhoneNumber;
    private int myCountryDialCode;
    private CallStateListener callStateListener;
    //CallDetector channel for notification manager
    private static final String CHANNEL_CALL_DETECTOR_ID = "CallDetector";
    private NotificationManagerCompat notificationManager;
    //final static fields for notification type
    private final static int NOTIFICATION_BLOCKED = 0;
    private final static int NOTIFICATION_ALLOWED = 1;
    private DatabaseReference mDatabase;

    private PhoneNumberHelper phoneNumberHelper;

    /**
     * Constructor.
     * Creating a Listener and Receiver for calls.
     *
     * @param _ctx  param of setting the app context for the CallDetector object
     */
    @SuppressLint("HardwareIds")
    public CallDetector(Context _ctx)
    {
        ctx = _ctx;
        callStateListener = new CallStateListener();
        notificationManager = NotificationManagerCompat.from(ctx);

        //phone number util validator
        phoneNumberHelper = new PhoneNumberHelper();

        /* TODO: refactor to keep all references in one place */
        //Database reference
        mDatabase = FirebaseDatabase.getInstance().getReference();
        //Notification channel
        createNotificationChannel();
    }

    /**
     * This method starts a Telephony service by the Telephony Manager instance on out context.
     * Settings a Listener on listening calls state.
     * Settings a registerReceiver on retrieving a outgoing calls.
     */
    @SuppressLint("HardwareIds")
    public void start()
    {
        tm = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
        tm.listen(callStateListener, PhoneStateListener.LISTEN_CALL_STATE);

        //Save the user phone number (declarant)
        //getMyPhoneNumber
        if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.READ_PHONE_NUMBERS)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(ctx, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) return;
        myPhoneNumber = !tm.getLine1Number().equals("") ? tm.getLine1Number() : tm.getSubscriberId();
        myPhoneNumber = !myPhoneNumber.equals("") ? myPhoneNumber : tm.getSimSerialNumber();
        myCountryDialCode = PhoneNumberUtil.getInstance().getCountryCodeForRegion(tm.getSimCountryIso().toUpperCase());
    }

    /**
     *  This method stops a listening incoming calls by set listener state on LISTEN_NONE.
     *  Unregisters receiver for outgoing calls.
     */
    void stop()
    {
        tm = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
        tm.listen(callStateListener, PhoneStateListener.LISTEN_NONE);
    }

    /**
     * Creates a notification channel for {@link CallDetector} notifications.
     */
    private void createNotificationChannel()
    {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            CharSequence name = ctx.getString(R.string.call_detector_channel_name);
            String description = ctx.getString(R.string.call_detector_channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_CALL_DETECTOR_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = ctx.getSystemService(NotificationManager.class);
            assert notificationManager != null;
            notificationManager.createNotificationChannel(channel);
        }
    }
}
