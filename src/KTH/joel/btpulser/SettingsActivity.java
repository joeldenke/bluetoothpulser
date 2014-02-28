package KTH.joel.btpulser;

    import android.annotation.SuppressLint;
    import android.graphics.Color;
    import android.os.Build;
    import android.os.Bundle;
    import android.preference.PreferenceActivity;
    import android.preference.PreferenceFragment;
    import android.view.LayoutInflater;
    import android.view.View;
    import android.view.ViewGroup;
    import android.widget.*;

/**
 * @description Preference activity, handling preference changes by user
 * @author Joel Denke, Mathias Westman
 *
 */
public class SettingsActivity extends PreferenceActivity
{
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        // Ugly hack to make sure this works on old Android OS
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            onCreatePreferenceActivity();
        } else {
            onCreatePreferenceFragment();
        }
    }

    /**
     * Wraps legacy {@link #onCreate(Bundle)} code for Android < 3 (i.e. API lvl
     * < 11).
     */
    @SuppressWarnings("deprecation")
    private void onCreatePreferenceActivity()
    {
        addPreferencesFromResource(R.xml.settings);
    }

    /**
     * Wraps {@link #onCreate(Bundle)} code for Android >= 3 (i.e. API lvl >=
     * 11).
     */
    @SuppressLint("NewApi")
    private void onCreatePreferenceFragment()
    {
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new MyPreferenceFragment ())
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

        // This will add a OK button in the bottom of the settings activity
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
