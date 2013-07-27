package com.praszapps.owetracker.ui.fragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.ListFragment;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.praszapps.owetracker.R;
import com.praszapps.owetracker.adapter.DueAdapter;
import com.praszapps.owetracker.bo.Due;
import com.praszapps.owetracker.bo.Friend;
import com.praszapps.owetracker.database.DatabaseHelper;
import com.praszapps.owetracker.ui.activity.MainActivity;
import com.praszapps.owetracker.ui.activity.RootActivity;
import com.praszapps.owetracker.util.Utils;

public class FriendDueFragment extends ListFragment {

	private View v;
	private Friend friend, updateFriend;
	private RootActivity rAct;
	private TextView textViewOweSummary, emptyTextView;
	private static TextView textViewDate;
	private ListView listViewTransactions;
	private ArrayList<Due> duesList = new ArrayList<Due>();
	private SQLiteDatabase db;
	private DueAdapter adapter;
	private Dialog d;
	private EditText editTextAmount, editTextReason;
	private Spinner spinnerGaveTook;
	private Button buttonSave;
	private static int calendarYear, calendarMonth, calendarDay;
	private static Calendar cld;
	@SuppressLint("SimpleDateFormat")
	private static SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		//Utils.showLog(getClass().getSimpleName(), "onCreateView() starts", Log.VERBOSE);
		
		//Setting the View
		v = inflater.inflate(R.layout.fragment_owe_details, container, false);
		//Setting the action bar
		if(MainActivity.isSinglePane) {
			ActionBar action = getActivity().getActionBar();
			action.setDisplayHomeAsUpEnabled(true);
		}
		//Initializing views and other variables
		emptyTextView = (TextView) v.findViewById(R.id.empty_duelist);
		textViewOweSummary = (TextView) v.findViewById(R.id.textViewOweSummary);
		listViewTransactions = (ListView) v.findViewById(android.R.id.list);
		listViewTransactions.setEmptyView(v.findViewById(R.id.empty_duelist));
		rAct = (RootActivity) getActivity();
		db = rAct.database;
		
		//Getting the data and populating the listview
		Bundle b = getArguments();
		if(b != null) {
			showDetails(b.getString("friendId"), b.getString("currency"));
			
		}
		
