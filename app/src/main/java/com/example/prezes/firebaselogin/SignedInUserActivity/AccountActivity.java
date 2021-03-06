package com.example.prezes.firebaselogin.SignedInUserActivity;

import android.accounts.Account;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.prezes.firebaselogin.ChatActivity.ChatActivity;
import com.example.prezes.firebaselogin.SignInActivity.MainActivity;
import com.example.prezes.firebaselogin.R;
import com.example.prezes.firebaselogin.SignedIn.ContactListActivity;
import com.example.prezes.firebaselogin.model.ChatMessage;
import com.firebase.ui.database.FirebaseListAdapter;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;

import static com.example.prezes.firebaselogin.R.layout.activity_account;
import static com.example.prezes.firebaselogin.R.layout.com_facebook_activity_layout;

public class AccountActivity extends AppCompatActivity implements View.OnClickListener,GoogleApiClient.OnConnectionFailedListener {


    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener authStateListener;
    private GoogleApiClient mGoogleApiClient;
    ProgressBar progressBar;
    TextView noUsersText;
    private static final String TAG = "UserList" ;
    private String receiver;
    private String loggedUserId;
    private String sender;
    public ChatActivity ch;


    private ValueEventListener mUserListListener;

    private FirebaseUser currentUser;
    DatabaseReference myRef;
    FirebaseDatabase firebaseDatabase;

    List<String> usernameList = new ArrayList<>();
    Set<String> names = new TreeSet<>();
    private FirebaseListAdapter<ChatMessage> adapter;
    ArrayAdapter arrayAdapter;
    ListView msgListView;
    private FirebaseAuth firebaseAuth;
    private String selectedFromList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(activity_account);

        //Add toolbar
        Toolbar toolbar = (Toolbar)  findViewById(R.id.toolbar);
        toolbar.setTitle("Chat");
        setSupportActionBar(toolbar);

//        receiver = getIntent().getStringExtra("receiver");

        //Add new message button
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.NewMessageButton);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(AccountActivity.this, ContactListActivity.class));
            }
        });

        //calling list view
        msgListView = (ListView) findViewById(R.id.userListView);
        noUsersText = (TextView)findViewById(R.id.noUsersText);
        //Firebase

        initFirebase();

        //getting current logged user
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        loggedUserId = currentUser.getEmail();
        sender = usernameFromEmail(loggedUserId);

        //populating list of users
        addEventFirebaseListener();


        //clicking specific user from list
        onClickListener(msgListView);

        //checking currently logged user
        mAuth = FirebaseAuth.getInstance();
        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {

                if (firebaseAuth.getCurrentUser() == null) {

                    startActivity(new Intent(AccountActivity.this, MainActivity.class));

                    Toast.makeText(getApplicationContext(), "Successfully logged out!", Toast.LENGTH_SHORT).show();

                } else {
                    // User is already signed in. Therefore, display
                    // a welcome Toast


                }
            }
        };

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API)
                .build();



//        if(!usernameList.isEmpty()){
//
//            noUsersText.setVisibility(View.GONE);
//            msgListView.setVisibility(View.VISIBLE);
//        }
//        else{
//            noUsersText.setVisibility(View.VISIBLE);
//            msgListView.setVisibility(View.GONE);
////            msgListView.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, usernameList));
//        }

    }

    private void initFirebase(){
        FirebaseApp.initializeApp(this);
        firebaseDatabase = FirebaseDatabase.getInstance();
        myRef = firebaseDatabase.getReference();

    }

    private void onClickListener(ListView userListView){

        userListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Intent intent = new Intent(AccountActivity.this, ChatActivity.class);

                // selected item
                selectedFromList =(String) (msgListView.getItemAtPosition(position));

                intent.putExtra("text", selectedFromList);

                startActivity(intent);

            }
        });

    }

    private void addEventFirebaseListener(){

        progressBar = (ProgressBar) findViewById(R.id.circular_progress);
        progressBar.setVisibility(View.VISIBLE);


        myRef.child("chats").child(sender).addValueEventListener(new ValueEventListener() {

                @Override
                public void onDataChange (DataSnapshot dataSnapshot){

                HashMap<String, Object> messages = (HashMap<String, Object>) dataSnapshot.getValue();

                if (usernameList.size() > 0) {
                    usernameList.clear();
                }

                try{
                    for (Object user : messages.values()) {
                        HashMap<String, Object> userMap = (HashMap<String, Object>) user;

                        String userName = userMap.get("chatWith").toString();
                        System.out.println("lista " + userName);

                            usernameList.add(userName);

                    }
                } catch (Exception ex){
                    Toast.makeText(getApplicationContext(),"No open chats.", Toast.LENGTH_LONG).show();

                }

                progressBar.setVisibility(View.INVISIBLE);

                arrayAdapter = new ArrayAdapter(AccountActivity.this, android.R.layout.simple_list_item_1, usernameList);
                msgListView.setAdapter(arrayAdapter);
                arrayAdapter.notifyDataSetChanged();

            }


            @Override
            public void onCancelled(DatabaseError databaseError) {
//                Toast.makeText(getApplicationContext(), "onCancelled" + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }




    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu,menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item){
        int id = item.getItemId();
        if(id == R.id.menuId){

            signOut();
            mAuth.signOut();

            return true;
        }

        return true;
    }


    @Override
    protected void onStart() {
        super.onStart();

        mAuth.addAuthStateListener(authStateListener);
        mGoogleApiClient.connect();

    }



    @Override
    public void onClick(View v) {

//        if (v.getId() == R.id.logOutButn) {
//            signOut();
//            mAuth.signOut();
//        }

    }

    private void updateUI(boolean isLogin){


    }

    private String usernameFromEmail(String email) {
        if (email.contains("@")) {
            return email.split("@")[0];
        } else {
            return email;
        }
    }

    private void signOut() {

        // Google sign out
        Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(@NonNull Status status) {

            }
        });
    }


    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(this, "Google Play Services error.", Toast.LENGTH_SHORT).show();
    }


}
