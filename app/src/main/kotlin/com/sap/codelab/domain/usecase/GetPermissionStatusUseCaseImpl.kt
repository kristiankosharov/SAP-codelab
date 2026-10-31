package com.sap.codelab.domain.usecase

import com.sap.codelab.data.permission.PermissionManager
import com.sap.codelab.domain.mapper.toDomain
import com.sap.codelab.domain.model.PermissionStatus
import javax.inject.Inject

internal class GetPermissionStatusUseCaseImpl @Inject constructor(
    private val permissionManager: PermissionManager,
) : GetPermissionStatusUseCase {

    override fun invoke(): PermissionStatus = permissionManager.currentPermissions().toDomain()
}
