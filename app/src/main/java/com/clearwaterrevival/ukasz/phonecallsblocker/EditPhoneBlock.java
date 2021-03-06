package com.clearwaterrevival.ukasz.phonecallsblocker;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.telephony.TelephonyManager;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.clearwaterrevival.ukasz.androidsqlite.Block;
import com.clearwaterrevival.ukasz.androidsqlite.DatabaseHandler;
import com.clearwaterrevival.ukasz.phonecallsblocker.phone_number_helper.PhoneNumberHelper;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.i18n.phonenumbers.PhoneNumberUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class EditPhoneBlock extends AppCompatActivity implements AdapterView.OnItemSelectedListener
{
    private Toolbar mActionBar;
    private TextView nrBlocked;
    private TextView nrBlocked2; //for contact name if available
    private Switch isPositiveSwitch;
    private Spinner category;
    private EditText description;
    private Button editButton;
    private String myPhoneNumber;
    private TelephonyManager tm;

    //Editing blocking
    Block block;

    //Database local and firebase handlers
    private DatabaseHandler db;
    private DatabaseReference mDatabase;


    /**
     * Initialize var instances and view for start {@link EditPhoneBlock} activity.
     *
     * @param savedInstanceState instance state
     */
    @Override
    @SuppressLint("HardwareIds")
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_phone_block);

        db = new DatabaseHandler(getApplicationContext());
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // TODO: Refactor: Consider keeping myPhoneNumber in external common place
        //getMyPhoneNumber
        tm = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_NUMBERS)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) return;
        myPhoneNumber = !tm.getLine1Number().equals("") ? tm.getLine1Number() : tm.getSubscriberId();
        myPhoneNumber = !myPhoneNumber.equals("") ? myPhoneNumber : tm.getSimSerialNumber();

        //set toolbar
        mActionBar = findViewById(R.id.edit_phone_block_toolbar);
        setSupportActionBar(mActionBar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        //nr info ---------------------------------------------------------------------------
        nrBlocked = findViewById(R.id.edit_phone_block_nr_blocked_textView);
        nrBlocked2 = findViewById(R.id.edit_phone_block_nr_blocked_textView2);
        isPositiveSwitch = findViewById(R.id.edit_phone_block_is_positive_switch);
        description = findViewById(R.id.edit_phone_block_descriptionEditText);

        //spinner --------------------------------------------------------------------------
        category = findViewById(R.id.edit_phone_block_spinner);
        loadCategoriesToSpinner(category);
        category.setOnItemSelectedListener(this);

        //Switch view depends on blocking type (positive or negative)
        isPositiveSwitch.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (isPositiveSwitch.isChecked())
                {
                    category.setVisibility(View.GONE);
                }
                else
                {
                    category.setVisibility(View.VISIBLE);
                }
            }
        });

        //set the fields as number info --------------------------------------------------------------------------
        Bundle b = getIntent().getExtras();
        String phoneNumber = "";
        if(b != null) phoneNumber = b.getString("phoneNumber");
        block = getBlock(phoneNumber);

        //Get validator phone number lib to format
        PhoneNumberHelper phoneNumberHelper = new PhoneNumberHelper();

        String contactName = phoneNumberHelper.getContactName(getApplicationContext(), block.getNrBlocked());
        String phoneNumberFormatted = phoneNumberHelper.formatPhoneNumber(block.getNrBlocked(), StartActivity.COUNTRY_CODE, PhoneNumberUtil.PhoneNumberFormat.NATIONAL);

        if(contactName != null)
        {
            nrBlocked.setText(contactName);
            nrBlocked2.setText(phoneNumberFormatted);
        }
        else
        {
            nrBlocked.setText(phoneNumberFormatted);
            nrBlocked2.setVisibility(View.GONE);
        }


        isPositiveSwitch.setChecked(!block.getNrRating());
        description.setText(block.getReasonDescription());
        category.setSelection(block.getReasonCategory());

        //edit button listener --------------------------------------------------------------------------
        editButton = findViewById(R.id.edit_phone_block_editButton);
        editButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                //change data
                block.setNrRating(!isPositiveSwitch.isChecked());
                block.setReasonDescription(String.valueOf(description.getText()));
                block.setReasonCategory(category.getSelectedItemPosition());

                //update with changed data
                updatePhoneBlock();
                finish();
            }
        });
    }

    /**
     * Loads categories from database to spinner.
     *
     * @param spinner spinner which will have a set adapter with loaded categories
     */
    public void loadCategoriesToSpinner(Spinner spinner)
    {
        DatabaseHandler db = new DatabaseHandler(this);
        List<String> categories = db.getAllCategories();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    /**
     * Gets {@link Block} from local database by passed blocked phone number.
     *
     * @param phoneNumber blocked number
     * @return {@link Block} if exist blocked number number
     */
    private Block getBlock(String phoneNumber)
    {
        return db.getBlocking(myPhoneNumber, phoneNumber);
    }

    /**
     * Updates the phone block with new edited data.
     *
     */
    private void updatePhoneBlock()
    {
        //LOCAL UPDATING
        //In listener button set properties to the newest one (edited)
        db.updateBlocking(block);

        //Refresh blockings after update
        PhoneBlockFragment.loadBlockingsExternal();

        //GLOBAL UPDATING - if sync is enabled
        boolean syncEnabled =  getApplicationContext().getSharedPreferences("data", Context.MODE_PRIVATE)
                .getBoolean("syncEnabled", false);

        if(syncEnabled)
        {
            final Query blockings = mDatabase
                    .child("blockings")
                    .orderByChild("nrDeclarantBlocked")
                    .equalTo(myPhoneNumber + "_" + block.getNrBlocked());

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
                            updateData.put("nrRating", block.getNrRating());
                            updateData.put("reasonCategory", block.getReasonCategory());
                            updateData.put("reasonDescription", block.getReasonDescription());
                            dataSnapshot.getChildren().iterator().next().getRef().updateChildren(updateData);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError)
                {
                    Toast.makeText(getApplicationContext(), R.string.error, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    /**
     * Catch the arrow back action as one of {@link MenuItem} item.
     *
     * @param item {@link MenuItem} selected menu item
     * @return this method applied to superclass with this item
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // handle arrow click here
        if (item.getItemId() == android.R.id.home)
        {
            onBackPressed(); // close this activity and return to previous activity (if there is any)
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
    {

    }

    @Override
    public void onNothingSelected(AdapterView<?> parent)
    {

    }
}
