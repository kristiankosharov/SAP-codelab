package com.sap.codelab.ui.components.map

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.GoogleMapComposable
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.sap.codelab.R
import com.sap.codelab.domain.model.GeoPoint
import com.sap.codelab.domain.model.REMINDER_RADIUS_METERS

// The only file that knows the map library (Google Maps Compose). Screens use these composables
// with domain GeoPoints, so another provider (e.g. OpenStreetMap) replaces only this file.

private const val STREET_ZOOM = 16f
private const val WORLD_ZOOM = 2f
private const val CIRCLE_FILL_ALPHA = 0.2f

/**
 * A map to pick a memo's location: a tap places the marker and the reminder radius around it.
 * The camera moves to [initialCenter] once it is known, unless a point is already picked.
 */
@Composable
fun LocationPickerMap(
    selected: GeoPoint?,
    initialCenter: GeoPoint?,
    showMyLocation: Boolean,
    onLocationPicked: (GeoPoint) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cameraPositionState = rememberCameraPositionState {
        position = selected?.let { CameraPosition.fromLatLngZoom(it.toLatLng(), STREET_ZOOM) }
            ?: CameraPosition.fromLatLngZoom(LatLng(0.0, 0.0), WORLD_ZOOM)
    }
    LaunchedEffect(initialCenter) { // visual only: follow the first location fix
        if (initialCenter != null && selected == null) {
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(initialCenter.toLatLng(), STREET_ZOOM))
        }
    }
    val areaColor = MaterialTheme.colorScheme.primary
    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        contentDescription = stringResource(R.string.map_picker_description),
        properties = MapProperties(isMyLocationEnabled = showMyLocation),
        uiSettings = MapUiSettings(myLocationButtonEnabled = showMyLocation, zoomControlsEnabled = false),
        onMapClick = { latLng -> onLocationPicked(GeoPoint(latLng.latitude, latLng.longitude)) },
    ) {
        selected?.let { point -> ReminderArea(point, areaColor) }
    }
}

/** A small, non-interactive map showing a memo's location and reminder radius. */
@Composable
fun LocationPreviewMap(
    point: GeoPoint,
    modifier: Modifier = Modifier,
) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(point.toLatLng(), STREET_ZOOM)
    }
    val areaColor = MaterialTheme.colorScheme.primary
    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        contentDescription = stringResource(R.string.map_preview_description),
        googleMapOptionsFactory = { GoogleMapOptions().liteMode(true) },
        uiSettings = MapUiSettings(
            zoomControlsEnabled = false,
            scrollGesturesEnabled = false,
            zoomGesturesEnabled = false,
            rotationGesturesEnabled = false,
            tiltGesturesEnabled = false,
            mapToolbarEnabled = false,
        ),
    ) {
        ReminderArea(point, areaColor)
    }
}

/** The memo's marker and the reminder radius around it. */
@Composable
@GoogleMapComposable
private fun ReminderArea(
    point: GeoPoint,
    color: Color,
) {
    val position = point.toLatLng()
    val markerState = remember(position) { MarkerState(position = position) }
    Marker(state = markerState)
    Circle(
        center = position,
        radius = REMINDER_RADIUS_METERS.toDouble(),
        strokeColor = color,
        strokeWidth = 2f,
        fillColor = color.copy(alpha = CIRCLE_FILL_ALPHA),
    )
}

private fun GeoPoint.toLatLng() = LatLng(latitude, longitude)
