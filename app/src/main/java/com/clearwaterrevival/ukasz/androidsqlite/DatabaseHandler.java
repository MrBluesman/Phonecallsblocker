package com.clearwaterrevival.ukasz.androidsqlite;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * DatabaseHandler for keeping blocks in database.
 * Extends SQLiteOpenHelper.
 * Created by Łukasz on 2017-03-20.
 */
public class DatabaseHandler extends SQLiteOpenHelper
{
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "blockedNumbers";
    //table blocking
    private static final String TABLE_BLOCKING = "blocking";
    //columns
    private static final String DECLARANT_KEY_T_B = "nr_declarant";
    private static final String BLOCKED_KEY_T_B = "nr_blocked";
    private static final String REASON_CATEGORY_T_B = "reason_category";
    private static final String REASON_DESCRIPTION_T_B = "reason_description";
    private static final String RATING_T_B = "nr_rating";

    //table category
    private static final String TABLE_CATEGORY = "category";
    //columns
    private static final String ID_KEY_T_C = "id";
    private static final String NAME_T_C = "name";

    //table blocking_registry
    private static final String TABLE_BLOCKING_REGISTRY = "blocking_registry";
    //columns
    private static final String ID_KEY_T_BR = "id";
    private static final String BLOCKED_KEY_T_BR = "nr_blocked";
    private static final String RATING_T_BR = "nr_rating";
    private static final String BLOCKING_DATE_T_BR = "nr_blocking_date";


