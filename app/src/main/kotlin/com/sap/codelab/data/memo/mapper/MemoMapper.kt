package com.sap.codelab.data.memo.mapper

import com.sap.codelab.data.memo.MemoData
import com.sap.codelab.data.memo.local.MemoEntity

internal fun MemoEntity.toData() = MemoData(
    id = id,
    title = title,
    description = description,
    latitude = latitude,
    longitude = longitude,
    isDone = isDone,
    isReminded = isReminded,
)

internal fun MemoData.toEntity() = MemoEntity(
    id = id,
    title = title,
    description = description,
    latitude = latitude,
    longitude = longitude,
    isDone = isDone,
    isReminded = isReminded,
)
