package com.example.lab5_starter;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements CityDialogFragment.CityDialogListener {

    private Button addCityButton;
    private ListView cityListView;

    private ArrayList<City> cityArrayList;
    private ArrayAdapter<City> cityArrayAdapter;

    private FirebaseFirestore db;
    private CollectionReference citiesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // views
        addCityButton = findViewById(R.id.buttonAddCity);
        cityListView = findViewById(R.id.listviewCities);

        // list + adapter
        cityArrayList = new ArrayList<>();
        cityArrayAdapter = new CityArrayAdapter(this, cityArrayList);
        cityListView.setAdapter(cityArrayAdapter);

        // firestore
        db = FirebaseFirestore.getInstance();
        citiesRef = db.collection("cities");

        // keep ListView in sync with Firestore (adds/updates/deletes persist)
        citiesRef.addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.e("Firestore", error.toString());
                return;
            }

            cityArrayList.clear();

            if (value != null) {
                for (QueryDocumentSnapshot snapshot : value) {
                    String name = snapshot.getString("name");
                    String province = snapshot.getString("province");

                    // guard against nulls
                    if (name != null && province != null) {
                        cityArrayList.add(new City(name, province));
                    }
                }
            }

            cityArrayAdapter.notifyDataSetChanged();
        });

        // add city
        addCityButton.setOnClickListener(view -> {
            CityDialogFragment cityDialogFragment = new CityDialogFragment();
            cityDialogFragment.show(getSupportFragmentManager(), "Add City");
        });

        // edit city (tap)
        cityListView.setOnItemClickListener((adapterView, view, i, l) -> {
            City city = cityArrayAdapter.getItem(i);
            CityDialogFragment cityDialogFragment = CityDialogFragment.newInstance(city);
            cityDialogFragment.show(getSupportFragmentManager(), "City Details");
        });

        // delete city (long press)
        cityListView.setOnItemLongClickListener((adapterView, view, i, l) -> {
            City city = cityArrayAdapter.getItem(i);
            if (city == null) return true;

            new AlertDialog.Builder(this)
                    .setTitle("Delete city?")
                    .setMessage("Delete " + city.getName() + " (" + city.getProvince() + ")?")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Delete", (dialog, which) -> deleteCity(city))
                    .show();

            return true;
        });
    }

    @Override
    public void updateCity(City city, String title, String year) {
        if (city == null) return;

        // keep the old doc id if the name changes (doc id is city name)
        String oldName = city.getName();

        city.setName(title);
        city.setProvince(year);
        cityArrayAdapter.notifyDataSetChanged();

        // if the name changed, delete the old doc first (otherwise you'd have duplicates)
        if (!oldName.equals(title)) {
            citiesRef.document(oldName).delete();
        }

        // upsert the updated city under its (possibly new) name
        citiesRef.document(city.getName()).set(city);
    }

    @Override
    public void addCity(City city) {
        if (city == null) return;

        // persist to Firestore.
        // the snapshot listener will update the ListView, so no need to manually add to the list.
        DocumentReference docRef = citiesRef.document(city.getName());
        docRef.set(city);
    }

    private void deleteCity(City city) {
        if (city == null) return;

        // persist the deletion. snapshot listener will refresh the ListView.
        citiesRef.document(city.getName()).delete();
    }
}
