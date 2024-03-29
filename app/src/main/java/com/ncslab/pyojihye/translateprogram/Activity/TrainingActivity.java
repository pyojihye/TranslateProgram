package com.ncslab.pyojihye.translateprogram.Activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.ncslab.pyojihye.translateprogram.Movement.ButtonMenuDataBase;
import com.ncslab.pyojihye.translateprogram.Movement.ButtonTrainingDataBase;
import com.ncslab.pyojihye.translateprogram.Movement.Const;
import com.ncslab.pyojihye.translateprogram.Movement.ModeTextView;
import com.ncslab.pyojihye.translateprogram.Movement.TrainingDataBase;
import com.ncslab.pyojihye.translateprogram.R;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.appindexing.Action;
import com.google.firebase.appindexing.FirebaseAppIndex;
import com.google.firebase.appindexing.FirebaseUserActions;
import com.google.firebase.appindexing.Indexable;
import com.google.firebase.appindexing.builders.Indexables;
import com.google.firebase.appindexing.builders.PersonBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.ncslab.pyojihye.translateprogram.Movement.Const.MESSAGE_URL;
import static com.ncslab.pyojihye.translateprogram.Movement.Const.Max;
import static com.ncslab.pyojihye.translateprogram.Movement.Const.replace;
import static com.ncslab.pyojihye.translateprogram.Movement.Const.screen;
import static com.ncslab.pyojihye.translateprogram.Movement.Const.wpm;

public class TrainingActivity extends AppCompatActivity {

    private final String TAG = "TrainingActivity";
    private final String ANONYMOUS = "ANONYMOUS";
    private final String MESSAGES_CHILD = "Training";
    private final String MESSAGES_BUTTON = "ButtonTraining";
    private final String MESSAGE_MENU="ButtonMenu";


    // Firebase instance variables
    private DatabaseReference mFirebaseDatabaseReference;
    private FirebaseRecyclerAdapter<TrainingDataBase, SelectModeActivity.MessageViewHolder> mFirebaseAdapter;
    private FirebaseRecyclerAdapter<TrainingDataBase, SelectModeActivity.MessageViewHolder> mFirebaseAdapter2;


    // Firebase instance variables
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;

    private String mUsername;
    private String mPhotoUrl;

    private ModeTextView modeTextViewTraining;
    private ImageView imageViewStart;
    private ImageView imageViewRewind;
    private ImageView imageViewForward;
    private TextView numberPercent;
    private boolean startChange = false;

    private int currentPosition;
    private int startPosition;
    private int endPosition;
    public String replaceTextView;
    private int num;
    public double percent;
    private boolean threadStart = false;
    private List<String> origin = new ArrayList<>();
    private TrainingThread trainingThread;
    private boolean change = false;
    public String st;
    public String str;
    private int position;
    private View view;
    StringBuffer buf;

    private boolean firstSet = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
//        Log.d(TAG, "onCreate()");
        if (!screen) {
            setTitle("Training Mode");
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_training_mode);

//            view=(View)findViewById(R.id.imageView);
            modeTextViewTraining = (ModeTextView) findViewById(R.id.ModeTextViewTraining);
            Typeface face = Typeface.createFromAsset(getAssets(), "D2Coding.ttc");
            modeTextViewTraining.setTypeface(face);
            imageViewStart = (ImageView) findViewById(R.id.imageViewStart);
            imageViewRewind = (ImageView) findViewById(R.id.imageViewRewind);
            imageViewForward = (ImageView) findViewById(R.id.imageViewForward);
            imageViewStart.setImageResource(R.drawable.start);
            numberPercent = (TextView) findViewById(R.id.numberPercent);
            modeTextViewTraining.setVisibility(View.INVISIBLE);

            trainingThread = new TrainingThread();

//        Log.d(TAG, "onCreate()");
            // Initialize Firebase Auth
            mFirebaseAuth = FirebaseAuth.getInstance();
            mFirebaseUser = mFirebaseAuth.getCurrentUser();
            if (mFirebaseUser == null) {
                // Not signed in, launch the Sign In activity
                startActivity(new Intent(this, LoginActivity.class));
                finish();
                return;
            } else {
                mUsername = mFirebaseUser.getDisplayName();
                if (mFirebaseUser.getPhotoUrl() != null) {
                    mPhotoUrl = mFirebaseUser.getPhotoUrl().toString();
                }
            }

