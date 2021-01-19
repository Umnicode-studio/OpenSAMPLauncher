package com.umnicode.samp_launcher.ui.settings

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.umnicode.samp_launcher.LauncherApplication
import com.umnicode.samp_launcher.R
import com.umnicode.samp_launcher.ui.home.PortFilter

class SettingsFragment : Fragment() {
    private lateinit var launcherApplication: LauncherApplication;

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceStatus: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_settings, container, false)
        launcherApplication = activity?.application as LauncherApplication;

        // Set nickname edit text and bind focus losing
        val nicknameEditText: EditText = root.findViewById(R.id.nickname_edit);
        nicknameEditText.setText(launcherApplication.userConfig.Nickname);

        nicknameEditText.onFocusChangeListener = object : View.OnFocusChangeListener {
            override fun onFocusChange(v: View?, hasFocus: Boolean) {
                if (!hasFocus){
                    val str:String = nicknameEditText.text.toString();
                    if (str.isNotEmpty()) {
                        launcherApplication.userConfig.Nickname = str;
                        launcherApplication.userConfig.Save();
                    }else{
                        nicknameEditText.setText(launcherApplication.userConfig.Nickname);
                    }
                }
            }
        };
        // Set ping timeout text and bind focus lose to userConfig.save()
        val pingTimeoutEditText: EditText = root.findViewById(R.id.ping_timeout_edit);
        pingTimeoutEditText.setText(launcherApplication.userConfig.PingTimeout.toString());
        pingTimeoutEditText.filters = Array(1) {PingTimeoutFilter()};

        pingTimeoutEditText.onFocusChangeListener = object : View.OnFocusChangeListener {
            override fun onFocusChange(v: View?, hasFocus: Boolean) {
                if (!hasFocus){
                    val str:String = pingTimeoutEditText.text.toString();
                    if (str.isNotEmpty()) {
                        launcherApplication.userConfig.PingTimeout = Integer.parseInt(str);
                        launcherApplication.userConfig.Save();
                    }else{
                        pingTimeoutEditText.setText(launcherApplication.userConfig.PingTimeout.toString());
                    }
                }
            }
        };

        return root
    }
}