    /**
     * Constructor which create a new instance od DatabaseHandler by call extended super.
     * SQLiteOpenHelper.
     *
     * @param context Context of application
     */
    public DatabaseHandler(Context context)
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }


    /**
     * Runs on creating a SQLite database.
     * Contains definition and structure of tables in database.
     *
     * @param db database which will be keeping SQLite tables defined in this method
     */
    @Override
    public void onCreate(SQLiteDatabase db)
    {
        //creating a category table
        String createCategoryTable = "CREATE TABLE " + TABLE_CATEGORY
                + "("
                + ID_KEY_T_C + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + NAME_T_C + " VARCHAR(30) NOT NULL"
                + ")";
        db.execSQL(createCategoryTable);

        String fillCategory = "INSERT INTO " + TABLE_CATEGORY + " (" + NAME_T_C + ") VALUES " +
                "('Telemarketing')," +
                "('Call center')," +
                "('Nękanie')," +
                "('Niechciane oferty')," +
                "('Głuchy telefon')," +
                "('Usługi finansowe')," +
                "('Polityczne')," +
                "('Oszustwo')," +
                "('Robot')," +
                "('Dowcip')," +
                "('Inne')";
        db.execSQL(fillCategory);

        //create a blocking table
        String createBlockingTable = "CREATE TABLE " + TABLE_BLOCKING
                + "("
                + DECLARANT_KEY_T_B + " VARCHAR(15) NOT NULL, "
                + BLOCKED_KEY_T_B + " VARCHAR(15) NOT NULL, "
                + REASON_CATEGORY_T_B + " INTEGER NOT NULL, "
                + REASON_DESCRIPTION_T_B + " TEXT, "
                + RATING_T_B + " BOOLEAN NOT NULL, "
                + "FOREIGN KEY (" + REASON_CATEGORY_T_B + ") REFERENCES " + TABLE_CATEGORY + "(" + ID_KEY_T_C + "), "
                + "PRIMARY KEY (" + DECLARANT_KEY_T_B + ", " + BLOCKED_KEY_T_B + ") "
                + ")";
        db.execSQL(createBlockingTable);

        //create a blocking registry table
        String createBlockingRegistryTable = "CREATE TABLE " + TABLE_BLOCKING_REGISTRY
                + "("
                + ID_KEY_T_BR + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + BLOCKED_KEY_T_BR + " VARCHAR(15) NOT NULL, "
                + RATING_T_BR + " BOOLEAN NOT NULL, "
                + BLOCKING_DATE_T_BR + " DATETIME NOT NULL"
                + ")";
        db.execSQL(createBlockingRegistryTable);
    }

    /**
     * Destroys database structure and creating new by call onCreate(db).
     *
     * @param db database which will be upgraded
     * @param oldVersion id of old database version
     * @param newVersion id of new database version
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CATEGORY);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BLOCKING);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BLOCKING_REGISTRY);
        onCreate(db);
    }

    /**
     * Adds a new blocking to database.
     *
     * @param block instance of block which will be add to database
     */
    public void addBlocking(Block block)
    {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(DECLARANT_KEY_T_B, block.getNrDeclarant());
        values.put(BLOCKED_KEY_T_B, block.getNrBlocked());
        values.put(REASON_CATEGORY_T_B, block.getReasonCategory());
        values.put(REASON_DESCRIPTION_T_B, block.getReasonDescription());
        values.put(RATING_T_B, block.getNrRating());

        db.insert(TABLE_BLOCKING, null, values);
        db.close();
    }

    /**
     * Gets Block instance from database.
     *
     * @param nr_declarant Phone number of declarant
     * @param nr_blocked Blocked phone number
     * @return Block instance from database
     */
    public Block getBlocking(String nr_declarant, String nr_blocked)
    {
        SQLiteDatabase db = this.getReadableDatabase();

        String selectBlockings = "SELECT * FROM " + TABLE_BLOCKING
                + " WHERE " + DECLARANT_KEY_T_B + "='" + nr_declarant + "'"
                + " AND " + BLOCKED_KEY_T_B + "='" + nr_blocked + "'"
                + " LIMIT 1";

        Block block = null;

        Cursor cursor = db.rawQuery(selectBlockings, null);
        Log.e("COS", String.valueOf(cursor));
        if( cursor != null && cursor.moveToFirst())
        {
            block = new Block(cursor.getString(0), cursor.getString(1), Integer.parseInt(cursor.getString(2)),
                    cursor.getString(3), "1".equals(cursor.getString(4)));
            cursor.close();
        }

        return block;
    }

    /**
     * Gets reason category by id.
     *
     * @param cat_id category id
     * @return {@link String} reason category if exists
     */
    public String getCategory(int cat_id)
    {
        SQLiteDatabase db = this.getReadableDatabase();

        String selectCategory = "SELECT * FROM " + TABLE_CATEGORY
                + " WHERE " + ID_KEY_T_C + "='" + (cat_id + 1) + "'"
                + " LIMIT 1;";

        String category = null;
        Cursor cursor = db.rawQuery(selectCategory, null);
        if(cursor != null && cursor.moveToFirst())
        {
            category = cursor.getString(cursor.getColumnIndex(NAME_T_C));
            cursor.close();
        }

        return category;
    }

    /**
     * Gets a list of all blockings for blocked phone number.
     *
     * @param nr_blocked Blocked phone number
     * @return list of all Block instances for blocked number in database
     */
    public List<Block> getNumberBlockings(String nr_blocked)
    {
        SQLiteDatabase db = this.getReadableDatabase();
        List<Block> toReturnList = new ArrayList<>();

        String selectNumberBlockings = "SELECT * FROM " + TABLE_BLOCKING
                + " WHERE " + BLOCKED_KEY_T_B + "='" + nr_blocked +"';";

        Cursor cursor = db.rawQuery(selectNumberBlockings, null);
        if(cursor.moveToFirst())
        {
            do
            {
                Block block = new Block();
                block.setNrDeclarant(cursor.getString(0));
                block.setNrBlocked(cursor.getString(1));
                block.setReasonCategory(Integer.parseInt(cursor.getString(2)));
                block.setReasonDescription(cursor.getString(3));
                block.setNrRating(Boolean.parseBoolean(cursor.getString(4)));

                toReturnList.add(block);
            }
            while (cursor.moveToNext());
        }
        cursor.close();

        return toReturnList;
    }

    /**
     * Gets all Block instances from database.
     *
     * @return list of all Blocks instances from database
     */
    public List<Block> getAllBlockings()
    {
        List<Block> toReturnList = new ArrayList<>();

        String selectAllBlockings = "SELECT * FROM " + TABLE_BLOCKING;
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(selectAllBlockings, null);
        if(cursor.moveToFirst())
        {
            do
            {
                Block block = new Block();
                block.setNrDeclarant(cursor.getString(0));
                block.setNrBlocked(cursor.getString(1));
                block.setNrDeclarantBlocked(block.getNrDeclarant() + "_" + block.getNrBlocked());
                block.setReasonCategory(Integer.parseInt(cursor.getString(2)));
                block.setReasonDescription(cursor.getString(3));
                block.setNrRating("1".equals(cursor.getString(4)));

                toReturnList.add(block);
            }
            while (cursor.moveToNext());
        }
        cursor.close();

        return toReturnList;
    }

    /**
     * Gets all Block instances from database filtered by rating.
     *
     * @param rating filter condition - get only positive or only negative
     * @return list of Blocks instances from database filtered by rating
     */
    public List<Block> getAllBlockings(boolean rating)
    {
        List<Block> toReturnList = new ArrayList<>();

        int sqlRating = rating ? 1 : 0;
        String selectBlockingsByRating = "SELECT * FROM " + TABLE_BLOCKING + "WHERE " + RATING_T_B + "='" + sqlRating + "';";
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(selectBlockingsByRating, null);
        if(cursor.moveToFirst())
        {
            do
            {
                Block block = new Block();
                block.setNrDeclarant(cursor.getString(0));
                block.setNrBlocked(cursor.getString(1));
                block.setReasonCategory(Integer.parseInt(cursor.getString(2)));
                block.setReasonDescription(cursor.getString(3));
                block.setNrRating(Boolean.parseBoolean(cursor.getString(4)));

                toReturnList.add(block);
            }
            while (cursor.moveToNext());
        }
        cursor.close();

        return toReturnList;
    }

    /**
     * Counts blockings for block number.
     *
     * @param nr_blocked blocked phone number
     * @return count of blockings for nr_blocked in database
     */
    public int getNumberBlockingsCount(String nr_blocked)
    {
        String countBlockings = "SELECT * FROM " + TABLE_BLOCKING
                + " WHERE " + BLOCKED_KEY_T_B + "=" + nr_blocked +";";
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(countBlockings, null);
        int count = cursor.getCount();
        cursor.close();

        return count;
    }

    /**
     * Counts blockings for block number filtered by rating.
     *
     * @param nr_blocked blocked phone number
     * @param rating filter condition - get only positive or only negative
     * @return count of blockings for nr_blocked in database filtered by rating
     */
    public int getNumberBlockingsCount(String nr_blocked, boolean rating)
    {
        int sqlRating = rating ? 1 : 0;
        String countBlockingsByRating = "SELECT * FROM " + TABLE_BLOCKING
                + " WHERE " + BLOCKED_KEY_T_B + "='" + nr_blocked
                + "' AND " + RATING_T_B + "=" + sqlRating + ";";
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(countBlockingsByRating, null);
        int count = cursor.getCount();
        cursor.close();

        return count;
    }

    /**
     * Counts blockings for block number filtered by rating.
     *
     * @param nr_declarant declarant phone number
     * @param nr_blocked blocked phone number
     * @param rating filter condition - get only positive or only negative
     * @return count of blockings for nr_blocked in database filtered by rating
     */
    public int getNumberBlockingsCount(String nr_declarant, String nr_blocked, boolean rating)
    {
        int sqlRating = rating ? 1 : 0;
        String countBlockingsByRating = "SELECT * FROM " + TABLE_BLOCKING
                + " WHERE " + BLOCKED_KEY_T_B + "=" + nr_blocked
                + " AND " + RATING_T_B + "=" + sqlRating + ";";
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(countBlockingsByRating, null);
        int count = cursor.getCount();
        cursor.close();

        return count;
    }


    /**
     * Checks if block exists in database.
     *
     * @param block Block instance which will be checked if exist in database
     * @return true if block exists or false if not exist
     */
    public boolean existBlock(Block block)
    {

        SQLiteDatabase db = this.getWritableDatabase();

        String selectBlockings = "SELECT * FROM " + TABLE_BLOCKING
                + " WHERE " + DECLARANT_KEY_T_B + "=" + "'" + block.getNrDeclarant() + "'"
                + " AND " + BLOCKED_KEY_T_B + "=" + "'" + block.getNrBlocked() + "'";

        Cursor cursor = db.rawQuery(selectBlockings, null);
        boolean toReturn = cursor.getCount() > 0;
        cursor.close();

        return toReturn;
    }

    /**
     * Checks if block exists in database.
     *
     * @param nr_declarant declarant phone number
     * @param nr_blocked blocked phone number
     * @param rating phone rating, true if negative, false if positive
     * @return true if block with nr_declarant and nr_blocked exists or false if not exist
     */
    public boolean existBlock(String nr_declarant, String nr_blocked, boolean rating)
    {
        SQLiteDatabase db = this.getWritableDatabase();

        int sqlRating = rating ? 1 : 0;
        String selectBlockings = "SELECT * FROM " + TABLE_BLOCKING
                + " WHERE " + DECLARANT_KEY_T_B + "=" + "'" + nr_declarant + "'"
                + " AND " + BLOCKED_KEY_T_B + "=" + "'" + nr_blocked + "'"
                + " AND " + RATING_T_B + "=" + sqlRating + ";";

        Cursor cursor = db.rawQuery(selectBlockings, null);
        boolean toReturn = cursor.getCount() > 0;
        cursor.close();

        return toReturn;
    }



    /**
     * Gets count of all blockings in database.
     *
     * @return count of all blockings in database
     */
    public int getBlockingsCount()
    {
        String countNumberBlockings = "SELECT * FROM " + TABLE_BLOCKING;
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(countNumberBlockings, null);
        int count = cursor.getCount();
        cursor.close();

        return count;
    }

    /**
     * Updates data for block in database.
     *
     * @param block block instance which will be updated
     * @return 1 of updated, 0 if not updated
     */
    public int updateBlocking(Block block)
    {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(DECLARANT_KEY_T_B, block.getNrDeclarant());
        values.put(BLOCKED_KEY_T_B, block.getNrBlocked());
        values.put(REASON_CATEGORY_T_B, block.getReasonCategory());
        values.put(REASON_DESCRIPTION_T_B, block.getReasonDescription());
        values.put(RATING_T_B, block.getNrRating());

        return db.update(TABLE_BLOCKING, values, DECLARANT_KEY_T_B + " = ?" +
                    " AND " + BLOCKED_KEY_T_B + " = ?",
            new String[] { String.valueOf(block.getNrDeclarant()), String.valueOf(block.getNrBlocked()) });
    }

    /**
     * Deletes instance of {@link Block} from database.
     *
     * @param block {@link Block} instance which will be deleted from database
     */
    public void deleteBlocking(Block block)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_BLOCKING, DECLARANT_KEY_T_B + " = ?" +
                        " AND " + BLOCKED_KEY_T_B + " = ?",
                new String[] { String.valueOf(block.getNrDeclarant()), String.valueOf(block.getNrBlocked()) });

        db.close();
    }

    /**
     * Deletes instance of {@link RegistryBlock} from database.
     *
     * @param registryBlock {@link RegistryBlock} instance which will be deleted from database
     */
    public void deleteRegistryBlocking(RegistryBlock registryBlock)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_BLOCKING_REGISTRY, BLOCKED_KEY_T_BR + " = ?" +
                        " AND " + BLOCKING_DATE_T_BR + " = ?",
                new String[] { String.valueOf(registryBlock.getNrBlocked()),
                        String.valueOf(registryBlock.getNrBlockingDateFormatted("MM/dd/yyyy HH:mm:ss")) });

        db.close();
    }

    /**
     * Deletes all registry blockings which blocked number is equal to
     * blocked number of instance of {@link RegistryBlock} from database.
     *
     * @param registryBlock {@link RegistryBlock} instance which blocked number determines deleting
     */
    public void deleteRegistryBlockings(RegistryBlock registryBlock)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_BLOCKING_REGISTRY, BLOCKED_KEY_T_BR + " = ?",
                new String[] { String.valueOf(registryBlock.getNrBlocked()) });

        db.close();
    }

    /**
     * Gets count of all categories in database.
     *
     * @return count of all categories in database
     */
    public int getCategoriesCount()
    {
        String countCategories = "SELECT * FROM " + TABLE_CATEGORY;
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(countCategories, null);
        return cursor.getCount();
    }

    /**
     * Deletes all categories from database.
     */
    public void clearCategories()
    {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CATEGORY);
        String createCategoryTable = "CREATE TABLE " + TABLE_CATEGORY
                + "("
                + ID_KEY_T_C + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + NAME_T_C + " VARCHAR(30) NOT NULL"
                + ")";
        db.execSQL(createCategoryTable);
    }

    /**
     * Updates categories by clear and fill again.
     */
    public void updateCategories()
    {
        clearCategories();
        fillCategories();
    }

    // CATEGORIES ----------------------------------------------------------------------------------

    /**
     * Fills categories table in database with predefined categories.
     */
    public void fillCategories()
    {
        SQLiteDatabase db = this.getWritableDatabase();
        String fillCategory = "INSERT INTO " + TABLE_CATEGORY + " (" + NAME_T_C + ") VALUES " +
                "('Kategoria1'),('Kategoria2')";
        db.execSQL(fillCategory);
    }

    /**
     * Gets list of all categories in database.
     *
     * @return list of all categories in database (List of String).
     */
    public List<String> getAllCategories()
    {
        List<String> toReturnList = new ArrayList<String>();

        String selectAllCategories = "SELECT * FROM " + TABLE_CATEGORY;
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(selectAllCategories, null);
        if(cursor.moveToFirst())
        {
            do
            {
                String category = cursor.getString(1);

                toReturnList.add(category);
            }
            while (cursor.moveToNext());
        }
        cursor.close();

        return toReturnList;
    }

    // BLOCKING REGISTRY ---------------------------------------------------------------------------
    /**
     * Adds a new registry blocking to database.
     *
     * @param rBlock instance of registry block which will be add to database
     */
    public void addBlockingRegistry(RegistryBlock rBlock)
    {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(BLOCKED_KEY_T_BR, rBlock.getNrBlocked());
        values.put(RATING_T_BR, rBlock.getNrRating());
        values.put(BLOCKING_DATE_T_BR, rBlock.getNrBlockingDateFormatted("MM/dd/yyyy HH:mm:ss"));

        db.insert(TABLE_BLOCKING_REGISTRY, null, values);
        db.close();
    }

    /**
     * Gets all RegistryBlock instances from database.
     *
     * @return list of all RegistryBlocks instances from database
     */
    public List<RegistryBlock> getAllRegistryBlockings() throws ParseException
    {
        //Format to retrieve a date in specified format
        @SuppressLint("SimpleDateFormat")
        DateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");

        List<RegistryBlock> toReturnList = new ArrayList<>();

        String selectAllRegistryBlockings = "SELECT * FROM " + TABLE_BLOCKING_REGISTRY
                + " ORDER BY " + ID_KEY_T_BR +" DESC";
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(selectAllRegistryBlockings, null);
        if(cursor.moveToFirst())
        {
            do
            {
                RegistryBlock rBlock = new RegistryBlock();
                rBlock.setNrBlocked(cursor.getString(1));
                rBlock.setNrRating("1".equals(cursor.getString(2)));
                rBlock.setNrBlockingDate(df.parse(cursor.getString(3)));

                toReturnList.add(rBlock);
            }
            while (cursor.moveToNext());
        }
        cursor.close();

        return toReturnList;
    }

    /**
     * Gets count of all categories in database.
     *
     * @return count of all categories in database
     */
    public int getRegistryBlockingsCount()
    {
        String countRegistryBlockings = "SELECT * FROM " + TABLE_BLOCKING_REGISTRY;
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(countRegistryBlockings, null);
        return cursor.getCount();
    }

    /**
     * Deletes all {@link RegistryBlock} instances from database.
     */
    public void clearRegistryBlockings()
    {
        SQLiteDatabase db = this.getWritableDatabase();
        //drop and create new a blocking registry table
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BLOCKING_REGISTRY);
        String createBlockingRegistryTable = "CREATE TABLE " + TABLE_BLOCKING_REGISTRY
                + "("
                + ID_KEY_T_BR + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + BLOCKED_KEY_T_BR + " VARCHAR(15) NOT NULL, "
                + RATING_T_BR + " BOOLEAN NOT NULL, "
                + BLOCKING_DATE_T_BR + " DATETIME NOT NULL"
                + ")";
        db.execSQL(createBlockingRegistryTable);
    }

}
