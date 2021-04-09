package com.example.videocall;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.opentok.android.OpentokError;
import com.opentok.android.Publisher;
import com.opentok.android.PublisherKit;
import com.opentok.android.Session;
import com.opentok.android.Stream;
import com.opentok.android.Subscriber;

import java.security.PublicKey;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class VideoChatActivity extends AppCompatActivity implements Session.SessionListener
, PublisherKit.PublisherListener {


    private static String API_KEY = "46760612";
    private static String SESSION_ID ="1_MX40Njc2MDYxMn5-MTU5MDM5MjA0NzI2MH5oclZWQ241UTJHUFF6QkM1cVBnZkFSNkR-fg";
    private static String TOKEN = "T1==cGFydG5lcl9pZD00Njc2MDYxMiZzaWc9MDAwNGE3MDdhZGM4MTExMDIzZDhiOWRiODA0NGY1NjRkOWNiYzM1YTpzZXNzaW9uX2lkPTFfTVg0ME5qYzJNRFl4TW41LU1UVTVNRE01TWpBME56STJNSDVvY2xaV1EyNDFVVEpIVUZGNlFrTTFjVkJuWmtGU05rUi1mZyZjcmVhdGVfdGltZT0xNTkwMzkyMTAxJm5vbmNlPTAuNjAwMDI2MDE3NjIxNTkxNSZyb2xlPXB1Ymxpc2hlciZleHBpcmVfdGltZT0xNTkyOTg0MTAwJmluaXRpYWxfbGF5b3V0X2NsYXNzX2xpc3Q9";
    private static final String LOG_TAG = VideoChatActivity.class.getSimpleName();
    private static final int RC_VIDEO_APP_PERM = 124;


    private FrameLayout mPublisherViewController;
    private FrameLayout mSubscriberViewController;
    private Session mSession;
    private Publisher mPublisher;
    private Subscriber mSubscriber;

    private ImageView closeVideoChatBtn;
    private DatabaseReference userRef;
    private String userId = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_chat);

        userRef = FirebaseDatabase.getInstance().getReference().child("user");
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        requestPermissions();

        closeVideoChatBtn = findViewById(R.id.close_video_chat_btn);
        closeVideoChatBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                userRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if(dataSnapshot.child(userId).hasChild("Ringing")){
                            userRef.child(userId).child("Ringing")
                                    .removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (mPublisher != null){
                                        mPublisher.destroy();
                                    }

                                    if (mSubscriber != null){
                                        mSubscriber.destroy();
                                    }

                                    Intent intent = new Intent(VideoChatActivity.this,Registration.class);
                                    startActivity(intent);
                                    finish();
                                }
                            });

                        }

                        if(dataSnapshot.child(userId).hasChild("Calling")){
                            userRef.child(userId).child("Calling")
                                    .removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (mPublisher != null){
                                        mPublisher.destroy();
                                    }

                                    if (mSubscriber != null){
                                        mSubscriber.destroy();
                                    }

                                    Intent intent = new Intent(VideoChatActivity.this,Registration.class);
                                    startActivity(intent);
                                    finish();
                                }
                            });


                        }

                        else {

                            if (mPublisher != null){
                                mPublisher.destroy();
                            }

                            if (mSubscriber != null){
                                mSubscriber.destroy();
                            }

                            Intent intent = new Intent(VideoChatActivity.this,Registration.class);
                            startActivity(intent);
                            finish();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }
        });


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        EasyPermissions.onRequestPermissionsResult(requestCode,permissions,grantResults,VideoChatActivity.this);
    }

    @AfterPermissionGranted(RC_VIDEO_APP_PERM)
    private void requestPermissions(){
        String[] perms = {Manifest.permission.INTERNET, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};

        if(EasyPermissions.hasPermissions(this,perms)){
            mPublisherViewController = findViewById(R.id.publisher_container);
            mSubscriberViewController = findViewById(R.id.subscriber_container);

            //initialize and connect to the session
            mSession = new Session.Builder(this,API_KEY,SESSION_ID).build();
            mSession.setSessionListener(VideoChatActivity.this);
            mSession.connect(TOKEN);
        }else {
            EasyPermissions.requestPermissions(this,"this app needs the mike and camera permission ,please allow",RC_VIDEO_APP_PERM);
        }
    }



    @Override
    public void onStreamCreated(PublisherKit publisherKit, Stream stream) {

    }

    @Override
    public void onStreamDestroyed(PublisherKit publisherKit, Stream stream) {

    }

    @Override
    public void onError(PublisherKit publisherKit, OpentokError opentokError) {

    }

    @Override
    public void onConnected(Session session) {

        //to publish a stream to the session
        Log.i(LOG_TAG,"Session Connected");
        mPublisher = new Publisher.Builder(this).build();
        mPublisher.setPublisherListener(VideoChatActivity.this);

        mPublisherViewController.addView(mPublisher.getView());

        if(mPublisher.getView() instanceof GLSurfaceView){
            ((GLSurfaceView) mPublisher.getView()).setZOrderOnTop(true);
        }

        mSession.publish(mPublisher);
    }

    @Override
    public void onDisconnected(Session session) {

        Log.i(LOG_TAG,"Stream disconnected");
    }


    //subscribing to the stream (receive stream)
    @Override
    public void onStreamReceived(Session session, Stream stream) {
        Log.i(LOG_TAG,"Strem Received");
        if(mSubscriber == null){
            mSubscriber = new Subscriber.Builder(this,stream).build();
            mSession.subscribe(mSubscriber);
            mSubscriberViewController.addView(mSubscriber.getView());
        }
    }

    @Override
    public void onStreamDropped(Session session, Stream stream) {

        Log.i(LOG_TAG,"Stream dropped");

        if(mSubscriber!=null){
            mSubscriber = null;
            mSubscriberViewController.removeAllViews();
        }
    }

    @Override
    public void onError(Session session, OpentokError opentokError) {

        Log.i(LOG_TAG,"Stream error");
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {


    }
}
