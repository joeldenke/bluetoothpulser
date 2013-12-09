package KTH.joel.btpulser;

    import android.graphics.Color;
    import android.os.Bundle;
    import android.preference.PreferenceActivity;
    import android.preference.PreferenceFragment;
    import android.view.LayoutInflater;
    import android.view.View;
    import android.view.ViewGroup;
    import android.widget.*;

/**
 * @description Preference activity, handling preference changes by user
 * @author Joel Denke
 *
 */
public class SettingsActivity extends PreferenceActivity
{
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.preference);
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new MyPreferenceFragment())
                .commit();
    }

    public class MyPreferenceFragment extends PreferenceFragment
    {
        @Override
        public void onCreate(final Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.settings);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            LinearLayout v = (LinearLayout) super.onCreateView(inflater, container, savedInstanceState);

            Button btn = new Button(getActivity().getApplicationContext());
            btn.setTextColor(Color.BLACK);
            btn.setText("OK");

            v.addView(btn);
            btn.setOnClickListener(new OkListener());

            return v;
        }
    }

    private class OkListener implements View.OnClickListener {
        public void onClick(View v) {
            finish();
        }
    }
}
