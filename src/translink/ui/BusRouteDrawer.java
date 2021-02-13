package ca.ubc.cs.cpsc210.translink.ui;

import android.content.Context;
import android.graphics.Canvas;
import ca.ubc.cs.cpsc210.translink.BusesAreUs;
import ca.ubc.cs.cpsc210.translink.model.Route;
import ca.ubc.cs.cpsc210.translink.model.RouteManager;
import ca.ubc.cs.cpsc210.translink.model.RoutePattern;
import ca.ubc.cs.cpsc210.translink.model.StopManager;
import ca.ubc.cs.cpsc210.translink.util.Geometry;
import ca.ubc.cs.cpsc210.translink.util.LatLon;
import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.ResourceProxy;
import org.osmdroid.bonuspack.overlays.Polyline;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import java.lang.reflect.Array;
import java.util.*;

// A bus route drawer
public class BusRouteDrawer extends MapViewOverlay {
    /**
     * overlay used to display bus route legend text on a layer above the map
     */
    private BusRouteLegendOverlay busRouteLegendOverlay;
    /**
     * overlays used to plot bus routes
     */
    private List<Polyline> busRouteOverlays;
    private Geometry geometry = new Geometry();

    /**
     * Constructor
     *
     * @param context the application context
     * @param mapView the map view
     */
    public BusRouteDrawer(Context context, MapView mapView) {
        super(context, mapView);
        busRouteLegendOverlay = createBusRouteLegendOverlay();
        busRouteOverlays = new ArrayList<>();
    }

    /**
     * Plot each visible segment of each route pattern of each route going through the selected stop.
     */
    public void plotRoutes(int zoomLevel) {
        updateVisibleArea();
        busRouteOverlays.clear();
        busRouteLegendOverlay.clear();
        if (StopManager.getInstance().getSelected() != null) {
            for (Route r : StopManager.getInstance().getSelected().getRoutes()) {
                busRouteLegendOverlay.add(r.getNumber());
                for (RoutePattern rp: r.getPatterns()) {
                    for (int i = 0; i < rp.getPath().size() - 2; i++) {
                        LatLon l1 = rp.getPath().get(i);
                        LatLon l2 = rp.getPath().get(i + 1);
                        makePolyLineIfWithinScreen(l1, l2, r, zoomLevel);
                    }
                }
            }
        }
    }

    private void makePolyLineIfWithinScreen(LatLon l1, LatLon l2, Route r, int zoomLevel) {
        if (geometry.rectangleIntersectsLine(northWest, southEast, l1, l2)) {
            List<GeoPoint> points = new ArrayList<>();
            Polyline polyline = new Polyline(context);
            points.add(geometry.gpFromLatLon(l1));
            points.add(geometry.gpFromLatLon(l2));
            polyline.setWidth(getLineWidth(zoomLevel));
            polyline.setColor(busRouteLegendOverlay.getColor(r.getNumber()));
            polyline.setPoints(points);
            busRouteOverlays.add(polyline);
        }
    }

    public List<Polyline> getBusRouteOverlays() {
        return Collections.unmodifiableList(busRouteOverlays);
    }

    public BusRouteLegendOverlay getBusRouteLegendOverlay() {
        return busRouteLegendOverlay;
    }


    /**
     * Create text overlay to display bus route colours
     */
    private BusRouteLegendOverlay createBusRouteLegendOverlay() {
        ResourceProxy rp = new DefaultResourceProxyImpl(context);
        return new BusRouteLegendOverlay(rp, BusesAreUs.dpiFactor());
    }

    /**
     * Get width of line used to plot bus route based on zoom level
     *
     * @param zoomLevel the zoom level of the map
     * @return width of line used to plot bus route
     */
    private float getLineWidth(int zoomLevel) {
        if (zoomLevel > 14) {
            return 7.0f * BusesAreUs.dpiFactor();
        } else if (zoomLevel > 10) {
            return 5.0f * BusesAreUs.dpiFactor();
        } else {
            return 2.0f * BusesAreUs.dpiFactor();
        }
    }
}
