package rs.elfak.jajac.pocketscanner;

import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.example.jajac.pocketscanner.R;

public class PageActivity extends AppCompatActivity {

    private int mPageIndex;

    private TextView mTextView;
    private LinearLayout mShareBtn;
    private AlertDialog mSubmitDg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_page);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mTextView = findViewById(R.id.activity_page_text);
        mShareBtn = findViewById(R.id.activity_page_btn_share);
        mShareBtn.setOnClickListener(v -> onShareClicked());

        mPageIndex = getIntent().getIntExtra("page", 0);

        showTranslation();
    }

    private void showTranslation() {
        Page page = DocumentHolder.getInstance().getPage(mPageIndex);

        mTextView.setText(page.getTranslation() + "\n\n" + page.getTranslation() + "\n\n" + page.getTranslation());
    }

    private void onShareClicked() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setView(R.layout.dialog_share);

        mSubmitDg = dialogBuilder.create();

        mSubmitDg.setOnShowListener(dialog -> {
            mSubmitDg.findViewById(R.id.dialog_share_cancel_btn).setOnClickListener(v -> mSubmitDg.dismiss());
            mSubmitDg.findViewById(R.id.dialog_share_submit_btn).setOnClickListener(v -> onSubmitClicked());
        });
        mSubmitDg.show();
    }

    private void onSubmitClicked() {
        RadioGroup radioGroup = mSubmitDg.findViewById(R.id.dialog_share_radio_group);
        int checkedResId = radioGroup.getCheckedRadioButtonId();
        RadioButton radioButton = mSubmitDg.findViewById(checkedResId);

        int durationInDays = (int) radioButton.getTag();

        mSubmitDg.dismiss();
    }

}
