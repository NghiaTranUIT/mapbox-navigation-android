package com.mapbox.services.android.navigation.ui.v5;

import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class NavigationPresenterTest {

  @Test
  public void onRouteOverviewButtonClick_cameraIsAdjustedToRoute() {
    NavigationContract.View view = mock(NavigationContract.View.class);
    NavigationPresenter presenter = new NavigationPresenter(view);

    presenter.onRouteOverviewClick();

    verify(view).updateCameraRouteOverview();
  }

  @Test
  public void onRouteOverviewButtonClick_recenterBtnIsShown() {
    NavigationContract.View view = mock(NavigationContract.View.class);
    NavigationPresenter presenter = new NavigationPresenter(view);

    presenter.onRouteOverviewClick();

    verify(view).showRecenterBtn();
  }

  @Test
  public void onRecenterBtnClick_recenterBtnIsHidden() {
    NavigationContract.View view = mock(NavigationContract.View.class);
    NavigationPresenter presenter = new NavigationPresenter(view);

    presenter.onRecenterClick();

    verify(view).hideRecenterBtn();
  }

  @Test
  public void onRecenterBtnClick_cameraIsResetToTracking() {
    NavigationContract.View view = mock(NavigationContract.View.class);
    NavigationPresenter presenter = new NavigationPresenter(view);

    presenter.onRecenterClick();

    verify(view).resetCameraPosition();
  }
}