		//Utils.showLog(getClass().getSimpleName(), "onCreateView() ends", Log.VERBOSE);
		return v;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.detail_menu, menu);
		
	}
	

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		if (v.getId()== android.R.id.list) {
		    String[] menuItems = getResources().getStringArray(R.array.array_due_item_options);
		    for (int i = 0; i<menuItems.length; i++) {
		      menu.add(Menu.NONE, i, i, menuItems[i]);
		    }
		  }
	}
	
	

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		super.onContextItemSelected(item);
		switch(item.getItemId()) {
		case 0:
			//TODO implement logic to edit due
			break;
		case 1:
			//TODO implement logic to delete due
			break;
		}
		return true;
		
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			//Go back to due list
			getFragmentManager().popBackStack();
            return true;
            
		case R.id.item_add_due:
			showAddDueDialog();
			return true;
			
		case R.id.item_edit_friend:
			showEditFriendDialog();
			return true;
			
		case R.id.item_close_due:
			Utils.showAlertDialog(getActivity(), "Delete/Reset friend?", "Do you want to delete the friend name or reset the due value to zero?", null, false, "Delete", "Reset to zero", null, new Utils.DialogResponse() {
				
				@Override
				public void onPositive() {
					// Delete all records of dues and friends
					DatabaseHelper.deleteAllFriendDues(friend.getId(), db);
					DatabaseHelper.deleteFriendRecord(friend.getId(), db);
					if(MainActivity.isSinglePane) {
						FragmentManager fm = getFragmentManager();
						fm.popBackStackImmediate();
					} else {
						updateDueList();
						OweboardFragment.updateListView();
						//startActivity(new Intent(getActivity(), MainActivity.class));
						setHasOptionsMenu(false); //TODO Test in tab
						//getActivity().finish(); //TODO improve logic
					}
				}
				
				@Override
				public void onNeutral() {
					//Do nothing
				}
				
				@Override
				public void onNegative() {
					// Delete dues and reset due value to zero
					DatabaseHelper.deleteAllFriendDues(friend.getId(), db);
					updateDueList();
					updateFriendSummary();
				}
			});
			
			return true;
			
		}
		
		
		
		return super.onOptionsItemSelected(item);
	}

	private void updateFriendSummary() {
		friend = DatabaseHelper.getFriendData(friend.getId(), db);
		textViewOweSummary.setText(friend.toString());
	}

	@Override
	public void onResume() {
		super.onResume();
		registerForContextMenu(getListView());
	}

	public void showDetails(String friendId, String currency) {
		setHasOptionsMenu(true);
		textViewOweSummary.setVisibility(TextView.VISIBLE);
		emptyTextView.setText(getResources().getString(R.string.strNoDueRecordsFound));
		friend = DatabaseHelper.getFriendData(friendId, db);
		if(friend != null) {
			textViewOweSummary.setText(friend.toString());
			duesList.clear();
			duesList = DatabaseHelper.getFriendDueList(friendId, db);
			if(duesList != null) {
				// Setting the adapter
				adapter = new DueAdapter(getActivity(), R.layout.owe_details_list_item, duesList, friend.formatCurrency(friend.getCurrency()));
				setListAdapter(adapter);
			}
			
		} else {
			
			//Show error message and close fragment
			Utils.showToast(getActivity(), getResources().getString(R.string.toast_msg_friend_data_get_failure), Toast.LENGTH_SHORT);
			getFragmentManager().popBackStack();
		}
	
	}
	
	private void showAddDueDialog() {
		d = new Dialog(getActivity());
		d.setContentView(R.layout.dialog_add_due);
		d.setTitle("Add Due");
		textViewDate = (TextView) d.findViewById(R.id.textViewDate);
		spinnerGaveTook = (Spinner) d.findViewById(R.id.spinnerGiveTake);
		ArrayAdapter<String> currencyAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, getResources().getStringArray(R.array.string_array_give_take));
		currencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerGaveTook.setAdapter(currencyAdapter);
		editTextAmount = (EditText) d.findViewById(R.id.editTextAmount);
		editTextAmount.setHint(R.string.label_hint_enter_amount);
		editTextReason = (EditText) d.findViewById(R.id.editTextReason);
		editTextReason.setHint(R.string.label_hint_enter_desc);
		buttonSave = (Button) d.findViewById(R.id.buttonSave);
		
		textViewDate.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				DialogFragment datepicker = new DatePickerFragment();
				datepicker.show(getFragmentManager(), "datePicker");
				
			}
		});
		
		buttonSave.setOnClickListener(new OnClickListener() {
			private Due addDue;
			@Override
			public void onClick(View v) {
				if(editTextAmount.getText().toString().equals("") || editTextAmount.getText().toString() == null) {
					Utils.showToast(getActivity(), getResources().getString(R.string.toast_msg_add_due_amount), Toast.LENGTH_SHORT);
					return;
				} else if(editTextReason.getText().toString().equals("") || editTextReason.getText().toString() == null) {
					Utils.showToast(getActivity(), getResources().getString(R.string.toast_msg_add_due_reason), Toast.LENGTH_SHORT);
					return;
				} else if(spinnerGaveTook.getSelectedItem().toString().equals(getResources().getString(R.string.array_givetake_item_select))) {
					Utils.showToast(getActivity(), getResources().getString(R.string.toast_msg_add_due_givetake), Toast.LENGTH_SHORT);
					return;
				} else if(textViewDate.getText().toString().equals(getResources().getString(R.string.label_add_date))) {
					Utils.showToast(getActivity(), getResources().getString(R.string.toast_msg_add_due_date), Toast.LENGTH_SHORT);
					return;
				} else {
					addDue = new Due();
					addDue.setDueId(Utils.generateUniqueID());
					addDue.setFriendId(friend.getId());
					addDue.setDate(cld.getTimeInMillis());
										
					// Convert amount according to selection
					int amt = 0;
					if(spinnerGaveTook.getSelectedItem().toString().equals(getResources().getString(R.string.array_givetake_item_gave))) {
						amt = Integer.parseInt(editTextAmount.getText().toString().trim());
					} else if(spinnerGaveTook.getSelectedItem().toString().equals(getResources().getString(R.string.array_givetake_item_took))) {
						amt = -(Integer.parseInt(editTextAmount.getText().toString().trim()));
					}
					
					addDue.setAmount(amt);
					addDue.setReason(editTextReason.getText().toString().trim());
					
					if(DatabaseHelper.addDue(addDue, db)) {
						Utils.showToast(getActivity(), getResources().getString(R.string.toast_msg_due_add_success), Toast.LENGTH_SHORT);
						DatabaseHelper.updateFriendDue(friend.getId(), db);
						updateDueList();
						updateFriendSummary();
						d.dismiss();
					} else {
						Utils.showToast(getActivity(), getResources().getString(R.string.toast_msg_due_add_failure), Toast.LENGTH_SHORT);
						d.dismiss();
					}
					return;
				}
			}
		});
		d.show();
	
	}
	
	@SuppressWarnings("unchecked")
	private void showEditFriendDialog() {
		d = new Dialog(getActivity());
		d.setContentView(R.layout.dialog_add__update_friend);
		d.setTitle("Add friend");
		final Spinner spinnerCurrency = (Spinner) d.findViewById(R.id.spinnerCurrency);
		ArrayAdapter<String> currencyAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, getResources().getStringArray(R.array.string_array_currency));
		currencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerCurrency.setAdapter(currencyAdapter);
		spinnerCurrency.setSelection(((ArrayAdapter<String>) spinnerCurrency.getAdapter()).getPosition(friend.getCurrency()));
		final EditText editTextfriendName = (EditText) d.findViewById(R.id.editTextFriendName);
		editTextfriendName.setText(friend.getName());
		Button buttonSave = (Button) d.findViewById(R.id.buttonSave);
		buttonSave.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {

				if(editTextfriendName.getText().toString().trim().equals("")|| editTextfriendName.getText().toString().trim() == null) {
					Utils.showToast(getActivity(), getResources().getString(R.string.toast_msg_add_friend_name), Toast.LENGTH_SHORT);
					return;
				} else if(spinnerCurrency.getSelectedItem().toString().equals(getResources().getString(R.string.array_currency_item_select))) {
					Utils.showToast(getActivity(), getResources().getString(R.string.toast_msg_add_friend_currency), Toast.LENGTH_SHORT);
					return;
				} else {
					//Add data to database
					updateFriend = new Friend();
					updateFriend.setId(friend.getId());
					updateFriend.setName(editTextfriendName.getText().toString().trim());
					updateFriend.setCurrency(spinnerCurrency.getSelectedItem().toString());
					
						if(DatabaseHelper.updateFriend(updateFriend, db)) {
							Utils.showToast(getActivity(), getResources().getString(R.string.toast_msg_update_friend_success), Toast.LENGTH_SHORT);
							
							friend = DatabaseHelper.getFriendData(friend.getId(), db);
							textViewOweSummary.setText(friend.toString());
							
							if(!MainActivity.isSinglePane) {
								OweboardFragment.updateListView();
							}
							updateDueList();
							d.dismiss();
							
						} else {
							Utils.showToast(getActivity(), getResources().getString(R.string.toast_msg_update_friend_failure), Toast.LENGTH_SHORT);
						}
						
					}
				}
		});
		d.show();
	}
	
	public static class DatePickerFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener {

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			// Use the current date as the default date in the picker
			if(!textViewDate.getText().toString().contains("/")) {
				final Calendar c = Calendar.getInstance();
				calendarYear = c.get(Calendar.YEAR);
				calendarMonth = c.get(Calendar.MONTH);
				calendarDay = c.get(Calendar.DAY_OF_MONTH);
			}
			// Create a new instance of DatePickerDialog and return it
			return new DatePickerDialog(getActivity(), this, calendarYear, calendarMonth, calendarDay);
		}

		public void onDateSet(DatePicker view, int year, int month, int day) {
			// Do something with the date chosen by the user
			
			cld = Calendar.getInstance();
			cld.set(Calendar.YEAR, year);
			cld.set(Calendar.MONTH, month);
			cld.set(Calendar.DATE, day);
			cld.set(Calendar.HOUR, 0);
			cld.set(Calendar.MINUTE, 0);
			cld.set(Calendar.SECOND, 0);
			cld.set(Calendar.MILLISECOND, 0);
			if (cld.getTime().after(new Date())){
				Utils.showToast(getActivity(), getResources().getString(R.string.ERROR_INVALID_DATE), Toast.LENGTH_SHORT);
				textViewDate.setText(getResources().getString(R.string.label_click_add_date));
			}else{
				calendarDay = day;
				calendarMonth = month;
				calendarYear = year;
				textViewDate.setText("Date: "+dateFormat.format(cld.getTimeInMillis()));
			}
			
			
			
		}
	}
	
	private void updateDueList() {
		OweboardFragment.updateListView();
		duesList = DatabaseHelper.getFriendDueList(friend.getId(), db);
		adapter.clear();
		adapter.setCurrency(friend.getCurrency());
		adapter.addAll(duesList);
		adapter.notifyDataSetChanged();
	}
	
	
}
