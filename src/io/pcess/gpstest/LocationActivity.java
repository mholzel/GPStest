package io.pcess.gpstest;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class LocationActivity extends Activity {

    /** The WGS84 equatorial radius */
    public static final double  a  = 6378137;

    /** The WGS84 ellipsoid eccentricity */
    public static final double  e  = 8.1819190842622e-2;

    /** The square of the WGS84 ellipsoid eccentricity (e^2) */
    private static final double e2 = Math.pow(e, 2);

    /** Convert GPS latitude, longitude, altitude to ECEF coordinates. */
    public double[] toCartesian(Location location) {
        return toCartesian(location.getLatitude(), location.getLongitude(), location.getAltitude());
    }

    /** Convert GPS latitude, longitude, altitude to ECEF coordinates. */
    public double[] toCartesian(double latitude, double longitude, double altitude) {
        double lat = Math.toRadians(latitude);
        double lon = Math.toRadians(longitude);
        double alt = altitude;
        double N = a / Math.sqrt(1 - e2 * Math.pow(Math.sin(lat), 2));

        double x = (N + alt) * Math.cos(lat) * Math.cos(lon);
        double y = (N + alt) * Math.cos(lat) * Math.sin(lon);
        double z = ((1 - e2) * N + alt) * Math.sin(lat);
        return new double[] { x, y, z };
    }

    /** The {@link TextView} where we will show the GPS data. */
    private TextView textView = null;

    @TargetApi(23)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        textView = new TextView(this);
        textView.setText("Waiting for GPS updates...");
        setContentView(textView);
    }

    @Override
    protected void onResume() {

        super.onResume();

        /*
         * On a post-Android 6.0 devices, check if the required permissions have
         * been granted.
         */
        if (Build.VERSION.SDK_INT >= 23) {
            checkPermissions();
        } else {
            registerForGPS();
        }
    }

    public void registerForGPS() {
        /*
         * Get the location manager, and create the location listener that we
         * will register with the manager.
         */
        LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        LocationListener listener = new LocationListener() {

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            @Override
            public void onProviderEnabled(String provider) {
            }

            @Override
            public void onProviderDisabled(String provider) {
            }

            @Override
            public void onLocationChanged(Location location) {

                /*
                 * Convert the location from latitutde,longitude,altitude to
                 * ECEF Cartesian coordinates.
                 */
                final double[] r = toCartesian(location);

                /*
                 * We want to see how much precision we would lose if we save
                 * the ECEF coordinates as floats instead of doubles.
                 */
                float x = (float) r[0];
                float y = (float) r[1];
                float z = (float) r[2];

                String s = "";
                s += "x: " + r[0] + System.lineSeparator() + (r[0] - x) + System.lineSeparator();
                s += "y: " + r[1] + System.lineSeparator() + (r[1] - y) + System.lineSeparator();
                s += "z: " + r[2] + System.lineSeparator() + (r[2] - z) + System.lineSeparator();
                s += "accuracy  : " + location.getAccuracy() + System.lineSeparator();
                s += "provider  : " + location.getProvider() + System.lineSeparator()
                        + System.lineSeparator();
                for (String key : location.getExtras().keySet()) {
                    s += key + ":" + System.lineSeparator() + location.getExtras().get(key)
                            + System.lineSeparator() + System.lineSeparator();
                }
                textView.setText(s);
            }
        };

        /*
         * Request location updates from all available providers. You should try
         * turning the GPS on and off to see what happens...
         */
        for (String provider : manager.getAllProviders()) {
            manager.requestLocationUpdates(provider, 10, 0, listener);
        }
    }

    /**
     * -----------------------------------------------------------------------
     *
     * Code needed to make sure we have the proper permissions on post Android
     * 6.0 devices...
     *
     * -----------------------------------------------------------------------
     */
    /** The code used when requesting permissions */
    private static final int PERMISSIONS_REQUEST = 4646;

    /**
     * Check if the required permissions have been granted, and
     * {@link #startNextActivity()} if they have. Otherwise
     * {@link #requestPermissions(String[], int)}.
     */
    private void checkPermissions() {
        String[] ungrantedPermissions = requiredPermissionsStillNeeded();
        if (ungrantedPermissions.length != 0) {
            requestPermissions(ungrantedPermissions, PERMISSIONS_REQUEST);
        } else {
            registerForGPS();
        }
    }

    /**
     * Convert the array of required permissions to a {@link Set} to remove
     * redundant elements. Then remove already granted permissions, and return
     * an array of ungranted permissions.
     */
    @TargetApi(23)
    private String[] requiredPermissionsStillNeeded() {

        Set<String> permissions = new HashSet<String>();
        for (String permission : getRequiredPermissions()) {
            permissions.add(permission);
        }
        for (Iterator<String> i = permissions.iterator(); i.hasNext();) {
            String permission = i.next();
            if (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
                Log.d(LocationActivity.class.getSimpleName(),
                        "Permission: " + permission + " already granted.");
                i.remove();
            } else {
                Log.d(LocationActivity.class.getSimpleName(),
                        "Permission: " + permission + " not yet granted.");
            }
        }
        return permissions.toArray(new String[permissions.size()]);
    }

    /**
     * Get the list of required permissions by searching the manifest. If you
     * don't think the default behavior is working, then you could try
     * overriding this function to return something like:
     *
     * <pre>
     * <code>
     * return new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
     * </code>
     * </pre>
     */
    public String[] getRequiredPermissions() {
        String[] permissions = null;
        try {
            permissions = getPackageManager().getPackageInfo(getPackageName(),
                    PackageManager.GET_PERMISSIONS).requestedPermissions;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        return permissions.clone();
    }

    /**
     * See if we now have all of the required dangerous permissions. Otherwise,
     * tell the user that they cannot continue without granting the permissions,
     * and then request the permissions again.
     */
    @TargetApi(23)
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[],
            int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST) {
            checkPermissions();
        }
    }

}