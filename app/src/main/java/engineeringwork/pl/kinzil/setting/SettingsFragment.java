package engineeringwork.pl.kinzil.setting;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import engineeringwork.pl.kinzil.R;
import engineeringwork.pl.kinzil.activity.MainActivity;
import engineeringwork.pl.kinzil.containers.DatabaseHelper;
import engineeringwork.pl.kinzil.containers.Setting;

public class SettingsFragment extends Fragment {
    private View view;
    private DatabaseHelper db;
    private TextView wheelSizeTxt;
    private TextView weightTxt;
    private Button editButton;
    private Setting setting;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.settings_fragment, container, false);

        db = DatabaseHelper.getInstance(getActivity());

        wheelSizeTxt = (TextView) view.findViewById(R.id.wheelSize_textView);
        weightTxt = (TextView) view.findViewById(R.id.weight_textView);
        editButton = (Button) view.findViewById(R.id.edit_Button);
        editButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                openDialog(setting);
            }
        });

        setting = db.getFirstLoginSetting(((MainActivity)getActivity()).getLogin());
        if(setting == null)
            setting = new Setting(((MainActivity)getActivity()).getLogin(), 0, 0);

        setData();
        return view;
    }

    private void setData()
    {
        wheelSizeTxt.setText("Wheel size: " + setting.getWheelSize() + "mm");
        weightTxt.setText("Your weight: " + setting.getWeight() + "kg");
    }

    private void openDialog(final Setting setting)
    {
        final Dialog login = new Dialog(getActivity(), R.style.AlertDialogCustom);
        login.setContentView(R.layout.dialog_edit_settings);
        login.setTitle("Edit");
        Button btnLogin = (Button) login.findViewById(R.id.btn_create);
        Button btnCancel = (Button) login.findViewById(R.id.btn_cancel);
        final EditText txtWheelSize = (EditText)login.findViewById(R.id.txt_wheel_size);
        final EditText txtWeight = (EditText)login.findViewById(R.id.txt_weight);
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!txtWheelSize.getText().toString().equals(""))
                    setting.setWheelSize(Integer.parseInt(txtWheelSize.getText().toString()));
                if(!txtWeight.getText().toString().equals(""))
                    setting.setWeight(Integer.parseInt(txtWeight.getText().toString()));
                setData();
                Setting tmp = db.getFirstLoginSetting(setting.getLogin());
                if(tmp == null) {
                    if (db.settingInsert(setting))
                        Toast.makeText(getContext(), "Data save", Toast.LENGTH_LONG).show();
                    else
                        Toast.makeText(getContext(), "Error, data not save", Toast.LENGTH_LONG).show();
                }
                else {
                    setting.setId(tmp.getId());
                    if (db.settingUpdate(setting))
                        Toast.makeText(getContext(), "Data update", Toast.LENGTH_LONG).show();
                    else
                        Toast.makeText(getContext(), "Error, data not update", Toast.LENGTH_LONG).show();
                }
                login.dismiss();
            }
        });
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                login.dismiss();
            }
        });
        login.show();
    }
}
