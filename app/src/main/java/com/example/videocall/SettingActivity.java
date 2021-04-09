package com.example.videocall;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.util.HashMap;

public class SettingActivity extends AppCompatActivity {


    private Button saveBtn ;
    private EditText usernameET,userBioET;
    private ImageView profileImageView;
    private static int galleryPick =1;
    private Uri imageUri;
    private StorageReference userProfileImageReference;
    private String downloadUrl;
    private DatabaseReference userRef;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        saveBtn = findViewById(R.id.save_settings_btn);
        usernameET = findViewById(R.id.username_settings);
        userBioET = findViewById(R.id.bio_settings);
        profileImageView = findViewById(R.id.settings_profile_image);
        userProfileImageReference = FirebaseStorage.getInstance().getReference().child("Profile Images");
        userRef = FirebaseDatabase.getInstance().getReference().child("user");
        progressDialog = new ProgressDialog(this);


        profileImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent galleryIntent = new Intent();
                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
                galleryIntent.setType("image/*");
                startActivityForResult(galleryIntent,galleryPick);
            }
        });

        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveUserData();
            }
        });

        retrieveUserInfo();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == galleryPick && resultCode == RESULT_OK && data!=null){
            imageUri = data.getData();
            profileImageView.setImageURI(imageUri);
        }
    }

    private void saveUserData() {
        final String getUserName = usernameET.getText().toString();
        final String getUserSatus = userBioET.getText().toString();

        if(imageUri == null){
            userRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if(dataSnapshot.child(FirebaseAuth.getInstance().getCurrentUser().getUid()).hasChild("image")){
                        // user has an old image
                        saveInfoOnlyWithoutImage();

                    }else{
                        Toast.makeText(SettingActivity.this, "please select image first", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }else if(getUserName.equals("")){
            Toast.makeText(this, "UserName is mandatory", Toast.LENGTH_SHORT).show();
        }else if(getUserSatus.equals("")){
            Toast.makeText(this, "bio is mandatory", Toast.LENGTH_SHORT).show();
        }else{
            progressDialog.setTitle("Account Settings");
            progressDialog.setMessage("Please wait...");
            progressDialog.show();
            final StorageReference filePath = userProfileImageReference
                    .child(FirebaseAuth.getInstance().getCurrentUser().getUid());
            final UploadTask uploadTask = filePath.putFile(imageUri);

            uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if(!task.isSuccessful()){
                        throw task.getException();
                    }
                    downloadUrl = filePath.getDownloadUrl().toString();
                    return filePath.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if(task.isSuccessful()){
                        downloadUrl = task.getResult().toString();

                        HashMap<String,Object> profileMap = new HashMap<>();
                        profileMap.put("uid",FirebaseAuth.getInstance().getCurrentUser().getUid());
                        profileMap.put("name",getUserName);
                        profileMap.put("status",getUserSatus);
                        profileMap.put("image",downloadUrl);

                        userRef.child(FirebaseAuth.getInstance().getCurrentUser().getUid()).updateChildren(profileMap)
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if(task.isSuccessful()){
                                            Toast.makeText(SettingActivity.this, "profile settings has been updated successfully", Toast.LENGTH_SHORT).show();
                                            Intent intent = new Intent(SettingActivity.this,ContactsActivity.class);
                                            startActivity(intent);
                                            finish();
                                            progressDialog.dismiss();
                                        }else{
                                            Toast.makeText(SettingActivity.this, "error in updating profile, please try again", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                    }
                }
            });
        }
    }

    private void saveInfoOnlyWithoutImage() {
        final String getUserName = usernameET.getText().toString();
        final String getUserSatus = userBioET.getText().toString();



        if(getUserName.equals("")){
            Toast.makeText(this, "UserName is mandatory", Toast.LENGTH_SHORT).show();
        }else if(getUserSatus.equals("")){
            Toast.makeText(this, "bio is mandatory", Toast.LENGTH_SHORT).show();
        }else{

            progressDialog.setTitle("Account Settings");
            progressDialog.setMessage("Please wait...");
            progressDialog.show();

            HashMap<String,Object> profileMap = new HashMap<>();
            profileMap.put("uid",FirebaseAuth.getInstance().getCurrentUser().getUid());
            profileMap.put("name",getUserName);
            profileMap.put("status",getUserSatus);

        userRef.child(FirebaseAuth.getInstance().getCurrentUser().getUid()).updateChildren(profileMap)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()){
                            Toast.makeText(SettingActivity.this, "profile settings has been updated successfully", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(SettingActivity.this,ContactsActivity.class);
                            startActivity(intent);
                            finish();
                            progressDialog.dismiss();
                        }else{
                            Toast.makeText(SettingActivity.this, "error in updating profile, please try again", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        }
    }

    private void retrieveUserInfo(){
        userRef.child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if(dataSnapshot.exists()){
                            String imageFromDb = dataSnapshot.child("image").getValue().toString();
                            String userNameFromDb = dataSnapshot.child("name").getValue().toString();
                            String userStatusFromDb = dataSnapshot.child("status").getValue().toString();

                            usernameET.setText(userNameFromDb);
                            userBioET.setText(userStatusFromDb);
                            Picasso.get().load(imageFromDb).placeholder(R.drawable.profile_image).fit().centerCrop().error(R.drawable.profile_image).into(profileImageView);//also displays default image if internet is not available
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
    }
}
