package com.example.sunchan.goodsaying;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

public class InputActivity extends AppCompatActivity {
    private static final String TAG = "MYD - InputAct";

    private EditText edit_text;
    private Toolbar mToolBar;

    private String mUserInput = "";

    private InputMethodManager imm;

    private DBHandler db;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new DBHandler(this);
        setContentView(com.example.sunchan.goodsaying.R.layout.input_layout);
        ScreenInsets.apply(this);

        setupViews();

        edit_text.requestFocus();

        //키보드 보이게 하는 부분
        imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);


    }

    public void onButtonClick(View view) {
        // 버튼 ID를 가져온다.
        switch (view.getId()) {
            case com.example.sunchan.goodsaying.R.id.btn_save:         // 저장 버튼 설정
                insertItem();
                //키보드를 없앤다.
                imm.hideSoftInputFromWindow(edit_text.getWindowToken(),0);

                break;
        }
    }


    private void setupViews() {
        edit_text = (EditText) findViewById(com.example.sunchan.goodsaying.R.id.edit_text);

        mToolBar = (Toolbar) findViewById(com.example.sunchan.goodsaying.R.id.sub_toolbar);
        setSupportActionBar(mToolBar);
        //mToolBar.setNavigationIcon(R.drawable.ic_chevron_left_black_24dp);

        getSupportActionBar().setTitle(com.example.sunchan.goodsaying.R.string.insert_title);
    }

    public void insertItem() {
        // check user input
        mUserInput = edit_text.getText().toString();

        if (mUserInput == null || mUserInput.length() == 0) {
            Toast.makeText(this, com.example.sunchan.goodsaying.R.string.warn_text, Toast.LENGTH_SHORT).show();
            edit_text.requestFocus();       // focus on
            return;
        }

        try {
            db.insertItem(mUserInput);
            Toast.makeText(this, R.string.insert_ok, Toast.LENGTH_SHORT).show();
            finish();
        } catch (android.database.SQLException error) {
            Toast.makeText(this, R.string.insert_fail, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        db.close();
        super.onDestroy();
    }
}
