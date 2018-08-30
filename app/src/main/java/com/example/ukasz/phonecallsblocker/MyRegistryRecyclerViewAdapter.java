package com.example.ukasz.phonecallsblocker;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.ukasz.androidsqlite.RegistryBlock;


import java.util.List;

/**
 * {@link RecyclerView.Adapter} that can display a {@link RegistryBlock} and makes a call to the
 */
public class MyRegistryRecyclerViewAdapter extends RecyclerView.Adapter<MyRegistryRecyclerViewAdapter.ViewHolder>
{

    private final List<RegistryBlock> mRegistryBlockings;
    private final RegistryAdapterListener mListener;
    //Context
    private final Context mContext;

    /**
     * ViewHolder class for single {@link RegistryBlock} view.
     */
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnLongClickListener
    {
        final View mViewContainer;
        //        final TextView mIdView;
        final TextView mNrRegisteredBlock;
        final TextView mDate;
        private LinearLayout mRegistryItemContainer;
        final RelativeLayout mItemOptionsContainer;


        /**
         * ViewHolder constructor.
         *
         * @param view single Block view
         */
        ViewHolder(View view)
        {
            super(view);
            mViewContainer = view;
//            mIdView = (TextView) view.findViewById(R.id.item_number);
            mNrRegisteredBlock = view.findViewById(R.id.registry_item_phone_number);
            mDate = view.findViewById(R.id.registry_item_date);
            mItemOptionsContainer = view.findViewById(R.id.registry_item_options_container);
            mRegistryItemContainer = view.findViewById(R.id.registry_item_container);
        }


        /**
         * Converts to {@link String text}.
         *
         * @return Stringified Block number
         */
        @Override
        public String toString()
        {
            return super.toString() + " '" + mNrRegisteredBlock.getText() + "'";
        }

        /**
         * Action after long click on single Block view.
         *
         * @param v single Block view
         * @return true
         */
        @Override
        public boolean onLongClick(View v)
        {
            mListener.onRowLongClicked(getAdapterPosition());
            v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            return true;
        }
    }

    /**
     * Constructor for creating a {@link MyPhoneBlockRecyclerViewAdapter} instance.
     *
     * @param context context of the application
     * @param registryBlockings list of registry blockings (registered blockings)
     * @param listener {@link RegistryAdapterListener listener} for catching events
     */
    MyRegistryRecyclerViewAdapter(Context context, List<RegistryBlock> registryBlockings, RegistryAdapterListener listener)
    {
        mContext = context;
        mRegistryBlockings = registryBlockings;
        mListener = listener;
    }

    /**
     * Inflates a {@link View} and attaches it to a {@link ViewHolder}
     * for single {@link RegistryBlock} instance.
     * @param parent a parent {@link ViewGroup} for get a context.
     * @param viewType type of {@link View}.
     * @return new {@link ViewHolder} for single {@link RegistryBlock} instance {@link View}.
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_registry, parent, false);
        return new ViewHolder(view);
    }

    /**
     * Allows an actions after bind a {@link MyPhoneBlockRecyclerViewAdapter.ViewHolder}.
     *
     * @param holder {@link MyRegistryRecyclerViewAdapter.ViewHolder} instance.
     * @param position position of element on the list (One of Blocks).
     */
    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position)
    {
        RegistryBlock rBlock = mRegistryBlockings.get(position);
        holder.mNrRegisteredBlock.setText(rBlock.getNrBlocked());
        holder.mDate.setText(rBlock.getNrBlockingDate().toString());

        //apply click events
        applyClickEvents(holder, position);
//        //show item menu
//        holder.mItemOptionsContainer.setOnClickListener(new View.OnClickListener()
//        {
//
//            @Override
//            public void onClick(View v)
//            {
//                //creating a popup menu
//                PopupMenu popup = new PopupMenu(mContext, holder.mItemOptionsContainer);
//                //inflating menu from xml resource
//                popup.inflate(R.menu.menu_registry_item);
//                //adding click listener
//                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener()
//                {
//                    @Override
//                    public boolean onMenuItemClick(MenuItem item)
//                    {
//                        switch (item.getItemId())
//                        {
//                            case R.id.menu_action_details:
//                                //handle details click
//                                break;
//                            case R.id.menu_action_delete:
//                                //handle delete click
//                                break;
//                            case R.id.menu_action_delete_all_related:
//                                //handle delete all related click
//                                break;
//                        }
//                        return false;
//                    }
//                });
//                //displaying the popup
//                popup.show();
//            }
//        });
    }

    /**
     * Applier for click events.
     * Sets up the listeners to catch click events.
     *
     * @param holder {@link MyRegistryRecyclerViewAdapter.ViewHolder holder} for Block at position
     * @param position position of single Block
     */
    private void applyClickEvents(MyRegistryRecyclerViewAdapter.ViewHolder holder, final int position)
    {
        holder.mItemOptionsContainer.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                mListener.onOptionsClicked(position);
            }
        });


        holder.mRegistryItemContainer.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                mListener.onRegistryRowClicked(position);
            }
        });

        holder.mRegistryItemContainer.setOnLongClickListener(new View.OnLongClickListener()
        {
            @Override
            public boolean onLongClick(View view)
            {
                mListener.onRowLongClicked(position);
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                return true;
            }
        });
    }

    @Override
    public int getItemCount()
    {
        return mRegistryBlockings.size();
    }

    /**
     * Interface for RegistryAdapterListener.
     */
    public interface RegistryAdapterListener
    {
        void onRegistryRowClicked(int position);

        void onRowLongClicked(int position);

        void onOptionsClicked(int position);
    }
}
