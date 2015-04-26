package com.example.admin.couchbasechatapp;

import android.app.Activity;

import android.app.Application;
import android.content.Context;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Document;
import com.couchbase.lite.LiveQuery;
import com.couchbase.lite.Manager;
import com.couchbase.lite.QueryEnumerator;
import com.couchbase.lite.android.AndroidContext;

import java.io.IOException;


public class MainActivity extends ActionBarActivity {

    private RecyclerView mRecyclerView;
    private RecyclerView.LayoutManager mLayoutManager;
    private RecyclerView.Adapter mAdapter;
    ImageButton send;
    Database mDatabase;
    String DB_NAME = "chat";
    Manager mManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            mManager = new Manager(new AndroidContext(this),Manager.DEFAULT_OPTIONS);
        } catch (IOException e) {
            Log.i("--MANAGER : mManager--",e.getMessage());
        }

        try {
            mDatabase = mManager.getDatabase(DB_NAME);
        } catch (CouchbaseLiteException e) {
            Log.i("--MANAGER : mDatabase--",e.getMessage());
        }

        // Setting up the recycler view
        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerView);

        // Use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        setupChatFeed();
        //--------------------------------------------------

        send = (ImageButton) findViewById(R.id.btnSend);
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final EditText msg = (EditText)findViewById(R.id.userMessage);
                try{
                    Message.createMessage(mDatabase,"Bob",msg.getText().toString());
                }
                catch (Exception ex){
                    Log.i("--POST CHAT--",ex.getMessage());
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private void setupChatFeed(){

        LiveQuery query = Message.allPostsQuery(mDatabase).toLiveQuery();

        // Use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        mAdapter = new Adapter(this,query);
        mRecyclerView.setAdapter(mAdapter);
    }
}

// Recycler View Adapter--------------------------
class Adapter extends RecyclerView.Adapter{

    private Context context;
    private LiveQuery query;
    private QueryEnumerator enumerator;

    public Adapter(Context context,LiveQuery query){
        this.context = context;
        this.query = query;

        query.addChangeListener(new LiveQuery.ChangeListener() {
            @Override
            public void changed(final LiveQuery.ChangeEvent event) {
                ((Activity)Adapter.this.context).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        enumerator = event.getRows();
                        notifyDataSetChanged();
                    }
                });
            }
        });
          query.start();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        final View view = LayoutInflater.from(context).inflate(R.layout.row,viewGroup,false);
        return new AdapterView(view);
    }


    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int i) {

        AdapterView holder = (AdapterView) viewHolder;
        final Document task = (Document) getItem(i);
        holder.username.setText((String)task.getProperty("username"));
        holder.message.setText((String)task.getProperty("message"));
        holder.dt.setText((String)task.getProperty("created_at"));
    }

    @Override
    public int getItemCount() {
        return enumerator != null ? enumerator.getCount() : 0;
    }

    public Object getItem(int i) {
        return enumerator != null ? enumerator.getRow(i).getDocument() : null;
    }
}
//------------------------------------------------

//Recycler View ViewHolder------------------------
class AdapterView extends RecyclerView.ViewHolder{

    TextView username,dt,message;

    public AdapterView(View itemView) {
        super(itemView);

        username = (TextView) itemView.findViewById(R.id.user);
        dt = (TextView)itemView.findViewById(R.id.date);
        message = (TextView) itemView.findViewById(R.id.messsage);
    }
}
//------------------------------------------------