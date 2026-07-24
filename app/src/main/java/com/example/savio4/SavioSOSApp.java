package com.example.savio4;

import android.app.Application;
import org.osmdroid.config.Configuration;

public class SavioSOSApp extends Application {

    // ISPRAVKA (jul 2026): OSM tile serveri blokiraju aplikacije koje se
    // predstavljaju kao "com.example.*" (izmena Tile Usage Policy).
    // Zato koristimo jedinstven identifikator aplikacije, na jednom mestu,
    // koji sve aktivnosti sa mapom moraju da koriste.
    public static final String OSM_USER_AGENT =
            "SAVIO-SOS/1.5 (github.com/Gagi78-creator/SAVIO-SOS)";

    @Override
    public void onCreate() {
        super.onCreate();

        // OBAVEZNO: Inicijalizacija OSM konfiguracije pre bilo čega drugog
        Configuration.getInstance().load(
                getApplicationContext(),
                getSharedPreferences("osmdroid", MODE_PRIVATE)
        );

        // KLJUČNO: UserAgent se podešava POSLE load() — load() ga uvek pregazi
        // vrednošću iz memorije ili imenom paketa (com.example.savio4 = blokiran).
        Configuration.getInstance().setUserAgentValue(OSM_USER_AGENT);

        // Snimi da i sačuvana konfiguracija ima ispravan UserAgent
        Configuration.getInstance().save(
                getApplicationContext(),
                getSharedPreferences("osmdroid", MODE_PRIVATE)
        );

        // Postavi putanju za offline mape (keš tile-ova)
        Configuration.getInstance().setOsmdroidBasePath(getFilesDir());
        Configuration.getInstance().setOsmdroidTileCache(getCacheDir());
    }
}