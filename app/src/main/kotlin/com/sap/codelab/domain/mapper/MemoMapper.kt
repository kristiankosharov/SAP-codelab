package com.sap.codelab.domain.mapper

import com.sap.codelab.data.location.GeoPointData
import com.sap.codelab.data.memo.MemoData
import com.sap.codelab.data.permission.PermissionsData
import com.sap.codelab.domain.model.GeoPoint
import com.sap.codelab.domain.model.Memo
import com.sap.codelab.domain.model.PermissionStatus

internal fun MemoData.toDomain() = Memo(
    id = id,
    title = title,
    description = description,
    location = GeoPoint(latitude, longitude),
    isDone = isDone,
    isReminded = isReminded,
)

internal fun GeoPointData.toDomain() = GeoPoint(latitude, longitude)

internal fun GeoPoint.toData() = GeoPointData(latitude, longitude)

internal fun PermissionsData.toDomain() = PermissionStatus(
    hasPreciseLocation = fineLocation,
    hasAnyLocation = fineLocation || coarseLocation,
    hasBackgroundLocation = backgroundLocation,
    hasNotifications = notifications,
)
