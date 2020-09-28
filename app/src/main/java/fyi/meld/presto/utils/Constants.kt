package fyi.meld.presto.utils

import fyi.meld.presto.R

object Constants
{
    val TAG = "Presto"
    val PERMISSIONS_REQUEST_CODE = 1001
    val CAMERA_PREVIEW_FADE_DURATION : Long = 1250
    val US_CURRENCY_REGEX = "^[+-]?[0-9]{1,3}(?:,?[0-9]{3})*\\.[0-9]{2}\$"
    val ItemTypeToDrawable = mapOf(
        ItemType.Fun to R.drawable.ic_fa_theater_masks,
        ItemType.Groceries to R.drawable.ic_fa_bread_slice,
        ItemType.Personal to R.drawable.ic_fa_beauty_salon,
        ItemType.Other to R.drawable.ic_fa_box)
    val DIGIT_REGEX = "@\"^\\d\$\"".toRegex()
}

enum class ItemType
{
    Groceries,
    Personal,
    Fun,
    Other
}