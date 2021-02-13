package ca.ubc.cs.cpsc210.translink.ui;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import ca.ubc.cs.cpsc210.translink.BusesAreUs;
import ca.ubc.cs.cpsc210.translink.R;
import ca.ubc.cs.cpsc210.translink.model.Route;
import ca.ubc.cs.cpsc210.translink.model.Stop;
import ca.ubc.cs.cpsc210.translink.model.StopManager;
import ca.ubc.cs.cpsc210.translink.util.Geometry;
import ca.ubc.cs.cpsc210.translink.util.LatLon;
import org.osmdroid.bonuspack.clustering.RadiusMarkerClusterer;
import org.osmdroid.bonuspack.overlays.Marker;
import org.osmdroid.views.MapView;

import java.util.HashMap;
import java.util.Map;

// A plotter for bus stop locations
public class BusStopPlotter extends MapViewOverlay {
    /**
     * clusterer
     */
    private RadiusMarkerClusterer stopClusterer;
    /**
     * maps each stop to corresponding marker on map
     */
    private Map<Stop, Marker> stopMarkerMap = new HashMap<>();
    /**
     * marker for stop that is nearest to user (null if no such stop)
     */
    private Marker nearestStnMarker;
    private Activity activity;
    private StopInfoWindow stopInfoWindow;
    private Geometry geometry = new Geometry();

    /**
     * Constructor
     *
     * @param activity the application context
     * @param mapView  the map view on which buses are to be plotted
     */
    public BusStopPlotter(Activity activity, MapView mapView) {
        super(activity.getApplicationContext(), mapView);
        this.activity = activity;
        nearestStnMarker = new Marker(mapView);
        stopInfoWindow = new StopInfoWindow((StopSelectionListener) activity, mapView);
        newStopClusterer();
    }

    public RadiusMarkerClusterer getStopClusterer() {
        return stopClusterer;
    }

    /**
     * Mark all visible stops in stop manager onto map.
     */
    public void markStops(Location currentLocation) {
        Drawable stopIconDrawable = activity.getResources().getDrawable(R.drawable.stop_icon);
        update();
        for (Stop s: StopManager.getInstance()) {
            if (geometry.rectangleContainsPoint(northWest, southEast, s.getLocn())) {
                Marker marker = new Marker(mapView);
                addRoutesToMarkerTitle(s, marker);
                marker.setIcon(stopIconDrawable);
                marker.setInfoWindow(stopInfoWindow);
                marker.setPosition(geometry.gpFromLatLon(s.getLocn()));
                marker.setRelatedObject(s);
                setMarker(s, marker);
                stopClusterer.add(marker);
            }
        }
        useCurrentLocation(currentLocation);
    }

    private void update() {
        updateVisibleArea();
        newStopClusterer();
    }

    private void useCurrentLocation(Location currentLocation) {
        if (currentLocation != null) {
            LatLon location = new LatLon(currentLocation.getLatitude(), currentLocation.getLongitude());
            Stop nearestStop = StopManager.getInstance().findNearestTo(location);
            updateMarkerOfNearest(nearestStop);
        }
    }

    private void addRoutesToMarkerTitle(Stop s, Marker marker) {
        marker.setTitle(s.getNumber() + " " + s.getName());
        for (Route r: s.getRoutes()) {
            marker.setTitle(marker.getTitle() + "\n" + r.getNumber());
        }
    }

    /**
     * Create a new stop cluster object used to group stops that are close by to reduce screen clutter
     */
    private void newStopClusterer() {
        stopClusterer = new RadiusMarkerClusterer(activity);
        stopClusterer.getTextPaint().setTextSize(20.0F * BusesAreUs.dpiFactor());
        int zoom = mapView == null ? 16 : mapView.getZoomLevel();
        if (zoom == 0) {
            zoom = MapDisplayFragment.DEFAULT_ZOOM;
        }
        int radius = 1000 / zoom;

        stopClusterer.setRadius(radius);
        Drawable clusterIconD = activity.getResources().getDrawable(R.drawable.stop_cluster);
        Bitmap clusterIcon = ((BitmapDrawable) clusterIconD).getBitmap();
        stopClusterer.setIcon(clusterIcon);
    }

    /**
     * Update marker of nearest stop (called when user's location has changed).  If nearest is null,
     * no stop is marked as the nearest stop.
     *
     * @param nearest stop nearest to user's location (null if no stop within StopManager.RADIUS metres)
     */
    public void updateMarkerOfNearest(Stop nearest) {
        Drawable stopIconDrawable = activity.getResources().getDrawable(R.drawable.stop_icon);
        Drawable closestStopIconDrawable = activity.getResources().getDrawable(R.drawable.closest_stop_icon);
        nearestStnMarker.setIcon(stopIconDrawable);
        if (nearest != null) {
            nearestStnMarker.setIcon(closestStopIconDrawable);
            nearestStnMarker.setRelatedObject(nearest);
            nearestStnMarker.setPosition(geometry.gpFromLatLon(nearest.getLocn()));
            addRoutesToNearestMarkerTitle(nearest);
            nearestStnMarker.setInfoWindow(stopInfoWindow);
            stopClusterer.add(nearestStnMarker);
            setMarker(nearest, nearestStnMarker);
        }
    }

    private void addRoutesToNearestMarkerTitle(Stop nearest) {
        nearestStnMarker.setTitle(nearest.getNumber() + " " + nearest.getName());
        for (Route r : nearest.getRoutes()) {
            nearestStnMarker.setTitle(nearestStnMarker.getTitle() + "\n" + r.getNumber());
        }
    }

    /**
     * Manage mapping from stops to markers using a map from stops to markers.
     * The mapping in the other direction is done using the Marker.setRelatedObject() and
     * Marker.getRelatedObject() methods.
     */
    private Marker getMarker(Stop stop) {
        return stopMarkerMap.get(stop);
    }

    private void setMarker(Stop stop, Marker marker) {
        stopMarkerMap.put(stop, marker);
    }

    private void clearMarker(Stop stop) {
        stopMarkerMap.remove(stop);
    }

    private void clearMarkers() {
        stopMarkerMap.clear();
    }
}
