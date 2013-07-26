package com.praszapps.owetracker.adapter;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.praszapps.owetracker.R;
import com.praszapps.owetracker.bo.Due;

public class DueAdapter extends ArrayAdapter<Due> {

	//Declaring variables
	LayoutInflater inflater;
	Context mContext;
	int layoutResourceId;
	ArrayList<Due> dueData = null;
	String currency;

	public DueAdapter(Context mContext, int layoutResourceId, ArrayList<Due> data, String currency) {

		//Initializing views
		super(mContext, layoutResourceId, data);
		this.mContext = mContext;
		this.layoutResourceId = layoutResourceId;
		this.dueData = data;
		this.currency = currency;

	}
	
	public void setCurrency(String currency) {
		this.currency = currency;
	}
	
	public void updateAdapter(ArrayList<Due> dueList) {
		// Updating adapter
	    this.dueData = dueList;
	    this.notifyDataSetChanged();
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		View listItem = convertView;
		// Inflating the oweboard_details_list_item.xml
		LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
		listItem = inflater.inflate(layoutResourceId, parent, false);
		TextView textViewDate = (TextView) listItem.findViewById(R.id.textViewDate);
		TextView textViewAmtDetails = (TextView) listItem.findViewById(R.id.textViewAmtDetails);
		TextView textViewReason = (TextView) listItem.findViewById(R.id.textViewReason);
		
		textViewDate.setText(dueData.get(position).getFormattedDate());
		String summary = null;
		
		if(dueData.get(position).getAmount() >=0 ) {
			summary = "Gave "+dueData.get(position).getCurrency()+Math.abs(dueData.get(position).getAmount());
		} else {
			summary = "Took "+dueData.get(position).getCurrency()+Math.abs(dueData.get(position).getAmount());
		}
		
		textViewAmtDetails.setText(summary);
		textViewReason.setText(dueData.get(position).getReason());
		dueData.get(position).setCurrency(currency);
		
		if (dueData.get(position).getAmount() <= 0) {
			listItem.setBackgroundResource(R.color.list_item_green_bg);
		} else {
			listItem.setBackgroundResource(R.color.list_item_red_bg);
		}
		return listItem;
	}
	
}
