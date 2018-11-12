package com.example.ukasz.permissions;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.ukasz.androidsqlite.Block;
import com.example.ukasz.androidsqlite.DatabaseHandler;
import com.example.ukasz.phonecallsblocker.PhoneBlockFragment;
import com.example.ukasz.phonecallsblocker.R;
import com.example.ukasz.phonecallsblocker.StartActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

/**
 * Class for grant permissions for the first run of app or when
 * user will take away the permissions from Apps settings
 */
public class PermissionsStartupActivity extends AppCompatActivity {

    //Clickable buttons
    private Button grantPermissionsButton;
    private CheckBox phoneStateCheckBoxPerm;
    private CheckBox contactsStateCheckBoxPerm;
    private CheckBox allowWindowsCheckBoxPerm;

    //Hidden warnings
    private TextView phoneStateWarningText;
    private ImageView phoneStateWarningImage;
    private TextView contactsStateWarningText;
    private ImageView contactsStateWarningImage;
    private TextView allowWindowsWarningText;
    private ImageView allowWindowsWarningImage;

    //const Permissions Codes
    final private static int ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE = 5555;
    final private static int READ_PHONE_STATE_PERMISSION_REQUEST_CODE = 5556;
    final private static int CALL_PHONE_PERMISSION_REQUEST_CODE = 5557;
    final private static int READ_CONTACTS_PERMISSION_REQUEST_CODE = 5558;

    /**
     * Method which runs on activity start and contains listener for button and checkboxes,
     * which are responsible for granting a permissions.
     * Without permissions user can't create a Phone Calls Blocking Activity
     * {@link StartActivity}.
     *
     * @param savedInstanceState saved app instances state
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        //View.VISIBLE || View.GONE
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permissions_startup);

        //Permissions -----------------------------------
        phoneStateCheckBoxPerm = findViewById(R.id.permissions_startup_activity_phone_checkbox);
        contactsStateCheckBoxPerm = findViewById(R.id.permissions_startup_activity_contacts_checkbox);
        allowWindowsCheckBoxPerm = findViewById(R.id.permissions_startup_activity_windows_checkbox);

        //Sets the visibility of elements
        setVisibility();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
        {
            //If we have granted permissions we can open a Main Activity
            if(hasGrantedPermissions(getApplicationContext()))
            {
                openStartActivity();
            }
//            else //If we haven't, we have to request for this permissions
//            {
//                phoneStateWarningText = findViewById(R.id.permissions_startup_activity_phone_checkbox_warning);
//                phoneStateWarningImage = findViewById(R.id.imageView4);
//
//                contactsStateWarningText = findViewById(R.id.permissions_startup_activity_contacts_checkbox_warning);
//                contactsStateWarningImage = findViewById(R.id.imageView5);
//
//                grantPermissionsButton.setOnClickListener(new View.OnClickListener()
//                {
//                    @Override
//                    public void onClick(View v)
//                    {
//                        if (phoneStateCheckBoxPerm.isChecked())
//                        {
//                            setOptionsUnVisible(phoneStateWarningText, phoneStateWarningImage);
//                        }
//                        else
//                        {
//                            phoneStateWarningText.setTextKeepState(getResources().getString(R.string.permissions_startup_activity_phone_checkbox_warning_nc));
//                            setOptionsVisible(phoneStateWarningText, phoneStateWarningImage);
//                        }
//                        if (contactsStateCheckBoxPerm.isChecked())
//                        {
//                            setOptionsUnVisible(contactsStateWarningText, contactsStateWarningImage);
//                        }
//                        else
//                        {
//                            contactsStateWarningText.setTextKeepState(getResources().getString(R.string.permissions_startup_activity_contacts_checkbox_warning_nc));
//                            setOptionsVisible(contactsStateWarningText, contactsStateWarningImage);
//                        }
//                    }
//                });
//            }
        }
        else //Android SDK >= M
        {
            //If we have granted permissions we can open a Main Activity
            if (hasGrantedPermissions(getApplicationContext()) && hasGrantedManageOverlayPermission(getApplicationContext()))
            {
                openStartActivity();
            }
//            else //If we haven't, we have to request for this permissions
//            {
//                phoneStateWarningText = findViewById(R.id.permissions_startup_activity_phone_checkbox_warning);
//                phoneStateWarningImage = findViewById(R.id.imageView4);
//                contactsStateWarningText = findViewById(R.id.permissions_startup_activity_contacts_checkbox_warning);
//                contactsStateWarningImage = findViewById(R.id.imageView5);
//                allowWindowsWarningText = findViewById(R.id.permissions_startup_activity_windows_checkbox_warning);
//                allowWindowsWarningImage = findViewById(R.id.imageView6);
//
//                grantPermissionsButton = findViewById(R.id.permissions_startup_activity_accept_button);
//                grantPermissionsButton.setOnClickListener(new View.OnClickListener()
//                {
//                    @Override
//                    public void onClick(View v)
//                    {
//                        if (phoneStateCheckBoxPerm.isChecked() && contactsStateCheckBoxPerm.isChecked() && allowWindowsCheckBoxPerm.isChecked())
//                        {
//                            if (!hasGrantedManageOverlayPermission(getApplicationContext())) requestManageOverlayPermission();
//                            if (!hasGrantedPermissions(getApplicationContext()))
//                            {
//                                requestReadPhoneStatePermission();
//                                requestReadContactsPermission();
//                            }
//                        }
//                        else
//                        {
//                            phoneStateWarningText.setTextKeepState(getResources().getString(R.string.permissions_startup_activity_phone_checkbox_warning_nc));
//                            contactsStateWarningText.setTextKeepState(getResources().getString(R.string.permissions_startup_activity_phone_checkbox_warning_nc));
//                            allowWindowsWarningText.setTextKeepState(getResources().getString(R.string.permissions_startup_activity_windows_checkbox_warning_nc));
//                            setOptionsVisible(allowWindowsWarningText, allowWindowsWarningImage);
//                            setOptionsVisible(phoneStateWarningText, phoneStateWarningImage);
//                            setOptionsVisible(contactsStateWarningText, contactsStateWarningImage);
//                        }
//                    }
//                });
//            }
        }
    }

    /**
     * Runs on the restart of {@link PermissionsStartupActivity}.
     */
    @Override
    protected void onRestart()
    {
        super.onRestart();
        setVisibility();
    }

