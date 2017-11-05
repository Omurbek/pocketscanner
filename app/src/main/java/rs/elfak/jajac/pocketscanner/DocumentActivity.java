package rs.elfak.jajac.pocketscanner;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
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

public class DocumentActivity extends AppCompatActivity {

    private int documentIndex;
    private String documentKey;

    private TextView translationText;
    private LinearLayout shareBtn;
    private AlertDialog submitDialog;

    private DatabaseReference documentsDb = FirebaseDatabase.getInstance().getReference("documents");
    private GeoFire documentsGeoFire = new GeoFire(FirebaseDatabase.getInstance().getReference("documentsGeoFire"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_page);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        translationText = findViewById(R.id.activity_page_text);
        shareBtn = findViewById(R.id.activity_page_btn_share);
        shareBtn.setOnClickListener(v -> onShareClicked());

        Intent callingIntent = getIntent();
        if (callingIntent.hasExtra("document-index")) {
            documentIndex = callingIntent.getIntExtra("document-index", 0);
            showTranslation();
        } else if (callingIntent.hasExtra("document-key")) {
            findViewById(R.id.activity_page_options_bar).setVisibility(View.GONE);
            documentKey = callingIntent.getStringExtra("document-key");
            getDocumentAndShowTranslation();
        }
    }

    private void showTranslation() {
        Document doc = DocumentsHolder.getInstance().getDocumentAt(documentIndex);
        setTranslationText(doc.getTranslation());
    }

    private void getDocumentAndShowTranslation() {
        documentsDb.child(documentKey).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Document doc = dataSnapshot.getValue(Document.class);
                setTranslationText(doc.getTranslation());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void setTranslationText(String text) {
        translationText.setText(text);
    }

    private void onShareClicked() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setView(R.layout.dialog_share);

        submitDialog = dialogBuilder.create();

        submitDialog.setOnShowListener(dialog -> {
            submitDialog.findViewById(R.id.dialog_share_cancel_btn).setOnClickListener(v -> submitDialog.dismiss());
            submitDialog.findViewById(R.id.dialog_share_submit_btn).setOnClickListener(v -> onSubmitClicked());
        });
        submitDialog.show();
    }

    private void onSubmitClicked() {
        int daysRelevant = getDaysRelevant();
        DocumentType docType = getDocumentType();

        Document doc = DocumentsHolder.getInstance().getDocumentAt(documentIndex);
        doc.setDaysRelevant(daysRelevant);
        doc.setType(docType);

        String newDocumentKey = documentsDb.push().getKey();
        documentsDb.child(newDocumentKey).setValue(doc).addOnSuccessListener(aVoid -> {
            GeoLocation geoLoc = new GeoLocation(doc.getLocation().getLat(), doc.getLocation().getLon());
            documentsGeoFire.setLocation(newDocumentKey, geoLoc, (key, error) -> {
                Toast.makeText(DocumentActivity.this, "Document shared!", Toast.LENGTH_SHORT).show();
            });
        });

        submitDialog.dismiss();
    }

    private int getDaysRelevant() {
        RadioGroup radioGroup = submitDialog.findViewById(R.id.dialog_share_radio_group);
        int checkedResId = radioGroup.getCheckedRadioButtonId();
        RadioButton radioButton = submitDialog.findViewById(checkedResId);

        return Integer.valueOf((String) radioButton.getTag());
    }

    private DocumentType getDocumentType() {
        RadioGroup radioGroup = submitDialog.findViewById(R.id.dialog_type_radio_group);
        int checkedResId = radioGroup.getCheckedRadioButtonId();
        RadioButton radioButton = submitDialog.findViewById(checkedResId);

        return DocumentType.values()[Integer.valueOf((String) radioButton.getTag())];
    }

}
