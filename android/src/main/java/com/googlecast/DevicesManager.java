package com.googlecast;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class DevicesManager {

    private static final String TAG = DevicesManager.class.getSimpleName();

    private long countScans = 0;

    private MediaRouter mediaRouter;
    private MediaRouteSelector mediaRouteSelector;
    private OnChangeRoutes onChangeRoutes;
    private MediaRouter.Callback callback = new MediaRouter.Callback() {
        @Override
        public void onRouteAdded(MediaRouter router, MediaRouter.RouteInfo route) {
            super.onRouteAdded(router, route);
            refreshRoute(router, route);
        }

        @Override
        public void onRouteRemoved(MediaRouter router, MediaRouter.RouteInfo route) {
            super.onRouteRemoved(router, route);
            refreshRoute(router, route);
        }

        @Override
        public void onRouteChanged(MediaRouter router, MediaRouter.RouteInfo route) {
            super.onRouteChanged(router, route);
            refreshRoute(router, route);
        }
    };

    public DevicesManager(Context context, MediaRouteSelector mediaRouteSelector, OnChangeRoutes onChangeRoutes) {
        this.mediaRouteSelector = mediaRouteSelector;
        this.onChangeRoutes = onChangeRoutes;

        mediaRouter = MediaRouter.getInstance(context);
    }

    public void startScan() {
        if (countScans == 0) {
            mediaRouter.addCallback(mediaRouteSelector, callback,
                    MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
        }
        countScans++;
        if (mediaRouter.isRouteAvailable(mediaRouteSelector,
                MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN)) {
            onChangeRoutes.onChangeRoutes(getRouters());
        }
        Log.d(TAG, "startScan: " + countScans);
    }

    public void stopScan() {
        if (countScans == 1) {
            mediaRouter.removeCallback(callback);
        }
        Log.d(TAG, "stopScan: " + (countScans - 1));
        if (countScans == 0) return;
        countScans--;
    }

    public List<MediaRouter.RouteInfo> getRouters() {
        List<MediaRouter.RouteInfo> routes = new ArrayList<>(mediaRouter.getRoutes());
        onFilterRoutes(routes);
        Collections.sort(routes, RouteComparator.sInstance);
        return routes;
    }

    private void refreshRoute(MediaRouter router, MediaRouter.RouteInfo routeInfo) {
        if (onFilterRoute(routeInfo)) {
            onChangeRoutes.onChangeRoutes(getRouters());
        }
    }

    private void onFilterRoutes(@NonNull List<MediaRouter.RouteInfo> routes) {
        for (int i = routes.size(); i-- > 0; ) {
            if (!onFilterRoute(routes.get(i))) {
                routes.remove(i);
            }
        }
    }

    private boolean onFilterRoute(@NonNull MediaRouter.RouteInfo route) {
        return !route.isDefaultOrBluetooth() && route.isEnabled()
                && route.matchesSelector(mediaRouteSelector);
    }

    public interface OnChangeRoutes {
        void onChangeRoutes(List<MediaRouter.RouteInfo> routes);
    }

    private static final class RouteComparator implements Comparator<MediaRouter.RouteInfo> {
        private static final RouteComparator sInstance = new RouteComparator();

        @Override
        public int compare(MediaRouter.RouteInfo lhs, MediaRouter.RouteInfo rhs) {
            return lhs.getName().compareToIgnoreCase(rhs.getName());
        }
    }
}
