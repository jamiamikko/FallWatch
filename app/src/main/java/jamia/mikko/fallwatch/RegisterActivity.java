package jamia.mikko.fallwatch;

import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import jamia.mikko.fallwatch.SidebarFragments.RegisterFragment;

public class RegisterActivity extends AppCompatActivity {

    public static final String USER_PREFERENCES = "UserPreferences";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        RegisterFragment fragment = new RegisterFragment();

        toRegisterFragment(fragment);
    }

    public void toRegisterFragment(RegisterFragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    public void toContactsFragment(ReadContactsFragment fragment) {

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();

    }

    public void saveToPreferences(String key, String value) {
        SharedPreferences prefs = getSharedPreferences(USER_PREFERENCES, MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = prefs.edit();

        prefsEditor.putString(key, value);

        prefsEditor.commit();
    }
}
