package com.example.videocall;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

public class ProfileActivity extends AppCompatActivity {


    private String receiverUserId="",receiverUserImage="",receiverUserName="";
    private ImageView backgroundProfileView;
    private TextView profileName;
    private Button addFriend,declineFriendRequest;
    private FirebaseAuth mAuth;
    private String senderUserId ;
    private String currentState = "new";
    private DatabaseReference friendRequestRef,contactsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        receiverUserId = getIntent().getExtras().get("visit_user_id").toString();
        receiverUserImage = getIntent().getExtras().get("profile_image").toString();
        receiverUserName = getIntent().getExtras().get("profile_name").toString();

        backgroundProfileView = findViewById(R.id.background_profile_view);
        profileName = findViewById(R.id.name_profile);
        addFriend = findViewById(R.id.add_friend);
        declineFriendRequest = findViewById(R.id.decline_friend_request);

        Picasso.get().load(receiverUserImage).fit().centerCrop().error(R.drawable.profile_image).into(backgroundProfileView);
        profileName.setText(receiverUserName);

        mAuth = FirebaseAuth.getInstance();
        senderUserId = mAuth.getCurrentUser().getUid();

        friendRequestRef = FirebaseDatabase.getInstance().getReference().child("Friend Requests");
        contactsRef = FirebaseDatabase.getInstance().getReference().child("Contacts");

        manageClickEvents();
    }

    private void manageClickEvents() {

        friendRequestRef.child(senderUserId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if(dataSnapshot.hasChild(receiverUserId)){
                            String requestType = dataSnapshot.child(receiverUserId).child("request_type").getValue().toString();

                            if(requestType.equals("sent")){
                                currentState = "request_sent";
                                addFriend.setText("Cancel Friend Request");
                            }else if(requestType.equals("received")){
                                currentState = "request_received";
                                addFriend.setText("Accept Friend Request");

                                declineFriendRequest.setVisibility(View.VISIBLE);
                                declineFriendRequest.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        cancelFriendRequest();
                                    }
                                });
                            }
                        }else{

                            contactsRef.child(senderUserId)
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot dataSnapshot) {
                                            if(dataSnapshot.hasChild(receiverUserId)){
                                                //contact saved both are friends
                                                currentState = "friends";
                                                addFriend.setText("Delete Contact");
                                            }else{
                                                currentState = "new";
                                            }
                                        }

                                        @Override
                                        public void onCancelled(DatabaseError databaseError) {

                                        }
                                    });

                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

        if(senderUserId.equals(receiverUserId)){
            addFriend.setVisibility(View.GONE);
        }else{
            addFriend.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(currentState.equals("new")){
                        sendFriendRequest();
                    }
                    if(currentState.equals("request_sent")){
                        cancelFriendRequest();
                    }
                    if(currentState.equals("request_received")){
                        acceptFriendRequest();
                    }
                    if(currentState.equals("request_sent")){
                        // already both are friends
                        cancelFriendRequest();
                    }
                }
            });
        }
    }

    private void acceptFriendRequest() {

        contactsRef.child(senderUserId).child(receiverUserId)
                .child("Contact").setValue("Saved")
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {

                        if(task.isSuccessful()){

                            contactsRef.child(receiverUserId).child(senderUserId)
                                    .child("Contact").setValue("Saved")
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {

                                            if(task.isSuccessful()){
                                                friendRequestRef.child(senderUserId).child(receiverUserId)
                                                        .removeValue()
                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                if(task.isSuccessful()){
                                                                    friendRequestRef.child(receiverUserId).child(senderUserId)
                                                                            .removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                        @Override
                                                                        public void onComplete(@NonNull Task<Void> task) {
                                                                            if(task.isSuccessful()){
                                                                                currentState = "friends";
                                                                                addFriend.setText("Delete Contact");
                                                                                declineFriendRequest.setVisibility(View.VISIBLE);
                                                                            }
                                                                        }
                                                                    });
                                                                }
                                                            }
                                                        });
                                            }
                                        }
                                    });

                        }

                    }
                });

    }

    private void cancelFriendRequest() {

        friendRequestRef.child(senderUserId).child(receiverUserId)
                .removeValue()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()){
                            friendRequestRef.child(receiverUserId).child(senderUserId)
                                    .removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if(task.isSuccessful()){
                                        currentState = "new";
                                        addFriend.setText("Add Friend");
                                    }
                                }
                            });
                        }
                    }
                });

    }


    private void sendFriendRequest() {

        friendRequestRef.child(senderUserId).child(receiverUserId)
                .child("request_type").setValue("sent")
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()){
                            friendRequestRef.child(receiverUserId).child(senderUserId)
                                    .child("request_type").setValue("received")
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if(task.isSuccessful()){
                                                currentState = "request_sent";
                                                addFriend.setText("Cancel Friend Request");
                                                Toast.makeText(ProfileActivity.this, "friend request sent", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                        }
                    }
                });


    }


}
