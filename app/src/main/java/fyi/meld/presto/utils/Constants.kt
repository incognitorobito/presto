package fyi.meld.presto.utils

import fyi.meld.presto.R

object Constants
{
    val TAG = "Presto"
    val PERMISSIONS_REQUEST_CODE = 1001
    val CAMERA_PREVIEW_FADE_DURATION : Long = 1250
    val NEW_ITEM_REQUEST_CODE = 9009
    val ItemTypeToDrawable = mapOf(
        ItemType.Fun to R.drawable.ic_fa_theater_masks,
        ItemType.Groceries to R.drawable.ic_fa_bread_slice,
        ItemType.Personal to R.drawable.ic_fa_beauty_salon,
        ItemType.Other to R.drawable.ic_fa_box)
}

enum class ItemType
{
    Groceries,
    Personal,
    Fun,
    Other
}