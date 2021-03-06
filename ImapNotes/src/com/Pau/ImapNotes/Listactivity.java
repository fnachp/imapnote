package com.Pau.ImapNotes;

import java.util.ArrayList;
import com.Pau.ImapNotes.Data.ConfigurationFile;
import com.Pau.ImapNotes.Data.NotesDb;
import com.Pau.ImapNotes.Miscs.Imaper;
import com.Pau.ImapNotes.Miscs.OneNote;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.SimpleAdapter;
import android.widget.ListView;

public class Listactivity extends Activity {
	private static final int LOGIN_BUTTON = 0;
	private static final int REFRESH_BUTTON = 1;
		
	private ArrayList<OneNote> noteList;
	private SimpleAdapter listToView;
	
	private ConfigurationFile settings;
	private Imaper imapFolder;
	private NotesDb storedNotes;
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        this.noteList = new ArrayList<OneNote>();
        ((ImapNotes)this.getApplicationContext()).SetNotesList(this.noteList);
        this.listToView = new SimpleAdapter(
        		getApplicationContext(),
                this.noteList,
                R.layout.note_element,
                new String[]{"title","date"},
                new int[]{R.id.noteTitle, R.id.noteInformation});
        ((ListView)findViewById(R.id.notesList)).setAdapter(this.listToView);
        
        this.settings = new ConfigurationFile(this.getApplicationContext());
        ((ImapNotes)this.getApplicationContext()).SetConfigurationFile(this.settings);
        
        this.imapFolder = new Imaper();
        ((ImapNotes)this.getApplicationContext()).SetImaper(this.imapFolder);
        
        this.storedNotes = new NotesDb(this.getApplicationContext());
        
        if (this.settings.GetUsername()==null && this.settings.GetPassword()==null){
            startActivityForResult(new Intent(this, AccontConfigurationActivity.class), Listactivity.LOGIN_BUTTON);
        
        } else {
        	this.storedNotes.OpenDb();
        	this.storedNotes.GetStoredNotes(this.noteList);
        	this.listToView.notifyDataSetChanged();
        	this.storedNotes.CloseDb();
        }
        
        ((ListView)findViewById(R.id.notesList)).setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View widget, int selectedNote, long arg3) {
				Intent toDetail = new Intent(widget.getContext(), NoteDetailActivity.class);
				toDetail.putExtra("selectedNote", ((Listactivity)widget.getContext()).noteList.get(selectedNote));
				startActivity(toDetail);
			}
          });
                
    }
    
    public void DeleteItem(View v){
    	this.noteList.remove(this.noteList.size()-1);
    	this.listToView.notifyDataSetChanged();
    	
    }
    
    public void RefreshList(){
		ProgressDialog loadingDialog = ProgressDialog.show(this, "ImapNotes" , "Refreshing notes list... ", true);

		new RefreshThread().execute(this.imapFolder, this.settings, this.noteList, this.listToView, loadingDialog, this.storedNotes);

    }
    
    class RefreshThread extends AsyncTask<Object, Void, Boolean>{
    	SimpleAdapter adapter;
    	ArrayList<OneNote> notesList;
    	NotesDb storedNotes;
    	
		@Override
		protected Boolean doInBackground(Object... stuffs) {
			this.adapter = ((SimpleAdapter)stuffs[3]);
			this.notesList = ((ArrayList<OneNote>)stuffs[2]);
			this.storedNotes = ((NotesDb)stuffs[5]);
	
			try {
				if(!((Imaper)stuffs[0]).IsConnected())
					((Imaper)stuffs[0]).ConnectToProvider(((ConfigurationFile)stuffs[1]).GetUsername(), ((ConfigurationFile)stuffs[1]).GetPassword());
				((Imaper)stuffs[0]).GetNotes(this.notesList);
		    	return true;
			} catch (Exception e) {
				Log.v("ImapNotes", e.getMessage());
			} finally {
				((ProgressDialog)stuffs[4]).dismiss();
				
			}
			
			return false;
		}
		
		protected void onPostExecute(Boolean result){
			if(result){
				this.storedNotes.OpenDb();
				this.storedNotes.ClearDb();
				for(OneNote n : this.notesList)
					this.storedNotes.InsertANote(n);
				this.storedNotes.CloseDb();
						
				this.adapter.notifyDataSetChanged();
			}
		}
    	
    }
    
    /***************************************************/
    public boolean onCreateOptionsMenu(Menu menu){
        menu.add(0, Listactivity.LOGIN_BUTTON, 0, "Account");
        //.setIcon(R.drawable.ic_menu_barcode);
        menu.add(0, Listactivity.REFRESH_BUTTON, 0, "Refresh");
        
        return true;

    }
    
    public boolean onOptionsItemSelected (MenuItem item){
        switch (item.getItemId()){
        	case Listactivity.LOGIN_BUTTON:
                startActivityForResult(new Intent(this, AccontConfigurationActivity.class), Listactivity.LOGIN_BUTTON);
                return true;
        	case Listactivity.REFRESH_BUTTON:
        		if(this.settings.GetUsername()==null && this.settings.GetPassword()==null)
                    startActivityForResult(new Intent(this, AccontConfigurationActivity.class), Listactivity.LOGIN_BUTTON);
        		else
        			this.RefreshList();
        		return true;
                    
        }
        
        return false;
        
    }
    
    /***************************************************/
    protected void onActivityResult(int requestCode, int resultCode, Intent data){ 
    	switch(requestCode){
    		case Listactivity.LOGIN_BUTTON:
    			if(resultCode==AccontConfigurationActivity.TO_REFRESH)
    				this.RefreshList();
    			
    	}
    	
    }
    
    
}