    /**
     * Runs on the resume of {@link PermissionsStartupActivity}.
     */
    @Override
    protected void onResume()
    {
        super.onResume();
        setVisibility();
    }

    /**
     * Sets options views passed by params as visible.
     *
     * @param t TextView to set as visible
     * @param i ImageView to set as visible
     */
    public void setOptionsVisible(TextView t, ImageView i)
    {
        t.setVisibility(View.VISIBLE);
        i.setVisibility(View.VISIBLE);
    }

    /**
     * Sets options views passed by params as unVisible.
     *
     * @param t TextView to set as unVisible
     * @param i ImageView to set as unVisible
     */
    public void setOptionsUnVisible(TextView t, ImageView i)
    {
        t.setVisibility(View.GONE);
        i.setVisibility(View.GONE);
    }

    /**
     * Sets the visibility of elements based on granted and not granted permissions.
     */
    public void setVisibility()
    {
        //Disable permissions if we have already granted them
        if(hasGrantedPhoneStatePermission(getApplicationContext()))
        {
            phoneStateCheckBoxPerm.setVisibility(View.GONE);
            findViewById(R.id.view).setVisibility(View.GONE);
            findViewById(R.id.permissions_startup_activity_phone_checkbox_header).setVisibility(View.GONE);
            findViewById(R.id.permissions_startup_activity_phone_checkbox_description).setVisibility(View.GONE);
        }

        //Disable permissions if we have already granted them
        if(hasGrantedReadContactsPermission(getApplicationContext()))
        {
            contactsStateCheckBoxPerm.setVisibility(View.GONE);
            findViewById(R.id.view2).setVisibility(View.GONE);
            findViewById(R.id.permissions_startup_activity_contacts_checkbox_header).setVisibility(View.GONE);
            findViewById(R.id.permissions_startup_activity_contacts_checkbox_description).setVisibility(View.GONE);
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
        {
            //disable manage overlay permission options
            allowWindowsCheckBoxPerm.setVisibility(View.GONE);
            findViewById(R.id.view3).setVisibility(View.GONE);
            findViewById(R.id.permissions_startup_activity_windows_checkbox_header).setVisibility(View.GONE);
            findViewById(R.id.permissions_startup_activity_windows_checkbox_description).setVisibility(View.GONE);
        }
        else
        {
            //Disable permissions if we have already granted them
            if(hasGrantedManageOverlayPermission(getApplicationContext()))
            {
                allowWindowsCheckBoxPerm.setVisibility(View.GONE);
                findViewById(R.id.view3).setVisibility(View.GONE);
                findViewById(R.id.permissions_startup_activity_windows_checkbox_header).setVisibility(View.GONE);
                findViewById(R.id.permissions_startup_activity_windows_checkbox_description).setVisibility(View.GONE);
            }
        }
    }

    /**
     * Opens a window to ask for a permission to read a phone state.
     */
    public void requestReadPhoneStatePermission()
    {
        //Request the permission
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.READ_PHONE_STATE},
                READ_PHONE_STATE_PERMISSION_REQUEST_CODE);
    }

    /**
     * Opens a window to ask for a permission to read a phone state.
     */
    public void requestReadContactsPermission()
    {
        //Request the permission
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.READ_CONTACTS},
                READ_CONTACTS_PERMISSION_REQUEST_CODE);
    }

    /**
     * Opens a window to ask for a permission to call phone.
     */
    public void requestCallPhonePermission(){
        //Request the permission
        Log.e("CallPhone", "true");
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CALL_PHONE},
                CALL_PHONE_PERMISSION_REQUEST_CODE);
    }

    /**
     * Checks if the standard permissions are granted.
     *
     * @param context context of the app
     * @return true if they are granted, false if they are not
     */
    public boolean hasGrantedPermissions(Context context)
    {
        return hasGrantedPhoneStatePermission(context)
                && hasGrantedReadContactsPermission(context);
    }

    /**
     * Checks if the read phone state permission is granted;
     *
     * @param context context of the app
     * @return true if it is granted, false if they is not
     */
    public boolean hasGrantedPhoneStatePermission(Context context)
    {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
                == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE)
                        == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Checks if the read contacts permission is granted;
     *
     * @param context context of the app
     * @return true if it is granted, false if it is not
     */
    public boolean hasGrantedReadContactsPermission(Context context)
    {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Checks if the manage overlay permission is granted.
     * Only for ANDROID SDK version >= M.
     *
     * @param context context of the app
     * @return true if it is granted, false if it is not
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public boolean hasGrantedManageOverlayPermission(Context context)
    {
        return Settings.canDrawOverlays(context);
    }

    /**
     * Opens a new activity with NO_HISTORY flag and destroys this startup activity.
     */
    public void openStartActivity()
    {
        Intent i = new Intent(new Intent(getApplicationContext(), StartActivity.class));
        startActivity(i);
        finish();
    }

    /**
     * Runs as a result of requesting for a grant a permissions.
     * Opens a {@link StartActivity} if all permissions are granted.
     *
     * @param requestCode code of the request, identify a request
     * @param permissions array of permissions
     * @param grantResults array of granted permissions
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults)
    {
        ImageView phoneStateWarningImage = findViewById(R.id.imageView4);
        TextView phoneStateWarningText = findViewById(R.id.permissions_startup_activity_phone_checkbox_warning);

        switch (requestCode)
        {
            case READ_PHONE_STATE_PERMISSION_REQUEST_CODE:
                {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    // permission was granted, we can open a Main Activity
                    if(hasGrantedManageOverlayPermission(getApplicationContext()))
                    {
                        requestCallPhonePermission();
                        //openStartActivity();
                    }
                    else setOptionsUnVisible(phoneStateWarningText, phoneStateWarningImage);
                }
                else
                {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    phoneStateWarningText.setTextKeepState(getResources().getString(R.string.permissions_startup_activity_phone_checkbox_warning));
                    setOptionsVisible(phoneStateWarningText, phoneStateWarningImage);
                }

                break;
            }
            case CALL_PHONE_PERMISSION_REQUEST_CODE:
            {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    // permission was granted, we can open a Main Activity
                    if(hasGrantedManageOverlayPermission(getApplicationContext()) && hasGrantedPermissions(getApplicationContext()))
                    {
                        openStartActivity();
                    }
                    else
                    {
                        // permission denied, boo! Disable the
                        // functionality that depends on this permission.
                        phoneStateWarningText.setTextKeepState(getResources().getString(R.string.permissions_startup_activity_phone_checkbox_warning));
                        setOptionsVisible(phoneStateWarningText, phoneStateWarningImage);
                    }
                }

                break;
            }
            case READ_CONTACTS_PERMISSION_REQUEST_CODE:
            {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    // permission was granted, we can open a Main Activity
                    if(hasGrantedManageOverlayPermission(getApplicationContext()) && hasGrantedPermissions(getApplicationContext()))
                    {
                        openStartActivity();
                    }
                    else
                    {
                        // permission denied, boo! Disable the
                        // functionality that depends on this permission.
                        contactsStateWarningText.setTextKeepState(getResources().getString(R.string.permissions_startup_activity_contacts_checkbox_warning));
                        setOptionsVisible(contactsStateWarningText, contactsStateWarningImage);
                    }
                }
            }
        }
    }


    /**
     * Checks manage overlay permission
     * If the app doesn't have a permission the settings windows for set permission will be opened
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void requestManageOverlayPermission()
    {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
        startActivityForResult(intent, ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE);
    }

    /**
     *  Runs after request for manage overlay permission.
     *  Opens a {@link StartActivity} if all permissions are granted.
     *
     * @param requestCode code of the request, identify a request
     * @param resultCode code of the requests result
     * @param data data
     */
    @TargetApi(Build.VERSION_CODES.M)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        ImageView allowWindowsWarningImage = findViewById(R.id.imageView6);
        TextView allowWindowsWarningText = findViewById(R.id.permissions_startup_activity_windows_checkbox_warning);

        if (requestCode == ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE)
        {
            if (!Settings.canDrawOverlays(this))
            {
                allowWindowsWarningText.setTextKeepState(getResources().getString(R.string.permissions_startup_activity_windows_checkbox_warning));
                setOptionsVisible(allowWindowsWarningText, allowWindowsWarningImage);
            }
            else if(hasGrantedPermissions(getApplicationContext())) openStartActivity();
            else setOptionsUnVisible(allowWindowsWarningText, allowWindowsWarningImage );
        }
    }
}
