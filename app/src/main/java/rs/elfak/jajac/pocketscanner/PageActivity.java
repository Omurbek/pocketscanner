package rs.elfak.jajac.pocketscanner;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.jajac.pocketscanner.R;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class PageActivity extends AppCompatActivity {

    private int mPageIndex;
    private String mPageKey;

    private TextView mTextView;
    private LinearLayout mShareBtn;
    private AlertDialog mSubmitDg;

    private DatabaseReference mDocumentsDb = FirebaseDatabase.getInstance().getReference("documents");
    private GeoFire mDocumentsGeoFire = new GeoFire(FirebaseDatabase.getInstance().getReference("documentsGeoFire"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_page);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mTextView = findViewById(R.id.activity_page_text);
        mShareBtn = findViewById(R.id.activity_page_btn_share);
        mShareBtn.setOnClickListener(v -> onShareClicked());

        Intent callingIntent = getIntent();
        if (callingIntent.hasExtra("page-index")) {
            mPageIndex = callingIntent.getIntExtra("page-index", 0);
            showTranslation();
        } else if (callingIntent.hasExtra("page-key")) {
            mPageKey = callingIntent.getStringExtra("page-key");
            getPageAndShowTranslation();
        }
    }

    private void showTranslation() {
        Page page = DocumentHolder.getInstance().getPage(mPageIndex);
        mTextView.setText(page.getTranslation());
    }

    private void getPageAndShowTranslation() {
        mDocumentsDb.child(mPageKey).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Page page = dataSnapshot.getValue(Page.class);
                mTextView.setText(page.getTranslation());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
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
        int daysRelevant = getDaysRelevant();
        Page page = DocumentHolder.getInstance().getPage(mPageIndex);
        page.setDaysRelevant(daysRelevant);

        String newDocumentKey = mDocumentsDb.push().getKey();
        mDocumentsDb.child(newDocumentKey).setValue(page).addOnSuccessListener(aVoid -> {
            GeoLocation geoLoc = new GeoLocation(page.getLocation().getLatitude(), page.getLocation().getLongitude());
            mDocumentsGeoFire.setLocation(newDocumentKey, geoLoc, (key, error) -> {
                Toast.makeText(PageActivity.this, "Document shared!", Toast.LENGTH_SHORT).show();
            });
        });

        mSubmitDg.dismiss();
    }

    private int getDaysRelevant() {
        RadioGroup radioGroup = mSubmitDg.findViewById(R.id.dialog_share_radio_group);
        int checkedResId = radioGroup.getCheckedRadioButtonId();
        RadioButton radioButton = mSubmitDg.findViewById(checkedResId);

        return Integer.valueOf((String) radioButton.getTag());
    }

}
