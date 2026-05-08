package com.example.heami.ui.doctor;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.heami.R;
import com.example.heami.utils.ExitDialogHelper;

public class DoctorPatientsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_patients);

        DoctorBottomNavManager.setup(this, DoctorBottomNavManager.TAB_PATIENTS);
        ExitDialogHelper.registerExitHandler(this);
    }
}
