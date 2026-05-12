package com.example.heami.ui.doctor;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.heami.R;
import com.example.heami.utils.ExitDialogHelper;

public class DoctorAppointmentsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_appointments);

        DoctorBottomNavManager.setup(this, DoctorBottomNavManager.TAB_APPOINTMENTS);
        ExitDialogHelper.registerExitHandler(this);
    }
}
