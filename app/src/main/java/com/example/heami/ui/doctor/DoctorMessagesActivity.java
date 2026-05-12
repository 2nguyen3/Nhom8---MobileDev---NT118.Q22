package com.example.heami.ui.doctor;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.example.heami.R;

public class DoctorMessagesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_messages);

        DoctorBottomNavManager.setup(this, DoctorBottomNavManager.TAB_MESSAGES);
        com.example.heami.utils.ExitDialogHelper.registerExitHandler(this);

        CardView cardChatItem1 = findViewById(R.id.cardChatItem1);
        if (cardChatItem1 != null) {
            cardChatItem1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(DoctorMessagesActivity.this, DoctorChatDetailActivity.class);
                    startActivity(intent);
                }
            });
        }
    }
}