            // New child entries
            mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();
            mFirebaseAdapter = new FirebaseRecyclerAdapter<TrainingDataBase, SelectModeActivity.MessageViewHolder>(
                    TrainingDataBase.class,
                    R.layout.item_message,
                    SelectModeActivity.MessageViewHolder.class,
                    mFirebaseDatabaseReference.child(MESSAGES_CHILD)) {


                @Override
                protected TrainingDataBase parseSnapshot(DataSnapshot snapshot) {
                    TrainingDataBase DataBase = super.parseSnapshot(snapshot);
                    if (DataBase != null) {
                        DataBase.setId(snapshot.getKey());
                    }
                    return DataBase;
                }


                @Override
                protected void populateViewHolder(SelectModeActivity.MessageViewHolder viewHolder,
                                                  TrainingDataBase DataBase, int position) {
                    viewHolder.messageTextView.setText(DataBase.getUserName());
                    viewHolder.messengerTextView.setText(DataBase.getText());

                    // write this message to the on-device index
                    FirebaseAppIndex.getInstance().update(getMessageIndexable(DataBase));

                    // log a view action on it
                    FirebaseUserActions.getInstance().end(getMessageViewAction(DataBase));
                }
            };
        }
    }

    private Action getMessageViewAction(TrainingDataBase dataBase) {
        return new Action.Builder(Action.Builder.VIEW_ACTION)
                .setObject(dataBase.getUserName(), MESSAGE_URL.concat(dataBase.getId()))
                .setMetadata(new Action.Metadata.Builder().setUpload(false))
                .build();
    }

    private Indexable getMessageIndexable(TrainingDataBase trainingDataBase) {
        PersonBuilder sender = Indexables.personBuilder()
                .setIsSelf(mUsername == trainingDataBase.getUserName())
                .setName(trainingDataBase.getText())
                .setUrl(MESSAGE_URL.concat(trainingDataBase.getId() + "/sender"));

        PersonBuilder recipient = Indexables.personBuilder()
                .setName(mUsername)
                .setUrl(MESSAGE_URL.concat(trainingDataBase.getId() + "/recipient"));

        Indexable messageToIndex = Indexables.messageBuilder()
                .setName(trainingDataBase.getText())
                .setUrl(MESSAGE_URL.concat(trainingDataBase.getId()))
                .setSender(sender)
                .setRecipient(recipient)
                .build();

        return messageToIndex;
    }

    @Override
    protected void onResume() {
//        Log.d(TAG, "onResume()");
        super.onResume();
        if (!screen) {
            replace.clear();
            BufferedReader bufferedReader = null;
            FileInputStream fileInputStream;
            String strPath = null;

            imageViewStart.setImageResource(R.drawable.start);
            modeTextViewTraining.setVisibility(View.INVISIBLE);
            imageViewRewind.setImageResource(0);
            imageViewForward.setImageResource(0);
            imageViewRewind.setClickable(false);
            imageViewForward.setClickable(false);

            try {
                File path = new File(Const.strPath);
                fileInputStream = new FileInputStream(path);

                if (fileInputStream != null) {
                    bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream));
                    buf = new StringBuffer();

                    while ((strPath = bufferedReader.readLine()) != null) {
                        buf.append(strPath + '\n');
                    }

                    int point = 1;

                    replace.clear();
                    origin.clear();

                    for (int i = 1; i < buf.length(); i++) {
                        if (buf.charAt(i) == ' ') {
                            replace.add(buf.substring(point, i + 1));
                            origin.add(buf.substring(point, i + 1));
                            point = i + 1;
                        }
                        if (buf.charAt(i) == '\n') {
                            replace.add(buf.substring(point, i));
                            replace.set(replace.size() - 1, replace.get(replace.size() - 1) + "\n");

                            origin.add(buf.substring(point, i));
                            origin.set(replace.size() - 1, origin.get(origin.size() - 1) + "\n");
                            point = i + 1;
                        }
                    }

                    fileInputStream.close();

                    str = "";
                    int size = 0;

                    if (replace.size() / 150 >= 1) {
                        size = 150;
                    } else {
                        size = replace.size();
                    }

                    for (int i = 0; i < size; i++) {
                        str += replace.get(i);
                    }
                    modeTextViewTraining.setText(str);
                    firstSet = true;

                    long time = System.currentTimeMillis();
                    SimpleDateFormat dayTime = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                    String str2 = dayTime.format(new Date(time));

                    TrainingDataBase dataBase = new TrainingDataBase(mUsername, str2, wpm, buf.toString());
                    mFirebaseDatabaseReference.child(MESSAGES_CHILD).push().setValue(dataBase);
                }
                num = 100;
                handler.sendEmptyMessage(1);
//            DataBase database = new DataBase(strItem,mUsername,mPhotoUrl);
//            mFirebaseDatabaseReference.child(MESSAGES_CHILD)
//                    .push().setValue(database);

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void FirstSet() {
        if (firstSet) {
            int position = 0;
            for (int i = 0; i < origin.size(); i++) {
                if (position + origin.get(i).length() < Max) {
                    position += origin.get(i).length();
                } else {
                    if (origin.get(i).contains(" ")) {
                        position = origin.get(i).length();
                        replace.set(i - 1, origin.get(i - 1) + "\n");
                        origin.set(i - 1, origin.get(i - 1) + "\n");
                    } else if (origin.get(i).contains("\n")) {
                        replace.set(i - 1, origin.get(i - 1) + "\n");
                        origin.set(i - 1, origin.get(i - 1) + "\n");
                        position = 0;
                    }
                }
            }
            firstSet = false;
        }
    }

    public void onStartClick(View v) {
//        Log.d(TAG, "onStartClick()");

//        view = v;

        if (!startChange) {

            long time = System.currentTimeMillis();
            SimpleDateFormat dayTime = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            String str2 = dayTime.format(new Date(time));

            ButtonTrainingDataBase dataBase = new ButtonTrainingDataBase(mUsername, str2, "Start Button(▶)");
            mFirebaseDatabaseReference.child(MESSAGES_BUTTON).push().setValue(dataBase);

            FirstSet();
            str = "";
            int size = 0;

            if (replace.size() / 150 >= 1) {
                size = 150;
            } else {
                size = replace.size();
            }

            for (int i = startPosition; i < size; i++) {
                str += replace.get(i);
            }
            modeTextViewTraining.setText(str);

            modeTextViewTraining.setVisibility(View.VISIBLE);
            startChange = true;
            imageViewStart.setImageResource(R.drawable.pause);
            imageViewRewind.setImageResource(0);
            imageViewForward.setImageResource(0);
            imageViewRewind.setClickable(false);
            imageViewForward.setClickable(false);
            if (threadStart) {
                trainingThread.restart();
            } else {
                trainingThread.start();
            }
        } else {

            long time = System.currentTimeMillis();
            SimpleDateFormat dayTime = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            String str2 = dayTime.format(new Date(time));

            ButtonTrainingDataBase dataBase = new ButtonTrainingDataBase(mUsername, str2, "Pause Button(||)");
            mFirebaseDatabaseReference.child(MESSAGES_BUTTON).push().setValue(dataBase);

            startChange = false;
            change = false;
            imageViewStart.setImageResource(R.drawable.start);
            imageViewRewind.setImageResource(R.drawable.past);
            imageViewForward.setImageResource(R.drawable.future);
            imageViewRewind.setClickable(true);
            imageViewForward.setClickable(true);
        }
    }

    public void onRewindClick(View v) {
//        Log.d(TAG, "onRewindClick()");

        long time = System.currentTimeMillis();
        SimpleDateFormat dayTime = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        String str2 = dayTime.format(new Date(time));

        ButtonTrainingDataBase dataBase = new ButtonTrainingDataBase(mUsername, str2, "Rewind Button(<<)");
        mFirebaseDatabaseReference.child(MESSAGES_BUTTON).push().setValue(dataBase);

        if (currentPosition != 0) {
            if (!change) {
                change = false;
                replace.set(currentPosition - 1, origin.get(currentPosition - 1));
            } else {
                String st = "";
                for (int i = 0; i < currentPosition - 1; i++) {
                    st += replace.get(i);
                }
                if (st.contains("\n")) {
                    startPosition = currentPosition - 1;
                    replace.set(currentPosition - 1, origin.get(currentPosition - 1));
                } else {
                    replace.set(currentPosition - 1, origin.get(currentPosition - 1));
                }
            }

            int start = 0;
            for (int i = 0; i < currentPosition - 1; i++) {
                if (replace.get(i).contains("\n")) {
                    start = i + 1;
                }
            }

            String str = "";
            for (int i = start; i < position; i++) {
                str += replace.get(i);
            }

            currentPosition--;
            replaceTextView = str;
            PercentCalculate();
            handler.sendEmptyMessage(0);
        } else {
            Snackbar snackbar = Snackbar.make(findViewById(R.id.numberPercent), R.string.snack_bar_training, Snackbar.LENGTH_LONG);

            View view = snackbar.getView();
            TextView textView = (TextView) view.findViewById(android.support.design.R.id.snackbar_text);
            textView.setTextColor(Color.WHITE);
            snackbar.show();
        }
    }

    public void onForwardClick(View v) {
//        Log.d(TAG, "onForwardClick()");

        long time = System.currentTimeMillis();
        SimpleDateFormat dayTime = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        String str2 = dayTime.format(new Date(time));

        ButtonTrainingDataBase dataBase = new ButtonTrainingDataBase(mUsername, str2, "Forward Button(>>)");
        mFirebaseDatabaseReference.child(MESSAGES_BUTTON).push().setValue(dataBase);

        change = true;
        Training();
    }

    private void TrainingInit() {
//        Log.d(TAG, "TrainingInit()");

        currentPosition = 0;
        startPosition = 0;
        endPosition = replace.size();
        position = 0;

//        Log.v("Word count: ", endPosition + "");
    }

    private void Training() {
//        Log.d(TAG, "Training()");
//        //전원버튼
//        IntentFilter offfilter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
//        registerReceiver(screenoff, offfilter);
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);

        if (currentPosition % 75 == 0) {
            if (position == 0) {
                if ((replace.size() - position) / 150 >= 1) {
                    position = 150;
                } else {
                    position = replace.size();
                }
            } else if ((replace.size() - position) / 150 >= 1) {
                position += 150;

                if (currentPosition % 150 == 0) {
                    position -= 150;
                }
            } else {
                position = replace.size();
            }
        }

        if (!pm.isScreenOn()) {
//            onStartClick(view);
            trainingThread.pause();
            screen = true;
            imageViewStart.setImageResource(R.drawable.start);
            imageViewRewind.setImageResource(R.drawable.past);
            imageViewForward.setImageResource(R.drawable.future);
            imageViewRewind.setClickable(true);
            imageViewForward.setClickable(true);
        } else {
            if (currentPosition < endPosition) {
                String text = "";

                String spacebar = "";
                text = replace.get(currentPosition);

                for (int i = 0; i < text.length(); i++) {
                    if (text.substring(i, text.length()).startsWith("\n")) {
                        spacebar = "\n";
                    }
                    if (text.substring(i, text.length()).equals("\n") && !text.equals("\n")) {
                        spacebar = spacebar + "\n";
                        replace.set(currentPosition, spacebar);
                        startPosition = currentPosition + 1;
                        break;
                    } else if (Character.getType(text.toCharArray()[i]) == 5) {
                        spacebar = spacebar + "  ";
                        replace.set(currentPosition, spacebar);
                    } else {
                        spacebar = spacebar + " ";
                        replace.set(currentPosition, spacebar);
                    }
                }

                int start = 0;
                for (int i = 0; i <= currentPosition; i++) {
                    if (replace.get(i).startsWith("\n")) {
                        start = i;
                    }
                    if (replace.get(i).endsWith("\n")) {
                        start = i + 1;
                    }
                }

                String str = "";
                for (int i = start; i < position; i++) {
                    str += replace.get(i);
                }
                if (str.startsWith("\n")) {
                    str = str.substring(1, str.length());
                }
                replaceTextView = str;

                handler.sendEmptyMessage(0);
                PercentCalculate();
                currentPosition++;

//            Log.v("startPosition :", startPosition + "");
//            Log.v("endPosition :", endPosition + "");
            } else {
                PercentCalculate();
                startChange = true;
                trainingThread.restart();
            }
        }
    }

    private void PercentCalculate() {
        percent = ((double) currentPosition / (double) endPosition) * 100;
        if (percent < 99 && startChange) {
            percent++;
        }
        num = 100 - (int) percent;
//        Log.v("num: ", num + "");
        handler.sendEmptyMessage(1);
    }


    class TrainingThread extends Thread {
        private boolean pause = false;

        void pause() {
            pause = true;
        }

        synchronized void restart() {
            notify();
            pause = false;
//            Log.d(TAG, "restart()");
        }

        @Override
        public void run() {
            threadStart = true;
            TrainingInit();
//            Log.d(TAG, "run()");

            while (true) {
                try {
                    if (startChange) {
                        sleep(60000 / wpm);
                        Training();
                        if (percent >= 100) {
                            pause();
                            handler.sendEmptyMessage(2);
                        }
                        synchronized (this) {
                            if (pause) {
                                wait();
                            }
                        }
                    } else {

                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    st = "";
                    for (int i = 0; i < Max; i++) {
                        st += " ";
                    }

                    if (replaceTextView.contains(st)) {
                        replaceTextView = replaceTextView.replaceAll(st, "");
//                        startPosition=currentPosition;
                    }
                    modeTextViewTraining.setText(replaceTextView);

                    break;
                case 1:
                    numberPercent.setText(num + "%");
                    break;
                case 2:
                    startChange = false;
                    AlertDialog.Builder d = new AlertDialog.Builder(TrainingActivity.this);
                    d.setTitle(getString(R.string.dialog_restart));
                    d.setMessage(getString(R.string.dialog_contents_restart));
                    d.setIcon(R.mipmap.ic_launcher);

                    d.setPositiveButton(getString(R.string.dialog_yes), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // TODO Auto-generated method stub
                            screen = false;
                            TrainingInit();
                            replace.clear();
                            origin.clear();
                            onResume();
                        }
                    });

                    d.setNegativeButton(getString(R.string.dialog_no), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // TODO Auto-generated method stub
                            finish();
                        }
                    });
                    d.show();
                    break;
            }
        }
    };

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
//        Log.d(TAG, "onKeyDown()");

        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            if (trainingThread.isAlive()) {
                trainingThread.pause();
                trainingThread.interrupt();
            }
            if (screenoff.isOrderedBroadcast()) {
                unregisterBroadcast();
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.sign_out_menu:
                long time = System.currentTimeMillis();
                SimpleDateFormat dayTime = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                String str = dayTime.format(new Date(time));

                ButtonMenuDataBase dataBase = new ButtonMenuDataBase(mUsername, str, "Sign Out", TAG);
                mFirebaseDatabaseReference.child(MESSAGE_MENU).push().setValue(dataBase);

                mFirebaseAuth.signOut();
                mUsername = ANONYMOUS;
                startActivity(new Intent(this, LoginActivity.class));
                return true;
            case R.id.developer:
                long time2 = System.currentTimeMillis();
                SimpleDateFormat dayTime2 = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                String str2 = dayTime2.format(new Date(time2));

                ButtonMenuDataBase dataBase2 = new ButtonMenuDataBase(mUsername, str2, "Developer Info", TAG);
                mFirebaseDatabaseReference.child(MESSAGE_MENU).push().setValue(dataBase2);
                Intent intent = new Intent(getApplicationContext(), DeveloperActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    //전원버튼
    private void unregisterBroadcast() {
        unregisterReceiver(screenoff);
    }

    BroadcastReceiver screenoff = new BroadcastReceiver() {
        public static final String Screenoff = "android.intent.action.SCREEN_OFF";

        @Override
        public void onReceive(Context context, Intent intent) {
//            if (intent.getAction().equals(Screenoff)) {
//                if (trainingThread.isAlive()) {
//                    startChange = false;
//                    change = false;
//                    imageViewStart.setImageResource(R.drawable.firstSet);
//                    imageViewRewind.setImageResource(R.drawable.Rewind);
//                    imageViewForward.setImageResource(R.drawable.Forward);
//                    imageViewRewind.setClickable(true);
//                    imageViewForward.setClickable(true);
//                }
//            }
            if (!intent.getAction().equals(Screenoff))
                return;
//            Log.e(TAG, "Screen off!!!!!!!");
        }
    };
}