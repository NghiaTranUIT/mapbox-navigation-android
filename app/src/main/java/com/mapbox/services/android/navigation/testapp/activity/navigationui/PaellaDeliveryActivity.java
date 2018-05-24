package com.mapbox.services.android.navigation.testapp.activity.navigationui;

import android.location.Location;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.core.constants.Constants;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.exceptions.InvalidLatLngBoundsException;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.services.android.navigation.testapp.R;
import com.mapbox.services.android.navigation.ui.v5.NavigationView;
import com.mapbox.services.android.navigation.ui.v5.NavigationViewOptions;
import com.mapbox.services.android.navigation.ui.v5.OnNavigationReadyCallback;
import com.mapbox.services.android.navigation.ui.v5.instruction.InstructionView;
import com.mapbox.services.android.navigation.ui.v5.listeners.NavigationListener;
import com.mapbox.services.android.navigation.ui.v5.summary.SummaryBottomSheet;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;
import com.mapbox.services.android.navigation.v5.navigation.NavigationUnitType;
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.utils.LocaleUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_TYPE_ARRIVE;

public class PaellaDeliveryActivity extends AppCompatActivity implements OnNavigationReadyCallback, NavigationListener,
  Callback<DirectionsResponse>, ProgressChangeListener {

  private static final int CAMERA_ANIMATION_DURATION = 1000;
  private NavigationView navigationView;
  private SummaryBottomSheet summaryBottomSheet;
  private InstructionView instructionView;
  private ProgressBar loading;
  private TextView message;
  private FloatingActionButton launchNavigationFab;
  private Point origin = Point.fromLngLat(-122.423579, 37.761689);
  private Point pickup = Point.fromLngLat(-122.424467, 37.761027);
  private Point destination = Point.fromLngLat(-122.426183, 37.760872);
  private DirectionsRoute route;
  private boolean paellaPickedUp = false;
  private Marker destinationMarker;
  private MapboxMap map;
  private Marker paella;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    setTheme(R.style.Theme_AppCompat_NoActionBar);
    super.onCreate(savedInstanceState);
    initializeViews(savedInstanceState);
    navigationView.initialize(this);
    launchNavigationFab.setOnClickListener(v -> launchNavigation());
  }

  @Override
  public void onNavigationReady() {
    fetchRoute();
  }

  @Override
  public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
    if (validRouteResponse(response)) {
      updateLoadingTo(false);
      route = response.body().routes().get(0);
      drawRoute();
    }
  }

  @Override
  public void onFailure(Call<DirectionsResponse> call, Throwable throwable) {
  }

  @Override
  public void onProgressChange(Location location, RouteProgress routeProgress) {
    boolean isCurrentStepArrival = routeProgress.currentLegProgress().currentStep().maneuver().type()
      .contains(STEP_MANEUVER_TYPE_ARRIVE);

    if (isCurrentStepArrival && !paellaPickedUp) {
      updateUiDelivering();
    } else if (isCurrentStepArrival && paellaPickedUp) {
      updateUiDelivered();
    }
  }

  @Override
  public void onCancelNavigation() {
    updateUiNavigationFinished();
    navigationView.stopNavigation();
    boundCameraToRoute();
  }

  @Override
  public void onNavigationFinished() {
  }

  @Override
  public void onNavigationRunning() {
  }

  private void fetchRoute() {
    Locale locale = LocaleUtils.getDeviceLocale(this);
    NavigationRoute builder = NavigationRoute.builder()
      .accessToken(getString(R.string.mapbox_access_token))
      .origin(origin)
      .destination(destination)
      .addWaypoint(pickup)
      .alternatives(true)
      .language(locale)
      .voiceUnits(NavigationUnitType.getDirectionsCriteriaUnitType(NavigationUnitType.NONE_SPECIFIED, locale))
      .build();
    builder.getRoute(this);
    updateLoadingTo(true);
  }

  private void launchNavigation() {
    NavigationViewOptions.Builder options = NavigationViewOptions.builder()
      .navigationListener(this)
      .progressChangeListener(this)
      .directionsRoute(route)
      .shouldSimulateRoute(true);
    navigationView.startNavigation(options.build());
    updateUiPickingUp();
  }

  private void initializeViews(@Nullable Bundle savedInstanceState) {
    setContentView(R.layout.activity_paella_delivery);
    navigationView = findViewById(R.id.navigationView);
    summaryBottomSheet = findViewById(R.id.summaryBottomSheet);
    summaryBottomSheet.setVisibility(View.INVISIBLE);
    instructionView = findViewById(R.id.instructionView);
    instructionView.setVisibility(View.INVISIBLE);
    loading = findViewById(R.id.loading);
    message = findViewById(R.id.message);
    launchNavigationFab = findViewById(R.id.launchNavigation);
    navigationView.onCreate(savedInstanceState);
  }

  private void updateLoadingTo(boolean isVisible) {
    if (isVisible) {
      loading.setVisibility(View.VISIBLE);
    } else {
      loading.setVisibility(View.INVISIBLE);
    }
  }

  private boolean validRouteResponse(Response<DirectionsResponse> response) {
    return response.body() != null && !response.body().routes().isEmpty();
  }

  private void drawRoute() {
    if (route.distance() > 25d) {
      launchNavigationFab.setVisibility(View.VISIBLE);
      navigationView.drawRoute(route);
      drawMarkers();
      boundCameraToRoute();
    } else {
      Snackbar.make(navigationView, R.string.error_select_longer_route, Snackbar.LENGTH_SHORT).show();
    }
  }

  private void drawMarkers() {
    navigationView.addMarker(Point.fromLngLat(
      route.legs().get(0).steps().get(0).maneuver().location().longitude(),
      route.legs().get(0).steps().get(0).maneuver().location().latitude()
    ));
    map = navigationView.getMapboxMap();
    Icon paellaIcon = IconFactory.getInstance(this).fromResource(R.drawable.paella_icon);
    paella = map.addMarker(new MarkerOptions()
      .position(new LatLng(37.760615, -122.424306))
      .icon(paellaIcon)
    );
    navigationView.addMarker(Point.fromLngLat(
      route.legs().get(0).steps().get(route.legs().get(0).steps().size() - 1).maneuver().location().longitude(),
      route.legs().get(0).steps().get(route.legs().get(0).steps().size() - 1).maneuver().location().latitude()
    ));
    Icon marker = IconFactory.getInstance(this).fromResource(R.drawable.map_marker_light);
    destinationMarker = map.addMarker(new MarkerOptions()
      .position(
        new LatLng(
          route.legs().get(route.legs().size() - 1).steps().get(route.legs().get(route.legs().size() - 1).steps()
            .size() - 1).maneuver().location().latitude(),
          route.legs().get(route.legs().size() - 1).steps().get(route.legs().get(route.legs().size() - 1).steps()
            .size() - 1).maneuver().location().longitude()
        ))
      .icon(marker)
    );
  }

  private void boundCameraToRoute() {
    if (route != null) {
      List<Point> routeCoords = LineString.fromPolyline(route.geometry(),
        Constants.PRECISION_6).coordinates();
      List<LatLng> bboxPoints = new ArrayList<>();
      for (Point point : routeCoords) {
        bboxPoints.add(new LatLng(point.latitude(), point.longitude()));
      }
      if (bboxPoints.size() > 1) {
        try {
          LatLngBounds bounds = new LatLngBounds.Builder().includes(bboxPoints).build();
          animateCameraBbox(bounds, CAMERA_ANIMATION_DURATION, new int[] {50, 100, 50, 100});
        } catch (InvalidLatLngBoundsException exception) {
          Toast.makeText(this, R.string.error_valid_route_not_found, Toast.LENGTH_SHORT).show();
        }
      }
    }
  }

  private void animateCameraBbox(LatLngBounds bounds, int animationTime, int[] padding) {
    CameraPosition position = navigationView.getMapboxMap().getCameraForLatLngBounds(bounds, padding);
    navigationView.getMapboxMap().animateCamera(CameraUpdateFactory.newCameraPosition(position), animationTime);
  }

  private void updateUiPickingUp() {
    summaryBottomSheet.setVisibility(View.VISIBLE);
    instructionView.setVisibility(View.VISIBLE);
    message.setText("Picking the paella up...");
    message.setVisibility(View.VISIBLE);
    map.removeMarker(destinationMarker);
    launchNavigationFab.hide();
  }

  private void updateUiDelivering() {
    paellaPickedUp = true;
    map.removeMarker(paella);
    message.setText("Delivering...");
  }

  private void updateUiDelivered() {
    message.setText("Delicious paella delivered!");
  }

  private void updateUiNavigationFinished() {
    summaryBottomSheet.setVisibility(View.INVISIBLE);
    instructionView.setVisibility(View.INVISIBLE);
    message.setVisibility(View.GONE);
    launchNavigationFab.show();
  }

  @Override
  public void onStart() {
    super.onStart();
    navigationView.onStart();
  }

  @Override
  public void onResume() {
    super.onResume();
    navigationView.onResume();
  }

  @Override
  public void onLowMemory() {
    super.onLowMemory();
    navigationView.onLowMemory();
  }

  @Override
  public void onBackPressed() {
    // If the navigation view didn't need to do anything, call super
    if (!navigationView.onBackPressed()) {
      super.onBackPressed();
    }
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    navigationView.onSaveInstanceState(outState);
    super.onSaveInstanceState(outState);
  }

  @Override
  protected void onRestoreInstanceState(Bundle savedInstanceState) {
    super.onRestoreInstanceState(savedInstanceState);
    navigationView.onRestoreInstanceState(savedInstanceState);
  }

  @Override
  public void onPause() {
    super.onPause();
    navigationView.onPause();
  }

  @Override
  public void onStop() {
    super.onStop();
    navigationView.onStop();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    navigationView.onDestroy();
  }
}
